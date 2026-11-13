# SPDX-License-Identifier: Apache-2.0
"""RVF assertion SQL pipeline, ported faithfully, plus a DuckDB dialect pass.

Stage 1 (import)    - port of AssertionsDatabaseImporter.addSqlTestToAssertion:
                      curr_/prev_/dependency_/v_ token rewriting and the
                      runid/assertionuuid column renames.
Stage 2 (transform) - port of MySqlQueryTransformer.transformSql: <RUNID>,
                      <ASSERTIONUUID>, <PROSPECTIVE>, <SNAPSHOT> etc.
Stage 3 (dialect)   - MySQL -> DuckDB. Deliberately tiny: DuckDB natively accepts
                      concat/if/ifnull/group_concat, so only four constructs need
                      touching, and substring_index is shimmed with a MACRO
                      instead of being rewritten per script.

sqlglot is imported unconditionally. A dialect stage that fell back to the
untranspiled MySQL when sqlglot was missing would publish a store that looks
complete and is silently wrong; a missing transpiler must stop the build.
"""
import re
import sys

import sqlglot
from sqlglot import exp

import procedures

# ---------------------------------------------------------------- statements

def split_statements(sql: str):
    """Quote-aware split on ';' (stands in for Presto's StatementSplitter)."""
    out, buf, quote, i = [], [], None, 0
    while i < len(sql):
        c = sql[i]
        if quote:
            buf.append(c)
            if c == "\\" and i + 1 < len(sql):
                buf.append(sql[i + 1]); i += 2; continue
            if c == quote:
                quote = None
        else:
            if c in "'\"":
                quote = c; buf.append(c)
            elif c == ";":
                s = "".join(buf).strip()
                if s:
                    out.append(s)
                buf = []
            else:
                buf.append(c)
        i += 1
    s = "".join(buf).strip()
    if s:
        out.append(s)
    return out


_DELIMITER_RE = re.compile(r"^\s*DELIMITER\s+(\S+)\s*$", re.I | re.M)


def split_with_delimiters(sql: str):
    """Split honouring MySQL `DELIMITER x` directives.

    A pre-requisites script written for the mysql client (as RVF's importer
    accepts) uses `DELIMITER //`, so a naive split on ';' shreds it. Mirrors
    MySqlQueryTransformer.transformToStatements.
    """
    parts, delim, buf = [], ";", []
    for line in sql.splitlines(keepends=True):
        m = _DELIMITER_RE.match(line)
        if m:
            chunk = "".join(buf)
            if chunk.strip():
                parts.extend(_split_on(chunk, delim))
            buf = []
            delim = m.group(1)
            continue
        buf.append(line)
    chunk = "".join(buf)
    if chunk.strip():
        parts.extend(_split_on(chunk, delim))
    return [p.strip() for p in parts if p.strip()]


def _split_on(sql: str, delim: str):
    if delim == ";":
        return split_statements(sql)
    out, buf, i, quote = [], [], 0, None
    while i < len(sql):
        c = sql[i]
        if quote:
            buf.append(c)
            if c == "\\" and i + 1 < len(sql):
                buf.append(sql[i + 1]); i += 2; continue
            if c == quote:
                quote = None
            i += 1
            continue
        if c in "'\"":
            quote = c; buf.append(c); i += 1; continue
        if sql.startswith(delim, i):
            out.append("".join(buf)); buf = []; i += len(delim); continue
        buf.append(c); i += 1
    if "".join(buf).strip():
        out.append("".join(buf))
    return out


# Secondary indexes have no DuckDB equivalent and no purpose in a columnar
# store - Parquet row-group statistics do that work.
_CREATE_INDEX_RE = re.compile(
    r"^\s*create\s+(?:unique\s+|fulltext\s+|spatial\s+)*index\b", re.I)
# MySQL storage-engine session default.
_SET_ENGINE_RE = re.compile(r"^\s*set\s+default_storage_engine\b", re.I)
# CREATE/DROP of a stored routine. DuckDB has neither procedures nor MySQL's
# scalar CREATE FUNCTION, and it does not need them: every routine the corpus
# defines is replaced by something equivalent that already exists by the time an
# assertion runs - procedures.REGISTRY unrolls the CALL sites into plain SQL, and
# the scalar CREATE FUNCTIONs are DuckDB MACROs carried in the store's `ports`.
# Skipped for the same reason CREATE INDEX is: not a statement that failed, a
# statement with no work left to do in this architecture. Keeping them made the
# resource assertions whose whole body is a routine definition report as errors,
# where the MySQL engine reports them with failureCount 0.
_ROUTINE_DEF_RE = re.compile(
    r"^\s*(create|drop)\s+(procedure|function)\b", re.I)

_CREATE_ROUTINE_RE = re.compile(r"\bcreate\s+(procedure|function)\b", re.I)


def split_statements_routine_aware(sql: str):
    """Like split_statements, but a CREATE PROCEDURE/FUNCTION body is kept whole.

    Routine bodies are full of semicolons; splitting on them shreds the
    definition. RVF's importer works around this by concatenating everything
    after the CREATE back together - same idea, done up front. Anything before
    the CREATE (typically DROP PROCEDURE IF EXISTS) still splits normally.
    """
    m = _CREATE_ROUTINE_RE.search(sql)
    if not m:
        return split_statements(sql)
    head, body = sql[:m.start()], sql[m.start():]
    stmts = split_statements(head)
    body = body.strip()
    if body.endswith(";"):
        body = body[:-1]
    if body:
        stmts.append(body)
    return stmts


def strip_comments(sql: str) -> str:
    sql = re.sub(r"/\*.*?\*/", "", sql, flags=re.S)
    sql = re.sub(r"--[^\n]*", "", sql)
    return sql


# ------------------------------------------------- stage 1: importer rewrite

# curr_<table>_<s|d|f>  ->  <PROSPECTIVE>.<table>_<SNAPSHOT|DELTA|FULL>
_PREFIX = {"curr_": "<PROSPECTIVE>", "prev_": "<PREVIOUS>",
           "dependency_": "<DEPENDENCY>"}
_SUFFIX = {"s": "<SNAPSHOT>", "d": "<DELTA>", "f": "<FULL>"}
_TOKEN_RE = re.compile(r"\b((curr|prev|dependency)_([a-z0-9_]+?)_([sdf]))\b", re.I)
_TEMP_RE = re.compile(r"\bv_([a-z0-9_]+)\b", re.I)


