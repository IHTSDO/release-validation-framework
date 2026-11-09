# SPDX-License-Identifier: Apache-2.0
"""Host-language replacements for RVF's MySQL stored procedures.

DuckDB has no procedural SQL. That is not the obstacle it looks like: these
procedures are not data logic. Each one loops `information_schema.tables` and
builds a string of dynamic SQL per table, which means the procedural part exists
only to work around MySQL's lack of a set-based way to say "every snapshot
table". The loop is over a FIXED table set - create-tables-mysql.sql is what
RVF creates for every release, and nothing in the corpus or the pre-requisites
creates another table ending in `_s` or `_d` (verified) - so the loop can be
unrolled before the run rather than during it.

So these are code GENERATORS, not executors:

    fn(tables, ctx, *call_args) -> [sql, ...]

`tables` is {table_name: {column, ...}} from the DDL. `ctx` carries the schema
and qa_result names. Both are known at PUBLISH time, which is the point: the
statements land in the precompiled store and any runtime just executes SQL. It
needs no procedure mechanism, and there is one implementation rather than one
per language.

Each generator reproduces the SQL MySQL would have generated statement for
statement, including the `details` text, so results are directly comparable.
Two deliberate deviations, both documented at the call site:
  * the assertion id is emitted QUOTED. MySQL interpolates it bare and gets away
    with it because RVF binds <ASSERTIONUUID> to a numeric assertion id
    (AssertionExecutionService: String.valueOf(assertion.getAssertionId())).
    Quoting works for either, and qa_result.assertion_id is VARCHAR here.
  * `cast(effectivetime as datetime)` is dropped. effectivetime is char(8)
    zero-padded YYYYMMDD, so lexical order IS chronological order - the same
    reasoning (and the same conclusion) as rvfsql.to_duckdb's _CAST_DT_RE pass,
    which cannot reach this generated SQL because it runs earlier.
"""
import re

def macros(schema: str = "prospective"):
    """The corpus's MySQL scalar CREATE FUNCTIONs, as DuckDB MACROs.

    Schema-qualified because a DuckDB cursor starts a fresh session, so a macro
    created into whatever search_path happened to be set is not reliably visible
    to the connection that calls it.

    cleanExpression is the only one: it is a pure string rewrite (it turns ( ) =
    and : into commas so an expression can be split on one delimiter), which is
    a MACRO one-liner. validate_snomedct_expression_concepts calls it, so if this
    is not installed that assertion fails with "Function cleanExpression does not
    exist" - which reads like a port bug and is a provisioning one.
    """
    return [f"""CREATE OR REPLACE MACRO {schema}.cleanExpression(str) AS
  replace(replace(replace(replace(str,'(',''),')',''),'=',','),':',',')"""]


def _tables(tables, suffix, name_filter=None, need_cols=()):
    """The DDL tables MySQL's `table_name like '%\\_s'` cursor would have found.

    Sorted, so the generated statement list is stable build to build; MySQL's
    information_schema order is unspecified and the insert order into qa_result
    does not affect any result.
    """
    out = []
    for t in sorted(tables):
        if not t.endswith(suffix):
            continue
        if name_filter and not name_filter(t):
            continue
        cols = {c.lower() for c in tables[t]}
        if need_cols and not all(c.lower() in cols for c in need_cols):
            continue
        out.append(t)
    return out


def _component(table, suffix):
    """MySQL's substring_index(tb_name,'_s',1) - split on the FIRST '_s'.

    Identical to stripping the suffix for all 60 tables in the DDL (no RF2 table
    name contains '_s' or '_d' before its own suffix), but written as MySQL wrote
    it so a future table name that does differ diverges visibly rather than
    silently.
    """
    head = table.split(suffix, 1)[0]
    return head


# ---------------------------------------------------------------------------
# %_s loops: one INSERT per snapshot table.

def validate_module_id(tables, ctx, dbname, runid, assertionid):
    """Flags rows whose moduleid is absent from the caller-built `module_id`."""
    schema = dbname.split(".")[0]
    out = []
    for t in _tables(tables, "_s", need_cols=("id", "moduleid")):
        comp = _component(t, "_s")
        out.append(
            f"insert into {ctx['qa_result']} (run_id, assertion_id, concept_id, details) "
            f"select {int(runid)}, '{assertionid}', 0, "
            f"concat('{comp}', ' ::id= ', a.id, ' ::module id: ', a.moduleid, "
            f"' is not in module dependency list') "
            f"from (select id, moduleid from {schema}.{t} "
            f"where moduleid not in (select moduleid from {schema}.module_id)) a")
    return out


