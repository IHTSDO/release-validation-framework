#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Execute every assertion in a store against an EMPTY release.

WHY THIS EXISTS

Publishing proves an assertion's SQL transpiles. It does not prove the result
runs. A precompiled statement can bind cleanly and still die at execution - a
column that does not exist, a function DuckDB spells differently, a relation no
DDL declares. On a real run that failure arrives hours later, inside a
validation, as an assertion that reported nothing. Nothing downstream can tell
"found no problems" from "never ran", which is the failure mode this whole
corpus exists to avoid.

So this binds and executes all of them against a schema built from the store's
own `tableColumns`, with every table present and empty. Empty data is the point:
the question is whether the statement EXECUTES, and an empty table answers it
for every assertion at once without needing a release. An assertion that
executes here returns no rows, which is NOT evidence that it finds the right
things and is not claimed to be - RVF's own tests against fixture releases are
what prove the findings themselves.

The environment mirrors what the engine builds, because a gate that fails for
its own reasons teaches nobody anything:

  * `rvf_results.qa_result` carries the same columns the engine creates,
    `skip_module_check` included - assertions insert into that column by name;
  * the store's `ports` macros are installed, which is where `substring_index`
    and `cleanexpression` come from;
  * `search_path` is the prospective release, as the engine sets it, so
    unqualified table names resolve.

    usage: pack_smoke.py <store.json>

Exit status is the number of assertions that failed to execute, so a workflow
can gate on it.
"""
import json
import pathlib
import sys

import pins
pins.require()

import duckdb

import rvfsql

# The three release schemas RVF's DuckDB engine attaches, by the literal names
# prepare()'s alias-stripping pass matches on. A realistic-looking name would
# silently skip that pass, which is a confusing way to learn the harness is
# misconfigured.
SCHEMAS = ("prospective", "previous", "dependency")
QA_RESULT = "rvf_results.qa_result"

# Exactly what DuckDbValidationService creates. Kept verbatim rather than
# summarised: an assertion that inserts into a column this omits fails here and
# passes in production, which would make this gate a source of false alarms.
QA_RESULT_COLUMNS = (
    "id BIGINT, run_id BIGINT, assertion_id VARCHAR, concept_id BIGINT, "
    "details VARCHAR, component_id VARCHAR, table_name VARCHAR, "
    "skip_module_check BOOLEAN"
)


def build_empty_release(con, store):
    """Every table the store knows about, in every release schema, with no rows."""
    con.execute("CREATE SCHEMA IF NOT EXISTS rvf_results")
    con.execute(f"CREATE TABLE IF NOT EXISTS {QA_RESULT} ({QA_RESULT_COLUMNS})")
    for schema in SCHEMAS:
        con.execute(f"CREATE SCHEMA IF NOT EXISTS {schema}")
        for table, columns in store["tableColumns"].items():
            con.execute(f"CREATE TABLE IF NOT EXISTS {schema}.{table} ({columns})")
    con.execute("SET search_path='prospective'")


def run(store_path):
    store = json.loads(pathlib.Path(store_path).read_text())
    con = duckdb.connect()
    build_empty_release(con, store)

    # Pre-requisites THEN ports, the order the engine applies them: a port may
    # build from a table a pre-requisite creates.
    setup = [s for p in store.get("prerequisites", []) for s in p["statements"]]
    setup += store.get("ports", [])
    for statement in rvfsql.bind(setup, run_id=0, assertion_id="prereq",
                                 prospective="prospective", previous="previous",
                                 dependency="dependency", qa_result=QA_RESULT):
        try:
            con.execute(statement)
        except Exception as e:                                      # noqa: BLE001
            print(f"SETUP: {type(e).__name__}: {str(e).splitlines()[0]}\n"
                  f"    {statement[:160]}", file=sys.stderr)
            return 1

    failures = []
    for uuid, assertion in sorted(store["assertions"].items()):
        bound = rvfsql.bind(
            assertion["statements"],
            run_id=424242,
            assertion_id=uuid,
            prospective="prospective",
            previous="previous",
            dependency="dependency",
            qa_result=QA_RESULT,
        )
        for statement in bound:
            try:
                con.execute(statement)
            except Exception as e:                                  # noqa: BLE001
                failures.append((assertion["file"], uuid,
                                 f"{type(e).__name__}: {str(e).splitlines()[0]}"))
                break

    # A second pass over the failures. A few assertions read a `res_*` table
    # that ANOTHER assertion creates, so in uuid order they can run before their
    # producer. Production runs them in manifest order and does not hit this.
    # Retrying once after everything else has run distinguishes an ordering
    # dependency from an assertion that cannot execute at all.
    retried = []
    for name, uuid, _ in failures:
        bound = rvfsql.bind(
            store["assertions"][uuid]["statements"],
            run_id=424242, assertion_id=uuid, prospective="prospective",
            previous="previous", dependency="dependency", qa_result=QA_RESULT)
        try:
            for statement in bound:
                con.execute(statement)
        except Exception as e:                                      # noqa: BLE001
            retried.append((name, uuid, f"{type(e).__name__}: {str(e).splitlines()[0]}"))
    if len(retried) < len(failures):
        print(f"             {len(failures) - len(retried)} passed on a second pass, "
              f"having depended on another assertion's output")
    failures = retried

    total = len(store["assertions"])
    print(f"executed     {total - len(failures)} of {total} assertions "
          f"against an empty release")
    if failures:
        print(f"\nFAILED to execute {len(failures)}:", file=sys.stderr)
        for name, uuid, error in failures:
            print(f"    {name} ({uuid})\n        {error}", file=sys.stderr)
    return len(failures)


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(__doc__, file=sys.stderr)
        raise SystemExit(2)
    raise SystemExit(run(sys.argv[1]))
