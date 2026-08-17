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
GUIDE = ROOT / "design/hallmark-guide.md"
UI_SOURCE_DIRS = (
    ROOT / "app/src/main/kotlin/io/trippilot/app/core/design",
    ROOT / "app/src/main/kotlin/io/trippilot/app/feature",
)
FUN_HEADER = re.compile(r"^(?:@\w+\s+)?(?:private |internal |public )?fun\s+(\w+)", re.MULTILINE)


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


def ui_sources() -> list[tuple[Path, str]]:
    files: list[tuple[Path, str]] = []
    for directory in UI_SOURCE_DIRS:
        for path in sorted(directory.rglob("*.kt")):
            if path.name == "TripPilotTheme.kt":
                continue
            files.append((path, path.read_text(encoding="utf-8")))
    if not files:
        fail("no UI sources found under core/design or feature")
    return files


def gate_inline_literals(sources: list[tuple[Path, str]]) -> None:
    """Gate 3 — no Color(0x...) literals outside TripPilotTheme.kt."""
    for path, source in sources:
        for match in re.finditer(r"Color\(0x[0-9A-Fa-f]{6,8}\)", source):
            line = source.count("\n", 0, match.start()) + 1
            fail(f"{path.relative_to(ROOT)}:{line} inline Color literal {match.group(0)} "
                 f"(hallmark-guide.md §gate-3: register the value in tokens.json and reference the token)")


def gate_single_primary_action(sources: list[tuple[Path, str]]) -> None:
    """Gate 5 — at most one PrimaryAction call per top-level function.

    Preview/showcase functions are exempt: they document the eight interaction
    states (hallmark-guide.md §3), they are not screens.
    """
    for path, source in sources:
        headers = list(FUN_HEADER.finditer(source))
        for index, header in enumerate(headers):
            name = header.group(1)
            if name == "PrimaryAction" or "Preview" in name or "Showcase" in name:
                continue
            body_start = header.end()
            body_end = headers[index + 1].start() if index + 1 < len(headers) else len(source)
            uses = len(re.findall(r"\bPrimaryAction\s*\(", source[body_start:body_end]))
            if uses > 1:
                line = source.count("\n", 0, body_start) + 1
                fail(f"{path.relative_to(ROOT)}:{line} function {name}() has {uses} PrimaryAction calls "
                     f"(hallmark-guide.md §gate-5: one primary action per screen)")


def gate_no_infinite_animation(sources: list[tuple[Path, str]]) -> None:
    """Gate 10 — no infinite/repeating animations."""
    banned = ("rememberInfiniteTransition", "infiniteRepeatable", "RepeatMode.Restart", "RepeatMode.Reverse")
    for path, source in sources:
        for pattern in banned:
            for match in re.finditer(re.escape(pattern), source):
                line = source.count("\n", 0, match.start()) + 1
                fail(f"{path.relative_to(ROOT)}:{line} uses {pattern} "
                     f"(hallmark-guide.md §gate-10: no infinite or repeating animation)")


def gate_no_ellipsis_on_utility_text(sources: list[tuple[Path, str]]) -> None:
    """Gate 12 — monospace utility text (labelMedium) must not ellipsize."""
    for path, source in sources:
        for match in re.finditer(r"Text\((?:[^()]|\([^()]*\))*\)", source):
            call = match.group(0)
            if "TextOverflow.Ellipsis" not in call:
                continue
            if "labelMedium" in call or "FontFamily.Monospace" in call:
                line = source.count("\n", 0, match.start()) + 1
                fail(f"{path.relative_to(ROOT)}:{line} utility (monospace) text uses ellipsis "
                     f"(hallmark-guide.md §gate-12: dates/times/codes must not hide meaning)")


def gate_repeated_surface_rhythm(sources: list[tuple[Path, str]]) -> None:
    """Gate 4 — no three-plus consecutive calls of the same surface component.

    Structural-variety rule (hallmark-guide.md §2): information with a
    different shape must not collapse into card-after-card. Language
    keywords and control flow are not components. Blank lines do not break
    a run; any other statement does.
    """
    keywords = {"if", "for", "while", "when", "return", "else"}
    # Primitives and layout helpers carry no surface identity; the gate targets
    # card/panel/document surfaces only (hallmark-guide.md §2).
    primitives = {
        "Text", "Spacer", "Icon", "Image", "Divider", "HorizontalDivider", "VerticalDivider",
        "Box", "Row", "Column", "LazyColumn", "LazyRow", "Checkbox", "RadioButton", "Switch",
        "Button", "TextButton", "OutlinedButton", "IconButton", "OutlinedTextField", "TextField",
    }
    for path, source in sources:
        headers = list(FUN_HEADER.finditer(source))
        for index, header in enumerate(headers):
            name = header.group(1)
            if "Preview" in name or "Showcase" in name:
                continue
            body_start = header.end()
            body = source[body_start:headers[index + 1].start() if index + 1 < len(headers) else len(source)]
            base_line = source.count("\n", 0, body_start) + 1
            run_name: str | None = None
            run_len = 0
            run_line = 0
            for offset, line in enumerate(body.splitlines()):
                match = re.match(r"^\s*(\w+)\s*\(", line)
                if match and match.group(1) not in keywords and match.group(1) not in primitives:
                    call = match.group(1)
                    if call == run_name:
                        run_len += 1
                        if run_len >= 3:
                            fail(f"{path.relative_to(ROOT)}:{base_line + run_line} function {name}() calls "
                                 f"{call}() {run_len} lines in a row "
                                 f"(hallmark-guide.md §gate-4: break repeated surface rhythm)")
                    else:
                        run_name, run_len, run_line = call, 1, offset
                elif match is None and line.strip() == "":
                    continue
                else:
                    run_name, run_len = None, 0


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
    if "slopGates" not in review or len(review["slopGates"]) != 15:
        fail("design-review.json must carry the 15 slopGates (hallmark-guide.md §4)")
    if not GUIDE.is_file():
        fail("design/hallmark-guide.md is missing")

    sources = ui_sources()
    gate_inline_literals(sources)
    gate_repeated_surface_rhythm(sources)
    gate_single_primary_action(sources)
    gate_no_infinite_animation(sources)
    gate_no_ellipsis_on_utility_text(sources)

    for area in content["areas"]:
        for page in area.get("pages", []):
            if not page.get("macrostructure"):
                fail(f"content-system.json page {page['id']} is missing its macrostructure "
                     f"(hallmark-guide.md §gate-15: register every screen)")

    print("PASS: tokens.json keys, theme mapping, content system, audit contracts, slop gates 3/4/5/10/12/15")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
