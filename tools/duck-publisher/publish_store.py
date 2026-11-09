#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Precompile the assertion corpus once, so a runtime need not transpile.

Transpilation is the only part of the DuckDB engine that needs Python: sqlglot
parses the MySQL assertion, rewrites it, and prints DuckDB. Everything after
that is textual substitution of a run's values into sentinels, which any
language can do. Splitting the two at the sentinel boundary is what lets RVF
execute these assertions against DuckDB without embedding a transpiler:

    publish time (here, Python)   raw MySQL -> precompile() -> statements + sentinels
    run time     (RVF, Java)      statements + values -> bind() -> execute

precompile_parity.py is the evidence this split is safe: for every run
configuration RVF produces, bind(precompile()) is byte-identical to the
single-phase prepare() over the whole corpus.

The sentinel table is written INTO the store rather than documented, because a
Java binder that hardcodes "rvfph_prospective_" is a copy of a Python private
that will drift silently. Read them from the store and the two cannot disagree.

Usage:
    publish_store.py --scripts <corpus>/scripts --ddl <create-tables-mysql.sql>
                     --manifest-root <corpus> --out store.json
                     [--prerequisites <dir>] [--ports-sql <file>]...
"""
import argparse
import hashlib
import json
import logging
import pathlib
import re
import subprocess
import sys

# The environment check runs before sqlglot is imported, so a missing or
# different transpiler (or duckdb) stops the publisher at startup with both
# versions named - never a fallback to untranspiled MySQL.
import pins
pins.require()

import sqlglot                                                   # noqa: E402
from sqlglot import exp                                          # noqa: E402

import manifest                                                  # noqa: E402
import procedures                                                # noqa: E402
import rf2map                                                    # noqa: E402
import rvfsql                                                    # noqa: E402

PREREQUISITES_SQL = "pre-requisites.sql"
FORMAT_VERSION = 1

# The corpus files RVF reads assertion-group membership from. Carried verbatim,
# so the runtime applies AssertionGroupImporter's own rules to them instead of a
# second implementation of those rules living here.
GROUPING_FILES = ("groups.xml", "policies.xml")


def grouping(manifest_root: pathlib.Path):
    """groups.xml and policies.xml from the corpus root, verbatim.

    Without them the store holds assertions no group selects, and a run that
    selects nothing reports a clean pass, so a missing file is fatal.
    """
    out = []
    for name in GROUPING_FILES:
        path = manifest_root / name
        if not path.is_file():
            raise SystemExit(f"FATAL: {path} not found. The store carries the "
                             "corpus's group rules; without them no run selects "
                             "any of its assertions.")
        text = path.read_text(errors="replace")
        out.append({"file": name,
                    "sha256": hashlib.sha256(text.encode()).hexdigest()[:16],
                    "text": text})
    return out


def publisher_identity():
    """sha256 over this publisher's own sources and pins.

    The sorted `name<TAB>sha256` lines of every *.py beside this file and of
    requirements.txt, recorded as generator.publisher, so a store or a pack
    names the publisher that built it.
    """
    here = pathlib.Path(__file__).resolve().parent
    files = sorted([p for p in here.glob("*.py")] + [here / "requirements.txt"],
                   key=lambda p: p.name)
    material = "\n".join(f"{p.name}\t{hashlib.sha256(p.read_bytes()).hexdigest()}"
                         for p in files)
    return "sha256:" + hashlib.sha256(material.encode()).hexdigest()



# A table a pre-requisites script creates is a real table at run time, so
# `schema.thattable.column` is schema.table.column and NOT a schema-qualified
# alias - get the set wrong and strip_schema_qualified_aliases removes the schema
# from a genuine table reference. Derived from the SQL rather than listed.
_CREATED_TABLE_RE = re.compile(
    r"\bcreate\s+(?:or\s+replace\s+)?(?:temporary\s+)?(?:table|view)\s+"
    r"(?:if\s+not\s+exists\s+)?`?([a-z_][a-z0-9_]*)",
    re.IGNORECASE)


def prerequisite_scripts(prereq_dir):
    return sorted(prereq_dir.glob("*.sql")) if prereq_dir else []


def known_tables(ddl_path: pathlib.Path, prereq_dir):
    """The fixed table set: the DDL plus whatever the pre-requisites create.

    Read from files rather than a live catalog, because there is no catalog at
    publish time. The DDL is the same set by construction - it is what RVF
    creates for every release - and pre-requisites are optional: a corpus whose
    assertions read only DDL tables (the international corpus) needs none.
    unresolved_tables() is what catches a corpus that needed them and did not
    get them.
    """
    tables = {t.lower() for t in rf2map.mysql_ddl(ddl_path)}
    for path in prerequisite_scripts(prereq_dir):
        tables.update(m.group(1).lower() for m in
                      _CREATED_TABLE_RE.finditer(path.read_text(errors="replace")))
    return tables


def column_names(ddl_path: pathlib.Path):
    """{table: {column, ...}} for every DDL table.

    Distinct from table_columns() (which emits DuckDB type text for placeholder
    creation): this is what unrolls a stored procedure's
    `information_schema.tables` loop at publish time. MySQL's loop is over the
    live catalog, but the catalog it walks IS this DDL - RVF creates exactly
    these tables, and nothing in the corpus creates another name ending in `_s`
    or `_d` - so the two agree by construction.
    """
    ddl = rf2map.mysql_ddl(ddl_path)
    return {t.lower(): {c for c, _ in rf2map.columns_from_ddl(stmt)}
            for t, stmt in ddl.items()}


def table_columns(ddl_path: pathlib.Path):
    """{table: "col TYPE, col TYPE"} for every DDL table, in DuckDB types."""
    ddl = rf2map.mysql_ddl(ddl_path)
    out = {}
    for t, stmt in ddl.items():
        cols = rf2map.columns_from_ddl(stmt)
        if cols:
            out[t.lower()] = ", ".join(f"{c} {rf2map.duck_type(ty)}" for c, ty in cols)
    return out


def ports(extra_sql=()):
    """DuckDB statements a runtime runs once before any assertion.

    They stand in for what MySQL gets from elsewhere: built-in functions DuckDB
    lacks, and the corpus's own scalar CREATE FUNCTIONs, which the routine-
    definition skip removes from the assertion statement lists. The schema is
    emitted as the <PROSPECTIVE> sentinel so a binder substitutes it through the
    same path as everything else rather than needing its own rule.

    store["ports"] is the ONLY setup the RVF runtime applies, so anything an
    assertion calls must be here. For the international corpus that is exactly
    two macros:
      * substring_index - complexmap-ifa-rule, extendedmap-ifa-rule,
        description-fsn-synonym-text, description-unique-preferred-terms and
        others call SUBSTRING_INDEX, which DuckDB does not have;
      * cleanExpression - the corpus defines it with CREATE FUNCTION and
        validateSnomedCtExpressionConcepts calls it.

    A corpus that needs more (its own macros, or a table its procedures would
    have built) supplies them with --ports-sql rather than by editing this.
    """
    schema = rvfsql._SENTINEL_BY_PLACEHOLDER["<PROSPECTIVE>"]
    out = [s.strip() for s in rvfsql.DUCKDB_PRELUDE.split(";") if s.strip()]
    out.extend(procedures.macros(schema))
    for path in extra_sql:
        out.extend(ports_from_file(path, schema))
    return out


def ports_from_file(path: pathlib.Path, schema: str):
    """A corpus's own ports, beyond the international corpus's: DuckDB SQL, `;`-separated.

    Written in DuckDB, not MySQL - these are not transpiled. `<PROSPECTIVE>` is
    replaced by the schema sentinel, so `<PROSPECTIVE>.mytable` binds to the
    release under test exactly as an assertion's table references do.
    """
    body = rvfsql.strip_comments(path.read_text())
    stmts = [s.strip().replace("<PROSPECTIVE>", schema)
             for s in rvfsql.split_statements(body) if s.strip()]
    if not stmts:
        raise SystemExit(f"FATAL: --ports-sql {path} contains no statements")
    return stmts


_SYSTEM_SCHEMAS = ("information_schema", "pg_catalog")


def _table_refs(statements):
    """(tables read or written, tables created) by a list of DuckDB statements.

    Parsed with sqlglot rather than matched by regex, because the references that
    matter here are as often unqualified (`from concept_active`) as qualified,
    and only a parse tells a table from a CTE or a column. DROP statements are
    not references: `drop table if exists x` is valid whether or not x exists.
    """
    refs, created = set(), set()
    # sqlglot logs a warning for every CALL it parses as a Command; this is an
    # analysis pass over statements already published, so keep it quiet.
    log = logging.getLogger("sqlglot")
    level = log.level
    log.setLevel(logging.ERROR)
    try:
        trees = [t for stmt in statements for t in _parse_or_nothing(stmt)]
    finally:
        log.setLevel(level)
    for tree in trees:
        if tree is None or isinstance(tree, exp.Drop):
            continue
        ctes = {c.alias_or_name.lower() for c in tree.find_all(exp.CTE)}
        for create in tree.find_all(exp.Create):
            if (create.args.get("kind") or "").upper() in ("TABLE", "VIEW"):
                target = create.this
                if isinstance(target, exp.Schema):
                    target = target.this
                if isinstance(target, exp.Table):
                    created.add(target.name.lower())
        for table in tree.find_all(exp.Table):
            name = table.name.lower()
            if not name or name in ctes or table.db.lower() in _SYSTEM_SCHEMAS:
                continue
            refs.add(name)
    return refs, created


def _parse_or_nothing(stmt):
    try:
        return sqlglot.parse(stmt, read="duckdb")
    except Exception:                                            # noqa: BLE001
        return []


def unresolved_tables(store):
    """{assertion file: [table, ...]} for tables nothing provides.

    A table is provided by the DDL or a pre-requisites script (knownTables), by
    a port, by qa_result, or by any assertion that creates it - a few assertions
    read a table another one builds. Anything else is a table no run will have:
    typically a corpus that needs pre-requisites published without them, which
    otherwise surfaces only at run time as "Table with name ... does not exist"
    on every assertion that reads it.
    """
    provided = set(store["knownTables"]) | {store["qaResultToken"]}
    provided |= _table_refs(store["ports"])[1]
    for p in store["prerequisites"]:
        provided |= _table_refs(p["statements"])[1]
    per_file = {}
    for a in store["assertions"].values():
        refs, created = _table_refs(a["statements"])
        provided |= created
        per_file[a["file"]] = refs
    return {f: sorted(refs - provided)
            for f, refs in sorted(per_file.items()) if refs - provided}


def build(scripts_dir, prereq_dir, ddl_path, manifest_root, ports_sql=()):
    tables = known_tables(ddl_path, prereq_dir)
    tcols = column_names(ddl_path)
    # The TYPE-text map, not tcols. Two assertions compare a SMALLINT mapGroup
    # to '' - valid MySQL, which coerces the literal to 0 - and DuckDB refuses
    # the cast per row, so both errored on every release with complexmap rows
    # and reported zero findings on releases without them. Rewriting that needs
    # to know which columns are numeric, which only this map carries.
    numerics = rvfsql.numeric_column_names(table_columns(ddl_path))
    if not numerics:
        raise SystemExit("the DDL yielded no numeric columns - the '' rewrite "
                         "would silently do nothing")

    static = manifest.index_by_basename(manifest.load_scripts(manifest_root))

    assertions, failures, unlisted = {}, [], []
    # rglob, not glob. The assertions repository ships its scripts in
    # per-category SUBDIRECTORIES (component-centric/, file-centric/, ...).
    # Pointed at that layout, a non-recursive glob matched nothing and published
    # a store with ZERO assertions - which a runtime reads as an empty corpus,
    # and an empty corpus reports no findings and PASSES.
    for path in sorted(scripts_dir.rglob("*.sql")):
        if path.name.lower() == PREREQUISITES_SQL:
            # RVF's importer special-cases this exact filename: it is setup, not
            # an assertion. Pre-requisites are published separately (below).
            continue
        raw = path.read_text(errors="replace")
        entry = static.get(path.name)
        if entry is None:
            # A .sql file the manifest does not declare is not an assertion.
            # RVF discovers assertions FROM the manifest, so such a file is
            # never run - the international corpus ships 93 of them, mostly
            # delta counterparts of snapshot assertions. Skip and say so.
            unlisted.append(path.name)
            continue
        try:
            stmts = rvfsql.precompile(raw, known_tables=tables,
                                      table_columns=tcols,
                                      numeric_columns=numerics)
        except Exception as e:                                  # noqa: BLE001
            failures.append((path.name, f"{type(e).__name__}: {e}"))
            continue
        assertions[entry["uuid"]] = {
            "file": path.name,
            "sha256": hashlib.sha256(raw.encode()).hexdigest()[:16],
            # Carried so a runtime can resolve assertion GROUPS without going
            # back to manifest.xml. text and keywords are what the group rules
            # match on.
            "text": entry.get("text", ""),
            "keywords": entry.get("category", ""),
            "severity": entry.get("severity", ""),
            "statements": stmts,
        }

    prerequisites = []
    for path in prerequisite_scripts(prereq_dir):
        raw = path.read_text(errors="replace")
        try:
            prerequisites.append({
                "file": path.name,
                "sha256": hashlib.sha256(raw.encode()).hexdigest()[:16],
                "statements": rvfsql.precompile(raw, known_tables=tables,
                                               table_columns=tcols,
                                               numeric_columns=numerics),
            })
        except Exception as e:                                  # noqa: BLE001
            failures.append((path.name, f"{type(e).__name__}: {e}"))

    store = {
        "formatVersion": FORMAT_VERSION,
        "generator": {
            "sqlglot": sqlglot.__version__,
            "python": sys.version.split()[0],
            "publisher": publisher_identity(),
        },
        # Everything a binder must substitute, as data. Order matters: it is the
        # order transform() applies, and overlapping tokens depend on it.
        "sentinels": [{"placeholder": p, "sentinel": s} for p, s in rvfsql._SENTINELS],
        "runIdSentinel": rvfsql._SENTINEL_RUNID,
        # qa_result is substituted by NAME, not via a sentinel - bind() rewrites
        # the bare word. A binder must do the same, so say so here.
        "qaResultToken": "qa_result",
        "knownTables": sorted(tables),
        # Column definitions for every table the DDL declares, so a runtime can
        # create an EMPTY placeholder for a table the release does not ship.
        # Without them an assertion that merely mentions an absent table dies
        # with "Table with name ccsRefset_f does not exist" instead of correctly
        # reporting no findings - most such assertions are "no bad rows in X",
        # which passes precisely because there are no rows to be bad.
        "tableColumns": table_columns(ddl_path),
        "ports": ports(ports_sql),
        # Empty unless --prerequisites is given. Kept as a key either way: the
        # pack digest covers it (see pack_identity).
        "prerequisites": prerequisites,
        # Group membership rules, so the store alone answers which assertions
        # a run's groups select. Covered by the pack digest.
        "grouping": grouping(manifest_root),
        "assertions": assertions,
    }
    for name, missing in unresolved_tables(store).items():
        failures.append((name, "reads table(s) that neither the DDL, the "
                               "pre-requisites, the ports nor another assertion "
                               "provide: " + ", ".join(missing)))
    return store, failures, unlisted


def pack_identity(name, version, corpus_root, assertions, prerequisites, grouping_files):
    """This store's own identity: what it is, and what it was built from.

    Both fields are derived, not stamped:

    * VERSION is the assertion corpus's own commit date. A rebuild of unchanged
      inputs therefore produces the same version - a wall-clock stamp would give
      the same corpus a new identity on every build - and dates are orderable,
      which a "requires at least" comparison needs and a commit sha cannot give.
    * DIGEST is over the per-assertion lines (uuid, source hash, keywords,
      severity, text), sorted, then the (pre-requisite file, source hash)
      pairs, then the (grouping file, source hash) pairs. It answers "the same
      assertions, from the same text, with the same manifest metadata, selected
      by the same group rules", independent of JSON key order, publisher
      formatting, or which sqlglot compiled it. RVF recomputes it on load and
      refuses a store whose declared digest does not match, so an edit to any
      of those fields is refused; an edit to the statements is not.

    Refuses to invent a version. A store whose identity is guessed is worse than
    one with none.
    """
    ref = date = ""
    if version:
        date = version
    else:
        try:
            out = subprocess.run(
                ["git", "-C", str(corpus_root), "log", "-1",
                 "--format=%H %cd", "--date=format:%Y.%m.%d"],
                capture_output=True, text=True, timeout=30)
            if out.returncode == 0 and out.stdout.strip():
                ref, date = out.stdout.strip().split(" ", 1)
        except Exception:                                        # noqa: BLE001
            pass
    if not date:
        raise SystemExit(
            f"FATAL: no --pack-version given and {corpus_root} is not a git "
            "checkout, so the corpus has no date to take a version from. Pass "
            "--pack-version explicitly rather than publishing a store nothing "
            "can be required of.")
    # Over everything the store would EXECUTE from the corpus, not only its
    # assertions: pre-requisites build tables assertions read, so a changed
    # pre-requisites script runs differently while the assertion set is
    # untouched; and the group rules decide which assertions a run selects.
    # An assertion's keywords and text are what those rules match on, its
    # severity decides failure or warning, and its text is what a report says.
    # Text last on its line, so a tab inside it cannot shift another field.
    # Each "\n--\n" separator is present even when its section is empty; RVF's
    # DuckStore computes the same material.
    material = "\n".join(
        f"{uuid}\t{a['sha256']}\t{a['keywords']}\t{a['severity']}\t{a['text']}"
        for uuid, a in sorted(assertions.items()))
    material += "\n--\n" + "\n".join(
        f"{p['file']}\t{p['sha256']}" for p in sorted(prerequisites,
                                                     key=lambda x: x["file"]))
    material += "\n--\n" + "\n".join(
        f"{g['file']}\t{g['sha256']}" for g in sorted(grouping_files,
                                                     key=lambda x: x["file"]))
    digest = hashlib.sha256(material.encode()).hexdigest()
    return {"name": name, "version": date, "corpusRef": ref,
            "digest": "sha256:" + digest, "assertions": len(assertions)}


def requirements(specs):
    """`PACK:atLeast=VERSION` or `PACK:digest=sha256:...` -> store entries.

    Exactly one constraint per entry, and nothing else accepted: the runtime
    refuses a requirement it does not understand rather than ignoring it, so a
    typo here must fail at publish rather than travel in the artefact.
    """
    out = []
    for spec in specs:
        pack, _, rest = spec.partition(":")
        key, _, value = rest.partition("=")
        if not pack or key not in ("atLeast", "digest") or not value:
            raise SystemExit(
                f"FATAL: --requires {spec!r} is not PACK:atLeast=VERSION or "
                "PACK:digest=sha256:...")
        out.append({"pack": pack, key: value})
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--scripts", required=True, type=pathlib.Path,
                    help="the corpus's scripts directory (searched recursively)")
    ap.add_argument("--ddl", required=True, type=pathlib.Path,
                    help="RVF's src/main/resources/sql/create-tables-mysql.sql")
    ap.add_argument("--manifest-root", type=pathlib.Path,
                    help="directory containing manifest.xml (default: scripts/..)")
    ap.add_argument("--out", required=True, type=pathlib.Path)
    ap.add_argument("--prerequisites", type=pathlib.Path,
                    help="optional directory of pre-requisite *.sql (MySQL, "
                         "transpiled like assertions) for a corpus whose "
                         "assertions read tables the DDL does not declare")
    ap.add_argument("--ports-sql", action="append", default=[], type=pathlib.Path,
                    metavar="FILE",
                    help="extra DuckDB setup statements (macros, derived tables) "
                         "for a corpus that needs them; `<PROSPECTIVE>` is the "
                         "release schema. Repeatable, applied in order.")
    ap.add_argument("--ported-call", action="append", default=[], metavar="NAME",
                    help="a stored procedure whose work a --ports-sql statement "
                         "does; its CALLs are dropped. Repeatable.")
    ap.add_argument("--ci-function", action="append", default=[], metavar="NAME",
                    help="a corpus stored function that returns a case-INSENSITIVE "
                         "(schema-default collation) string; regex matches on its "
                         "result get the 'i' flag. Repeatable.")
    ap.add_argument("--stamp", default="",
                    help="optional build stamp recorded in the store")
    ap.add_argument("--pack-name", default="international",
                    help="this store's identity in a report and in a merge")
    ap.add_argument("--pack-version", default="",
                    help="override the version taken from the corpus commit date")
    ap.add_argument("--requires", action="append", default=[],
                    metavar="PACK:atLeast=VERSION|digest=SHA",
                    help="what this pack needs of another in the same merge; "
                         "repeatable. Refused at merge with both versions named, "
                         "rather than failing at run time on a missing macro.")
    a = ap.parse_args()

    if a.prerequisites and not a.prerequisites.is_dir():
        raise SystemExit(f"FATAL: --prerequisites {a.prerequisites} is not a directory")
    if a.prerequisites and not prerequisite_scripts(a.prerequisites):
        raise SystemExit(f"FATAL: --prerequisites {a.prerequisites} holds no *.sql")
    procedures.PORTED_ELSEWHERE.update(n.lower() for n in a.ported_call)
    rvfsql.CI_RETURNING_FUNCTIONS.update(n.lower() for n in a.ci_function)

    root = a.manifest_root or a.scripts.parent
    store, failures, unlisted = build(a.scripts, a.prerequisites, a.ddl, root,
                                      ports_sql=a.ports_sql)
    if a.stamp:
        store["generator"]["stamp"] = a.stamp
    store["pack"] = pack_identity(a.pack_name, a.pack_version, root,
                                  store["assertions"], store["prerequisites"],
                                  store["grouping"])
    if a.requires:
        store["requires"] = requirements(a.requires)

    # A store with no assertions is never a valid outcome - it is what a wrong
    # --scripts path produces, and it is indistinguishable downstream from a
    # release with nothing wrong with it. Refuse to write it.
    if not store["assertions"]:
        print(f"REFUSING to write {a.out}: no assertions were found under "
              f"{a.scripts}. A store with no assertions reports every release as "
              f"clean. Check --scripts and --manifest-root.")
        return 1

    a.out.write_text(json.dumps(store, indent=1))
    n_stmt = sum(len(v["statements"]) for v in store["assertions"].values())
    n_pre = sum(len(p["statements"]) for p in store["prerequisites"])
    print(f"pack         {store['pack']['name']} {store['pack']['version']} "
          f"{store['pack']['digest'][:23]} (corpus {store['pack']['corpusRef'][:12] or 'not a checkout'})")
    for need in store.get("requires", []):
        key = "atLeast" if "atLeast" in need else "digest"
        print(f"requires     {need['pack']} {key} {need[key]}")
    print(f"assertions   {len(store['assertions'])}")
    print(f"statements   {n_stmt} (+{n_pre} pre-requisite)")
    print(f"ports        {len(store['ports'])}")
    print(f"grouping     {', '.join(g['file'] for g in store['grouping'])}")
    print(f"knownTables  {len(store['knownTables'])}")
    print(f"procPorts    {len(procedures.REGISTRY)} stored procedures unrolled")
    if procedures.UNPORTED:
        # Loud, but not fatal: the assertion fails at run time exactly as it does
        # today. Refusing to publish would take the other assertions down with it.
        print("UNPORTED     stored-procedure CALLs with no DuckDB port:")
        for name, n in sorted(procedures.UNPORTED.items()):
            print(f"             {name} ({n} call{'s' if n > 1 else ''})")
    print(f"wrote        {a.out} ({a.out.stat().st_size} bytes)")
    if unlisted:
        print(f"not in manifest, skipped: {len(unlisted)}")
        for name in unlisted[:5]:
            print(f"    {name}")

    if failures:
        # Loud, and non-zero. A store that silently drops assertions produces a
        # run that looks complete and is not.
        print(f"\nFAILED {len(failures)}:", file=sys.stderr)
        for name, err in failures[:20]:
            print(f"    {name}: {err}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
