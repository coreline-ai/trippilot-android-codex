#!/usr/bin/env python3
"""Validate tokens.json, audit contracts, and Compose theme mapping."""

from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TOKENS = ROOT / "design" / "tokens.json"
THEME = ROOT / "app/src/main/kotlin/io/trippilot/app/core/design/TripPilotTheme.kt"
TYPE = ROOT / "app/src/main/kotlin/io/trippilot/app/core/design/TripPilotTypography.kt"
QA = ROOT / "design/audit/design-qa.config.json"
CONTENT = ROOT / "design/audit/content-system.json"
ASSETS = ROOT / "design/audit/asset-manifest.json"
REVIEW = ROOT / "design/audit/design-review.json"
SCREEN_MAP = ROOT / "design/screen-map.md"
ASSET_DOC = ROOT / "docs/asset-manifest.md"
TEST_TOKENS = ROOT / "app/src/androidTest/assets/design-tokens.json"
SKILL = ROOT / ".grok/skills/trippilot-design-loop/SKILL.md"


def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)


def load_json(path: Path) -> dict:
    if not path.is_file():
        fail(f"missing {path.relative_to(ROOT)}")
    return json.loads(path.read_text(encoding="utf-8"))


def hex_from_color_ctor(source: str, name: str) -> str | None:
    match = re.search(rf"private val {name} = Color\(0x([0-9A-Fa-f]{{8}})\)", source)
    if match:
        return f"#{match.group(1)[2:].upper()}"
    return None


def main() -> int:
    tokens = load_json(TOKENS)
    qa = load_json(QA)
    content = load_json(CONTENT)
    assets = load_json(ASSETS)
    review = load_json(REVIEW)
    screen_map = SCREEN_MAP.read_text(encoding="utf-8")
    theme = THEME.read_text(encoding="utf-8")
    typography = TYPE.read_text(encoding="utf-8")

    for mode in ("light", "dark"):
        colors = tokens["color"][mode]
        for key in (
            "primary", "onPrimary", "wayfinding", "onWayfinding", "boarding", "onBoarding",
            "ai", "onAi", "success", "error", "surface", "onSurface",
        ):
            if key not in colors:
                fail(f"tokens.json {mode}.{key} missing")
    for token_name, size in tokens["typography"].items():
        if int(size["sizeSp"]) <= 0:
            fail(f"typography {token_name} size must be positive")
    if tokens["shapeDp"]["action"] != 14 or tokens["shapeDp"]["surface"] != 20 or tokens["shapeDp"]["hero"] != 28:
        fail("shapeDp action/surface/hero must stay 14/20/28")
    if tokens["layout"]["minTouchTargetDp"] != 48:
        fail("minTouchTargetDp must stay 48")
    if qa["touch"]["primaryActionMinDp"] != 52:
        fail("primaryActionMinDp must stay 52")

    named = {
        "PilotNavy": hex_from_color_ctor(theme, "PilotNavy"),
        "CloudPaper": hex_from_color_ctor(theme, "CloudPaper"),
        "SurfaceInk": hex_from_color_ctor(theme, "SurfaceInk"),
        "Cyan": hex_from_color_ctor(theme, "Cyan"),
        "BoardingOrange": hex_from_color_ctor(theme, "BoardingOrange"),
        "StampViolet": hex_from_color_ctor(theme, "StampViolet"),
        "SignalRed": hex_from_color_ctor(theme, "SignalRed"),
    }
    expected_named = {
        "PilotNavy": tokens["color"]["light"]["primary"].upper(),
        "CloudPaper": tokens["color"]["light"]["surface"].upper(),
        "SurfaceInk": tokens["color"]["light"]["onSurface"].upper(),
        "Cyan": tokens["color"]["light"]["wayfinding"].upper(),
        "BoardingOrange": tokens["color"]["light"]["boarding"].upper(),
        "StampViolet": tokens["color"]["light"]["ai"].upper(),
        "SignalRed": tokens["color"]["light"]["error"].upper(),
    }
    for name, expected in expected_named.items():
        actual = named[name]
        if actual != expected:
            fail(f"TripPilotTheme {name} is {actual}, tokens.json wants {expected}")

    if "secondary = Cyan" not in theme or "tertiary = StampViolet" not in theme:
        fail("wayfinding/ai must map to ColorScheme secondary/tertiary")
    if "RoundedCornerShape(14.dp)" not in theme or "RoundedCornerShape(20.dp)" not in theme or "RoundedCornerShape(28.dp)" not in theme:
        fail("theme shapes must include 14/20/28 dp")
    if "TripPilotActionShape = RoundedCornerShape(14.dp)" not in theme:
        fail("TripPilotActionShape must stay 14.dp")
    for marker, size in (("displaySmall", 28), ("headlineSmall", 22), ("titleMedium", 17), ("bodyLarge", 16), ("labelLarge", 14), ("labelMedium", 13)):
        if f"{marker} = TextStyle(" not in typography or f"fontSize = {size}.sp" not in typography:
            fail(f"TripPilotTypography {marker} must stay {size}.sp")

    areas = {area["id"] for area in content["areas"]}
    if areas != {"journey", "prepare", "storage", "help"}:
        fail("content-system.json must contain the four purpose areas")
    if len(content.get("journeyCaptures", [])) != 8:
        fail("content-system.json must map all 8 Design Journey captures")
    for area in content["areas"]:
        if area["label"] not in screen_map:
            fail(f"screen-map.md missing area label {area['label']}")
        app_source = (ROOT / "app/src/main/kotlin/io/trippilot/app/core/design/TripPilotApp.kt").read_text(encoding="utf-8")
        generated_area_tag = 'testTag("trip_area_${candidate.name.lowercase()}")'
        if area["testTag"] not in app_source and generated_area_tag not in app_source:
            fail(f"TripPilotApp.kt missing area testTag {area['testTag']}")

    doc = ASSET_DOC.read_text(encoding="utf-8")
    for asset in assets["assets"]:
        source = ROOT / asset["source"]
        if not source.is_file():
            fail(f"missing asset source {asset['source']}")
        if asset["source"] not in doc:
            fail(f"docs/asset-manifest.md missing {asset['source']}")
        if asset.get("runtime"):
            runtime = ROOT / asset["runtime"]
            if not runtime.is_file():
                fail(f"missing runtime asset {asset['runtime']}")

    if TEST_TOKENS.is_file():
        if hashlib.sha256(TOKENS.read_bytes()).digest() != hashlib.sha256(TEST_TOKENS.read_bytes()).digest():
            fail("androidTest design-tokens.json must match design/tokens.json")
    else:
        fail("app/src/androidTest/assets/design-tokens.json is missing")

    if not SKILL.is_file():
        fail("trippilot-design-loop skill is missing")
    skill = SKILL.read_text(encoding="utf-8")
    required_section, _, _ = skill.partition("## Forbidden")
    for banned in ("Playwright", "index.html", "data-frame"):
        if banned in required_section:
            fail(f"design-loop skill must not require {banned}")
    if "emulator-*" not in skill:
        fail("design-loop skill must force emulator-*")

    required_review = set(review["screens"])
    if "list-empty" not in required_review or "itinerary" not in required_review:
        fail("design-review.json is missing required screens")

    print("PASS: tokens.json keys, theme mapping, content system, and audit contracts")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
