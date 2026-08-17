# Phase 5 screenshot goldens

This directory contains the four approved **API 28+ emulator, light-theme, 1.0x font-scale** UI
baselines used by `Phase5ScreenshotGoldenTest`. Compose cannot capture dialog windows below API
28, so API 26 is intentionally rejected by the runner.

- `01-trip-list-empty.png` — local-only empty list
- `02-trip-summary.png` — fixed-date local trip summary
- `03-draft-review.png` — fake structured draft review (no network/runtime credential)
- `04-external-confirmation.png` — backup confirmation before the Android SAF picker

Update them only after a deliberate visual review:

```bash
scripts/run_phase5_screenshot_golden.sh update
```

The script refuses physical devices, clears only the debug emulator app package, fixes the
emulator to light/1.0x, captures candidates, copies them into this directory, then reruns the
same test in compare mode. The image pixels are test fixtures, not app assets and are never
packaged in the release APK.
