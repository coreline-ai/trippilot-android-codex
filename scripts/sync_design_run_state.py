#!/usr/bin/env python3
"""Aggregate design-loop evidence into RUN_STATE.json."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_OUT = ROOT / "app/build/reports/qa/design-loop"


def fail(message: str, code: int = 1) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(code)


def load(path: Path) -> dict:
    if not path.is_file():
        fail(f"missing {path}")
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        fail(f"invalid JSON {path}: {error}")
    raise AssertionError


STATIC_SLOP_GATES = (
    "3-inline-literals",
    "4-repeated-surface-rhythm",
    "5-multiple-primary-actions",
    "10-infinite-animation",
    "12-ellipsis-on-utility-text",
    "15-content-system-registration",
)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default=str(ROOT))
    parser.add_argument("--review", default="design/audit/design-review.json")
    parser.add_argument("--qa-config", default="design/audit/design-qa.config.json")
    parser.add_argument("--evidence", help="optional JSON with test/capture flags")
    parser.add_argument("--out-dir", default=str(DEFAULT_OUT))
    args = parser.parse_args()
    root = Path(args.root)
    review = load(root / args.review)
    qa = load(root / args.qa_config)
    evidence = load(Path(args.evidence)) if args.evidence else {
        "tokenContract": "pass",
        "unitLintBuild": "pass",
        "goldenVerify": "pass",
        "captures": [],
        "physicalInstrumentation": False,
    }

    out_dir = Path(args.out_dir)

    issues = {
        "P1": list(review.get("issues", {}).get("P1", [])),
        "P2": list(review.get("issues", {}).get("P2", [])),
        "P3": list(review.get("issues", {}).get("P3", [])),
    }
    blockers: list[str] = []

    if evidence.get("physicalInstrumentation"):
        issues["P1"].append("physical instrumentation attempted")
    if evidence.get("tokenContract") != "pass":
        issues["P1"].append("token contract failed")
        blockers.append("tokenContract")
    if evidence.get("goldenVerify") == "fail":
        issues["P1"].append("required golden failure")
        blockers.append("goldenVerify")

    # Static slop gates (hallmark-guide.md §4) are deterministic; a failed or
    # unrecorded static gate is a P2 block. Visual gates stay pending until a
    # human approves the screens — that is handled by the screens map below.
    slop_gates = review.get("slopGates", {})
    for key in STATIC_SLOP_GATES:
        status = slop_gates.get(key, "missing")
        if status != "pass":
            issues["P2"].append(f"slop gate {key} {status}")
            blockers.append(f"slop:{key}")

    required_goldens = qa["golden"]["requiredExisting"]
    present = set(evidence.get("captures", []))
    golden_dir = root / "app/src/androidTest/assets/screenshot-goldens"
    for name in required_goldens:
        if not (golden_dir / name).is_file():
            issues["P1"].append(f"required capture missing: {name}")
            blockers.append(name)

    unapproved = [key for key, screen in review.get("screens", {}).items() if not screen.get("approved")]
    if unapproved:
        blockers.append("visual review unapproved")

    if issues["P1"] or issues["P2"] or blockers:
        status = "blocked"
    elif issues["P3"]:
        status = "passed-with-notes"
    else:
        status = "completed"

    state = {
        "schema": "trippilot-design-run-state/v1",
        "status": status,
        "blockers": blockers,
        "issues": issues,
        "unapprovedScreens": unapproved,
        "requiredGoldensPresent": all((golden_dir / name).is_file() for name in required_goldens),
        "optionalGoldens": {
            name: (golden_dir / name).is_file()
            for name in qa["golden"]["files"]
            if name not in required_goldens
        },
        "capturesSeen": sorted(present),
    }
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "RUN_STATE.json").write_text(json.dumps(state, indent=2) + "\n", encoding="utf-8")
    summary = [
        f"# Design loop state: {status}",
        "",
        f"- blockers: {', '.join(blockers) if blockers else 'none'}",
        f"- P1: {len(issues['P1'])}",
        f"- P2: {len(issues['P2'])}",
        f"- P3: {len(issues['P3'])}",
    ]
    (out_dir / "RUN_STATE.md").write_text("\n".join(summary) + "\n", encoding="utf-8")
    print(f"{status}: wrote {out_dir / 'RUN_STATE.json'}")
    return 0 if status in {"completed", "passed-with-notes"} else 1


if __name__ == "__main__":
    raise SystemExit(main())
