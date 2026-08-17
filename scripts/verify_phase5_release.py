#!/usr/bin/env python3
"""Static verification for Phase 5 explicit approval and local privacy boundaries."""

from __future__ import annotations

from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/kotlin/io/trippilot/app"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    raise SystemExit(1)


def read(relative: str) -> str:
    path = ROOT / relative
    if not path.is_file():
        fail(f"missing required file: {relative}")
    return path.read_text(encoding="utf-8")


def main() -> None:
    manifest = read("app/src/main/AndroidManifest.xml")
    external_ui = read("app/src/main/kotlin/io/trippilot/app/feature/external/ExternalActionsSection.kt")
    calendar = read("app/src/main/kotlin/io/trippilot/app/core/external/CalendarWriteCoordinator.kt")
    handoff = read("app/src/main/kotlin/io/trippilot/app/core/external/ExternalHandoff.kt")
    ics = read("app/src/main/kotlin/io/trippilot/app/core/external/IcsCodec.kt")
    reminder = read("app/src/main/kotlin/io/trippilot/app/core/reminders/ReadinessReminderCoordinator.kt")
    privacy = read("docs/privacy-and-egress.md")
    backup = read("app/src/main/kotlin/io/trippilot/app/core/data/TripBackup.kt")
    app_gradle = read("app/build.gradle.kts")
    baseline_generator = read("baselineprofile/src/main/kotlin/io/trippilot/app/baselineprofile/BaselineProfileGenerator.kt")
    startup_benchmark = read("baselineprofile/src/main/kotlin/io/trippilot/app/baselineprofile/StartupBenchmark.kt")
    baseline_runner = read("scripts/run_baseline_profile.sh")
    benchmark_runner = read("scripts/run_startup_benchmark.sh")
    golden_runner = read("scripts/run_phase5_screenshot_golden.sh")
    performance = read("docs/performance-baseline.md")

    for marker in (
        'android:allowBackup="false"', 'android:fullBackupContent="false"', 'android:dataExtractionRules="@xml/data_extraction_rules"',
        'android:usesCleartextTraffic="false"', 'android.permission.WRITE_CALENDAR', 'android.permission.POST_NOTIFICATIONS',
        'android.permission.RECEIVE_BOOT_COMPLETED',
    ):
        if marker not in manifest:
            fail(f"manifest privacy/permission marker missing: {marker}")
    if "android.permission.INTERNET" in manifest:
        fail("TripPilot must not request INTERNET permission")
    for marker in ("ConfirmActionSheet", "Calendar 반영 전 확인", "ICS 파일 내보내기", "로컬 백업 내보내기", "외부 앱으로 열기"):
        if marker not in external_ui:
            fail(f"explicit confirmation UI marker missing: {marker}")
    for marker in ("CalendarActionStatus.APPROVED", "CalendarActionStatus.EXECUTED", "CalendarActionStatus.FAILED", "containsMarker"):
        if marker not in calendar:
            fail(f"Calendar approval ledger marker missing: {marker}")
    for marker in ("Intent.ACTION_VIEW", "resolveActivity", "startActivity"):
        if marker not in handoff:
            fail(f"user-confirmed Android handoff marker missing: {marker}")
    for marker in ("BEGIN:VCALENDAR", "foldLines", "75 UTF-8 octets"):
        if marker not in ics:
            fail(f"ICS writer marker missing: {marker}")
    for marker in ("ReadinessReminderPolicy.evaluate", "setAndAllowWhileIdle", "resyncAllAfterBoot", "POST_NOTIFICATIONS"):
        if marker not in reminder:
            fail(f"reminder opt-in/boot marker missing: {marker}")
    for marker in ("Calendar", "SAF", "알림", "사용자 확인"):
        if marker not in privacy:
            fail(f"privacy document is missing Phase 5 disclosure: {marker}")

    for marker in ("baselineProfile(project(\":baselineprofile\"))", "libs.androidx.profileinstaller", "automaticGenerationDuringBuild = false", "saveInSrc = true"):
        if marker not in app_gradle:
            fail(f"Baseline Profile app configuration missing: {marker}")
    for marker in ("BaselineProfileRule", "초안 검토 열기", "여행 초안 만들기", "TRIPPILOT_PACKAGE"):
        if marker not in baseline_generator:
            fail(f"Baseline Profile local journey missing: {marker}")
    for marker in ("MacrobenchmarkRule", "StartupTimingMetric", "StartupMode.COLD", "iterations = 5"):
        if marker not in startup_benchmark:
            fail(f"cold-start measurement marker missing: {marker}")
    for name, script in (("baseline runner", baseline_runner), ("benchmark runner", benchmark_runner), ("golden runner", golden_runner)):
        for marker in ("Refusing non-emulator", "API", "ANDROID_SERIAL"):
            if marker not in script:
                fail(f"{name} must constrain execution to an emulator: {marker}")
    for image in ("01-trip-list-empty.png", "02-trip-summary.png", "03-draft-review.png", "04-external-confirmation.png"):
        image_path = ROOT / "app/src/androidTest/assets/screenshot-goldens" / image
        if not image_path.is_file() or image_path.stat().st_size < 1024:
            fail(f"approved screenshot golden missing: {image}")
    for profile_name in ("baseline-prof.txt", "startup-prof.txt"):
        profile = ROOT / "app/src/main/generated/baselineProfiles" / profile_name
        if not profile.is_file() or profile.stat().st_size < 20_000:
            fail(f"generated Baseline Profile missing or unexpectedly small: {profile_name}")
        profile_text = profile.read_text(encoding="utf-8")
        if "Lio/trippilot/app/" not in profile_text:
            fail(f"generated Baseline Profile does not include TripPilot rules: {profile_name}")
    for marker in ("timeToInitialDisplayMs", "emulator", "물리 기기", "ProfileInstaller"):
        if marker not in performance:
            fail(f"performance evidence is incomplete: {marker}")
    lowered_backup = backup.lower()
    for marker in ("val calendaraction", "val oauth", "val prompt", "val response", "token:", "credential:"):
        if marker in lowered_backup:
            fail(f"backup contains prohibited external/transient field marker: {marker}")
    print("PASS: Calendar/Intent/SAF/reminder routes are guarded by explicit confirmation UI")
    print("PASS: no INTERNET, cloud/D2D backup, cleartext, or transient external state enters backup")
    print("PASS: emulator-only screenshot and Baseline Profile evidence is present; physical release gate remains separate")


if __name__ == "__main__":
    main()