def importer_rewrite(sql: str) -> str:
    def repl(m):
        pfx = _PREFIX[m.group(2).lower() + "_"]
        return f"{pfx}.{m.group(3).lower()}_{_SUFFIX[m.group(4).lower()]}"
    sql = _TOKEN_RE.sub(repl, sql)
    sql = _TEMP_RE.sub(lambda m: f"<TEMP>.{m.group(1)}", sql)
    # column renames applied by the importer
    sql = re.sub(r"\bassertiontext\s*,", "", sql, flags=re.I)
    sql = sql.replace("'<ASSERTIONTEXT>',", "")
    # Case-SENSITIVE, exactly as Java's String.replaceAll: the lowercase column
    # names must be renamed while the uppercase <RUNID>/<ASSERTIONUUID>
    # placeholders survive untouched for the substitution stage.
    sql = re.sub(r"\brunid\b", "run_id", sql)
    sql = re.sub(r"\bassertionuuid\b", "assertion_id", sql)
    return sql


# ------------------------------------------- stage 2: runtime substitutions

# MySqlQueryTransformer binds <INTERNATIONAL_MODULES> to RF2Constants.
# SCTID_CORE_MODULE - one id, not the String[] INTERNATIONAL_MODULES that class
# also declares. So the "must not contain international module" assertion never
# checks the model module 900000000000012004. Reproduced, not corrected: this is
# an upstream behaviour, and changing it here would silently move a result.
SCTID_CORE_MODULE = "900000000000207008"


# MySqlQueryTransformer's own defaults, and the reason they are not "":
#   <MODULEID>  falls back to RF2Constants.SCTID_CORE_MODULE when the run
#               configures no defaultModuleId. An empty string here put '' into
#               a BIGINT concept_id and the assertion died with "Could not
#               convert string '' to INT64" - a binding gap that reads as an
#               engine bug.
#   <VERSION>   is the third '_'-separated part of the prospective version, or
#               the literal NOT_SUPPLIED. It appears only inside details text.
DEFAULT_MODULE_ID = SCTID_CORE_MODULE
DEFAULT_VERSION = "NOT_SUPPLIED"


def transform(sql: str, *, run_id, assertion_id, prospective, previous=None,
              dependency=None, qa_result, module_id=DEFAULT_MODULE_ID,
              module_ids="", version=DEFAULT_VERSION):
    sql = strip_comments(sql)
    sql = sql.replace("<RUNID>", str(run_id))
    sql = sql.replace("<ASSERTIONUUID>", str(assertion_id))
    sql = sql.replace("<MODULEID>", module_id)
    sql = sql.replace("<MODULEIDS>", module_ids)
    # Newer assertions use <INCLUDED_MODULES>, which this RVF version's
    # transformer does not substitute (so they fail on MySQL too). The SQL
    # branches on `'NULL' = '<INCLUDED_MODULES>'`, so NULL is the documented
    # "no module filter configured" default.
    sql = sql.replace("<INCLUDED_MODULES>", module_ids if module_ids else "NULL")
    sql = sql.replace("<INTERNATIONAL_MODULES>", SCTID_CORE_MODULE)
    sql = sql.replace("<VERSION>", version)
    sql = re.sub(r"\bqa_result\b", qa_result, sql)
    sql = sql.replace("<PROSPECTIVE>", prospective)
    sql = sql.replace("<TEMP>", prospective)
    if previous:
        sql = sql.replace("<PREVIOUS>", previous)
    if dependency:
        sql = sql.replace("<DEPENDENCY>", dependency)
    sql = sql.replace("<SNAPSHOT>", "s").replace("<DELTA>", "d").replace("<FULL>", "f")
    sql = sql.replace("[[:<:]]", r"\\b").replace("[[:>:]]", r"\\b")
    return sql


def needs(sql_raw: str):
    """Which release schemas an assertion references (pre-substitution)."""
    return {
        "previous": "<PREVIOUS>" in sql_raw or bool(re.search(r"\bprev_", sql_raw, re.I)),
        "dependency": "<DEPENDENCY>" in sql_raw or bool(re.search(r"\bdependency_", sql_raw, re.I)),
    }


# ------------------------------------------------- stage 3: DuckDB dialect

# MySQL substring_index emulated exactly, including negative counts.
DUCKDB_PRELUDE = """
CREATE OR REPLACE MACRO substring_index(str, delim, cnt) AS
  CASE WHEN cnt >= 0
       THEN array_to_string(list_slice(string_split(str, delim), 1, cnt), delim)
       ELSE array_to_string(list_slice(string_split(str, delim),
              -cnt * -1, len(string_split(str, delim))), delim)
  END;
"""

# RF2 has no real DATE columns - every `cast(x as datetime)` is applied to a
# zero-padded YYYYMMDD string (effectivetime, or a literal compared against it).
# Lexical ordering equals chronological ordering there, so dropping the cast
# preserves max()/</>/= semantics exactly while avoiding DuckDB's stricter
# timestamp parser, which rejects "20240301".
_CAST_OPERAND = r"('[^']*'|[\w.`\"]+)"
_CAST_DT_RE = re.compile(
    r"cast\s*\(\s*" + _CAST_OPERAND + r"\s+as\s+datetime\s*\)", re.I)
# `cast(x as binary)` is MySQL's way of forcing a byte-wise comparison past the
# default case-insensitive collation - the CAST form of the `BINARY x` operator
# stripped by _BINARY_OP_RE, and dropped for the same reason: DuckDB's VARCHAR
# comparison is already byte-wise, so the cast is a no-op that DuckDB cannot
# perform. Left in, sqlglot renders it as CAST(... AS BLOB) and DuckDB refuses
# any term containing a non-ASCII byte ("Invalid byte encountered in STRING ->
# BLOB conversion"), which fails the assertion on the first © in a copyright
# notice rather than on anything the assertion is about.
_CAST_BINARY_RE = re.compile(
    r"cast\s*\(\s*" + _CAST_OPERAND
    + r"\s+as\s+(?:binary|varbinary)(?:\s*\(\s*\d+\s*\))?\s*\)", re.I)
# MySQL server/session tuning knobs; meaningless to DuckDB, and its parser
# overflows INT32 on things like `SET tmp_table_size = 1024*1024*1024*2`.
_MYSQL_SET_RE = re.compile(
    r"^\s*set\s+(session\s+|global\s+|@@)?"
    r"(tmp_table_size|max_heap_table_size|bulk_insert_buffer_size|"
    r"group_concat_max_len|sql_mode|myisam_sort_buffer_size|"
    r"session\s+\w+)\b", re.I)
