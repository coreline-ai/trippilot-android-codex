#!/usr/bin/env python3
"""Fixture checks for design-loop state aggregation."""

from __future__ import annotations

import json
import subprocess
import sys
import tempfile
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SYNC = ROOT / "scripts/sync_design_run_state.py"


def run(review: dict, evidence: dict, extra: list[str] | None = None) -> tuple[int, dict]:
    with tempfile.TemporaryDirectory() as directory:
        review_path = Path(directory) / "review.json"
        evidence_path = Path(directory) / "evidence.json"
        out_dir = Path(directory) / "out"
        review_path.write_text(json.dumps(review), encoding="utf-8")
        evidence_path.write_text(json.dumps(evidence), encoding="utf-8")
        command = [
            sys.executable,
            str(SYNC),
            "--root",
            str(ROOT),
            "--review",
            str(review_path),
            "--evidence",
            str(evidence_path),
            "--out-dir",
            str(out_dir),
        ]
        if extra:
            command.extend(extra)
        completed = subprocess.run(command, check=False, capture_output=True, text=True)
        state_path = out_dir / "RUN_STATE.json"
        state = json.loads(state_path.read_text(encoding="utf-8")) if state_path.is_file() else {}
        return completed.returncode, state


def approved_review(issues: dict | None = None) -> dict:
    screens = {
        name: {
            "composition": "pass",
            "korean_type": "pass",
            "spacing": "pass",
            "interaction": "pass",
            "accessibility": "pass",
            "approved": True,
        }
        for name in (
            "list-empty",
            "briefing",
            "itinerary",
            "readiness",
            "reservations",
            "sources",
            "draft-review",
            "external-confirmation",
        )
    }
    return {"schema": "trippilot-design-review/v1", "screens": screens, "issues": issues or {"P1": [], "P2": [], "P3": []}}


def evidence(**overrides: object) -> dict:
    payload = {
        "tokenContract": "pass",
        "unitLintBuild": "pass",
        "goldenVerify": "pass",
        "captures": [],
        "physicalInstrumentation": False,
    }
    payload.update(overrides)
    return payload


def main() -> int:
    ok_code, ok_state = run(approved_review(), evidence())
    if ok_code != 0 or ok_state.get("status") != "completed":
        raise SystemExit(f"expected completed fixture, got {ok_code} {ok_state}")

    p1_code, p1_state = run(approved_review({"P1": ["token-runtime mismatch"], "P2": [], "P3": []}), evidence())
    if p1_code == 0 or p1_state.get("status") != "blocked":
        raise SystemExit("P1 fixture must block")

    p2_code, p2_state = run(approved_review({"P1": [], "P2": ["cta clipped"], "P3": []}), evidence())
    if p2_code == 0 or p2_state.get("status") != "blocked":
        raise SystemExit("P2 fixture must block")

    missing_code, _ = run(approved_review(), evidence(tokenContract="fail"))
    if missing_code == 0:
        raise SystemExit("token failure must block")

    physical_code, _ = run(approved_review(), evidence(physicalInstrumentation=True))
    if physical_code == 0:
        raise SystemExit("physical instrumentation must block")

    notes_code, notes_state = run(approved_review({"P1": [], "P2": [], "P3": ["600dp alignment"]}), evidence())
    if notes_code != 0 or notes_state.get("status") != "passed-with-notes":
        raise SystemExit("P3-only fixture must pass with notes")

    unapproved = json.loads((ROOT / "design/audit/design-review.json").read_text(encoding="utf-8"))
    pending_code, _ = run(unapproved, evidence())
    if pending_code == 0:
        raise SystemExit("unapproved visual review must block")

    print("PASS: design-loop state fixtures")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
