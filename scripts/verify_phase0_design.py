#!/usr/bin/env python3
"""Deterministic checks for the Phase 0 TripPilot design contract."""

from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TOKENS = ROOT / "design" / "tokens.json"
MANIFEST = ROOT / "docs" / "asset-manifest.md"
SCREEN_MAP = ROOT / "design" / "screen-map.md"
REQUIRED_DOCS = (
    ROOT / "design" / "design-direction.md",
    ROOT / "design" / "tokens.md",
    SCREEN_MAP,
    MANIFEST,
    ROOT / "docs" / "test-matrix.md",
)
REQUIRED_ASSETS = (
    "app-mark.svg",
    "route-ribbon.svg",
    "empty-trips.svg",
    "empty-itinerary.svg",
    "ai-connection-required.svg",
)
PARITY_IDS = tuple(f"PAR-{index:02d}" for index in range(1, 18))


def relative_luminance(hex_color: str) -> float:
    value = hex_color.lstrip("#")
    if not re.fullmatch(r"[0-9A-Fa-f]{6}", value):
        raise ValueError(f"Invalid hex color: {hex_color}")

    def channel(component: str) -> float:
        raw = int(component, 16) / 255.0
        return raw / 12.92 if raw <= 0.04045 else ((raw + 0.055) / 1.055) ** 2.4

    red, green, blue = channel(value[0:2]), channel(value[2:4]), channel(value[4:6])
    return 0.2126 * red + 0.7152 * green + 0.0722 * blue


def contrast_ratio(first: str, second: str) -> float:
    light, dark = sorted((relative_luminance(first), relative_luminance(second)), reverse=True)
    return (light + 0.05) / (dark + 0.05)


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    raise SystemExit(1)


def main() -> None:
    for document in REQUIRED_DOCS:
        if not document.is_file():
            fail(f"required design document is missing: {document.relative_to(ROOT)}")

    tokens = json.loads(TOKENS.read_text(encoding="utf-8"))
    for mode in ("light", "dark"):
        colors = tokens["color"][mode]
        for background, foreground, minimum in tokens["contrastPairs"]:
            ratio = contrast_ratio(colors[background], colors[foreground])
            if ratio < float(minimum):
                fail(
                    f"{mode} {foreground} on {background} contrast {ratio:.2f}:1 "
                    f"is below {minimum}:1"
                )
            print(f"PASS: {mode} {foreground} on {background} = {ratio:.2f}:1")

    manifest = MANIFEST.read_text(encoding="utf-8")
    for asset_name in REQUIRED_ASSETS:
        relative_path = f"design/assets/{asset_name}"
        asset = ROOT / relative_path
        if not asset.is_file():
            fail(f"required asset is missing: {relative_path}")
        source = asset.read_text(encoding="utf-8")
        if re.search(
            r"(?:href|xlink:href)\s*=\s*[\"'](?:https?:)?//",
            source,
            flags=re.IGNORECASE,
        ):
            fail(f"remote URL found in SVG: {relative_path}")
        digest = hashlib.sha256(asset.read_bytes()).hexdigest()
        if relative_path not in manifest:
            fail(f"asset is not registered in manifest: {relative_path}")
        if digest not in manifest:
            fail(f"asset hash is absent or stale in manifest: {relative_path}")
        print(f"PASS: manifest registration and local-only source for {relative_path}")

    screen_map = SCREEN_MAP.read_text(encoding="utf-8")
    for parity_id in PARITY_IDS:
        if parity_id not in screen_map:
            fail(f"screen map does not trace {parity_id}")
    for contract_marker in ("RouteRibbon", "TalkBack", "360dp", "600dp", "2.0x"):
        if contract_marker not in screen_map:
            fail(f"screen map missing design contract marker: {contract_marker}")
    print("PASS: all applied/modified parity contracts have a screen and test trace")
    print("PASS: Phase 0 design contract verification completed")


if __name__ == "__main__":
    main()
