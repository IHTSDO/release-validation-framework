#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Self-test for the stored-procedure ports.

These generators replace MySQL procedural code. Several of the assertions that
call them rarely produce findings on real releases, so agreement with the MySQL
engine's report on a release says little about them. This file is the safety
net instead.

Every case is one the MySQL body's behaviour can be read off directly, and the
two DELIBERATE deviations are asserted as deviations, so that if someone later
"fixes" them the test says so rather than going quietly green.

Usage:  procedures_selftest.py [--ddl create-tables-mysql.sql]
Exit status is 1 on any failure, so it can gate a build.
"""
import argparse
import pathlib
import sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import pins
pins.require()

import duckdb

import procedures
import rf2map

# Where this sits in RVF: tools/duck-publisher/.
DDL = (pathlib.Path(__file__).resolve().parent.parent.parent
       / "src" / "main" / "resources" / "sql" / "create-tables-mysql.sql")
CTX = {"qa_result": "qa_result", "prospective": "curr", "previous": "prev",
       "temp": "curr"}

FAILURES = []


def check(name, got, want):
    if got != want:
        FAILURES.append(f"{name}\n     got:  {got!r}\n     want: {want!r}")
        print(f"  FAIL {name}")
    else:
        print(f"  ok   {name}")


def fresh():
    con = duckdb.connect()
    con.execute("SET old_implicit_casting=true")
    for s in ("curr", "prev"):
        con.execute(f"CREATE SCHEMA {s}")
    con.execute("""CREATE TABLE qa_result(
        run_id BIGINT, assertion_id VARCHAR, concept_id BIGINT, details VARCHAR,
        component_id VARCHAR, table_name VARCHAR, skip_module_check VARCHAR)""")
    return con


def run(con, stmts):
    for s in stmts:
        con.execute(s)


def details(con):
    return [r[0] for r in con.execute(
        "select details from qa_result order by details").fetchall()]


# ---------------------------------------------------------------- module id

def test_validate_module_id():
    con = fresh()
    con.execute("CREATE TABLE curr.concept_s(id BIGINT, moduleid BIGINT)")
    con.execute("INSERT INTO curr.concept_s VALUES (1, 111), (2, 999)")
    con.execute("CREATE TABLE curr.module_id(moduleid BIGINT)")
    con.execute("INSERT INTO curr.module_id VALUES (111)")
    tables = {"concept_s": {"id", "moduleid"}, "module_id": {"moduleid"}}
    run(con, procedures.validate_module_id(tables, CTX, "curr", 7, "A-1"))
    check("validate_module_id flags only the undeclared module", details(con),
          ["concept ::id= 2 ::module id: 999 is not in module dependency list"])
    check("validate_module_id carries run_id and assertion_id",
          con.execute("select distinct run_id, assertion_id from qa_result")
             .fetchall(), [(7, "A-1")])


def test_validate_refset_id_takes_min_id_per_group():
    """MySQL groups by refsetid and takes min(id), so a refset with 500 bad
    members produces ONE row, not 500. Easy to 'improve' by accident."""
    con = fresh()
    con.execute("CREATE TABLE curr.simplerefset_s(id BIGINT, refsetid BIGINT)")
    con.execute("INSERT INTO curr.simplerefset_s VALUES (5, 777), (3, 777), (9, 111)")
    con.execute("CREATE TABLE curr.refset_id(refsetid BIGINT)")
    con.execute("INSERT INTO curr.refset_id VALUES (111)")
    tables = {"simplerefset_s": {"id", "refsetid"}, "refset_id": {"refsetid"}}
    run(con, procedures.validate_refset_id(tables, CTX, "curr", 7, "A-1"))
    check("validate_refset_id emits one row per refsetid, id = min(id)",
          details(con),
          ["simplerefset ::id= 3 ::refset id: 777 is not in refset descriptor list"])


def test_only_refset_tables_are_scanned_for_refset_id():
    tables = {"concept_s": {"id", "refsetid"},
              "simplerefset_s": {"id", "refsetid"},
              "mrcmDomainRefset_s": {"id", "refsetid"}}
    got = procedures.validate_refset_id(tables, CTX, "curr", 1, "A")
    check("validate_refset_id skips non-refset tables", len(got), 2)


# --------------------------------------------------- inactivated component

def _inactivated_fixture():
    con = fresh()
    cols = "(id BIGINT, effectivetime VARCHAR, active VARCHAR, moduleid BIGINT)"
    for t in ("langrefset_d", "relationship_d", "concept_d"):
        con.execute(f"CREATE TABLE curr.{t}{cols}")
    for t in ("langrefset_s", "relationship_s", "concept_s"):
        con.execute(f"CREATE TABLE prev.{t}{cols}")
    return con


def test_inactivated_component_module():
    con = _inactivated_fixture()
    # flagged: inactivated, module moved, effectivetime not older
    con.execute("INSERT INTO curr.langrefset_d VALUES (10,'20260831','0',222)")
    con.execute("INSERT INTO prev.langrefset_s VALUES (10,'20260731','1',111)")
    # not flagged: same module
    con.execute("INSERT INTO curr.langrefset_d VALUES (11,'20260831','0',111)")
    con.execute("INSERT INTO prev.langrefset_s VALUES (11,'20260731','1',111)")
    # not flagged: still active in the prospective release
    con.execute("INSERT INTO curr.langrefset_d VALUES (12,'20260831','1',222)")
    con.execute("INSERT INTO prev.langrefset_s VALUES (12,'20260731','1',111)")
    # not flagged: the prospective row PREDATES the previous one
    con.execute("INSERT INTO curr.langrefset_d VALUES (13,'20260630','0',222)")
    con.execute("INSERT INTO prev.langrefset_s VALUES (13,'20260731','1',111)")
    # inferred relationships are excluded outright, so this must NOT appear
    con.execute("INSERT INTO curr.relationship_d VALUES (20,'20260831','0',222)")
    con.execute("INSERT INTO prev.relationship_s VALUES (20,'20260731','1',111)")
    tables = {t: {"id", "effectivetime", "active", "moduleid"} for t in
              ("langrefset_d", "langrefset_s", "relationship_d",
               "relationship_s", "concept_d", "concept_s")}
    run(con, procedures.validate_inactivated_component_module(
        tables, CTX, "curr", "prev", 7, "A-1"))
    check("inactivated-component-module flags exactly the module move",
          details(con),
          ["Language refset member: id= 10 was inactivated in module 222 "
           "but was previously published in module 111."])
    check("inactivated-component-module sets skip_module_check",
          con.execute("select distinct skip_module_check from qa_result").fetchall(),
          [("1",)])
    check("inactivated-component-module records the qualified table",
          con.execute("select distinct table_name from qa_result").fetchall(),
          [("curr.langrefset_d",)])


def test_inferred_relationship_is_the_only_exclusion():
    tables = {t: {"id", "effectivetime", "active", "moduleid"} for t in
              ("relationship_d", "relationship_s",
               "stated_relationship_d", "stated_relationship_s")}
    got = procedures.validate_inactivated_component_module(
        tables, CTX, "curr", "prev", 1, "A")
    check("stated_relationship is NOT excluded with relationship", len(got), 1)
    check("and it is the stated one that survives",
          "stated_relationship_d" in got[0], True)


# ------------------------------------------------------------- findAncestors

def _ancestor_fixture(edges):
    con = fresh()
    con.execute("CREATE TABLE curr.relationship_s"
                "(sourceid BIGINT, destinationid BIGINT, active VARCHAR, typeid BIGINT)")
    for src, dst in edges:
        con.execute("INSERT INTO curr.relationship_s VALUES (?,?,'1',116680003)",
                    [src, dst])
    con.execute("CREATE TABLE curr.attributedescription_and_attributetype_"
                "concept_ids(concept_id BIGINT)")
    return con


def test_find_ancestors_linear_chain():
    con = _ancestor_fixture([(1, 2), (2, 3), (3, 4)])
    con.execute("INSERT INTO curr.attributedescription_and_attributetype_"
                "concept_ids VALUES (1)")
    run(con, procedures.find_ancestors({}, CTX))
    parents = con.execute("select parents from ancestors").fetchone()[0]
    check("find_ancestors walks the whole chain",
          sorted(parents.split(",")), ["", "2", "3", "4"])
    check("find_ancestors keeps MySQL's leading comma",
          parents.startswith(","), True)


def test_find_ancestors_no_parents_is_empty_string():
    con = _ancestor_fixture([(9, 8)])
    con.execute("INSERT INTO curr.attributedescription_and_attributetype_"
                "concept_ids VALUES (5)")
    run(con, procedures.find_ancestors({}, CTX))
    check("a concept with no parents still gets a row, with ''",
          con.execute("select concept_id, parents from ancestors").fetchall(),
          [(5, "")])


def test_find_ancestors_follows_every_parent():
    """DELIBERATE deviation. MySQL assigns the parent with a scalar subquery, so
    two active parents raise ER_SUBQUERY_NO_1_ROW and kill the assertion. The
    CTE follows both. If this ever asserts one parent again, the port has been
    'corrected' back into the upstream defect."""
    con = _ancestor_fixture([(1, 2), (1, 3), (2, 4)])
    con.execute("INSERT INTO curr.attributedescription_and_attributetype_"
                "concept_ids VALUES (1)")
    run(con, procedures.find_ancestors({}, CTX))
    parents = con.execute("select parents from ancestors").fetchone()[0]
    check("find_ancestors follows every parent, not just one",
          sorted(x for x in parents.split(",") if x), ["2", "3", "4"])


def test_find_ancestors_terminates_on_a_cycle():
    """MySQL's WHILE loop would spin forever. UNION in a recursive CTE stops."""
    con = _ancestor_fixture([(1, 2), (2, 1)])
    con.execute("INSERT INTO curr.attributedescription_and_attributetype_"
                "concept_ids VALUES (1)")
    run(con, procedures.find_ancestors({}, CTX))
    check("a cyclic IS-A terminates",
          sorted(x for x in con.execute("select parents from ancestors")
                 .fetchone()[0].split(",") if x), ["1", "2"])


