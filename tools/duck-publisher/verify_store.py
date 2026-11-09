#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Does the SERIALISED store still bind to what prepare() produces?

precompile_parity.py proves precompile()+bind() == prepare() in memory. This
proves the JSON round-trip does not change that: a store is only useful if what
comes back out is what went in.

Run it with RVF's configuration. The schema names are not release names - RVF's
DuckDB engine attaches the databases as literally "prospective"/"previous"/
"dependency", and prepare()'s alias-stripping pass matches on exactly those
three. Passing a realistic-looking name like rvf_int_20260801 makes prepare()
skip the pass and reports false divergences on the assertions that qualify an
alias with a schema. The store is right in that comparison and prepare() is
wrong, which is a confusing way to learn you configured the test badly.

    usage: verify_store.py <store.json> --scripts <corpus>/scripts --ddl <create-tables-mysql.sql>
"""
import argparse
import json
import pathlib
import sys

import publish_store
import rvfsql

ap = argparse.ArgumentParser()
ap.add_argument("store", type=pathlib.Path)
ap.add_argument("--scripts", required=True, type=pathlib.Path)
ap.add_argument("--ddl", required=True, type=pathlib.Path)
a = ap.parse_args()

store = json.loads(a.store.read_text())
# The corpus is in per-category subdirectories; the store records bare names.
by_name = {p.name: p for p in a.scripts.rglob("*.sql")}
tables = set(store["knownTables"])
# The same DDL-derived inputs the publisher passes to precompile(), so that
# procedure unrolling and the numeric '' rewrite happen on both sides.
tcols = publish_store.column_names(a.ddl)
numerics = rvfsql.numeric_column_names(publish_store.table_columns(a.ddl))

# All three releases present. With one absent, transform() leaves its literal
# <PREVIOUS>/<DEPENDENCY> placeholder in the bound text, prepare()'s sqlglot
# stage cannot parse that and returns the statement untranspiled, and the
# comparison fails on SQL that cannot execute on either path (RVF does not run
# an assertion whose release is absent). precompile_parity.py classifies it.
CFG = dict(run_id=7, prospective="prospective", previous="previous",
           dependency="dependency", qa_result="rvf_results.qa_result")
same = diff = missing = 0
bad = []
for uuid, assertion in store["assertions"].items():
    path = by_name.get(assertion["file"])
    if path is None:
        missing += 1
        continue
    raw = path.read_text(errors="replace")
    expected = rvfsql.prepare(raw, "duckdb", known_tables=tables,
                              table_columns=tcols, numeric_columns=numerics,
                              assertion_id=uuid, **CFG)
    actual = rvfsql.bind(assertion["statements"], assertion_id=uuid, **CFG)
    if expected == actual:
        same += 1
    else:
        diff += 1
        if len(bad) < 3:
            bad.append((assertion["file"], expected, actual))

print(f"assertions compared : {same + diff}")
print(f"byte-identical      : {same}")
print(f"divergent           : {diff}")
print(f"source not found    : {missing}")
for f, e, x in bad:
    print(f"\n--- {f}")
    for i, (ee, xx) in enumerate(zip(e, x)):
        if ee != xx:
            print(f"  stmt {i} expected: {ee[:160]}")
            print(f"  stmt {i} actual  : {xx[:160]}")
            break
    if len(e) != len(x):
        print(f"  statement COUNT differs: {len(e)} vs {len(x)}")
sys.exit(1 if diff or missing else 0)