def validate_refset_id(tables, ctx, dbname, runid, assertionid):
    """Snapshot refset tables only; one row per offending refsetid, because
    MySQL takes min(id) per group rather than every member."""
    schema = dbname.split(".")[0]
    out = []
    for t in _tables(tables, "_s", lambda n: "refset" in n.lower(),
                     need_cols=("id", "refsetid")):
        comp = _component(t, "_s")
        out.append(
            f"insert into {ctx['qa_result']} (run_id, assertion_id, concept_id, details) "
            f"select {int(runid)}, '{assertionid}', 0, "
            f"concat('{comp}', ' ::id= ', a.id, ' ::refset id: ', a.refsetid, "
            f"' is not in refset descriptor list') "
            f"from (select min(id) as id, refsetid from {schema}.{t} "
            f"where refsetid not in (select refsetid from {schema}.refset_id) "
            f"group by refsetid) a")
    return out


def validate_stand_alone_product_module_id(tables, ctx, dbname, runid, assertionid,
                                           international_modules):
    """Flags any snapshot row carrying an International module id.

    NOTE for upstream: `international_modules` is bound by MySqlQueryTransformer
    to RF2Constants.SCTID_CORE_MODULE - ONE id (900000000000207008) - even though
    RF2Constants also declares a String[] INTERNATIONAL_MODULES. So the assertion
    named "must not contain international module" never checks the model module
    900000000000012004. Reproduced as-is; flagged in UPSTREAM-FEEDBACK.
    """
    schema = dbname.split(".")[0]
    mods = international_modules.strip()
    out = []
    for t in _tables(tables, "_s", need_cols=("id", "moduleid")):
        comp = _component(t, "_s")
        out.append(
            f"insert into {ctx['qa_result']} (run_id, assertion_id, concept_id, details) "
            f"select {int(runid)}, '{assertionid}', 0, "
            f"concat('{comp}', ' ::id= ', a.id, ' ::module id: ', a.moduleid, "
            f"' must not be in Internationl modules') "
            f"from (select id, moduleid from {schema}.{t} "
            f"where moduleid in ({mods})) a")
    return out


# ---------------------------------------------------------------------------
# %_d loops.

def _changes_in_expected_modules(tables, ctx, schema, runid, assertionid):
    """Every delta table except moduledependencyrefset_d: flag changes made in a
    module outside the set the module dependency refset declares."""
    expected = (f"select distinct referencedComponentId from "
                f"{schema}.moduledependencyrefset_d where active = '1' "
                f"UNION select distinct moduleid from "
                f"{schema}.moduledependencyrefset_d where active = '1'")
    out = []
    for t in _tables(tables, "_d", need_cols=("id", "moduleid")):
        if t == "moduledependencyrefset_d":
            continue
        comp = _component(t, "_d")
        out.append(
            f"insert into {ctx['qa_result']} (run_id, assertion_id, concept_id, details) "
            f"select {int(runid)}, '{assertionid}', 0, "
            f"concat('{comp}', ' ::id= ', a.id, ' ::module id: ', a.moduleid, "
            f"' was made in the release but not in the expected set of modules') "
            f"from (select id, moduleid from {schema}.{t} "
            f"where moduleid not in ({expected})) a")
    return out


def validate_changes_in_expected_modules_edition(tables, ctx, dbname, runid,
                                                 assertionid):
    return _changes_in_expected_modules(tables, ctx, dbname.split(".")[0], runid,
                                        assertionid)


def validate_changes_in_expected_modules_extension(tables, ctx, dbname, runid,
                                                   assertionid, *rest):
    # The EDITION and EXTENSION procedures have identical bodies upstream; only
    # the assertion that calls them (and hence the group it lands in) differs.
    return _changes_in_expected_modules(tables, ctx, dbname.split(".")[0], runid,
                                        assertionid)


