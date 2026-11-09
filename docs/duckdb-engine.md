# The DuckDB engine

RVF runs a validation in five passes: structural, SQL assertions, Drools, MRCM and
traceability. Only the SQL-assertion pass depends on a database. With

    rvf.execution.engine=duckdb

that pass runs on an embedded [DuckDB](https://duckdb.org) instead of MySQL. No MySQL
server, no datasource and no JPA are created; the other four passes are unchanged.
`mysql` remains the default, and any value other than `duckdb` means `mysql`.

## How it works

| step | MySQL engine | DuckDB engine |
|---|---|---|
| assertions | imported from the corpus checkout into the assertion tables at startup | read from the assertion store, a precompiled artefact (below) |
| releases | loaded into `rvf_*` schemas with `load data local infile` | unpacked and loaded with `read_csv` into a database file created for the run and deleted after it |
| SQL | each statement transformed per run, procedures created and `CALL`ed | statements transpiled to DuckDB once, at publish time; procedure `CALL`s unrolled into plain SQL; the run only substitutes its values |
| results | `qa_result` in `rvf_master` | `qa_result` in the run's database |

Classes, in `org.ihtsdo.rvf.core.service.duck`:

- `DuckDbValidationService`: the `SqlAssertionValidationService` for this engine. It
  unpacks the files `ReleaseAcquisitionService` acquired (`DuckReleaseUnpacker`, which
  applies `excludedRF2Files` exactly as `ReleaseDataManager` does), materialises them
  (`DuckMaterialiser`, using `RF2FileTableMapper` and the DDL's column types), runs the
  resource assertions and then the requested groups, and fills the report
  (`DuckFailuresExtractor`: whitelist, extension-module filtering and concept FSNs, as
  `MysqlFailuresExtractor` does). Extension, delta-only and stand-alone runs follow the
  MySQL service's sequence. So do the releases it derives: several dependencies are
  validated as their union; a package without Delta files gets its delta tables, and one
  without Full files its full tables, by `ReleaseDataManager`'s rules in its order; a
  delta-only run with no previous release validates the delta as the snapshot.
- `DuckAssertionService`: the `AssertionService` for this engine, over the store, so
  `/assertions`, `/groups` and the validation submission endpoints work. The assertion
  administration endpoints stay MySQL-only; the store is read-only.
- `DuckStore`, `DuckStoreLocator`, `DuckAssertionSource`: the store, where it comes from,
  and its assertions and group membership.
- `DuckBinder`, `DuckDbAssertionExecutionService`: binding a run's values into a statement,
  and executing an assertion's statements.
- `DuckReleaseCache`: optional cache of materialised previous/dependency releases.

### What differs from MySQL by design

- **A comparison with a release the run does not have is not run.** A statement that
  names `<PREVIOUS>` or `<DEPENDENCY>` when no such release was supplied is skipped, and an
  assertion none of whose `qa_result` writers ran is reported with failure count `-1` and
  "Not run: requires <PREVIOUS>". The MySQL engine binds an empty schema and reports
  the comparison against no rows as a result. Two exceptions, where an empty release is
  the right answer: a first-time release (`firstTimeRelease`, which `/run-post` sets when
  `previousRelease` is blank, or a previous release named `rvf.empty-release-file`) binds
  `<PREVIOUS>` to an empty schema, as MySQL does; and a
  statement whose only use of the dependency is an anti-join (`LEFT JOIN ... IS NULL`, or
  `NOT EXISTS (SELECT ... FROM <DEPENDENCY>...)`) gets the same answer from an empty schema
  as MySQL's empty dependency schema gives, and runs against one.
- **An assertion with only some statements skipped says so.** Its failure count is what
  the statements that ran found, and the report item carries a `note` ("Partly run: N of M
  statement(s) skipped because they require <PREVIOUS>, ..."). The item is not counted as
  incomplete: the statements that ran are a result. Only the DuckDB engine sets `note`;
  it is absent from MySQL reports.
- **What assertion SQL can reach.** A run loads its releases first: it reads the release
  directories, and builds or attaches (read-only) the release cache, with the engine's own
  SQL and the store's table shapes, which a store must declare as plain `name TYPE`
  columns or it is refused. Before any of the store's statements runs, the connection is
  confined to the run's own directory, `rvf-<runId>` in the work directory (its database,
  spill and unpacked releases), and locked: `allowed_directories` set to that directory,
  `enable_external_access=false`, `autoinstall_known_extensions=false`,
  `autoload_known_extensions=false`, `lock_configuration=true`. Assertions cannot read
  other runs' files or the release cache, write outside the run's directory, reach the
  network or load extensions. A release-cache build is confined to the release directory it
  reads and the cache directory.
- **Extension combine failures abort the SQL pass**, where MySQL logs them and runs the
  remaining assertions against a partly combined schema.
- **RF2 files are matched to columns by their header** when the header names exactly the
  declared columns; `load data local infile` is positional.
- **`<ASSERTIONUUID>` binds to the assertion UUID**, not to the numeric id the MySQL
  import assigns.

## Configuration

| property | default | |
|---|---|---|
| `rvf.execution.engine` | `mysql` | `duckdb` selects this engine |
| `rvf.duck.store` | blank | a store file; blank uses the store bundled in the artefact |
| `rvf.duck.work.directory` | `${java.io.tmpdir}` | where each run's database file is created; needs room for the materialised releases plus spill (about 1.6 GB for the SNOMED CT-AU edition), on local disk |
| `rvf.duck.threads` | `0` | `0` uses the CPUs available to the JVM. DuckDB sizes its own pool from the host's cores and ignores container CPU limits. Applies to runs and to release-cache builds |
| `rvf.duck.memory.limit` | blank | e.g. `8GB`; must carry a unit (checked at startup). Blank keeps DuckDB's default of 80% of host RAM, which ignores container limits. Applies to runs and to release-cache builds |
| `rvf.duck.cache.max-gb` | `0` | size of the cache of materialised previous/dependency releases; `0` disables it |
| `rvf.duck.cache.directory` | `<work directory>/release-cache` | |

`rvf.assertion.resource.local.path` (the corpus checkout) is not read in this mode. The
previous and dependency releases are acquired by file name, through the Module Storage
Coordinator or release storage, as on the MySQL engine. `/releases`, which manages loaded
MySQL schemas, is not available.

### The native library and `java.io.tmpdir`

The DuckDB JDBC driver (`org.duckdb:duckdb_jdbc` 1.5.5.1) carries its native library
inside the jar, extracts it to `java.io.tmpdir` and loads it from there. It has no setting
for another extraction directory. On a host whose temporary directory is mounted `noexec`
the load fails, so start the JVM with a temporary directory that allows execution:

    JAVA_TOOL_OPTIONS=-Djava.io.tmpdir=/var/lib/rvf/tmp

`rvf.duck.work.directory` defaults to `java.io.tmpdir` too. The container image declares
`/tmp` as a volume, which Docker and Kubernetes mount with execution allowed. In DuckDB
mode the application opens an in-memory database at startup, so a library that cannot be
loaded stops the application with a message naming `java.io.tmpdir`, rather than failing
every validation. The Maven build runs tests with `java.io.tmpdir` set to `target/tmp`
for the same reason.

## The assertion store

`duck/store.json` in the artefact holds, for every assertion `manifest.xml` declares,
its uuid, text, keywords, severity, source file and source hash, and its statements
transpiled to DuckDB; the corpus's `groups.xml` and `policies.xml`, verbatim; the DDL's
table shapes; the two macros the corpus needs (`substring_index`, `cleanExpression`); and
the store's identity:

    "pack": {"name": "international", "version": "2026.10.13",
             "corpusRef": "c3a6fb27b2193925be7bce94ec1c4b4b03c8764f",
             "digest": "sha256:24f61da9...", "assertions": 361}

The version is the corpus commit's date. The digest covers every assertion's source hash,
manifest text, keywords (the manifest's category) and severity, the pre-requisites and the
grouping files, and is recomputed on load: a store in which any of those was edited after
publication is refused. It does not cover the transpiled statements, the ports or the
table shapes, so that a republish with another sqlglot keeps the identity of the same
corpus. Every validation report records the identity of the store it executed
(`assertionPacks` in the report).

The store is built by `tools/duck-publisher` (Python, sqlglot) from the assertion corpus;
see its README. The server needs no Python.

## Checks

| test | checks |
|---|---|
| `DuckDbEngineContextTest` | the application starts with `duckdb` and no reachable database |
| `ExecutionEngineContextTest` | the MySQL bean set is unchanged; `duckdb` registers this engine's beans |
