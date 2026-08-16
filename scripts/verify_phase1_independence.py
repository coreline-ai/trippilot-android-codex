#!/usr/bin/env python3
"""Reject accidental OpenMinis, Grok, credential, or raw CLI coupling in Phase 1."""

from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE_ROOTS = (ROOT / "app" / "src", ROOT / "build.gradle.kts", ROOT / "settings.gradle.kts")
FORBIDDEN_PROJECT_REFERENCES = ("com.openminis", "grok-cli-pack", "grok")
FORBIDDEN_PORT_PATTERNS = (
    r"\bval\s+(?:access)?token\b",
    r"\bval\s+credential",
    r"auth\.json",
    r"raw(?:Command|Argument|Executable)",
    r"localhost",
    r"127\.0\.0\.1",
)


def source_files() -> list[Path]:
    files: list[Path] = []
    for root in SOURCE_ROOTS:
        if root.is_file():
            files.append(root)
        elif root.is_dir():
            files.extend(path for path in root.rglob("*") if path.suffix in {".kt", ".kts", ".xml"})
    return sorted(files)


def main() -> None:
    failures: list[str] = []
    for path in source_files():
        content = path.read_text(encoding="utf-8")
        lower = content.lower()
        # Phase 2 has one contractually allowed literal: the public legacy backup
        # schema. It is not a dependency, package, database migration, or OAuth
        # integration; any other occurrence is still accidental coupling.
        if "openminis" in lower:
            allowed = path == ROOT / "app/src/main/kotlin/io/trippilot/app/core/data/TripBackup.kt"
            if not allowed or lower.count("openminis") != 1 or '"openminis.trip-backup"' not in lower:
                failures.append(f"{path.relative_to(ROOT)} contains forbidden project reference: openminis")
        for forbidden in FORBIDDEN_PROJECT_REFERENCES:
            if forbidden in lower:
                failures.append(f"{path.relative_to(ROOT)} contains forbidden project reference: {forbidden}")

    contract = (ROOT / "app/src/main/kotlin/io/trippilot/app/core/codex/CodexRuntimePort.kt")
    content = contract.read_text(encoding="utf-8")
    for pattern in FORBIDDEN_PORT_PATTERNS:
        if re.search(pattern, content, flags=re.IGNORECASE):
            failures.append(f"CodexRuntimePort contains forbidden boundary field/pattern: {pattern}")

    if failures:
        for failure in failures:
            print(f"FAIL: {failure}")
        raise SystemExit(1)

    print("PASS: source and Gradle files have no OpenMinis or Grok coupling")
    print("PASS: CodexRuntimePort has no token, credential, raw CLI, or localhost contract")


if __name__ == "__main__":
    main()
