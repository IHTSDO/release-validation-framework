#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Byte-equality gate for publish-time transpilation.

precompile()/bind() only earns its place if it is INDISTINGUISHABLE from
prepare(): for every assertion, every run configuration, and every statement,

    bind(precompile(sql, known_tables), **kw)  ==  prepare(sql, "duckdb", known_tables=..., **kw)

character for character. That is the whole point of the test. A sentinel that
round-trips through sqlglot even slightly differently - an identifier that gets
quoted, a numeral that changes an AST node type and trips _fix_loose_group_by,
a placeholder that suppresses one of to_duckdb()'s textual pre-passes - shows up
here as a diff on ~200 files. The alternative is discovering it as a wrong
validation result months later, which is exactly how the truncation and
double-escape defects were found.

Two corpora are checked:

  * the real one (every *.sql under --assertions, plus any --prerequisites);
    and
  * synthetic probes, for placeholders and positions a corpus may not
    exercise: <PREVIOUS>, <DEPENDENCY>, <TEMP>, <DELTA>, <MODULEID(S)>,
    <VERSION>, <INCLUDED_MODULES>, <RUNID> unquoted in a numeric slot, a GROUP
    BY that could tempt ANY_VALUE-wrapping, a three-part schema.alias.column.
    They get covered explicitly rather than declared safe by absence of
    evidence.

