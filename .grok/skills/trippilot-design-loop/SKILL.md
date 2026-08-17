---
name: trippilot-design-loop
description: Run TripPilot's closed Android design QA loop. Use when the user asks for 디자인 루프, 디자인 QA, 토큰 계약, golden 검수, or runs /trippilot-design-loop.
---

# TripPilot design loop

Detect token-runtime drift, visual regression, and accessibility gaps before calling the work done. Do not redesign the product or generate images.

## When to run what

- Large UI / new screen: full loop via `scripts/run_android_design_qa.sh`
- Small UI tweak: `python3 scripts/verify_design_contract.py`, then `:app` unit/lint, then the golden verify for any touched core screen
- Asset-only change: use `trippilot-design-assets`, then re-check journey shots 5–6 if empty art changed

## Order

1. Read `design/tokens.json`, `design/screen-map.md`, `design/audit/design-qa.config.json`
2. Static contract: `python3 scripts/verify_design_contract.py` and `python3 scripts/verify_phase0_design.py`
3. `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
4. Emulator only (`ANDROID_SERIAL=emulator-*`): `:app:connectedDebugAndroidTest`, journey capture, golden verify
5. `python3 scripts/sync_design_run_state.py`
6. Stop if `RUN_STATE` is not `completed` or `passed-with-notes`

## Blockers

- P1 or P2 open → not complete
- Missing required capture, token failure, golden mismatch, or unapproved visual review → not complete
- P3 only → `passed-with-notes`
- Serial is not `emulator-*` → fail before uninstall or instrumentation
- User device: `adb install -r` only. No connected tests, uninstall, or clear

## Visual approval

`design/audit/design-review.json` is the versioned review template. A human sets each required screen `approved` to true after looking at the contact sheet. `app/build/reports/qa/design-loop/RUN_STATE.json` is generated and must stay untracked.

Do not auto-update goldens. Use `scripts/run_phase5_screenshot_golden.sh update` only after review.

## Forbidden

Playwright, `index.html`, required image generation, Room/OAuth/Calendar changes, physical instrumentation.