# Component labels for the inactivated-module message, exactly as the MySQL CASE
# lists them. Anything absent falls back to the underscore-to-space form.
_COMPONENT_LABEL = {
    "concept": "Concept",
    "description": "Description",
    "stated_relationship": "Stated relationship",
    "relationship_concrete_values": "Relationship concrete values",
    "langrefset": "Language refset member",
    "textdefinition": "Text definition",
    "simplerefset": "Simple refset member",
    "simplemaprefset": "Simple map refset member",
    "extendedmaprefset": "Extended map refset member",
    "complexmaprefset": "Complex map refset member",
    "associationrefset": "Association refset member",
    "attributevaluerefset": "Attribute value refset member",
    "moduledependencyrefset": "Module dependency refset member",
    "refsetdescriptor": "Refset descriptor member",
    "owlexpressionrefset": "OWL expression refset member",
    "expressionassociationrefset": "Expression association refset member",
    "mrcmdomainrefset": "MRCM domain refset member",
    "mrcmattributedomainrefset": "MRCM attribute domain refset member",
    "mrcmattributerangerefset": "MRCM attribute range refset member",
    "mrcmmodulescoperefset": "MRCM module scope refset member",
    "mapcorrelationoriginrefset": "Map correlation origin refset member",
    "descriptiontype": "Description type refset member",
}


def validate_inactivated_component_module(tables, ctx, prospective_dbname,
                                          previous_dbname, runid, assertionid):
    """Components inactivated in this release must stay in the module that
    previously published them.

    Called twice by its assertion - once against <PREVIOUS>, once against
    <DEPENDENCY>. When no such release is configured, bind() leaves the
    placeholder in place and the statement fails, which is what MySQL does too:
    an assertion that needs a release we do not hold must not silently pass.

    `skip_module_check` is set to 1 because the offending row is BY DEFINITION in
    an unexpected module - without it, RVF's own module filter would drop every
    finding this assertion produces.
    """
    prospective = prospective_dbname.split(".")[0]
    previous = previous_dbname.split(".")[0]
    out = []
    for t in _tables(tables, "_d", need_cols=("id", "moduleid", "effectivetime")):
        comp = t.replace("curr_", "").replace("_d", "")
        # Inferred relationships are excluded upstream: a module move on
        # inactivation is expected there, because the classifier reassigns them.
        if comp == "relationship":
            continue
        prev_t = t.replace("_d", "_s")
        if prev_t not in tables:
            continue
        label = _COMPONENT_LABEL.get(comp, comp.replace("_", " "))
        out.append(
            f"insert into {ctx['qa_result']} (run_id, assertion_id, concept_id, "
            f"details, component_id, table_name, skip_module_check) "
            f"select {int(runid)}, '{assertionid}', 0, "
            f"concat('{label}', ': id= ', a.id, ' was inactivated in module ', "
            f"a.moduleid, ' but was previously published in module ', "
            f"a.previous_moduleid, '.'), "
            f"a.id, '{prospective}.{t}', 1 "
            f"from (select t1.id, t1.moduleid, t2.moduleid as previous_moduleid "
            f"from {prospective}.{t} t1 "
            f"inner join {previous}.{prev_t} t2 on t1.id = t2.id "
            f"where t1.active = '0' and t2.active = '1' "
            f"and t1.moduleid <> t2.moduleid "
            f"and t1.effectivetime >= t2.effectivetime) a")
    return out


# ---------------------------------------------------------------------------
# Graph walks: MySQL fixpoint loops -> one recursive CTE.

def find_ancestors(tables, ctx, concept_ids_table=None, ancestors_table=None):
    """MySQL walks IS-A upward one row at a time and accumulates a comma-joined
    string per concept, which the caller then probes with LIKE '%<id>%'.

    Ported as a recursive CTE producing the same `ancestors(concept_id, parents)`
    shape, including the leading comma and the empty string for a concept with no
    parents, so the caller's LIKE behaves identically - LIKE on a joined string
    is order-independent, which is why not reproducing MySQL's depth order is
    safe.

    Two places where this is deliberately NOT bug-for-bug faithful, both making
    the DuckDB version strictly more correct:
      * MySQL assigns the parent with `SET fid = (SELECT destinationId ...)`, a
        scalar subquery, so a concept with two active parents raises ER_SUBQUERY_
        NO_1_ROW and the whole assertion dies. It survives today only because
        refset-descriptor metadata concepts happen to be single-parent. The CTE
        follows every parent, i.e. the real transitive closure.
      * MySQL's accumulator is VARCHAR(1000) and truncates silently past ~55
        ancestors; and a cyclic IS-A would spin its WHILE loop forever. UNION in
        a recursive CTE terminates and does not truncate.

    Two signatures, because the corpus has both. The original findAncestors()
    reads v_attributedescription_and_attributetype_concept_ids and writes
    `ancestors`. Since VAL-529 it is findAncestors(conceptIdsTable,
    ancestorsTable): it reads the DISTINCT non-null concept_id of the first
    table and (re)creates the second. Both names are resolved unqualified, as
    MySQL's dynamic SQL resolves them in the session's database.
    """
    p = ctx["prospective"]
    if concept_ids_table is None:
        seed = f"{ctx['temp']}.attributedescription_and_attributetype_concept_ids"
        target = "ancestors"
    else:
        for name in (concept_ids_table, ancestors_table):
            # Spliced into SQL as identifiers; anything else is not a table name.
            if not name or not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", name):
                raise ValueError(f"findAncestors: {name!r} is not a table name")
        seed = (f"(select distinct concept_id from {concept_ids_table} "
                f"where concept_id is not null)")
        target = ancestors_table
    return [f"""create or replace table {target} as
with recursive chain(concept_id, ancestor_id) as (
    select c.concept_id, r.destinationid
    from {seed} c
    join {p}.relationship_s r
      on r.sourceid = c.concept_id and r.active = '1' and r.typeid = '116680003'
    union
    select ch.concept_id, r.destinationid
    from chain ch
    join {p}.relationship_s r
      on r.sourceid = ch.ancestor_id and r.active = '1' and r.typeid = '116680003'
),
joined as (
    select concept_id, ',' || string_agg(ancestor_id::varchar, ',') as parents
    from chain group by concept_id
)
select c.concept_id, coalesce(j.parents, '') as parents
from {seed} c
left join joined j on j.concept_id = c.concept_id"""]


