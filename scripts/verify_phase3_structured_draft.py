#!/usr/bin/env python3
"""Static Phase 3 contract, no-raw-persistence, and no-external-action checks."""

from __future__ import annotations

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/kotlin/io/trippilot/app"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    raise SystemExit(1)


def read(path: Path) -> str:
    if not path.is_file():
        fail(f"missing required file: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def main() -> None:
    contract = read(SOURCE / "integration/codex/contract/TripDraftContract.kt")
    parser = read(SOURCE / "integration/codex/contract/TripDraftParser.kt")
    runtime = read(SOURCE / "core/codex/FakeCodexRuntime.kt")
    review = read(SOURCE / "feature/drafts/DraftPlannerSection.kt")
    repository = read(SOURCE / "core/data/TripRepository.kt")
    privacy = read(ROOT / "docs/privacy-and-egress.md")

    for marker in (
        "data class TripPlanningRequest", "data class TripPlanDraft", "data class DraftDay",
        "data class DraftItineraryItem", "data class DraftReservation", "data class DraftPackingSuggestion",
        "data class DraftPreparationSuggestion", "data class SourceCandidate", "data class WeatherAdvisoryDraft",
        "data class ApprovedDraftSelection",
    ):
        if marker not in contract:
            fail(f"versioned contract marker missing: {marker}")
    for marker in ("ignoreUnknownKeys = false", "validateTripPlan", "validateWeatherAdvisory", "validateApprovedSelection"):
        if marker not in parser:
            fail(f"strict parser/validator marker missing: {marker}")
    for marker in ("FakeCodexScenario", "CONTRACT_VIOLATION", "LATE_COMPLETION", "DraftStreamEvent.Stopped"):
        if marker not in runtime:
            fail(f"fake stream fixture marker missing: {marker}")
    for marker in ("구조화 JSON 직접 붙여넣기", "선택한 항목만 여행에 반영", "날씨 참고 (정보성)", "contentDescription = \"$summary 선택\""):
        if marker not in review:
            fail(f"review/approval UI marker missing: {marker}")
    for marker in ("database.withTransaction", "applyApprovedDraft", "ItemOrigin.AI"):
        if marker not in repository:
            fail(f"approved local transaction marker missing: {marker}")
    if "rawJson" in repository:
        fail("raw JSON must not reach the Room repository")

    database_text = "\n".join(path.read_text(encoding="utf-8") for path in (SOURCE / "core/data/db").rglob("*.kt"))
    for marker in ("Prompt", "Response", "OAuth", "Token", "Credential", "DraftEntity"):
        if marker in database_text:
            fail(f"database contains prohibited transient/credential persistence marker: {marker}")

    # Phase 5 intentionally owns user-confirmed system handoffs in dedicated external files.
    # This verifier keeps proving that the Phase 3 draft path itself has no egress or logging.
    phase3_roots = (
        SOURCE / "core/codex",
        SOURCE / "core/data",
        SOURCE / "integration/codex/contract",
    )
    app_text = "\n".join(
        path.read_text(encoding="utf-8")
        for root in phase3_roots
        for path in root.rglob("*.kt")
    ) + "\n" + read(SOURCE / "feature/drafts/TripDraftViewModel.kt")
    for marker in ("ACTION_VIEW", "WebView", "http://localhost", "127.0.0.1", "android.util.Log", "println("):
        if marker in app_text:
            fail(f"Phase 3 source contains forbidden egress/log marker: {marker}")
    if "Device OAuth" not in privacy or "원문" not in privacy or "사용자" not in privacy:
        fail("privacy and egress document lacks required boundary disclosure")
    print("PASS: versioned structured draft contract and strict parser are present")
    print("PASS: fake stream and user-review-only UI contracts are present")
    print("PASS: Room has no draft/prompt/response/OAuth persistence and Phase 3 has no external action/log path")


if __name__ == "__main__":
    main()