# `col REGEXP 'pat'`  ->  regexp_matches(col, 'pat')
_REGEXP_RE = re.compile(
    r"([\w.`\"]+)\s+(NOT\s+)?REGEXP\s+('(?:[^'\\]|\\.)*')", re.I)
_ENGINE_RE = re.compile(r"\bENGINE\s*=\s*\w+", re.I)
# `effectivetime > NOW()` - compare like for like against the YYYYMMDD string.
_EFFECTIVETIME_NOW_RE = re.compile(
    r"([\w.`\"]*effectivetime)\s*(<=|>=|<>|<|>|=)\s*NOW\(\s*\)", re.I)
_DISPLAY_WIDTH_RE = re.compile(
    r"\b(bigint|smallint|mediumint|tinyint|int)\s*\(\s*\d+\s*\)", re.I)
# MySQL's default charset is case-INSENSITIVE, so assertions wanting exact
# comparison say `BINARY term` or `COLLATE utf8_bin`. DuckDB's VARCHAR comparison
# is already byte-wise, so both are no-ops there. Leaving them in makes sqlglot
# emit a BLOB cast, which then fails on any non-ASCII term.
_BINARY_OP_RE = re.compile(r"(?<!as )\bbinary\s+(?=[\w`\"'])", re.I)
_COLLATE_RE = re.compile(r"\bcollate\s+\w+", re.I)
# DuckDB needs no secondary indexes; these statements have no equivalent.
# Covers `add index`, `add unique index`, `add FULLTEXT index`, `add key`, ...
_ALTER_INDEX_RE = re.compile(
    r"^\s*alter\s+table\s+.*?\badd\s+(?:(?:fulltext|unique|spatial|clustered)\s+)*"
    r"(index|key)\b", re.I | re.S)


def _strip_indexes(tree):
    """MySQL declares secondary indexes inline in CREATE TABLE. DuckDB has no
    equivalent and does not need one - Parquet/zone maps replace them. Drop them.
    """
    changed = False
    for schema in tree.find_all(exp.Schema):
        keep = [e for e in schema.expressions
                if not isinstance(e, exp.IndexColumnConstraint)]
        if len(keep) != len(schema.expressions):
            schema.set("expressions", keep)
            changed = True
    return changed


def _normalise_exists(tree):
    """`EXISTS (SELECT <anything> ...)` is semantically independent of its select
    list. Several assertions accidentally reference an OUTER alias there (e.g.
    `not exists (select r.id from s where s.sourceid = c.id)` inside a query
    already aliasing r). MySQL ignores it; DuckDB treats it as a real correlation
    and decorrelates into a blowup - one such assertion spilled 156 GiB and OOMed.
    Rewriting the select list to a constant is semantics-preserving and removes
    the spurious correlation entirely.
    """
    changed = False
    for ex in tree.find_all(exp.Exists):
        inner = ex.this
        if not isinstance(inner, exp.Select):
            continue
        # Only safe when the select list is purely plain columns. If it holds an
        # aggregate or an alias, the list is load-bearing: e.g.
        #   not exists (select count(*) as total from ... having total > 1)
        # would lose `total` and leave the HAVING dangling.
        if inner.args.get("having") or inner.args.get("group"):
            continue
        if not all(isinstance(e, exp.Column) for e in inner.expressions):
            continue
        cur = [e.sql() for e in inner.expressions]
        if cur != ["1"]:
            inner.set("expressions", [exp.Literal.number(1)])
            changed = True
    return changed


def _having_without_group_by(tree):
    """MySQL allows HAVING with no GROUP BY and no aggregate, using it to filter
    on a select alias:

        SELECT id, (LENGTH(term)-LENGTH(REPLACE(term,' ',''))+1) AS total
        FROM description_d WHERE ... HAVING total > 35

    DuckDB treats HAVING as implying aggregation and demands the columns be
    grouped. DuckDB does allow a select alias in WHERE, so moving the predicate
    there is exact.
    """
    changed = False
    for select in tree.find_all(exp.Select):
        having = select.args.get("having")
        if not having or select.args.get("group"):
            continue
        if any(e.find(exp.AggFunc) for e in select.expressions):
            continue
        if having.this.find(exp.AggFunc):
            continue
        select.set("having", None)
        where = select.args.get("where")
        cond = having.this
        select.set("where", exp.Where(
            this=exp.and_(where.this, cond) if where else cond))
        changed = True
    return changed


# MySQL decides case sensitivity per OPERAND, from its collation - not globally.
# RVF declares `term ... collate utf8_bin`, which is case-SENSITIVE and matches
# DuckDB's default. But a stored function returns the schema default collation,
# utf8mb4_0900_ai_ci, which is case-INSENSITIVE, so `fn(x) RLIKE 'abc'` matches
# "Abc" on MySQL where `term RLIKE 'abc'` does not.
#
# So only comparisons whose operand is a varchar-returning stored function are
# case-insensitive. Making regex case-insensitive GLOBALLY was tried and
# rejected (measured on the SNOMED CT-AU edition: it fixed the one assertion
# that compares a function result and broke four that compare the column).
#
# The international corpus defines no such function, so the set is empty. A
# corpus that does names its functions here (publish_store.py --ci-function),
# because the transpiler cannot see a function's return collation from a call.
CI_RETURNING_FUNCTIONS = set()


def _case_insensitive_on_function_results(tree):
    """Add the 'i' regex flag only where MySQL's collation makes it apply."""

    def is_ci_operand(node):
        # A user-defined call parses as exp.Anonymous with the function NAME in
        # .this. Do not use sql_name() here - it returns the literal "ANONYMOUS"
        # for these, which is truthy and silently matches nothing.
        if isinstance(node, exp.Anonymous):
            return str(node.this).lower() in CI_RETURNING_FUNCTIONS
        if isinstance(node, exp.Func):
            return (node.sql_name() or "").lower() in CI_RETURNING_FUNCTIONS
        return False

    changed = False
    for node in list(tree.find_all(exp.RegexpLike)):
        if not (is_ci_operand(node.this) or is_ci_operand(node.expression)):
            continue
        node.replace(exp.Anonymous(
            this="regexp_matches",
            expressions=[node.this.copy(), node.expression.copy(),
                         exp.Literal.string("i")]))
        changed = True
    return changed


