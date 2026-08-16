# TripPilot 테스트 매트릭스

실행 명령과 결과는 기능 구현 시 이 문서에 누적한다. `PASS`는 실제 명령 종료 코드와 확인 결과가 남은 경우에만 쓴다. Phase 0은 문서·에셋 검증만 수행한다.

## Phase 0 결과

| ID | 검증 | 환경 / 명령 | 결과 | 증거 |
|---|---|---|---|---|
| T-DESIGN-01 | token AA 대비·manifest hash·외부 URL·필수 문서 | `python3 scripts/verify_phase0_design.py` | PASS | 2026-08-16, exit 0; 모든 semantic pair 4.75:1 이상, 5개 SVG local-only/hash 일치 |
| T-DESIGN-02 | 360/600/840dp, 1.0/1.3/2.0x wireframe 검토 | `design/screen-map.md` 수동 계약 검토 | PASS | 2026-08-16; compact/list-detail, text overflow 및 action 위치 규칙 확인 |
| T-DESIGN-03 | parity 행 ↔ screen/test ID 연결 | `python3 scripts/verify_phase0_design.py` | PASS | 2026-08-16, exit 0; PAR-01~PAR-17 화면·Phase·최소 test ID 확인 |
| T-DESIGN-04 | TalkBack·motion·token hardcoding 방지 계약 | design contract 수동 검토 | PASS | 2026-08-16; semantics, 48dp target, reduce-motion, semantic token 규칙 확인 |

## Phase 1 결과

| ID | 검증 | 환경 / 명령 | 결과 | 증거 |
|---|---|---|---|---|
| T-FOUNDATION-01 | unit test, lint, debug APK | `./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon` | PASS | 2026-08-16, exit 0; `app-debug.apk` 생성 |
| T-FOUNDATION-02 | OpenMinis/Grok/credential/raw CLI 경계 검색 | `python3 scripts/verify_phase1_independence.py` | PASS | 2026-08-16, exit 0; source·Gradle OpenMinis/Grok 0건, Port 금지 field 0건 |
| T-FOUNDATION-03 | clean checkout independent build | `git clone --no-local` 후 `ANDROID_HOME=... ./gradlew assembleDebug --no-daemon` | PASS | 2026-08-16, `/private/tmp/trippilot-phase1-clean.P4yhzv`; exit 0 + Phase 0/1 verifier PASS |
| T-RUNTIME-FAKE-01 | fake runtime 로그인·연결·로그아웃·stream 거부/완료 | `FakeCodexRuntimeTest` via `testDebugUnitTest` | PASS | 2026-08-16, 3 unit tests passed |
| T-RUNTIME-UI-01 | Compose runtime 화면, fake login/complete/logout | `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest --no-daemon` | PASS | 2026-08-16, TripPilot_API_26 (API 26 arm64) 1/1 passed |
| T-VISUAL-01 | light/dark component smoke | emulator `screencap` | PASS | `app/build/reports/qa/phase1-light.png`, `phase1-dark.png`; RouteRibbon, SVG empty state, Violet status 확인 |
| T-VISUAL-02 | compact 2.0x font / landscape width smoke | emulator `font_scale`, orientation + `screencap` | PASS | `phase1-font2-compact.png`, `phase1-light-landscape.png`; content vertical scroll, title/label clipping 없음 |
| T-PD20-01 | physical device Compose test 탐색 | PD20 (`0123456789ABCDEF`) `connectedDebugAndroidTest` | 환경 비통과 | 2026-08-16: Activity resume 상태가 확보되지 않아 Compose hierarchy 없음. APK 직접 설치·Activity process 기동은 확인했고, P1 Gate는 동일 UI test가 emulator에서 PASS한 것으로 충족. Phase 5 실기기 QA에서 재검증. |

## 이후 Phase 예약 매트릭스

| 영역 | 최소 환경 | 대표 검증 |
|---|---|---|
| 로컬 MVP | Android emulator + offline | T-TRIP-CRUD-01, T-ITINERARY-BOUNDARY-01, T-BACKUP-COPY-01 |
| 공유/외부 | Android emulator | T-SHARE-TEXT-ONLY-01, T-EXTERNAL-CONFIRM-01 |
| AI 계약 | local fake runtime | T-DRAFT-PARTIAL-APPLY-01, T-DRAFT-RESERVATION-01, T-WEATHER-READONLY-01 |
| Codex OAuth | arm64 real device, user-approved test account | OAuth start/cancel/return/logout/stream-stop |
| release QA | emulator + arm64 device | light/dark, 1.0/1.3/2.0x, TalkBack, process recreation, clean install |
# Test matrix

| Phase | Date | Scope | Command / evidence | Result |
|---|---|---|---|---|
| P0 | 2026-08-16 | Token contrast, local asset manifest, parity traces | `python3 scripts/verify_phase0_design.py` | PASS |
| P1 | 2026-08-16 | Independent foundation / no forbidden runtime boundary | `python3 scripts/verify_phase1_independence.py` | PASS |
| P1 | 2026-08-16 | JVM unit, lint, debug APK | `./gradlew testDebugUnitTest lintDebug assembleDebug` | PASS |
| P1 | 2026-08-16 | Emulator UI smoke | `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest` | PASS |
| P2 | 2026-08-16 | Local-only static boundary: no INTERNET, only text/plain share, no browser/map/localhost or data-store secret path | `python3 scripts/verify_phase2_local_first.py` | PASS |
| P2 | 2026-08-16 | Domain validation, templates, reminder policy, backup size/schema/version/legacy public mapping | `./gradlew testDebugUnitTest` | PASS |
| P2 | 2026-08-16 | Room CRUD, duplicate reservation/source, recheck upsert/history, itinerary source/calendar cascade, share cancel/expiry, default preservation; text/plain-only intent routing; backup new-ID nested restore | `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest` | PASS (5 tests) |
| P2 | 2026-08-16 | Local UI flow: empty list → create trip → itinerary → Preparation → Packing → reservation → source → recreate → delete confirmation | `MainActivitySmokeTest` on `TripPilot_API_26` | PASS |
| P2 | 2026-08-16 | Light/dark visual smoke, compact emulator | `app/build/reports/qa/phase2-light.png`, `phase2-dark.png` | PASS after disabling Android force-dark |

## Notes

- P2 dark screenshot must be driven with `adb shell cmd uimode night yes`; writing the secure setting alone produced an intermediate, non-authoritative frame on API 26.
- PD20 direct connected UI test remains a separate device-activity-resume limitation recorded in Phase 1; P2's gate uses the reproducible `TripPilot_API_26` emulator.
