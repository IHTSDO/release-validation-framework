# SPDX-License-Identifier: Apache-2.0
"""Refuse to run outside the pinned environment.

Imported, and require() called, by every entry point BEFORE sqlglot or duckdb
is imported. The store's statements are sqlglot's output byte for byte, so a
missing transpiler must not degrade to untranspiled MySQL and a different one
must not quietly print different SQL; duckdb is what the checks execute
against. Either kind of mismatch, or a Python other than the pinned minor
version, is a startup failure naming both versions, never a fallback. Bump a
pin in requirements.txt deliberately and review the store diff that follows.

Reads installed distribution metadata only: no import of the pinned packages
and no network.
"""
import pathlib
import re
import sys
from importlib import metadata

REQUIREMENTS = pathlib.Path(__file__).resolve().parent / "requirements.txt"
# The Python minor version requirements.txt is written for.
PYTHON = (3, 12)
_PIN_RE = re.compile(r"^\s*([A-Za-z0-9_.-]+)\s*==\s*([^\s#;]+)")


def pins(requirements=REQUIREMENTS):
    """{distribution: version} for every `name==version` line."""
    out = {}
    for line in requirements.read_text().splitlines():
        m = _PIN_RE.match(line)
        if m:
            out[m.group(1).lower()] = m.group(2)
    return out


def mismatches(requirements=REQUIREMENTS):
    """[(distribution, pinned, installed or None)] for every pin not satisfied."""
    bad = []
    for name, pinned in sorted(pins(requirements).items()):
        try:
            installed = metadata.version(name)
        except metadata.PackageNotFoundError:
            installed = None
        if installed != pinned:
            bad.append((name, pinned, installed))
    return bad


def require(requirements=REQUIREMENTS):
    if sys.version_info[:2] != PYTHON:
        raise SystemExit(
            f"FATAL: this Python ({sys.executable}) is "
            f"{sys.version_info.major}.{sys.version_info.minor}, pinned "
            f"{PYTHON[0]}.{PYTHON[1]}.\nCreate the pinned environment "
            "(README.md) and run from it.")
    found = pins(requirements)
    if "sqlglot" not in found or "duckdb" not in found:
        raise SystemExit(f"FATAL: {requirements} must pin sqlglot and duckdb")
    bad = mismatches(requirements)
    if bad:
        lines = [f"  {name}: pinned {pinned}, installed {installed or 'NOT INSTALLED'}"
                 for name, pinned, installed in bad]
        raise SystemExit(
            f"FATAL: this Python ({sys.executable}) does not match "
            f"{requirements}:\n" + "\n".join(lines)
            + "\nCreate the pinned environment (README.md) and run from it.")