def _unicode_posix_classes(tree):
    """Rewrite POSIX classes in every regex pattern the tree carries.

    An AST pass, not a textual one, because sqlglot - not `_regexp_to_duckdb` -
    is what converts `x REGEXP 'p'` for a statement it can parse, so a textual
    pass over the input never sees the pattern that reaches DuckDB.

    Both node shapes are handled: sqlglot's own `RegexpLike`, and the
    `regexp_matches` Anonymous that `_case_insensitive_on_function_results`
    produces - so this pass is order-independent with respect to it.
    """

    def rewrite(literal):
        if not isinstance(literal, exp.Literal) or not literal.is_string:
            return False
        translated = unicode_posix_classes(literal.this)
        if translated == literal.this:
            return False
        literal.set("this", translated)
        return True

    changed = False
    for node in list(tree.find_all(exp.RegexpLike)):
        changed = rewrite(node.expression) or changed
    for node in list(tree.find_all(exp.Anonymous)):
        if str(node.this).lower() != "regexp_matches":
            continue
        args = node.expressions
        if len(args) >= 2:
            changed = rewrite(args[1]) or changed
    return changed


def _fix_loose_group_by(tree):
    changed = False
    for select in tree.find_all(exp.Select):
        group = select.args.get("group")
        if not group:
            continue
        keys = {g.sql(dialect="mysql").lower() for g in group.expressions}
        # Only an UNQUALIFIED group key may match on bare name. Deriving bare
        # names from qualified keys made `b.id` look grouped when the key was
        # `a.id` - different tables, so it must still be wrapped.
        unqualified = {k for k in keys if "." not in k}

        def grouped(col):
            return (col.sql(dialect="mysql").lower() in keys
                    or col.name.lower() in unqualified)

        for proj in list(select.expressions):
            if isinstance(proj, exp.Alias):
                # Explicit alias already fixes the output name; just wrap inside.
                for col in list(proj.this.find_all(exp.Column)):
                    if grouped(col) or col.find_ancestor(exp.AggFunc):
                        continue
                    col.replace(exp.AnyValue(this=col.copy()))
                    changed = True
            elif isinstance(proj, exp.Column):
                if grouped(proj):
                    continue
                # Alias back to the original column name. Without this the
                # projection is renamed to any_value(id), and a derived table
                # referenced downstream as b.id stops resolving.
                proj.replace(exp.Alias(this=exp.AnyValue(this=proj.copy()),
                                       alias=exp.to_identifier(proj.name)))
                changed = True
            else:
                for col in list(proj.find_all(exp.Column)):
                    if grouped(col) or col.find_ancestor(exp.AggFunc):
                        continue
                    col.replace(exp.AnyValue(this=col.copy()))
                    changed = True
    return changed


def loose_group_by_fix(stmt: str) -> str:
    """MySQL (without ONLY_FULL_GROUP_BY, which is RVF's production setting) lets
    a SELECT return columns absent from GROUP BY, picking an arbitrary row.
    DuckDB rejects this. Wrap exactly those columns in ANY_VALUE, which has the
    identical 'arbitrary row' semantics.

    Surgical on purpose: statements that don't hit this case are returned byte
    for byte, so sqlglot round-tripping can't perturb anything else.
    """
    try:
        trees = sqlglot.parse(stmt, read="mysql")
    except Exception:
        return stmt

    changed = False
    for tree in trees:
        if tree is None:
            continue
        for select in tree.find_all(exp.Select):
            group = select.args.get("group")
            if not group:
                continue
            keys = {g.sql(dialect="mysql").lower() for g in group.expressions}
            for proj in select.expressions:
                target = proj.this if isinstance(proj, exp.Alias) else proj
                if isinstance(target, exp.Column):
                    s = target.sql(dialect="mysql").lower()
                    # bare column name also counts as grouped (a.id vs id)
                    if s in keys or target.name.lower() in {
                            k.split(".")[-1] for k in keys}:
                        continue
                    if target.find_ancestor(exp.AggFunc):
                        continue
                    target.replace(exp.AnyValue(this=target.copy()))
                    changed = True
    if not changed:
        return stmt
    return ";\n".join(t.sql(dialect="duckdb") for t in trees if t is not None)


_MYSQL_ESCAPES = {"0": "\0", "'": "'", '"': '"', "b": "\b", "n": "\n",
                  "r": "\r", "t": "\t", "Z": "\x1a", "\\": "\\"}


def decode_mysql_string(body: str) -> str:
    """Apply MySQL's string-literal escape processing.

    This is the crux of porting regex literals. MySQL decodes backslash escapes
    in the *literal* before the regex engine ever sees it, so `'[\\\\\\t\\Z]'`
    reaches the matcher as backslash, TAB, SUB. DuckDB does no such decoding -
    single-quoted strings are taken verbatim. Emitting the MySQL-decoded bytes
    directly is therefore the only way to get identical matching.

    Getting this wrong is not a subtle failure: a mangled character class
    silently matches ordinary letters and floods the report with false failures.
    """
    out, i = [], 0
    while i < len(body):
        c = body[i]
        if c == "\\" and i + 1 < len(body):
            nxt = body[i + 1]
            out.append(_MYSQL_ESCAPES.get(nxt, nxt))
            i += 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


# MySQL 8's regex is ICU, whose POSIX classes are UNICODE-aware; DuckDB's is
# RE2, whose POSIX classes are ASCII-ONLY. Measured on the SNOMED CT-AU
# edition's 1,826,331 active terms, `[[:alnum:]]+/[[:alnum:]]+` selects 76,165
# rows on MySQL and 76,164 on DuckDB - the missing one being "History of -
# vertigo/Meniere disease", whose accented letter RE2 does not count as
# alphanumeric.
#
# One row today, but the shape is a silent blind spot: an assertion that checks
# term formatting stops seeing accented content, reports nothing, and reads as a
# pass. `[\p{L}\p{N}]` restores it exactly - 76,165, including that row.
#
# Only the classes the corpora actually use are translated. An untranslated one
# is REFUSED rather than passed through, because passing it through is the
# failure this comment describes.
_POSIX_TO_RE2 = {
    "alnum": r"\p{L}\p{N}",
    "alpha": r"\p{L}",
    "digit": r"\p{Nd}",
    "lower": r"\p{Ll}",
    "upper": r"\p{Lu}",
}
_POSIX_CLASS_RE = re.compile(r"\[:([a-z]+):\]")


def unicode_posix_classes(pattern: str) -> str:
    """ICU's Unicode-aware POSIX classes, as RE2 property classes.

    Applied to the pattern INSIDE its brackets: `[[:alnum:]]` is a bracket
    expression containing the class, so the replacement is the property set
    without its own brackets and the surrounding ones are kept - which also
    makes `[[:alnum:]_-]` come out right.
    """
    def repl(m):
        name = m.group(1)
        replacement = _POSIX_TO_RE2.get(name)
        if replacement is None:
            raise ValueError(
                f"POSIX class [:{name}:] has no verified RE2 equivalent. "
                "MySQL's ICU classes are Unicode-aware and RE2's are ASCII-only, "
                "so passing it through would silently narrow the match. Add it to "
                "_POSIX_TO_RE2 with a measurement.")
        return replacement
    return _POSIX_CLASS_RE.sub(repl, pattern)