def test_find_ancestors_ignores_inactive_and_non_isa():
    con = fresh()
    con.execute("CREATE TABLE curr.relationship_s"
                "(sourceid BIGINT, destinationid BIGINT, active VARCHAR, typeid BIGINT)")
    con.execute("INSERT INTO curr.relationship_s VALUES "
                "(1,2,'0',116680003), (1,3,'1',999999)")
    con.execute("CREATE TABLE curr.attributedescription_and_attributetype_"
                "concept_ids(concept_id BIGINT)")
    con.execute("INSERT INTO curr.attributedescription_and_attributetype_"
                "concept_ids VALUES (1)")
    run(con, procedures.find_ancestors({}, CTX))
    check("inactive and non-IS-A relationships are not ancestors",
          con.execute("select parents from ancestors").fetchone()[0], "")


# ------------------------------------------------------ generation-time rules

def test_identifier_table_is_skipped():
    """identifier_s has no `id` column (its key is alternateIdentifier), so
    MySQL's generated `select id, moduleid from identifier_s` raises Unknown
    column - inside a procedure with no handler, which aborts the CALL and
    silently drops every table the cursor had not reached yet. Skipping the
    table is the only outcome that produces a complete result.

    Run over the real DDL. A DDL that does not declare the RF2 Identifier file
    gets its columns added, so the rule is tested either way."""
    ddl = rf2map.mysql_ddl(DDL)
    tables = {t: {c for c, _ in rf2map.columns_from_ddl(v)}
              for t, v in ddl.items()}
    assert "concept_s" in tables, f"{DDL} declares no concept_s - wrong file?"
    tables.setdefault("identifier_s", {
        "identifierschemeid", "alternateidentifier", "effectivetime", "active",
        "moduleid", "referencedcomponentid"})
    check("identifier_s really lacks an id column",
          "id" in tables["identifier_s"], False)
    got = procedures.validate_module_id(tables, CTX, "curr", 1, "A")
    n_s = sum(1 for t in tables if t.endswith("_s"))
    check("validate_module_id covers every _s table but identifier_s",
          len(got), n_s - 1)
    check("and identifier_s appears in none of the generated statements",
          any("identifier_s" in s for s in got), False)


