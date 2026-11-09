<!-- SPDX-License-Identifier: Apache-2.0 -->
# DuckDB assertion store publisher

Builds the assertion store, `store.json`: the SQL assertion corpus, transpiled
once from MySQL to DuckDB, for RVF's DuckDB execution engine. RVF's Maven build
runs it on every build, over the corpus it has just checked out (below).

Transpilation is the only part of that engine that needs Python. sqlglot parses
each MySQL assertion, rewrites it and prints DuckDB; everything after that is
textual substitution of a run's values (run id, assertion uuid, schema names)
into sentinels, which the engine does in Java. Precompiling keeps the dialect
work out of the server, and makes the transpiled corpus a reviewable, diffable
artefact rather than something regenerated invisibly on every run.

## Inputs and output

| | |
|---|---|
| assertion corpus | a checkout of `snomed-release-validation-assertions` (`./checkout-resources.sh` clones `develop` for a SNAPSHOT build, `master` for a release): `scripts/`, `manifest.xml`, `groups.xml` and `policies.xml` |
| DDL | `src/main/resources/sql/create-tables-mysql.sql` - table names, columns and types come from here, nowhere else |
| output | `--out`; in RVF's build `target/generated-resources/duck/store.json`, packaged as `duck/store.json` |

Only scripts `manifest.xml` declares are published, under the manifest's uuid,
text, category and severity. The international corpus also ships ~93 `.sql`
files no manifest entry names (mostly delta counterparts of snapshot
assertions); RVF never runs those on either engine, so the publisher lists them
as skipped.

## Pinned environment

Python 3.12 with the exact versions in `requirements.txt` (duckdb 1.5.5,
sqlglot 30.18.0, defusedxml 0.7.1). `pins.require()` refuses any other Python
minor version. The build creates it as `target/duck-publisher-venv`; by hand:

    uv venv --python 3.12 target/duck-publisher-venv
    uv pip install --python target/duck-publisher-venv/bin/python -r tools/duck-publisher/requirements.txt

The published statements are sqlglot's output byte for byte, so a different
sqlglot changes statements no corpus change asked for; duckdb is what the checks
execute against. Every entry point (`publish_store.py` and the four checks)
starts with `pins.require()`, which compares the installed version of every
package `requirements.txt` pins and exits non-zero, naming pinned and installed
versions, on any mismatch or missing package - before sqlglot or duckdb is
imported. Bump a pin deliberately and review the store diff that follows.

### No fallback without sqlglot

A dialect pass that imported sqlglot lazily and returned the statement
unchanged on `ImportError` would, on a machine without sqlglot, publish a store
of **untranspiled MySQL** that looks complete and is silently wrong. So
`pins.require()` fails at startup, and `rvfsql.py` imports sqlglot
unconditionally, so not even a direct import of the module can run without it.
The only fallback is for a statement sqlglot cannot *parse*;
`precompile_parity.py` and `pack_smoke.py` report those, nothing hides them.

## In RVF's build

The store is not checked in. `pom.xml` runs `build-store.sh` in
`generate-resources`, right after `checkout-resources.sh` has cloned the corpus,
so the store in the jar and the container image is always built from the
corpus commit the MySQL engine imports. The script

1. creates, or reuses, the pinned environment in `target/duck-publisher-venv`
   (`uv venv --no-python-downloads --python 3.12` when `uv` is installed,
   otherwise `python3.12 -m venv`; then `requirements.txt`);
2. deletes any previous store and runs `publish_store.py` over the checkout and
   the DDL into `target/generated-resources/duck/store.json`, which the build
   packages as `duck/store.json`.

**The build needs Python 3.12.** With neither `python3.12` nor `uv` with a 3.12
installed, the script stops the build with a message saying so; there is no
checked-in store to fall back to. A failed publish (an assertion that does not
transpile, or reads a table nothing provides) fails the build too, so a corpus
commit the DuckDB engine cannot run is found when RVF is built, not at run
time.

The publisher needs only the corpus checkout, the DDL and the pinned
environment: it opens no network connection (traced: zero `socket`/`connect`
calls in a full publish, including the `git log` it runs on the corpus to date
the pack) and writes only `--out` (plus Python's `__pycache__` unless
`PYTHONDONTWRITEBYTECODE=1`, which the script sets). A full publish of the
international corpus takes about 4 s wall (2.8 s CPU) and peaks at about 40 MB
RSS; creating the environment the first time downloads the pinned packages.
The version is the corpus commit's date, so a rebuild of the same corpus
produces the same identity.

The build also publishes a second store, from the fixed corpus commit
`duck.digest.corpus.ref` in `pom.xml` (`--ref`, into `target/duck-digest/`),
for RVF's `AssertionCorpusDigestTest`, whose per-assertion recording must not
move with every corpus commit.

## Running it by hand

To publish a pack from another corpus, or to look at a store outside the build,
from the repository root:

    ./checkout-resources.sh
    target/duck-publisher-venv/bin/python tools/duck-publisher/publish_store.py \
      --scripts       snomed-release-validation-assertions/scripts \
      --manifest-root snomed-release-validation-assertions \
      --ddl           src/main/resources/sql/create-tables-mysql.sql \
      --out           /tmp/store.json

At assertions `develop` c3a6fb27 (2026-10-13) with the current DDL this reports
`assertions 361`, `statements 791 (+0 pre-requisite)`, `ports 2` and 93
scripts "not in manifest, skipped". sqlglot prints `Unsupported property
charactersetproperty` / `collateproperty` for MySQL table options it drops; that
is expected.