def _regexp_to_duckdb(m):
    col, negate, lit = m.group(1), m.group(2), m.group(3)
    decoded = unicode_posix_classes(decode_mysql_string(lit[1:-1]))
    call = f"regexp_matches({col}, '{decoded.replace(chr(39), chr(39) * 2)}')"
    # `term not regexp '...'` must become NOT regexp_matches(term, '...'), not
    # regexp_matches(not, '...') - the old pattern captured `not` as the column.
    return f"NOT {call}" if negate else call


_DEFAULT_SCHEMA_NAMES = ("prospective", "previous", "dependency")
_QUALIFIED_ALIAS_RE = re.compile(
    r"\b(prospective|previous|dependency)\.(\w+)\.(\w+)\b", re.I)
# re.compile is not cached the way the re module-level functions are, and this
# pass runs per assertion, so keep the two variants (bound names / sentinels).
_ALIAS_RE_CACHE = {_DEFAULT_SCHEMA_NAMES: _QUALIFIED_ALIAS_RE}


def _alias_re(schema_names):
    pattern = _ALIAS_RE_CACHE.get(schema_names)
    if pattern is None:
        pattern = re.compile(
            r"\b(" + "|".join(re.escape(s) for s in schema_names)
            + r")\.(\w+)\.(\w+)\b", re.I)
        _ALIAS_RE_CACHE[schema_names] = pattern
    return pattern


def strip_schema_qualified_aliases(sql: str, known_tables,
                                   schema_names=_DEFAULT_SCHEMA_NAMES) -> str:
    """MySQL accepts `db.alias.column`; DuckDB does not.

    Verified against MySQL 8: `select rvf_x.la.id from rvf_x.concept_s la`
    returns a row. DuckDB reads the three parts as catalog.schema.table and
    errors. Only strip when the middle part is not a real table, so genuine
    schema.table.column references are left alone.

    schema_names is parametrised for precompile(), which runs this pass while the
    schema position still holds a sentinel rather than the bound schema name. The
    default is exactly the previous hardcoded alternation, so prepare() is
    unaffected.
    """
    pattern = _alias_re(tuple(schema_names))

    def repl(m):
        schema, middle, col = m.group(1), m.group(2), m.group(3)
        if middle.lower() in known_tables:
            return m.group(0)
        return f"{middle}.{col}"
    return pattern.sub(repl, sql)


_NUMERIC_TYPE_PREFIXES = ("bigint", "int", "smallint", "tinyint", "mediumint",
                          "decimal", "numeric", "double", "float", "real")

# `numcol = ''` / `numcol <> ''`, qualified or not. Deliberately anchored on the
# empty-string literal: a non-empty literal compared to a numeric column is a
# different question (MySQL would parse a leading number out of it), and no
# assertion in either corpus does that.
_EMPTY_STRING_CMP_RE = re.compile(r"\b((?:\w+\.)?(\w+))\s*(=|!=|<>)\s*''")


def numeric_column_names(table_columns):
    """Column names the DDL declares numeric in EVERY table that has them.

    Takes the TYPE-TEXT form - {table: "id BIGINT, mapgroup SMALLINT"} - and
    raises on the name-only form, because there is a map of each shape in this
    codebase and they are interchangeable by accident: publish_store's
    column_names() gives {table: {names}} for procedure unrolling, and its
    table_columns() gives the type text. Handed the wrong one, a tolerant
    implementation would find no numeric columns, rewrite nothing, and leave the
    two assertions this exists to fix erroring exactly as before - a silent
    no-op, which is the failure mode worth being noisy about.

    Name-keyed rather than table-keyed on purpose. Resolving `a.mapGroup` to a
    table means alias scope analysis, and RF2 column names are globally
    type-consistent - mapgroup is SMALLINT wherever it appears. The all() is
    what keeps that honest: a name declared numeric in one table and VARCHAR in
    another is left alone rather than guessed at.
    """
    types = {}
    for table, spec in (table_columns or {}).items():
        if not isinstance(spec, str):
            raise TypeError(
                f"numeric_column_names needs the type-text column map, got "
                f"{type(spec).__name__} for {table}. Pass table_columns(ddl), "
                f"not column_names(ddl).")
        for col in spec.split(","):
            parts = col.strip().split(None, 1)
            if len(parts) == 2:
                types.setdefault(parts[0].lower(), set()).add(
                    parts[1].split("(")[0].strip().lower())
    return {name for name, seen in types.items()
            if all(t.startswith(_NUMERIC_TYPE_PREFIXES) for t in seen)}


def _numeric_vs_empty_string(sql: str, numeric_columns) -> str:
    """`mapGroup = ''` -> `mapGroup = 0` where mapGroup is numeric.

    MySQL coerces the empty string to 0 and compares; DuckDB refuses to cast it
    and the statement dies with "Could not convert string '' to INT16" - per
    row, so an empty table raises nothing and a populated one fails.

    That cost two assertions silently. component-centric-snapshot-complexmap-
    group.sql and its extendedmap twin ask `mapGroup IS NULL OR mapGroup = ''`
    against a SMALLINT column; they errored on every release carrying complexmap
    rows, and a release without such rows raised nothing, so both reported zero
    findings and read as passes. Found by running the corpus against a fixture
    that does have rows.

    Rewriting rather than leaving it to the corpus: `= ''` is valid MySQL and
    the corpus is upstream's. The blank it looks for loads as 0 through MySQL's
    loader, so 0 is what the assertion means.
    """
    if not numeric_columns:
        return sql

    def rewrite(m):
        return (f"{m.group(1)} {m.group(3)} 0"
                if m.group(2).lower() in numeric_columns else m.group(0))

    return _EMPTY_STRING_CMP_RE.sub(rewrite, sql)


# A statement whose tail is transaction control, because the script before it
# omitted its semicolon: `insert ... ) commit`. A STANDALONE commit is dropped
# by the split loops below, but a glued one is part of the statement and DuckDB
# fails to parse it. One assertion in the international corpus is written this
# way - release-type-snapshot-delta-validation-expression-association-refset.sql
# - and it is the only statement in the whole 360-assertion store containing the
# token, so nothing detected it until a fixture had rows in the table.
#
# Stripped rather than refused: the token is meaningless in DuckDB, which
# autocommits, and refusing would drop the assertion from the store, which the
# corpus gate would then read as a missing assertion rather than a malformed
# script. The upstream fix is the missing semicolon.
_TXN_TAIL_RE = re.compile(r"[\s;]*\b(commit|rollback|begin|start\s+transaction)\s*$", re.I)


