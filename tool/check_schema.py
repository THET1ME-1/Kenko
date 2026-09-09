#!/usr/bin/env python3
"""Сверяет базу с последней схемой Room двумя путями: обновление и чистая установка.

Room не проверяет таблицы, пришедшие из assets/kenko.db, поэтому расхождение
всплывает только на устройстве — падением при первом запросе.

    python3 tool/check_schema.py
"""

import json
import os
import re
import shutil
import sqlite3
import sys
import tempfile

SCHEMAS = "app/schemas/com.looker.kenko.data.local.KenkoDatabase"
ASSET = "app/src/main/assets/kenko.db"
MIGRATIONS = "app/src/main/kotlin/com/looker/kenko/data/local/Migrations.kt"


def schema(version):
    with open(f"{SCHEMAS}/{version}.json", encoding="utf-8") as file:
        return json.load(file)["database"]


def create(db, database):
    for entity in database["entities"]:
        db.execute(entity["createSql"].replace("${TABLE_NAME}", entity["tableName"]))
        for index in entity.get("indices", []):
            db.execute(index["createSql"].replace("${TABLE_NAME}", entity["tableName"]))


def compare(db, database):
    problems = []
    for entity in database["entities"]:
        table = entity["tableName"]
        have = {row[1] for row in db.execute(f"PRAGMA table_info(`{table}`)")}
        want = {field["columnName"] for field in entity["fields"]}
        if want - have:
            problems.append(f"{table}: не хватает колонок {sorted(want - have)}")
        if have - want:
            problems.append(f"{table}: лишние колонки {sorted(have - want)}")
        want_keys = sorted(
            (key["table"], key["columns"][0], key["referencedColumns"][0])
            for key in entity.get("foreignKeys", [])
        )
        have_keys = sorted(
            (row[2], row[3], row[4])
            for row in db.execute(f"PRAGMA foreign_key_list(`{table}`)")
        )
        if want_keys != have_keys:
            problems.append(f"{table}: ключи ждём {want_keys}, есть {have_keys}")
        want_index = sorted(index["name"] for index in entity.get("indices", []))
        have_index = sorted(
            row[1]
            for row in db.execute(f"PRAGMA index_list(`{table}`)")
            if not row[1].startswith("sqlite_")
        )
        if want_index != have_index:
            problems.append(f"{table}: индексы ждём {want_index}, есть {have_index}")
    return problems


def migrations_since(version):
    with open(MIGRATIONS, encoding="utf-8") as file:
        source = file.read()
    tail = source[source.index(f"val MIGRATION_{version}_{version + 1}"):]
    pattern = r'execSQL\(\s*(?:"([^"]+)"|"""(.*?)"""\.trimIndent\(\))\s*,?\s*\)'
    return [plain or block for plain, block in re.findall(pattern, tail, re.S)]


def main():
    versions = sorted(
        int(name.removesuffix(".json"))
        for name in os.listdir(SCHEMAS)
        if name.endswith(".json")
    )
    latest = schema(versions[-1])
    oldest = schema(versions[0] + 2)  # первая схема, с которой живут релизы
    failures = []

    upgraded = sqlite3.connect(":memory:")
    create(upgraded, oldest)
    for statement in migrations_since(oldest["version"]):
        upgraded.execute(statement)
    for problem in compare(upgraded, latest):
        failures.append(f"обновление {oldest['version']}→{latest['version']}: {problem}")

    with tempfile.TemporaryDirectory() as folder:
        copy = os.path.join(folder, "kenko.db")
        shutil.copy(ASSET, copy)
        fresh = sqlite3.connect(copy)
        create(fresh, latest)
        for problem in compare(fresh, latest):
            failures.append(f"чистая установка: {problem}")

    if failures:
        print("\n".join(failures))
        return 1
    print(f"база сходится со схемой {latest['version']}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
