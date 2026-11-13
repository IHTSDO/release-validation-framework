# Assertion packs (DuckDB engine)

An assertion pack is an assertion store published by `tools/duck-publisher` from another
assertion corpus: a national extension's assertions, or a project's own. The DuckDB engine
merges the packs a deployment pins with its base store (the bundled store, or
`rvf.duck.store`) at runtime, so adding or updating such assertions needs a configuration
change instead of a rebuild of RVF. Packs are not available on the MySQL engine.

## What a pack is

The same JSON as the bundled store (see [duckdb-engine.md](duckdb-engine.md)): transpiled
statements per assertion with their source hashes, the corpus's `groups.xml` and
`policies.xml`, the DDL table shapes, any macros the corpus needs, and an identity:

    PYTHONDONTWRITEBYTECODE=1 /tmp/duckenv/bin/python tools/duck-publisher/publish_store.py \
      --scripts <corpus>/scripts --manifest-root <corpus> \
      --ddl src/main/resources/sql/create-tables-mysql.sql \
      --pack-name example-pack --requires international:atLeast=2026.10.13 \
      --out example-pack.json
    sha256sum example-pack.json      # the digest a deployment pins

A corpus that needs pre-requisite tables or extra macros passes `--prerequisites`,
`--ports-sql`, `--ported-call` or `--ci-function` (see `tools/duck-publisher/README.md`).

## Merge rules

`DuckStorePacks.merge` combines the base and the packs in order and refuses, listing every
conflict, when:

- two packs have the same name: a requirement and a report name a pack by name alone;
- `formatVersion`, the sentinel table, `runIdSentinel`, `qaResultToken` or the sqlglot
  version differ: they are the contract with the binder, and two sqlglot versions are two
  SQL dialects;
- two packs declare one table with different columns;
- two packs carry different assertions under one uuid (an identical repetition is allowed:
  a pack built from a combined corpus repeats the base's assertions);
- two packs define one macro with different bodies, or one pre-requisite file with
  different content;
- a pack has no grouping files: no group would select its assertions;
- a pack's `requires` does not hold: `atLeast` a version (versions are `YYYY.MM.DD`;
  anything else is refused rather than compared as text) or an exact `digest`. An unknown
  requirement key is refused.

A refusal names the pack that made the earlier definition and the pack that disagrees with it.

Each pack's assertions are put in groups by that pack's own `groups.xml` and
`policies.xml`; an assertion repeated in two packs belongs to the union of their groups.
The merged store records each pack's name, version, digest, corpus commit and assertion
count, and every validation report records that list (`assertionPacks`).

Before a merged corpus is used, every assertion in it is executed against empty tables, so
a pack calling a macro the base does not define is refused when the packs are loaded, not
during a validation. That check runs on a DuckDB connection locked down with no file access
before anything from the packs reaches it, and a validation runs the packs' statements only
once its connection is confined to the run's own directory; see
[duckdb-engine.md](duckdb-engine.md).

## Configuration

    rvf.assertion.packs=name=example-pack;version=2026.10.20;uri=https://example.org/example-pack.json;sha256=<hex>

One entry per pack, comma-separated; fields separated by semicolons. `uri` may be `https:`
or `file:`. `sha256` is the digest of the bytes and is required: a pack is executable SQL,
and it is verified before it is parsed. `authHeader=<value>` adds an `Authorization`
header for a private location (keep the value in a secret, not in the URL; it is not
logged). Full specs like this one, `file:` locations and credentials are accepted only
here, in the deployment's configuration.

Configured packs are loaded when the corpus is first loaded (at startup). If they cannot
be fetched, verified, merged or executed, the corpus is not loaded and validations fail
with the reason; RVF does not fall back to the base store alone, which would report fewer
assertions as a clean pass.

### Channels

A channel lets a pack be pinned by version alone (`example-pack@2026.10.20`), with the URL
and digest read from an index the channel publishes:

    rvf.assertion.channels=name=example;index=https://example.org/index.json;packs=example-pack

    {"channel": "example",
     "versions": [{"version": "2026.10.20", "uri": "https://example.org/example-pack-2026.10.20.json",
                   "sha256": "<hex>", "assertions": 120,
                   "requires": [{"pack": "international", "atLeast": "2026.10.13"}],
                   "published": "2026-10-20", "draft": false}]}

A channel serves only the pack names in its `packs` list, and an index that names a
different channel is refused. The configuration decides where packs come from, not the
index: every `uri` in an index must have the index's own scheme and host, or a host the
channel lists in `allowedHosts=<host>|<host>` (for an index that points at a separate
download host); otherwise the index is refused.

## Endpoints

| | |
|---|---|
| `GET /assertions/packs` | the packs the loaded corpus was assembled from (base included), and whether the configured packs differ from the loaded ones |
| `POST /assertions/packs/refresh` | fetch the configured packs and replace the corpus. Nothing changes on failure; 409 lists merge conflicts, 502 a fetch, digest or execution failure. A validation already running keeps the corpus it started with. It loads only configured packs; like every other endpoint it requires an authenticated caller, and RVF has no role model to restrict it further |
| `GET /assertions/packs/channels` | the configured channels and the versions each offers |

## Pinning packs for one run

`POST /run-post` and `/run-post-via-s3` take `assertionPacks`: one `name@version` per pack,
for a version a configured channel offers. The run uses the base store merged with those
packs instead of the deployment's default; the corpus is built and verified exactly as a
refresh builds it, without changing what other runs use. Re-running a report with the
packs it recorded reproduces its assertion set.

A request cannot name a location, a digest or a credential: a value that is not
`name@version` (a full spec, `uri=`, `file:`, `authHeader`) is refused with HTTP 400 before
the run is queued, and the engine refuses it again if it arrives by another route. Packs
not on a channel are pinned in `rvf.assertion.packs`.