def find_descendants(tables, ctx, concept_ids):
    """Downward counterpart of find_ancestors. Seeds carry a NULL destinationid,
    matching MySQL's `INSERT INTO descendants(sourceid) VALUES (conceptId)`.

    No `active = '1'` filter, unlike find_ancestors, because the MySQL procedure
    has none - it joins curr_stated_relationship_s on typeid alone, so the
    descendant closure walks retired IS-A relationships where the ancestor
    closure does not. That asymmetry looks like an oversight in the corpus's
    procedure rather than a decision, but changing it here would make the port
    disagree with the MySQL engine on real content. Do not "fix" it without
    changing the corpus too.
    """
    seeds = [s.strip() for s in str(concept_ids).split(",") if s.strip()]
    if not seeds:
        return ["create or replace table descendants"
                "(sourceid bigint, destinationid bigint)"]
    values = ", ".join(f"({int(s)})" for s in seeds)
    p = ctx["prospective"]
    return [f"""create or replace table descendants as
with recursive d(sourceid, destinationid) as (
    select v.c::bigint as sourceid, null::bigint as destinationid
    from (values {values}) as v(c)
    union
    select t.sourceid, t.destinationid
    from {p}.stated_relationship_s t
    join d on t.destinationid = d.sourceid
    where t.typeid = '116680003'
)
select sourceid, destinationid from d"""]


# ---------------------------------------------------------------------------
# Expression columns: MySQL builds one dynamic INSERT per ROW to split a string.

def validate_expression_concepts(tables, ctx, run_id, assertion_id, table_name,
                                 column_name, refset_name):
    """Every concept id embedded in an expression column must be active.

    MySQL needs a cursor plus a generated-per-row INSERT purely because it has no
    string-split function: it literally builds one statement per row to explode
    the expression on spaces. DuckDB has UNNEST(string_split(...)), so this is a
    single set-based statement instead of N dynamic ones.
    """
    p = ctx["prospective"]
    tbl = table_name if "." in table_name else f"{p}.{table_name}"
    return [f"""insert into {ctx['qa_result']}
    (run_id, assertion_id, concept_id, details, component_id, table_name)
select {int(run_id)}, '{assertion_id}', result.conceptId,
       concat('{refset_name}', ':id=', result.id, ':ConceptId=',
              result.conceptId, ' referenced in the column ', '{column_name}',
              ' is not an active concept.'),
       result.id, '{table_name}'
from (
    select any_value(a.conceptId) as conceptId, a.id
    from (
        select t.id as id, cast(tok as bigint) as conceptId
        from {tbl} t,
             unnest(string_split(trim(t.{column_name}), ' ')) as u(tok)
        where t.active = '1'
          and regexp_matches(tok, '^[0-9]{{6,20}}$')
    ) a
    left join {p}.concept_s b on a.conceptId = b.id
    where b.id is null or b.active = '0'
    group by a.id
) as result"""]