The publisher exits non-zero, after writing, if any script fails to precompile
or any assertion reads a table nothing provides (below). It **refuses to write a
store with zero assertions** - that is what a wrong `--scripts` path produces,
and an empty store reports no findings and therefore passes every validation.

Then check the result:

    P=target/duck-publisher-venv/bin/python; T=tools/duck-publisher; S=snomed-release-validation-assertions/scripts
    $P $T/verify_store.py /tmp/store.json --scripts $S --ddl src/main/resources/sql/create-tables-mysql.sql
    $P $T/pack_smoke.py /tmp/store.json
    $P $T/precompile_parity.py --assertions $S
    $P $T/procedures_selftest.py

| check | proves |
|---|---|
| `verify_store.py` | the serialised store binds to exactly what the single-phase `prepare()` produces, for every assertion |
| `pack_smoke.py` | every assertion's statements execute against an empty release with the store's own `tableColumns` and `ports` |
| `precompile_parity.py` | `bind(precompile(sql)) == prepare(sql)` over the corpus and synthetic probes, for six run configurations; any divergence must fall in a documented class |
| `procedures_selftest.py` | the host-language ports of the stored procedures reproduce the MySQL bodies, including two deliberate, asserted deviations |

## What the store contains

| key | |
|---|---|
| `assertions` | uuid -> file, source `sha256` (first 16 hex), text, keywords, severity, `statements` |
| `sentinels`, `runIdSentinel`, `qaResultToken` | what a binder substitutes, as data, in application order |
| `knownTables` | DDL tables plus any tables `--prerequisites` scripts create |
| `tableColumns` | DuckDB column definitions per DDL table, so the engine can create an empty table for a file the release does not ship |
| `ports` | DuckDB setup statements run once before any assertion |
| `prerequisites` | precompiled pre-requisite scripts; empty for the international corpus |
| `grouping` | the corpus's `groups.xml` and `policies.xml`, verbatim, with their source `sha256`; RVF resolves group membership from these with `AssertionGroupImporter`'s own rules, so the store alone says which assertions a group selects |
| `pack` | name, version (corpus commit date, or `--pack-version`), corpus commit, digest |
| `generator` | sqlglot and Python versions, and `publisher`: SHA-256 over the sorted `name<TAB>sha256` lines of this directory's `*.py` and `requirements.txt` |

### Ports

For the international corpus `ports` is exactly two macros: `substring_index`
(MySQL built-in, called by 14 scripts) and `cleanExpression` (defined by the
corpus with `CREATE FUNCTION`, called by the SNOMED CT expression validation
procedure). Stored-procedure `CALL`s are not ports: `procedures.py` unrolls them
into plain SQL at publish time, over the fixed DDL table set.

### What the digest covers

`pack.digest` is SHA-256 over the sorted
`uuid<TAB>sha256<TAB>keywords<TAB>severity<TAB>text` lines of every assertion,
then `\n--\n`, then the sorted `file<TAB>sha256` lines of every pre-requisite
(none for the international corpus), then `\n--\n`, then the sorted
`file<TAB>sha256` lines of the grouping files. It identifies *which
assertions, from which source text, with which manifest text, category and
severity, selected by which group rules*. It deliberately does not cover the
statements, ports, `tableColumns` or generator: the same corpus republished
with another sqlglot keeps its digest. RVF recomputes the digest on load and
refuses a store whose declared digest does not match, so an edit to any field
the digest covers is refused; an edit to a statement, a port or
`tableColumns` is not (`pack_smoke.py` executes a store's statements, and RVF's
`AssertionCorpusDigestTest` records what each finds on its regression fixture).

RVF's `BundledStoreTest` checks that the bundled store is internally consistent
and records the corpus commit checked out beside the project, which is what
the build published it from.

## Other corpora

Nothing in the publisher is specific to one corpus. A corpus that needs more
than the international one supplies it on the command line:

| option | for |
|---|---|
| `--prerequisites DIR` | MySQL pre-requisite scripts building tables the DDL does not declare (e.g. `*_active`); transpiled like assertions and recorded in the digest |
| `--ports-sql FILE` | extra DuckDB setup (macros, derived tables), `;`-separated, not transpiled; `<PROSPECTIVE>` is the release schema. Repeatable |
| `--ported-call NAME` | a stored procedure whose work a `--ports-sql` statement already does; its `CALL`s are dropped instead of failing |
| `--ci-function NAME` | a corpus stored function returning a case-insensitive string; regex matches on its result get the `i` flag, as MySQL's collation implies |
| `--pack-name`, `--requires` | identity of a pack merged with another at run time, and what it needs of that other pack |

Every assertion's table references are checked against the DDL, the
pre-requisites, the ports and tables other assertions create; a reference to
anything else fails the publish, naming the assertion and table. That is what
catches a corpus published without the pre-requisites it needs, which would
otherwise surface only at run time as "Table with name ... does not exist".

## Files

| file | |
|---|---|
| `build-store.sh` | RVF's build step: pinned environment, then `publish_store.py` over the checkout |
| `publish_store.py` | entry point: corpus + DDL -> store.json |
| `rvfsql.py` | RVF's import/transform pipeline ported, plus the MySQL -> DuckDB dialect pass; `precompile()`/`bind()` and the reference `prepare()` |
| `procedures.py` | host-language ports of the corpus's stored procedures, as SQL generators |
| `rf2map.py` | reads `create-tables-mysql.sql`: tables, columns, DuckDB types |
| `manifest.py` | reads `manifest.xml` |
| `pins.py` | the startup check against `requirements.txt` |
| `verify_store.py`, `precompile_parity.py`, `pack_smoke.py`, `procedures_selftest.py` | checks, above |
