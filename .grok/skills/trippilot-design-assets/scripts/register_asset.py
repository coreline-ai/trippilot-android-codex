#!/usr/bin/env python3
"""Register an approved slot source in docs/asset-manifest.md."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from datetime import date
from pathlib import Path


SKILL_DIR = Path(__file__).resolve().parents[1]
ROOT = SKILL_DIR.parents[2]
SLOTS_PATH = SKILL_DIR / "references" / "allowed-slots.json"
MANIFEST = ROOT / "docs" / "asset-manifest.md"
REMOTE = re.compile(r"(?:href|xlink:href)\s*=\s*[\"'](?:https?:)?//", re.I)


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def slot_by_id(slot_id: str) -> dict:
    catalog = json.loads(SLOTS_PATH.read_text(encoding="utf-8"))
    for slot in catalog["slots"]:
        if slot["id"] == slot_id:
            return slot
    fail(f"unknown slot {slot_id!r}; allowed: {[item['id'] for item in catalog['slots']]}")
    raise AssertionError


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser(description="Hash a slot SVG and upsert its manifest row.")
    parser.add_argument("--slot", required=True)
    args = parser.parse_args()
    slot = slot_by_id(args.slot)
    source = ROOT / slot["source"]
    if not source.is_file():
        fail(f"missing source: {slot['source']}")
    text = source.read_text(encoding="utf-8")
    if REMOTE.search(text):
        fail(f"remote URL in {slot['source']}")
    digest = sha256(source)
    android = slot["android"]
    row = (
        f"| `{slot['source']}` | {slot['role']} | TripPilot 팀 자체 제작, "
        f"{date.today().isoformat()} | Proprietary to TripPilot project | TripPilot | original | "
        f"`{digest}` | {date.today().isoformat()} | `{android}` |"
    )
    manifest = MANIFEST.read_text(encoding="utf-8")
    pattern = re.compile(rf"^\| `{re.escape(slot['source'])}` \|.*$", re.M)
    if pattern.search(manifest):
        manifest = pattern.sub(row, manifest)
        action = "updated"
    else:
        lines = manifest.splitlines()
        insert_at = None
        for index, line in enumerate(lines):
            if line.startswith("| `") and "design/assets/" in line:
                insert_at = index + 1
        if insert_at is None:
            fail("could not find a design/assets row to insert after")
        lines.insert(insert_at, row)
        manifest = "\n".join(lines) + "\n"
        action = "inserted"
    MANIFEST.write_text(manifest, encoding="utf-8")
    print(f"PASS: {action} manifest row for {slot['id']} sha256={digest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
