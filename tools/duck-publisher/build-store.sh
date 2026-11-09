#!/bin/bash
# SPDX-License-Identifier: Apache-2.0
#
# Publishes the assertion store from a corpus checkout in the pinned environment. Run by
# the Maven build (pom.xml) after checkout-resources.sh, so the store in the artefact is
# always built from the corpus the build checked out. Any failure fails the build; a
# previous store is deleted first, never reused.
#
#   build-store.sh --venv DIR --corpus DIR --ddl FILE --out FILE [--ref SHA --checkout DIR]
#
#   --venv      the pinned environment, created from requirements.txt if absent
#   --corpus    a git checkout of snomed-release-validation-assertions
#   --ref       publish the corpus at commit SHA rather than at the checkout's HEAD. The
#               commit is checked out into --checkout, from --corpus if it has it,
#               otherwise fetched from --corpus's origin.
set -euo pipefail

HERE=$(cd "$(dirname "$0")" && pwd)
REQUIREMENTS=$HERE/requirements.txt
VENV= CORPUS= DDL= OUT= REF= CHECKOUT=
while [ $# -gt 0 ]; do
  case $1 in
    --venv) VENV=$2 ;;
    --corpus) CORPUS=$2 ;;
    --ddl) DDL=$2 ;;
    --out) OUT=$2 ;;
    --ref) REF=$2 ;;
    --checkout) CHECKOUT=$2 ;;
    *) echo "build-store.sh: unknown argument $1" >&2; exit 2 ;;
  esac
  shift 2
done
: "${VENV:?--venv}" "${CORPUS:?--corpus}" "${DDL:?--ddl}" "${OUT:?--out}"
if [ -n "$REF" ]; then : "${CHECKOUT:?--checkout is required with --ref}"; fi

fail() { echo "ERROR: $*" >&2; exit 1; }
no_python() {
  fail "the DuckDB assertion store is generated during the build and needs Python 3.12" \
    "(python3.12 with its venv module, or uv with a Python 3.12 installed). None was found," \
    "so the build stops rather than ship a store that does not match the assertion corpus." \
    "See tools/duck-publisher/README.md."
}

rm -f "$OUT" "$OUT.tmp"
[ -f "$CORPUS/manifest.xml" ] || fail "no assertion corpus at $CORPUS (checkout-resources.sh clones it)"

start=$(date +%s)

# The pinned environment. pins.require() in the publisher refuses any other Python minor
# version or package version, so a stale or foreign environment fails rather than drifts.
# A half-created environment is removed, so the next build starts again instead of reusing it.
create_venv() {
  rm -rf "$VENV"
  if command -v uv >/dev/null 2>&1; then
    uv venv --quiet --no-python-downloads --python 3.12 "$VENV" || { rm -rf "$VENV"; no_python; }
  elif command -v python3.12 >/dev/null 2>&1; then
    python3.12 -m venv "$VENV" || { rm -rf "$VENV"; no_python; }
  else
    no_python
  fi
}
[ -x "$VENV/bin/python" ] || create_venv
if command -v uv >/dev/null 2>&1; then
  uv pip install --quiet --python "$VENV/bin/python" -r "$REQUIREMENTS"
else
  # A reused environment uv created has no pip; without uv, recreate it with python3.12.
  "$VENV/bin/python" -m pip --version >/dev/null 2>&1 || create_venv
  "$VENV/bin/python" -m pip install --quiet --disable-pip-version-check -r "$REQUIREMENTS"
fi

SOURCE=$CORPUS
if [ -n "$REF" ]; then
  if [ "$(git -C "$CHECKOUT" rev-parse -q --verify HEAD 2>/dev/null || true)" != "$REF" ]; then
    rm -rf "$CHECKOUT"
    git -c init.defaultBranch=pinned init -q "$CHECKOUT"
    if ! git -C "$CHECKOUT" -c uploadpack.allowAnySHA1InWant=true fetch -q --depth 1 \
        "$(cd "$CORPUS" && pwd)" "$REF" 2>/dev/null; then
      git -C "$CHECKOUT" fetch -q --depth 1 "$(git -C "$CORPUS" remote get-url origin)" "$REF" \
        || fail "cannot fetch assertion corpus commit $REF"
    fi
    git -C "$CHECKOUT" checkout -q --detach FETCH_HEAD
  fi
  SOURCE=$CHECKOUT
fi

mkdir -p "$(dirname "$OUT")"
PYTHONDONTWRITEBYTECODE=1 "$VENV/bin/python" "$HERE/publish_store.py" \
  --scripts "$SOURCE/scripts" --manifest-root "$SOURCE" --ddl "$DDL" --out "$OUT.tmp" \
  || { rm -f "$OUT.tmp"; fail "publishing the assertion store from $SOURCE failed (above)"; }
mv "$OUT.tmp" "$OUT"
echo "Assertion store $OUT built from $SOURCE at $(git -C "$SOURCE" rev-parse HEAD) in $(( $(date +%s) - start )) s"