def validate_snomedct_expression_concepts(tables, ctx, run_id, assertion_id):
    """LOINC ExpressionAssociation refset: every id inside the expression must be
    a known concept.

    Distinct from validate_expression_concepts: it splits cleanExpression()
    output (which turns ( ) = : into commas) rather than spaces, and does NOT
    filter to numeric tokens. MySQL compares the VARCHAR token against a BIGINT
    id, so non-numeric junk coerces to 0 and never matches; try_cast reproduces
    that (null never matches) without relying on implicit coercion.
    """
    p = ctx["prospective"]
    return [f"""insert into {ctx['qa_result']}
    (run_id, assertion_id, concept_id, details, component_id, table_name)
select {int(run_id)}, '{assertion_id}', try_cast(result.conceptId as bigint),
       concat('Concept: id=', result.conceptId,
              ' referenced in the ExpressionAssociationRefset SNAPSHOT is unknown.'),
       null, 'curr_expressionassociationrefset_s'
from (
    select distinct u.tok as conceptId
    from {p}.expressionassociationrefset_s t,
         unnest(string_split(cleanExpression(t.expression), ',')) as u(tok)
    where trim(u.tok) <> ''
) result
left join {p}.concept_s b on try_cast(result.conceptId as bigint) = b.id
where b.id is null"""]


REGISTRY = {
    "findancestors": find_ancestors,
    "finddescendants": find_descendants,
    "validateconceptidsinmrcmdomainrefsetexpression_procedure":
        validate_expression_concepts,
    "validateconceptidsinmrcmattributerangerefsetexpression_procedure":
        validate_expression_concepts,
    "validatesnomedctexpressionconcepts_procedure":
        validate_snomedct_expression_concepts,
    "validate_module_id": validate_module_id,
    "validate_refset_id": validate_refset_id,
    "validate_stand_alone_product_module_id": validate_stand_alone_product_module_id,
    "validate_inactivated_component_module": validate_inactivated_component_module,
    "validate_changes_in_expected_modules_edition":
        validate_changes_in_expected_modules_edition,
    "validate_changes_in_expected_modules_extension":
        validate_changes_in_expected_modules_extension,
}

# Procedures whose DuckDB equivalent is built elsewhere - by a statement in the
# store's `ports`. A CALL to one of these is DROPPED at publish time: it has
# nothing left to do, and emitting it anyway means shipping a statement that is
# guaranteed to fail.
#
# Keeping the CALL "so the published statement list shows what the source asked
# for" leaves a setup statement that always fails, which makes a setup failure
# count of 1 normal - and a normal-looking failure count is how a REAL
# regression hides. With this dropped, the expected count is zero and any
# failure at all can be fatal.
#
# Empty for the international corpus. A corpus whose ports stand in for one of
# its procedures names it with publish_store.py --ported-call.
PORTED_ELSEWHERE = set()

# CALLs seen with no port. Populated by expand() at publish time so a gap is
# reported rather than discovered as a run-time parse error.
UNPORTED = {}

_CALL_RE = re.compile(r"^\s*call\s+(\w+)\s*\((.*)\)\s*$", re.I | re.S)
_ARG_RE = re.compile(r"""\s*(?:'((?:[^'\\]|\\.)*)'|([^,]+))\s*(?:,|$)""")


def parse_call(stmt):
    m = _CALL_RE.match(stmt)
    if not m:
        return None
    name = m.group(1).lower()
    args = []
    for a in _ARG_RE.finditer(m.group(2)):
        if a.group(1) is not None:
            args.append(a.group(1))
        elif a.group(2) is not None and a.group(2).strip():
            args.append(a.group(2).strip())
    return name, args


def expand(stmt, tables, ctx):
    """`stmt` -> the statements MySQL's procedure would have generated.

    Returns None when `stmt` is not a CALL, and also when it is a CALL with no
    port - in which case the name is recorded in UNPORTED and the statement is
    left for the caller to emit unchanged. It then fails at run time exactly as
    it does today, which is the honest outcome: refusing to publish would take
    the other 400-odd assertions down with it, and quietly dropping the
    statement would turn a failing assertion into a passing one.

    Returns an EMPTY LIST for a CALL in PORTED_ELSEWHERE: handled, and needing no
    statements of its own.
    """
    parsed = parse_call(stmt)
    if not parsed:
        return None
    name, args = parsed
    fn = REGISTRY.get(name)
    if fn is None:
        if name in PORTED_ELSEWHERE:
            # Handled, and needing no statements: an empty list, NOT None. None
            # means "not something this module deals with", and the caller emits
            # those unchanged.
            return []
        UNPORTED[name] = UNPORTED.get(name, 0) + 1
        return None
    return fn(tables, ctx, *args)