def strip_transaction_tail(stmt: str):
    """(statement, stripped_token or None)."""
    m = _TXN_TAIL_RE.search(stmt)
    if not m or m.start() == 0:
        return stmt, None
    return stmt[:m.start()].rstrip(), m.group(1)


def to_duckdb(sql: str, numeric_columns=None) -> str:
    """Textual pre-passes applied before the sqlglot AST stage.

    Order matters: BINARY/COLLATE are stripped FIRST, because they can sit
    between a column and its REGEXP operand (`BINARY term NOT REGEXP BINARY
    '...'`) and would otherwise stop the regex rewrite from matching at all.

    NOTE: an attempt to make REGEXP/LIKE case-insensitive here - on the theory
    that MySQL's utf8_unicode_ci collation implies it - was tested against the
    MySQL engine's report (on the SNOMED CT-AU edition) and REJECTED. It fixed
    one assertion and broke four others, so the divergences are not a collation
    difference. Do not retry without new evidence.
    """
    sql = _BINARY_OP_RE.sub("", sql)
    sql = _COLLATE_RE.sub("", sql)
    # effectivetime is a zero-padded YYYYMMDD string, so lexical ordering is
    # identical to chronological ordering: dropping the cast preserves the exact
    # semantics of both max() and equality, and avoids DuckDB's stricter parser.
    sql = _CAST_DT_RE.sub(r"\1", sql)
    sql = _CAST_BINARY_RE.sub(r"\1", sql)
    # Do NOT decode MySQL string escapes here. sqlglot re-parses the statement
    # with read="mysql" and decodes them itself, so pre-decoding runs the escape
    # rules TWICE: ' \\(product name\\)$' became ' (product name)$', turning
    # literal parens into a regex group that matched nothing - which inverted a
    # NOT REGEXP and flagged all 14,696 correctly-tagged FSNs as failures.
    # sqlglot handles `x REGEXP 'p'` -> regexp_matches(x, 'p') correctly on its
    # own; the only thing it leaves broken is \Z, which RE2 rejects. Emit that
    # double-escaped so sqlglot's decode yields the \x1a RE2 understands.
    sql = sql.replace(chr(92) + "Z", chr(92) + chr(92) + "x1a")
    sql = _ENGINE_RE.sub("", sql)
    sql = _DISPLAY_WIDTH_RE.sub(r"\1", sql)
    sql = _EFFECTIVETIME_NOW_RE.sub(r"\1 \2 strftime(now(), '%Y%m%d')", sql)
    sql = _numeric_vs_empty_string(sql, numeric_columns)
    return sql


def duckdb_statement(stmt: str) -> str:
    """Full MySQL -> DuckDB conversion for one statement.

    sqlglot does the dialect work (notably MySQL's double-quoted string literals,
    which DuckDB would read as identifiers); three AST passes handle what sqlglot
    leaves alone. Anything sqlglot cannot parse falls back to the textual form
    rather than being dropped.
    """
    try:
        trees = sqlglot.parse(stmt, read="mysql")
    except Exception:
        return stmt
    trees = [t for t in trees if t is not None]
    if not trees:
        return stmt
    for tree in trees:
        _strip_indexes(tree)
        _normalise_exists(tree)
        _having_without_group_by(tree)
        _fix_loose_group_by(tree)
        _case_insensitive_on_function_results(tree)
        _unicode_posix_classes(tree)
    try:
        return ";\n".join(t.sql(dialect="duckdb") for t in trees)
    except Exception:
        return stmt


def _procedure_ctx(*, qa_result, prospective, temp=None):
    """The schema names procedures.expand needs. <TEMP> resolves to the
    prospective schema, exactly as transform() and bind() resolve it."""
    return {"qa_result": qa_result, "prospective": prospective,
            "temp": temp if temp is not None else prospective}


def _emit(stmts, s, table_columns, ctx):
    """Append one split statement, unrolling a CALL into the statements MySQL's
    procedure would have generated.

    Unrolling happens BEFORE duckdb_statement: the generated SQL is already
    DuckDB, and putting it through sqlglot again would re-quote its literals for
    no benefit.

    Without `table_columns` there is nothing to unroll a %_s loop over, so CALLs
    pass through untouched - the behaviour before unrolling existed. Callers that
    have the DDL (publish_store) should always pass it; procedures.UNPORTED then
    reports any CALL that still has no port.
    """
    if table_columns and procedures.parse_call(s):
        expanded = procedures.expand(s, table_columns, ctx)
        if expanded is not None:
            stmts.extend(expanded)
            return
    stmts.append(duckdb_statement(s))


def prepare(raw_sql: str, dialect: str, known_tables=None,
            table_columns=None, numeric_columns=None, **kw):
    """Full pipeline -> list of executable statements for the given dialect.

    The single-phase path, kept as the reference implementation: precompile()/
    bind() below must produce byte-identical output for dialect="duckdb", and
    precompile_parity.py proves it over the whole corpus. Do not "simplify" this
    into a wrapper around them - a shared bug would then cancel out in the
    comparison and the parity test would go green on wrong output.
    """
    sql = importer_rewrite(raw_sql)
    sql = transform(sql, **kw)
    if dialect == "duckdb":
        if known_tables:
            sql = strip_schema_qualified_aliases(sql, known_tables)
        sql = to_duckdb(sql, numeric_columns)
    stmts = []
    source = (split_with_delimiters(sql) if _DELIMITER_RE.search(sql)
              else split_statements_routine_aware(sql))
    for s in source:
        s, txn = strip_transaction_tail(s)
        low = s.strip().lower()
        if not low:
            continue
        if dialect == "duckdb" and low in ("commit", "begin", "start transaction"):
            continue  # DuckDB autocommits; a bare COMMIT errors
        if dialect == "duckdb":
            if _ALTER_INDEX_RE.match(s) or _CREATE_INDEX_RE.match(s):
                continue  # no secondary indexes in the columnar architecture
            if _MYSQL_SET_RE.match(s) or _SET_ENGINE_RE.match(s):
                continue  # MySQL-only tuning knob
            if _ROUTINE_DEF_RE.match(s):
                continue  # replaced by procedures.REGISTRY / store ports
            _emit(stmts, s, table_columns,
                  _procedure_ctx(qa_result=kw["qa_result"],
                                 prospective=kw["prospective"]))
            continue
        stmts.append(s)
    return stmts


