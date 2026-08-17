# TripPilot Baseline Profile generator

This test-only module profiles only local flows: startup, list, trip creation/detail, and the fake
structured-draft review. It has no Codex runtime/OAuth, network, Calendar, browser, map, or file
action path. The same review screen is additionally guarded by the deterministic Compose golden.

Run the guarded local command from an **API 33+ emulator**:

```bash
scripts/run_baseline_profile.sh
```

It refuses physical devices. The generated rules are filtered to `io.trippilot.app.**` and stored
by the app's Baseline Profile Gradle plugin in `app/src/main/generated/baselineProfiles/`.

Measure the cold-start candidate separately with:

```bash
scripts/run_startup_benchmark.sh
```

The measurement writes Macrobenchmark JSON/CSV output under
`baselineprofile/build/outputs/connected_android_test_additional_output/`; it is a reproducible
API 35 emulator signal, not a physical-device performance claim.
