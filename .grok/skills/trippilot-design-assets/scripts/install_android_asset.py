#!/usr/bin/env python3
"""Install a registered VectorDrawable for an allowed empty-state slot."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
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
    fail(f"unknown slot {slot_id!r}")
    raise AssertionError


def main() -> int:
    parser = argparse.ArgumentParser(description="Check registration then keep the Android drawable in place.")
    parser.add_argument("--slot", required=True)
    args = parser.parse_args()
    slot = slot_by_id(args.slot)
    source = ROOT / slot["source"]
    android = ROOT / slot["android"]
    if not source.is_file():
        fail(f"missing source: {slot['source']}")
    if REMOTE.search(source.read_text(encoding="utf-8")):
        fail(f"remote URL in {slot['source']}")
    digest = hashlib.sha256(source.read_bytes()).hexdigest()
    manifest = MANIFEST.read_text(encoding="utf-8")
    if slot["source"] not in manifest or digest not in manifest:
        fail(f"{slot['source']} is not registered with its current hash in docs/asset-manifest.md")
    if not android.is_file():
        fail(f"missing VectorDrawable: {slot['android']}")
    if "<bitmap" in android.read_text(encoding="utf-8"):
        fail(f"{slot['android']} must stay a VectorDrawable")
    print(f"PASS: {slot['id']} registered and installed at {slot['android']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