def test_unported_call_is_recorded_not_raised():
    procedures.UNPORTED.clear()
    got = procedures.expand("call TableIsPopulated('x')", {"concept_s": {"id"}}, CTX)
    check("an unported CALL returns None so the caller emits it unchanged",
          got, None)
    check("and is recorded for the publish-time report",
          dict(procedures.UNPORTED), {"tableispopulated": 1})
    procedures.UNPORTED.clear()


def test_ported_elsewhere_call_expands_to_nothing():
    # An EMPTY LIST, not None: "handled, and needing no statements". None would
    # make the caller emit the CALL unchanged, which is a statement guaranteed to
    # fail - and one permanently-failing setup statement makes a setup failure
    # count of 1 look normal, which is how a real regression hides.
    check("nothing is ported elsewhere by default", procedures.PORTED_ELSEWHERE, set())
    procedures.UNPORTED.clear()
    procedures.PORTED_ELSEWHERE.add("build_closure")
    try:
        check("a CALL ported elsewhere expands to nothing at all",
              procedures.expand("CALL build_Closure()", {"concept_s": {"id"}}, CTX), [])
        check("and is not reported as a gap", dict(procedures.UNPORTED), {})
    finally:
        procedures.PORTED_ELSEWHERE.discard("build_closure")


def test_parse_call_arguments():
    check("quoted and bare args both parse",
          procedures.parse_call(
              "call validate_module_id('sch',424242,'uuid-1')"),
          ("validate_module_id", ["sch", "424242", "uuid-1"]))
    check("a multi-line CALL parses",
          procedures.parse_call("call f(\n 'a',\n 'b'\n)"), ("f", ["a", "b"]))
    check("a non-CALL is not a CALL", procedures.parse_call("select 1"), None)


def main():
    global DDL
    ap = argparse.ArgumentParser()
    ap.add_argument("--ddl", type=pathlib.Path, default=DDL,
                    help="RVF's create-tables-mysql.sql (default: %(default)s)")
    DDL = ap.parse_args().ddl
    tests = [v for k, v in sorted(globals().items()) if k.startswith("test_")]
    for t in tests:
        print(f"{t.__name__}:")
        t()
    print()
    if FAILURES:
        print(f"FAILED {len(FAILURES)} check(s):")
        for f in FAILURES:
            print(f"  - {f}")
        return 1
    print(f"all checks passed ({len(tests)} tests)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
