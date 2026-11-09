# SPDX-License-Identifier: Apache-2.0
"""RVF's create-tables-mysql.sql, read as data.

The DDL is the single source of truth for table names, column names and column
types: it is what RVF creates for every release schema, so the assertions are
written against exactly these tables. The publisher reads it rather than
restating any of it, which keeps the store in step with RVF by construction.
"""
import pathlib
import re

_CREATE_RE = re.compile(r"^create table (\w+)\((.*?)^\) engine=myisam[^;]*;",
                        re.S | re.M | re.I)


def mysql_ddl(create_sql_path: pathlib.Path):
    """Extract {table_name: full CREATE TABLE statement} from RVF's own DDL file."""
    sql = create_sql_path.read_text()
    ddl = {}
    for m in _CREATE_RE.finditer(sql):
        ddl[m.group(1).lower()] = m.group(0).rstrip().rstrip(";")
    return ddl


def columns_from_ddl(ddl_stmt: str):
    """[(column, mysql_type), ...] for one CREATE TABLE, in declaration order."""
    body = ddl_stmt.split("(", 1)[1]
    cols = []
    for line in body.splitlines():
        line = line.strip().rstrip(",")
        if not line or line.lower().startswith(("key ", "primary key", ")")):
            continue
        m = re.match(r"^(\w+)\s+(.+)$", line)
        if m:
            cols.append((m.group(1).lower(), m.group(2).strip()))
    return cols


def duck_type(mysql_type: str) -> str:
    """RVF's MySQL column type -> the DuckDB type a runtime should create.

    Deliberately keeps bigint ids as BIGINT and effectivetime as a string exactly
    as the MySQL schema does, so that any assertion relying on string comparison
    of effectivetime behaves identically. Getting smallint wrong silently turns
    numeric comparisons into string comparisons, so every type is mapped
    explicitly and anything new fails loudly.
    """
    t = mysql_type.lower()
    if t.startswith("bigint"):
        return "BIGINT"
    if t.startswith("smallint"):
        return "SMALLINT"
    if t.startswith("mediumint") or t.startswith("tinyint") or t.startswith("int"):
        return "INTEGER"
    if t.startswith(("char(", "varchar(", "text")):
        return "VARCHAR"
    if t.startswith(("decimal", "double", "float")):
        return "DOUBLE"
    raise ValueError(f"unmapped MySQL type: {mysql_type!r}")