# ------------------------------- publish-time transpilation (precompile/bind)
#
# Transpilation is a pure function of the assertion TEXT: nothing in stage 3
# consults the run id, the assertion uuid or the schema names. Yet prepare() runs
# it on every assertion of every run, and the argument for moving it is NOT
# speed - it is a fraction of a second for a whole corpus, nothing against a
# run dominated by assertion SQL execution. The argument is that a per-run
# transpile has no artefact and no gate: duckdb_statement's parse-failure
# fallback silently hands back untranspiled MySQL, which then fails (or worse,
# succeeds differently) at run time, once per run, invisibly. Transpiled once at
# publish time it is a reviewable artefact, diffable across corpus versions, and
# a fallback is discovered when the assertion is published rather than in a
# validation report.
#
# The obstacle is ordering: prepare() substitutes the placeholders BEFORE the
# sqlglot stage, so to move transpilation earlier the placeholders have to
# survive `sqlglot.parse`. They cannot survive as written - `<PROSPECTIVE>`
# lexes as a less-than operator followed by an identifier and the parse fails
# (which, thanks to duckdb_statement's fallback, would silently return every
# assertion untranspiled: a whole-corpus regression with no error message).
#
# So each placeholder is swapped for a SENTINEL that is lexically inert, and
# bind() swaps the sentinel for the run's value with plain str.replace - no
# sqlglot, no regex engine on the SQL structure. Two properties make that safe:
#
#   1. The substitution is a textual identity on statement boundaries, so doing
#      it after the split gives the same result as prepare() doing it before.
#      No bound value contains a quote or a semicolon (run id, uuid, schema
#      name), so the quote-aware splitter cannot land differently either.
#   2. duckdb_statement()'s parse-failure fallback returns the statement text
#      unchanged, so a statement sqlglot cannot handle still carries its
#      sentinels and bind() still fixes it up. The fallback keeps working.
#
# Sentinel form, per placeholder position:
#
#   <RUNID>              -> a NUMERAL. It is the one placeholder that can sit in
#                           a numeric position (qa_result.run_id is BIGINT, and
#                           the corpus writes it both as '<RUNID>' and bare). A
#                           numeral keeps the AST node type the bound value
#                           would have had - exp.Literal(number), not
#                           exp.Column - which matters because _fix_loose_group_by
#                           wraps ungrouped COLUMNS in ANY_VALUE. An identifier
#                           sentinel in `select <RUNID>, x ... group by x` would
#                           get wrapped and the bound output would differ.
#                           Inside single quotes ('<RUNID>') a digit run is just
#                           as inert; the numeral covers both positions.
#   schema positions     -> a bare IDENTIFIER (<PROSPECTIVE>, <TEMP>,
#                           <PREVIOUS>, <DEPENDENCY>). `sent.concept_s` parses as
#                           db.table exactly as `prospective.concept_s` does, and
#                           sqlglot emits unquoted identifiers verbatim.
#                           Identifier shape also keeps the textual pre-passes in
#                           to_duckdb() firing in the same places: _CAST_DT_RE
#                           matches `cast(x as datetime)` only when x is
#                           [\w.`"]+, which `prospective.effectivetime` satisfies
#                           and `<PROSPECTIVE>.effectivetime` does not. Keeping
#                           the angle brackets would have silently disabled that
#                           pass.
#   <ASSERTIONUUID>,     -> bare identifiers too. These sit inside quotes in
#   <MODULEID(S)>,          practice, where anything without a backslash or quote
#   <INCLUDED_MODULES>,     rides through as a string literal untouched; the
#   <VERSION>               identifier shape is what makes them survive if an
#                           assertion ever uses them unquoted.
#   qa_result            -> NO sentinel. It is already a bare identifier, so it
#                           transpiles as itself and bind() applies the same
#                           \bqa_result\b rewrite transform() does.
#   <SNAPSHOT>/<DELTA>/  -> NOT sentinels: bound here, at precompile time. They
#   <FULL>                  are constants ("s"/"d"/"f") with no run-specific
#                           input, and binding them early is not just tidier, it
#                           is required. They occur as identifier SUFFIXES
#                           (concept_<SNAPSHOT>), and strip_schema_qualified_
#                           aliases decides whether to strip a three-part
#                           reference by testing the middle part for membership
#                           in known_tables. With a sentinel in the suffix the
#                           middle part reads `concept_rvfph_snapshot_`, which is
#                           not in known_tables, so `prospective.concept_s.id`
#                           would be stripped when prepare() leaves it alone.
#                           Same reasoning applies to the [[:<:]]/[[:>:]] word-
#                           boundary rewrite, also constant, also done here.
#
# Every IDENTIFIER sentinel ends in "_" as a terminator. Without it <MODULEID>'s
# sentinel would be a prefix of <MODULEIDS>'s and binding the first would corrupt
# the second into `moduleid IN ()` - the angle brackets provided that separation
# for free, and dropping them means providing it explicitly. (Confirmed by
# mutation: precompile_parity.py catches its removal.) The numeral needs no
# terminator, being a substring of no other sentinel.
#
# precompile() refuses text that already contains a sentinel rather than
# mis-bind it.
#
# NOT reintroduced here, both previously tested and rejected (see to_duckdb):
# pre-decoding MySQL string escapes, and global regex case-insensitivity.
# precompile() calls the same to_duckdb()/duckdb_statement() as prepare(), so
# there is exactly one copy of those rules and no opportunity to re-litigate.

_SENTINEL_RUNID = "424242424242424242"

# Ordered exactly as transform() applies them, so the two paths cannot diverge
# on overlapping tokens.
_SENTINELS = (
    ("<RUNID>", _SENTINEL_RUNID),
    ("<ASSERTIONUUID>", "rvfph_assertionuuid_"),
    ("<MODULEID>", "rvfph_moduleid_"),
    ("<MODULEIDS>", "rvfph_moduleids_"),
    ("<INCLUDED_MODULES>", "rvfph_included_modules_"),
    ("<VERSION>", "rvfph_version_"),
    ("<PROSPECTIVE>", "rvfph_prospective_"),
    ("<TEMP>", "rvfph_temp_"),
    ("<PREVIOUS>", "rvfph_previous_"),
    ("<DEPENDENCY>", "rvfph_dependency_"),
)
_SENTINEL_BY_PLACEHOLDER = dict(_SENTINELS)

