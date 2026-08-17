#!/usr/bin/env python3
"""Static companion check for the Phase 5 screenshot regression contract."""

from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
TEST = ROOT / "app/src/androidTest/kotlin/io/trippilot/app/Phase5ScreenshotGoldenTest.kt"
RUNNER = ROOT / "scripts/run_phase5_screenshot_golden.sh"
GOLDENS = ROOT / "app/src/androidTest/assets/screenshot-goldens"
EXPECTED = [
    "01-trip-list-empty.png",
    "02-trip-summary.png",
    "03-draft-review.png",
    "04-external-confirmation.png",
]

errors: list[str] = []
source = TEST.read_text(encoding="utf-8") if TEST.exists() else ""
for image in EXPECTED:
    path = GOLDENS / image
    if not path.is_file() or path.stat().st_size < 1024:
        errors.append(f"missing or too-small golden: {path.relative_to(ROOT)}")
    elif path.read_bytes()[:8] != b"\x89PNG\r\n\x1a\n":
        errors.append(f"golden is not a PNG: {path.relative_to(ROOT)}")
    if image not in source:
        errors.append(f"test does not reference {image}")

if "updateGoldens" not in source or "captureToImage" not in source:
    errors.append("test must use explicit updateGoldens mode and Compose captureToImage")
if "Build.VERSION_CODES.P" not in source or "assumeTrue" not in source:
    errors.append("test must skip API < 28; dialog capture is not supported there")
if "MAX_CHANGED_PERCENT" not in source or "MAX_AVERAGE_CHANNEL_DELTA" not in source:
    errors.append("test must define bounded image-difference thresholds")

runner = RUNNER.read_text(encoding="utf-8") if RUNNER.exists() else ""
if not RUNNER.exists() or not (RUNNER.stat().st_mode & 0o111):
    errors.append("golden runner must be executable")
for required in ("Refusing non-emulator", "uninstall", "uimode night no", "font_scale 1.0", "updateGoldens"):
    if required not in runner:
        errors.append(f"golden runner missing guard: {required}")

if errors:
    print("Phase 5 screenshot golden verification failed:", file=sys.stderr)
    print("\n".join(f"- {error}" for error in errors), file=sys.stderr)
    sys.exit(1)
print("Phase 5 screenshot golden verification passed.")