Usage:  precompile_parity.py --assertions <corpus>/scripts [--ddl FILE] [--prerequisites DIR]
Exit status is 1 if anything differs, so it can gate a build.
"""
import argparse
import difflib
import hashlib
import pathlib
import re
import sys
import time

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import pins
pins.require()

import sqlglot

import rf2map
import rvfsql

HERE = pathlib.Path(__file__).resolve().parent
# Where this sits in RVF: tools/duck-publisher/.
DEFAULT_DDL = HERE.parent.parent / "src" / "main" / "resources" / "sql" / "create-tables-mysql.sql"


def known_tables_from_schema(ddl_path, prereq_dir):
    """The table names precompile() may treat as real tables.

    They are not a run input: they are the fixed RF2 schema
    (create-tables-mysql.sql) plus any tables a pre-requisites script builds.
    Both are on disk at publish time, which is what makes
    strip_schema_qualified_aliases legal in precompile().
    """
    names = set(rf2map.mysql_ddl(ddl_path))
    for f in sorted(pathlib.Path(prereq_dir).glob("*.sql")) if prereq_dir else []:
        for m in re.finditer(r"create\s+table\s+(?:if\s+not\s+exists\s+)?(\w+)",
                             f.read_text(errors="replace"), re.I):
            names.add(m.group(1).lower())
    names.add("qa_result")
    return names


def corpus(assertions_dir, prereq_dir):
    # rglob: the assertions repository keeps scripts in per-category subdirs.
    files = sorted(pathlib.Path(assertions_dir).rglob("*.sql"))
    if prereq_dir:
        files += sorted(pathlib.Path(prereq_dir).glob("*.sql"))
    out = []
    for f in files:
        # Any stable uuid-shaped id will do; only stability matters here.
        h = hashlib.sha256(f.name.encode()).hexdigest()
        uuid = f"{h[0:8]}-{h[8:12]}-{h[12:16]}-{h[16:20]}-{h[20:32]}"
        out.append((f.name, f.read_text(errors="replace"), uuid))
    return out


# Positions and placeholders the real corpus never exercises. Each one is SQL
# that must transpile identically whether the placeholder is bound before or
# after sqlglot sees it.
#
# These probes were verified to actually bite, by mutating the sentinels and
# checking the test goes red - a probe that cannot fail is decoration:
#   * <RUNID>'s numeral -> an identifier: runid-numeric-with-group-by becomes
#     `SELECT ANY_VALUE(1) AS 1, moduleid, COUNT(*) ...` (invalid SQL), flagged
#     unexplained on all 6 configs. This is the measured justification for the
#     numeral sentinel, not a theory about it.
#   * dropping the "_" terminator from <MODULEID>'s sentinel: module-filters
#     binds <MODULEIDS> through <MODULEID>'s value, giving `moduleid IN ()`.
#   * a sentinel that is a reserved word ("select"): caught even earlier, by
#     precompile()'s collision guard, on the first script that contains it.
SYNTHETIC = [
    ("runid-numeric-position",
     "insert into qa_result (run_id, assertion_id) "
     "select <RUNID>, '<ASSERTIONUUID>' from curr_concept_s"),
    ("runid-numeric-with-group-by",
     # The reason <RUNID>'s sentinel is a numeral: as an identifier it would be
     # an ungrouped column and _fix_loose_group_by would wrap it in ANY_VALUE.
     "select <RUNID>, moduleid, count(*) from curr_concept_s group by moduleid"),
     ("runid-in-limit",
     "select id from curr_concept_s order by id limit <RUNID>"),
    ("previous-and-dependency",
     "select p.id from prev_concept_s p join dependency_concept_s d on d.id = p.id "
     "where p.effectivetime > d.effectivetime"),
    ("temp-table",
     "insert into v_scratch select id from curr_description_d where active = '1'"),
    ("delta-and-full",
     "select d.id from curr_concept_d d join curr_concept_f f on f.id = d.id"),
    ("module-filters",
     "select id from curr_concept_s where moduleid = '<MODULEID>' "
     "and moduleid in (<MODULEIDS>) and effectivetime = '<VERSION>'"),
    ("included-modules",
     "select id from curr_concept_s where 'NULL' = '<INCLUDED_MODULES>' "
     "or moduleid in (<INCLUDED_MODULES>)"),
    ("schema-qualified-alias",
     # curr_concept_s -> <PROSPECTIVE>.concept_s, a REAL table, so the three-part
     # reference must survive; the alias form must be stripped.
     "select <PROSPECTIVE>.concept_s.id, <PROSPECTIVE>.la.term "
     "from curr_concept_s, curr_description_s la"),
    ("cast-datetime-on-placeholder",
     # to_duckdb's _CAST_DT_RE only matches when the operand is [\\w.`\"]+, which
     # a sentinel satisfies and a raw <PROSPECTIVE> does not.
     "select max(cast(<PROSPECTIVE>.concept_s.effectivetime as datetime)) "
     "from curr_concept_s"),
    ("regexp-and-word-boundary",
     r"select id from curr_description_s where term regexp '[[:<:]]foo\Z' "
     r"and term not regexp binary '\\(x\\)$'"),
    ("qa-result-qualified",
     "insert into qa_result (run_id, assertion_id, details) "
     "select '<RUNID>', '<ASSERTIONUUID>', qa_result.details from qa_result"),
    ("delimiter-routine",
     "DELIMITER //\ncreate procedure p() begin select '<RUNID>'; end //\n"
     "DELIMITER ;"),
    ("unparseable-fallback",
     # duckdb_statement cannot parse this; both paths must fall back to the same
     # text, with bind() still substituting into the fallback.
     "this is not sql at all <PROSPECTIVE> '<RUNID>' <<>>"),
]


# Run configurations. The first four are shapes RVF's DuckDB engine produces
# (release schemas attached as literally prospective/previous/dependency, any of
# previous/dependency possibly absent); the rest vary every input that
# transform() branches on. The last two are deliberately NOT production shaped -
# they are what expose divergence classes B and C below.
PRODUCTION_SCHEMAS = ("prospective", "previous", "dependency")


def configs(known_tables):
    """(label, production_shaped, kwargs). production_shaped means RVF can
    actually produce this configuration; over the real corpus those may differ
    only in statements RVF skips (class S)."""
    return [
        ("prospective only (no previous release)", True,
         dict(known_tables=known_tables, run_id=1, prospective="prospective",
              previous=None, qa_result="rvf_results.qa_result")),
        ("with previous release", True,
         dict(known_tables=known_tables, run_id=1, prospective="prospective",
              previous="previous", qa_result="rvf_results.qa_result")),
        ("with previous + dependency", True,
         dict(known_tables=known_tables, run_id=7, prospective="prospective",
              previous="previous", dependency="dependency",
              qa_result="rvf_results.qa_result")),
        ("no known_tables (alias stripping off)", True,
         dict(known_tables=None, run_id=1, prospective="prospective",
              previous="previous", qa_result="rvf_results.qa_result")),
        ("non-production schema names", False,
         dict(known_tables=known_tables, run_id=7, prospective="p_1",
              previous="v_1", dependency="d_1", qa_result="rvf.qa_result")),
        ("module/version fragments, non-canonical spacing", False,
         dict(known_tables=known_tables, run_id=123456789,
              prospective="prospective", previous="previous",
              dependency="dependency", qa_result="rvf_results.qa_result",
              module_id="900000000000207008",
              module_ids="'900000000000207008','900000000000012004'",
              version="20240301")),
    ]


# ---------------------------------------------------------------------------
# The three ways the two paths CAN differ. Each is a normalisation that must
# make the two outputs equal - not a whitelist of file names, so a new defect in
# the same file still fails. Anything no classifier explains is a hard failure.
#
# On a production configuration the corpus is identical statement for statement
# apart from class S, statements no engine executes. Written down rather than
# papered over, because the reasons are the interesting part.

UNBOUND_RELEASES = ("<PREVIOUS>", "<DEPENDENCY>")


def _class_skipped_unbound_release(cfg, expected, actual):
    """S: the differing statements still name a release the run does not hold.

    transform() and bind() both leave <PREVIOUS>/<DEPENDENCY> as literal text
    when no such release is configured. prepare() then hands sqlglot
    `<PREVIOUS>.concept_s`, the parse fails on the `<`, and the statement comes
    back untranspiled; bind() puts the same literal into already-transpiled SQL.
    The texts differ, but neither runs: RVF's MySqlQueryTransformer drops any
    statement still naming a release it does not have, and the DuckDB engine's
    binder does the same, per statement. Accepted on a production configuration
    only when EVERY differing statement carries the placeholder on both sides.
    """
    if len(expected) != len(actual):
        return False
    differing = [(e, a) for e, a in zip(expected, actual) if e != a]
    return bool(differing) and all(
        any(p in e for p in UNBOUND_RELEASES) and any(p in a for p in UNBOUND_RELEASES)
        for e, a in differing)


def _parses_as_mysql(stmt):
    try:
        return bool([t for t in sqlglot.parse(stmt, read="mysql") if t])
    except Exception:
        return False


def _class_prepare_fell_back(cfg, expected, actual):
    """A: the BOUND text does not parse, so prepare() never transpiled it.

    duckdb_statement returns the statement unchanged when sqlglot cannot parse
    it. That decision is made on whatever text the stage is handed, and the two
    paths hand it different text - prepare() the bound SQL, precompile() the
    sentinel SQL - so where one is parseable and the other is not, the outputs
    differ by an entire transpilation. Detected structurally (prepare's differing
    statement does not parse), not by file name.

    Reached by a fragment placeholder whose value makes the SQL unparseable, e.g.
    a module LIST substituted into the quoted `'NULL' = '<INCLUDED_MODULES>'`
    comparison, which yields nested quotes - SQL that is broken whichever engine
    sees it. (The other way to reach it, an unbound release, is class S.)

    Making these byte-identical would mean publishing a second, untranspiled copy
    of every statement carrying a fragment placeholder purely to reproduce a
    parse failure on SQL that cannot execute either way. Rejected: it doubles the
    published artefact and complicates its storage format to preserve the exact
    text of a guaranteed error.
    """
    if len(expected) != len(actual):
        return False
    differing = [e for e, a in zip(expected, actual) if e != a]
    return bool(differing) and not any(_parses_as_mysql(e) for e in differing)


def _class_schema_name_not_hardcoded(cfg, expected, actual):
    """B: schema named something other than prospective/previous/dependency.

    strip_schema_qualified_aliases keys on a hardcoded alternation of those three
    names, so prepare() only strips `schema.alias.column` when the caller happens
    to use them - with prospective="p_1" it leaves `p_1.la.referencedcomponentid`,
    which DuckDB rejects as catalog.schema.table. precompile() keys on the
    sentinel, which is present regardless of the eventual name, so it strips
    correctly. bind(precompile) is therefore RIGHT here and prepare() is wrong;
    this is a latent bug in prepare() that stays hidden because RVF attaches the
    releases as "prospective"/"previous"/"dependency". Not fixed in prepare()
    because prepare() is the frozen reference this test compares against.
    """
    names = [cfg.get(k) for k in ("prospective", "previous", "dependency")]
    extra = [n for n in names if n and n not in PRODUCTION_SCHEMAS]
    if not extra:
        return False

    def norm(stmts):
        return [_strip_schema_prefixes(s, extra) for s in stmts]
    return norm(expected) == norm(actual)


def _strip_schema_prefixes(s, names):
    for n in names:
        s = s.replace(n + ".", "")
    return s


def _class_fragment_value_reformatted(cfg, expected, actual):
    """C: a placeholder whose value is a SQL FRAGMENT, not a single token.

    <MODULEIDS> and <INCLUDED_MODULES> expand to a comma-separated list. prepare()
    splices it in before sqlglot, so sqlglot re-prints the IN list in its own
    style; bind() splices it in afterwards, so the caller's spacing survives:

        prepare:          moduleid IN ('...207008', '...012004')
        bind(precompile): moduleid IN ('...207008','...012004')

    Semantically identical, and it disappears entirely when the caller passes the
    list already in sqlglot's canonical `'a', 'b'` form (verified). Inherent to
    the reordering: no sentinel can make sqlglot format text it never saw; the
    fix is to canonicalise the module list at the call site.
    """
    if not (cfg.get("module_ids") or cfg.get("module_id") or cfg.get("version")):
        return False

    def norm(stmts):
        return [re.sub(r",\s+", ",", s) for s in stmts]
    return norm(expected) == norm(actual)


CLASSIFIERS = [
    ("S: differs only in statements naming an absent release, which RVF skips",
     _class_skipped_unbound_release),
    ("A: bound text unparseable, so prepare returned it untranspiled",
     _class_prepare_fell_back),
    ("B: schema name not in prepare's hardcoded alternation",
     _class_schema_name_not_hardcoded),
    ("C: fragment-valued placeholder reformatted by sqlglot",
     _class_fragment_value_reformatted),
]


def classify(cfg, expected, actual):
    for label, fn in CLASSIFIERS:
        if fn(cfg, expected, actual):
            return label
    return None


def compare(name, raw, uuid, cfg, classified, unexplained):
    kw = dict(cfg)
    known_tables = kw.pop("known_tables")
    kw["assertion_id"] = uuid
    expected = rvfsql.prepare(raw, "duckdb", known_tables=known_tables, **kw)
    actual = rvfsql.bind(rvfsql.precompile(raw, known_tables=known_tables), **kw)
    if expected == actual:
        return True
    label = classify(kw, expected, actual)
    if label:
        classified.setdefault(label, []).append(name)
    else:
        unexplained.append((name, expected, actual))
    return False


def report_failure(name, expected, actual):
    print(f"\n  UNEXPLAINED {name}")
    if len(expected) != len(actual):
        print(f"    statement count differs: prepare={len(expected)} "
              f"bind(precompile)={len(actual)}")
    for i, (e, a) in enumerate(zip(expected, actual)):
        if e == a:
            continue
        print(f"    --- first differing statement (index {i}) ---")
        for line in difflib.unified_diff(
                e.splitlines(), a.splitlines(),
                fromfile="prepare", tofile="bind(precompile)", lineterm="",
                n=1):
            print("    " + line)
        break


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--assertions", required=True,
                    help="the corpus's scripts directory (searched recursively)")
    ap.add_argument("--ddl", type=pathlib.Path, default=DEFAULT_DDL,
                    help="RVF's create-tables-mysql.sql (default: %(default)s)")
    ap.add_argument("--prerequisites", default=None,
                    help="optional directory of pre-requisite *.sql")
    ap.add_argument("--max-failures-shown", type=int, default=3)
    a = ap.parse_args()

    known_tables = known_tables_from_schema(a.ddl, a.prerequisites)

    # The collision guard has to work, or a corpus that happened to contain a
    # sentinel would be bound into silently wrong SQL.
    try:
        rvfsql.precompile("select rvfph_prospective_ from dual")
        print("FAIL: precompile accepted text already containing a sentinel")
        return 1
    except ValueError:
        pass
    items = corpus(a.assertions, a.prerequisites)
    synth = [(f"synthetic:{n}", sql, "11111111-2222-3333-4444-555555555555")
             for n, sql in SYNTHETIC]
    print(f"corpus: {len(items)} assertion scripts, {len(synth)} synthetic probes, "
          f"{len(known_tables)} known tables")

    total = ident = 0
    all_unexplained = []
    classes = {}
    for label, production_shaped, cfg in configs(known_tables):
        classified, unexplained = {}, []
        n = ok = n_corpus = ok_corpus = 0
        for is_corpus, group in ((True, items), (False, synth)):
            for name, raw, uuid in group:
                r = compare(name, raw, uuid, cfg, classified, unexplained)
                n += 1
                ok += r
                if is_corpus:
                    n_corpus += 1
                    ok_corpus += r
        total += n
        ident += ok
        note = "" if ok == n else "   " + "; ".join(
            f"{k.split(':')[0]}x{len(v)}" for k, v in classified.items())
        if unexplained:
            note += f"   {len(unexplained)} UNEXPLAINED"
        print(f"  {label:46s} corpus {ok_corpus}/{n_corpus}  "
              f"all {ok}/{n}{note}")
        for f in unexplained[:a.max_failures_shown]:
            report_failure(*f)
        if len(unexplained) > a.max_failures_shown:
            print(f"    ... and {len(unexplained) - a.max_failures_shown} more: "
                  + ", ".join(f[0] for f in unexplained[a.max_failures_shown:]))
        all_unexplained.extend(unexplained)
        for k, v in classified.items():
            classes.setdefault(k, set()).update(v)
        # On a production-shaped config the only acceptable corpus divergence
        # is class S (statements no engine executes). Any other classified corpus
        # divergence would mean a classifier is excusing something RVF can
        # actually run - fail rather than absorb it.
        not_skipped = sorted(
            name for k, names in classified.items() if not k.startswith("S:")
            for name in names if not name.startswith("synthetic:"))
        if production_shaped and not_skipped:
            print(f"    {len(not_skipped)} CORPUS divergence(s) other than class S "
                  f"on a production configuration: {', '.join(not_skipped[:5])}")
            all_unexplained.append(
                (f"{label}: corpus not clean on a production config", [], []))

    if classes:
        print("\nclassified divergences (S never executes; A, B and C require a "
              "non-production configuration; see the classifier docstrings):")
        for k in sorted(classes):
            names = sorted(classes[k])
            shown = ", ".join(names[:4]) + (" ..." if len(names) > 4 else "")
            print(f"  {k}\n      {len(names)} case(s): {shown}")

    # Measure the per-run saving rather than assert one. It is real but SMALL in
    # absolute terms against a run whose time is dominated by
    # assertion SQL execution, so speed is not the argument for precompiling -
    # having a reviewable transpiled artefact, and finding parse-failure
    # fallbacks once at publish instead of silently on every run, is.
    cfg = dict(configs(known_tables)[0][2])
    kt = cfg.pop("known_tables")
    t = time.perf_counter()
    for name, raw, uuid in items:
        rvfsql.prepare(raw, "duckdb", known_tables=kt, assertion_id=uuid, **cfg)
    t_prepare = time.perf_counter() - t
    pre = [rvfsql.precompile(raw, known_tables=kt) for _, raw, _ in items]
    t = time.perf_counter()
    for stmts, (name, raw, uuid) in zip(pre, items):
        rvfsql.bind(stmts, assertion_id=uuid, **cfg)
    t_bind = time.perf_counter() - t
    print(f"\nper-run SQL preparation over {len(items)} scripts: "
          f"prepare {t_prepare:.2f}s -> bind {t_bind:.4f}s "
          f"({t_prepare / max(t_bind, 1e-9):.0f}x less; small next to the run "
          f"itself - not the reason to do this)")

    print(f"\nTOTAL {ident}/{total} byte-identical, "
          f"{total - ident} classified, {len(all_unexplained)} unexplained")
    return 1 if all_unexplained else 0


if __name__ == "__main__":
    sys.exit(main())