# The schema-position sentinels, for strip_schema_qualified_aliases.
_SENTINEL_SCHEMAS = tuple(
    _SENTINEL_BY_PLACEHOLDER[p]
    for p in ("<PROSPECTIVE>", "<TEMP>", "<PREVIOUS>", "<DEPENDENCY>"))


def _install_sentinels(sql: str) -> str:
    """transform(), with sentinels in place of the run's values.

    Mirrors transform() statement for statement and in the same order, minus the
    two things bind() must do instead (qa_result, and the conditional <PREVIOUS>/
    <DEPENDENCY> substitution) and plus the constants it can do now.
    """
    for placeholder, sentinel in _SENTINELS:
        if sentinel in sql:
            # A corpus that already contains a sentinel would be silently
            # mis-bound, so refuse rather than guess.
            raise ValueError(
                f"sentinel {sentinel!r} occurs in the assertion text; "
                "precompile cannot distinguish it from a placeholder")
        sql = sql.replace(placeholder, sentinel)
    # Constants: no run input, so bind them now (see the note above on why
    # <SNAPSHOT> in particular must not survive as a sentinel).
    sql = sql.replace("<SNAPSHOT>", "s").replace("<DELTA>", "d") \
             .replace("<FULL>", "f")
    # A constant, exactly like <SNAPSHOT>: MySqlQueryTransformer substitutes a
    # compile-time literal with no run input, so binding it here rather than
    # carrying a sentinel keeps bind() and transform() in step.
    sql = sql.replace("<INTERNATIONAL_MODULES>", SCTID_CORE_MODULE)
    sql = sql.replace("[[:<:]]", r"\\b").replace("[[:>:]]", r"\\b")
    return sql


def precompile(raw_sql: str, known_tables=None, table_columns=None,
               numeric_columns=None):
    """Stage 1-3 with no run-specific input -> DuckDB statements with sentinels.

    Run once per assertion at PUBLISH time. The result is stable for the lifetime
    of the assertion text and is the input to bind().

    known_tables is not a run input: it is the fixed set of RF2 table names in
    the schema (create-tables-mysql.sql plus any tables a pre-requisites script
    creates), so it is available at publish time. It only decides whether a
    three-part reference is schema.table.column or schema.alias.column.

    Returns a LIST of statements, not one blob. Deliberate: re-splitting
    transpiled DuckDB output is not safe (a CREATE PROCEDURE body keeps its
    internal semicolons, and generated string literals can contain them), so the
    split must happen once, here. Serialise the list - JSON, or one row per
    statement - rather than joining and re-splitting it later.
    """
    sql = importer_rewrite(raw_sql)
    sql = strip_comments(sql)          # transform() does this first; so do we
    sql = _install_sentinels(sql)
    if known_tables:
        sql = strip_schema_qualified_aliases(sql, known_tables,
                                             schema_names=_SENTINEL_SCHEMAS)
    sql = to_duckdb(sql, numeric_columns)
    stmts = []
    source = (split_with_delimiters(sql) if _DELIMITER_RE.search(sql)
              else split_statements_routine_aware(sql))
    for s in source:
        s, txn = strip_transaction_tail(s)
        if txn:
            # Not silent: the script is malformed and upstream should fix it.
            print(f"rvfsql: stripped a trailing {txn.upper()} from a statement - "
                  f"the script is missing a semicolon before it", file=sys.stderr)
        low = s.strip().lower()
        if not low:
            continue
        if low in ("commit", "begin", "start transaction"):
            continue  # DuckDB autocommits; a bare COMMIT errors
        if _ALTER_INDEX_RE.match(s) or _CREATE_INDEX_RE.match(s):
            continue  # no secondary indexes in the columnar architecture
        if _MYSQL_SET_RE.match(s) or _SET_ENGINE_RE.match(s):
            continue  # MySQL-only tuning knob
        if _ROUTINE_DEF_RE.match(s):
            continue  # replaced by procedures.REGISTRY / store ports
        # A CALL unrolls into the per-table statements MySQL would have
        # generated. Doing it HERE rather than at run time is what lets a
        # DuckDB runtime with no procedure mechanism - including the Java one -
        # execute a stored-procedure assertion: the loop over the snapshot
        # tables is over a fixed DDL set, so it is not run-specific input.
        # The sentinels sit in the generated SQL untouched, and bind() rewrites
        # them there exactly as it does anywhere else.
        _emit(stmts, s, table_columns,
              _procedure_ctx(
                  qa_result="qa_result",
                  prospective=_SENTINEL_BY_PLACEHOLDER["<PROSPECTIVE>"],
                  temp=_SENTINEL_BY_PLACEHOLDER["<TEMP>"]))
    return stmts


def bind(precompiled, *, run_id, assertion_id, prospective, previous=None,
         dependency=None, qa_result, module_id=DEFAULT_MODULE_ID,
         module_ids="", version=DEFAULT_VERSION):
    """Substitute one run's values into precompile() output. Textual only.

    Same keyword arguments as transform(), and the same value rules - including
    that <PREVIOUS>/<DEPENDENCY> are left as the literal placeholder when no such
    release is configured. That is not an oversight being propagated: it is what
    transform() does, so an assertion referencing a release that is absent fails
    the same way it does on MySQL instead of silently querying the wrong schema.
    A runtime filters those assertions out beforehand, using needs().
    """
    out = []
    for stmt in precompiled:
        s = stmt
        s = s.replace(_SENTINEL_RUNID, str(run_id))
        s = s.replace(_SENTINEL_BY_PLACEHOLDER["<ASSERTIONUUID>"],
                      str(assertion_id))
        s = s.replace(_SENTINEL_BY_PLACEHOLDER["<MODULEID>"], module_id)
        s = s.replace(_SENTINEL_BY_PLACEHOLDER["<MODULEIDS>"], module_ids)
        s = s.replace(_SENTINEL_BY_PLACEHOLDER["<INCLUDED_MODULES>"],
                      module_ids if module_ids else "NULL")
        s = s.replace(_SENTINEL_BY_PLACEHOLDER["<VERSION>"], version)
        s = re.sub(r"\bqa_result\b", qa_result, s)
        s = s.replace(_SENTINEL_BY_PLACEHOLDER["<PROSPECTIVE>"], prospective)
        s = s.replace(_SENTINEL_BY_PLACEHOLDER["<TEMP>"], prospective)
        s = s.replace(_SENTINEL_BY_PLACEHOLDER["<PREVIOUS>"],
                      previous if previous else "<PREVIOUS>")
        s = s.replace(_SENTINEL_BY_PLACEHOLDER["<DEPENDENCY>"],
                      dependency if dependency else "<DEPENDENCY>")
        out.append(s)
    return out
