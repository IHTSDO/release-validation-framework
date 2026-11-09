# SPDX-License-Identifier: Apache-2.0
"""Read the assertion corpus's manifest.xml.

The manifest is what RVF imports assertions from, so it is the only source of
an assertion's uuid - the key that joins a DuckDB result back to the assertion
RVF reports - and of the text, keywords and severity its group rules match on.
"""
import pathlib

# defusedxml guards against XXE / billion-laughs in untrusted XML. Required, not
# optional: requirements.txt pins it.
import defusedxml.ElementTree as ET


def load_scripts(repo: pathlib.Path):
    """-> {sqlFile: {'uuid','text','category','severity'}} from manifest.xml."""
    out = {}
    man = repo / "manifest.xml"
    if not man.is_file():
        return out
    root = ET.parse(man).getroot()
    for el in root.iter("script"):
        f = el.get("sqlFile")
        if not f:
            continue
        out[f] = {"uuid": el.get("uuid"),
                  "text": el.get("text") or "",
                  "category": el.get("category") or "",
                  "severity": el.get("severity") or ""}
    return out


def index_by_basename(scripts):
    """manifest.xml stores bare filenames; the corpus is in category subdirs."""
    return {pathlib.PurePath(k).name: v for k, v in scripts.items()}
