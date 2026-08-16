#!/usr/bin/env python3
"""Static Phase 2 local-first and explicit-share-boundary checks."""

from __future__ import annotations

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
SOURCE = ROOT / "app/src/main/kotlin/io/trippilot/app"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    raise SystemExit(1)


def main() -> None:
    manifest = MANIFEST.read_text(encoding="utf-8")
    if "android.permission.INTERNET" in manifest:
        fail("Phase 2 local MVP must not request INTERNET permission")
    if 'android:mimeType="text/plain"' not in manifest or "android.intent.action.SEND" not in manifest:
        fail("plain-text share intake intent is missing")
    if 'android:mimeType="*/*"' in manifest:
        fail("share intake must not accept all MIME types")

    local_roots = (SOURCE / "core/data", SOURCE / "core/model", SOURCE / "core/design", SOURCE / "feature")
    content = "\n".join(
        path.read_text(encoding="utf-8")
        for root in local_roots
        for path in root.rglob("*.kt")
    )
    forbidden = ("ACTION_VIEW", "Browser", "WebView", "http://localhost", "127.0.0.1")
    for marker in forbidden:
        if marker in content:
            fail(f"Phase 2 source contains a non-local external action marker: {marker}")
    for marker in ("accessToken", "refreshToken", "auth.json", "credential"):
        if marker.lower() in content.lower():
            fail(f"Phase 2 source contains forbidden credential storage marker: {marker}")

    backup = (SOURCE / "core/data/TripBackup.kt").read_text(encoding="utf-8")
    for marker in ("MAX_BYTES = 2 * 1024 * 1024", "restoreInputs", "SCHEMA = \"trippilot.trip-backup\""):
        if marker not in backup:
            fail(f"backup contract marker missing: {marker}")

    print("PASS: manifest exposes text/plain SEND only and has no INTERNET permission")
    print("PASS: Phase 2 source has no browser/map/localhost action or credential storage path")
    print("PASS: local backup size/schema/new-copy contract is present")


if __name__ == "__main__":
    main()
