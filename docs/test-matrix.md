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
| P3 | 2026-08-16 | versioned draft 계약, strict unknown-field/date/URL/enum/count 검증, fake stream fixture, raw persistence·외부 action 차단 | `python3 scripts/verify_phase3_structured_draft.py` | PASS |
| P3 | 2026-08-16 | JVM contract/fake runtime/unit, lint, debug APK | `JAVA_HOME=... ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon` | PASS |
| P3 | 2026-08-16 | Room atomic apply/idempotency/Calendar no-write, fake review no-write, late completion·Weather no-write, Compose request→edit/exclude→partial apply, existing local CRUD/share/backup | `JAVA_HOME=... ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest --console=plain --no-daemon` | PASS (9 tests, TripPilot_API_26 API 26 arm64) |
| P3 | 2026-08-16 | compact light/dark request-screen visual smoke, draft item TalkBack label | `app/build/reports/qa/phase3-request-light.png`, `phase3-request-dark.png`; `contentDescription = "$summary 선택"` static check | PASS |
| P4 | 2026-08-16 | historical public bridge 재감사: Grok/general chat/terminal/raw transport 노출, TripPilot custom OAuth/raw CLI/localhost 우회 없음 | 당시 `python3 scripts/verify_phase4_gate.py`; `docs/runtime-spike.md` | PASS — public bridge direct-use는 BLOCKED였음; 아래 source-pinned Codex-only adapter 결과로 대체 |
| P5 | 2026-08-16 | Calendar approval ledger/marker/retry, explicit Intent/SAF/reminder/privacy boundary, no Internet/backup/D2D/cleartext | `python3 scripts/verify_phase5_release.py` | PASS |
| P5 | 2026-08-16 | RFC 5545 all-day/timed UTC, UTF-8 75-octet folding, empty selection; reminder D-7 scheduling; backup contract | `JAVA_HOME=... ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon` | PASS — 21 JVM tests, lint 0 errors, debug APK |
| P5 | 2026-08-16 | fake Calendar permission/marker/idempotency/failed retry, explicit external confirmation UI, existing draft/local data integrity | `JAVA_HOME=... ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest --console=plain --no-daemon` | PASS — 12 tests, TripPilot_API_26 API 26 arm64, 0 failures/errors |
| P5 | 2026-08-16 | clean debug install launch + Compose empty-state TalkBack hierarchy visual smoke | `adb install -r app-debug.apk`, `uiautomator dump`, `screencap` | PASS — `app/build/reports/qa/phase5-light-launch.png` |
| P5 | 2026-08-16 | unsigned release artifact with release lint vital | `JAVA_HOME=... ./gradlew assembleRelease --console=plain --no-daemon` | PASS — `app/build/outputs/apk/release/app-release-unsigned.apk` (7.7MB); signing key 미사용 |
| P5 | 2026-08-16 | deterministic screenshot golden: 빈 목록·여행 요약·초안 검토·외부 실행 확인 | `ANDROID_SERIAL=emulator-5556 scripts/run_phase5_screenshot_golden.sh update` 후 `... verify` | PASS — Phone_API_35 (API 35), light/1.0x font, 4/4 approved PNG 비교 |
| P5 | 2026-08-16 | local Baseline Profile: startup·목록·로컬 여행 생성/상세·fake 초안 검토 | `ANDROID_SERIAL=emulator-5556 scripts/run_baseline_profile.sh` | PASS — Phone_API_35 (API 35), 195.945s; `baseline-prof.txt`/`startup-prof.txt` 각 19,836줄, TripPilot 규칙 1,631개 |
| P5 | 2026-08-16 | cold-start Macrobenchmark (5회) | `ANDROID_SERIAL=emulator-5556 scripts/run_startup_benchmark.sh` | PASS — Phone_API_35 (API 35) `timeToInitialDisplayMs`: min 359.9 / median 403.7 / max 617.4ms; emulator-only |
| P5 | 2026-08-16 | final JVM unit, lint, debug APK | `JAVA_HOME=... ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon` | PASS — 21 JVM tests, lint 0 errors, debug APK |
| P5 | 2026-08-16 | final API 26 integration/UI regression | `JAVA_HOME=... ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest --console=plain --no-daemon` | PASS — 13 tests, 0 failures/errors, 1 expected skip (API < 28 golden dialog capture) |
| P5 | 2026-08-16 | final unsigned release + embedded profile | `JAVA_HOME=... ./gradlew assembleRelease --console=plain --no-daemon`; `unzip -l ...apk` | PASS — 8.1MB unsigned APK; `assets/dexopt/baseline.prof` 17,578 bytes and `.profm` 477 bytes. non-minified synthetic rule D8 warnings are recorded in `docs/performance-baseline.md`. |
| P4 | 2026-08-16 | pinned GPL vendor provenance, required rootfs/Python/official Codex CLI/Gateway inputs, public credential/raw CLI/Grok ban, Codex-only UDS adapter guard, no history resume/raw draft clear | `python3 scripts/verify_phase1_independence.py`; `python3 scripts/verify_phase4_gate.py` | PASS — source-pinned adapter only; `grok-cli-pack` executable/UI/fallback 0; public TripPilot token/credential/raw CLI/localhost surface 0 |
| P4 | 2026-08-16 | real OAuth asset compile and packaging | `JAVA_HOME=... ./gradlew :app:compileDebugKotlin :app:assembleSecureDebug --console=plain --no-daemon` | PASS — `app-secureDebug.apk` built for arm64; official Codex CLI asset 222,231,296 bytes before APK compression; APK about 116 MB |
| P4 | 2026-08-16 | secureDebug APK native asset / ABI inspection | arm64 API 26 emulator (`emulator-5554`), `unzip -l app-secureDebug.apk`; device ABI `arm64-v8a` | PASS — Alpine/PRoot native libraries, Python/Gateway packs and official CLI asset are present; no OAuth credential was supplied |
| P4 | 2026-08-16 | final static suite, debug unit/lint/build, secureDebug build | `python3 scripts/verify_phase0_design.py` through `verify_phase5_release.py`; `JAVA_HOME=... ./gradlew testDebugUnitTest lintDebug assembleDebug :app:assembleSecureDebug --console=plain --no-daemon` | PASS — all static verifiers; 21 JVM tests, lint 0 errors, debug and secureDebug APKs created |
| P4 | 2026-08-16 | build-variant permission boundary | `apkanalyzer manifest permissions app-debug.apk` / `app-secureDebug.apk` | PASS — `debug` has no `INTERNET`; only `secureDebug` includes `INTERNET` for user-started official Codex runtime |
| P4 | 2026-08-16 | secureDebug installation and cold launch without OAuth action | `adb -s emulator-5554 install -r app-secureDebug.apk`; `am start`; UI hierarchy/process inspection | PASS — `io.trippilot.app.securedebug` process running and local empty-trip screen rendered; Device OAuth/login browser was not started |
| P4 | 2026-08-16 | physical Samsung arm64 install and activity launch | Samsung SM-S931N (`R3CY40PXCAP`, Android API 36, `arm64-v8a`): `adb install -r app-secureDebug.apk`; `am start` | PASS — streamed install and `MainActivity` start succeeded; process PID 30700. Phone was locked/NotificationShade-covered during inspection, so visible UI/OAuth navigation remains user-unlock test. |
| P4 | 2026-08-16 | Device OAuth success/cancel/browser return/live model/structured stream/logout, actual session artifact audit | user-owned OpenAI browser approval on installed secureDebug APK | NOT RUN — requires user authentication; TripPilot did not request, read or record credentials |

## 2026-08-16 디자인 개선 결과

| ID | 검증 | 환경 / 명령 | 결과 | 증거 |
|---|---|---|---|---|
| T-REFRESH-01 | Field Route artwork provenance·hash·design token/independence/local-first static contracts | `python3 scripts/verify_phase0_design.py && python3 scripts/verify_phase1_independence.py && python3 scripts/verify_phase2_local_first.py` | PASS | 2026-08-16, exit 0; contrast pair PASS, SVG manifest/local-only PASS, OpenMinis/Grok/credential/INTERNET ban 유지 |
| T-REFRESH-02 | JourneyHero 포함 JVM unit·lint·debug APK | `JAVA_HOME=... ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain --no-daemon` | PASS | 2026-08-16, BUILD SUCCESSFUL; debug APK 생성, lint report 생성 |
| T-REFRESH-03 | JourneyHero의 기존 trip click semantics·로컬 CRUD/draft/external UI 회귀 | `JAVA_HOME=... ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest --console=plain --no-daemon` | PASS | 2026-08-16; TripPilot_API_26 13 tests, failures 0/errors 0, 기존 API<28 golden 1건 skip |
| T-REFRESH-04 | full Gradle connected suite | `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest --console=plain --no-daemon` | BLOCKED (외부 모듈) | TripPilot 테스트 전, vendored `alpine-runtime-android`의 `NativePtyBridgeInstrumentedTest#forkptyExecOwnsControllingTerminalAndDeliversKernelWinchWithoutInputLoss`가 API 26 emulator에서 SIGWINCH assertion으로 실패. TripPilot source/UI 변경과 무관하며 T-REFRESH-03은 PASS. |
| T-REFRESH-05 | 빈 여행 목록의 compact visual smoke | emulator-5554 debug APK install/launch + `adb exec-out screencap -p` | PASS | `app/build/reports/qa/design-refresh-list-empty.png`; hero text 대비, CTA 도달 위치, artwork crop을 육안 확인 |
| T-REFRESH-06 | Samsung arm64 debug install/launch | SM-S931N (`R3CY40PXCAP`): `adb install -r app-debug.apk`; `am start`; `pidof` | PASS | 2026-08-16; `io.trippilot.app.debug` 설치·MainActivity launch·PID `31798` 확인. 기존 `secureDebug` OAuth 실험 패키지는 변경하지 않음. |
| T-REFRESH-07 | 1턴 로컬 도쿄 여행을 실제 Compose UI로 작성하고 모든 현재 제품 surface를 캡처 | `ANDROID_SERIAL=emulator-5556 scripts/run_design_journey_capture.sh` | PASS | 2026-08-16; `도쿄 가을 기록`(3일), 일정·준비·짐·예약·출처를 직접 입력. 목록 featured, 개요, 일정, 준비, 예약, 출처, AI 초안 검토, 외부 backup 확인 8종 캡처. OAuth/network/외부 승인 실행 0건. |
| T-REFRESH-08 | 초안 기본 요약 카드 → 항목 선택·수정 → 부분 반영 | `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.trippilot.app.DraftPlannerUiTest --console=plain --no-daemon` | PASS | 2026-08-16; 1 test. 제외한 일정은 저장되지 않고 수정한 `Edited Hotel` 예약만 local Room에 반영됨. |
| T-REFRESH-09 | current redesign screenshot golden 4종 | `ANDROID_SERIAL=emulator-5556 scripts/run_phase5_screenshot_golden.sh update` 후 `... verify` | PASS | 2026-08-16; list/summary/draft-review/external-confirmation 4/4 pixel comparison. AI review baseline은 compact summary card를 사용. |
| T-REFRESH-10 | final static boundary, JVM unit, lint, debug APK | `verify_phase0_design.py && verify_phase1_independence.py && verify_phase2_local_first.py`; `./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain --no-daemon` | PASS | 2026-08-16; contrast·asset manifest·OpenMinis/Grok/credential/INTERNET ban 유지, lint 0 errors, debug APK 생성. |
| T-REFRESH-11 | final app UI/integration regression | `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest --console=plain --no-daemon` | PASS | 2026-08-16; 14 app tests PASS, opt-in DesignJourney와 API<28 golden 2 skipped, failures/errors 0. |
| T-REFRESH-12 | latest Samsung debug deployment | SM-S931N (`R3CY40PXCAP`): `adb install -r app-debug.apk`; `am start`; `pidof` | PASS | 2026-08-16; 최신 Field Route Journal build 설치·실행, debug process PID `541`. `secureDebug` OAuth 실험 패키지는 미변경. |

## Notes

- P2 dark screenshot must be driven with `adb shell cmd uimode night yes`; writing the secure setting alone produced an intermediate, non-authoritative frame on API 26.
- PD20 direct connected UI test remains a separate device-activity-resume limitation recorded in Phase 1; P2's gate uses the reproducible `TripPilot_API_26` emulator.
- P3 screenshot files are manual deterministic-fixture smoke evidence. Phase 5 adds the approved API 28+ emulator golden comparison; API 26 shared UI suite skips dialog capture because Compose cannot capture dialog content below API 28.
- P4는 source-pinned Codex-only adapter로 구현·static/package verification을 통과했다. real Device OAuth success/cancel/browser return/live model/structured stream/logout은 사용자 계정 승인 없이 실행할 수 없으므로 NOT RUN 상태다. 사용자 계정/브라우저/OAuth credential은 요청하거나 기록하지 않았다.
- P5 provider-level Calendar account, notification permission/boot, real map/browser app handoff, arm64 physical-device test, signed release는 실행하지 않았다. 각각 사용자 승인·실제 device/account 또는 별도 release pipeline이 필요하다. golden과 Baseline Profile은 emulator 결과일 뿐 이 실기기 Gate를 대체하지 않는다.
- T-REFRESH-04의 upstream native test는 본 디자인 작업의 수정 범위를 벗어난 vendored Alpine runtime 검증 실패다. TripPilot UI gate는 app-only instrumentation suite(T-REFRESH-03)로 분리해 검증했다.


## 2026-08-16 Trip Briefing GUI — Phase 0

| ID | 검증 | 환경 / 명령 | 결과 | 증거 |
|---|---|---|---|---|
| T-BRIEF-0-01 | Trip Briefing direction, screen map, component contracts, token metadata | `design/design-direction.md`, `design/screen-map.md`, `design/tokens.*`, `design/visual-baseline.md` review | PASS | 2026-08-16; 브리핑·일정·준비·보관함·도움의 compact/medium/expanded contract, `JourneyStageStrip` semantics, approval boundary, primary action rule 확정 |
| T-BRIEF-0-02 | contrast/asset provenance/parity trace | `python3 scripts/verify_phase0_design.py` | PASS | 2026-08-16, exit 0; light/dark semantic contrast 4.75:1 이상, 5개 SVG local-only/hash, PAR-01~17 trace PASS |
| T-BRIEF-0-03 | design diff whitespace | `git diff --check -- design/...` | PASS | 2026-08-16, exit 0 |

## 2026-08-16 Trip Briefing GUI — 구현 및 QA 누적

| ID | 검증 | 환경 / 명령 | 결과 | 증거 |
|---|---|---|---|---|
| T-BRIEF-1-01 | 브리핑 shell, data-driven stage, 목록/상세 local click semantics | `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest --console=plain --no-daemon` | PASS | 2026-08-16; API 26 app suite 20 tests, 2 expected skip(API<28 capture), 0 failures. `JourneyStageCalculatorTest`는 1일·다일·시작 전·여행 중·종료 후·일정 없음 상태를 포함한다. |
| T-BRIEF-1-02 | legacy RouteRibbon의 고정 완료일/선택일 호출 부재 | `rg "RouteRibbon|completedDays\\s*=\\s*0|selectedDay\\s*=\\s*1" app/src/main/kotlin app/src/test app/src/androidTest` | PASS | 2026-08-16; data-bearing replacement 주석 외 production/test 호출 0건. |
| T-BRIEF-2-01 | 브리핑·일정·준비·예약·출처의 1턴 로컬 여행 여정 | `ANDROID_SERIAL=emulator-5556 scripts/run_design_journey_capture.sh` | PASS | `app/build/reports/qa/design-journey/01~08-*.png`; 기본 팩·수동 항목·일정·예약·출처·AI review·외부 승인 확인 8 화면. OAuth/network/승인 실행 0건. |
| T-BRIEF-3-01 | 기본 팩/기존 수동·AI 항목 보존, Room migration, backup, TalkBack label | JVM + API 26 app suite | PASS | 2026-08-16 full suite; catalog/repository/backup/migration/accessibility regression 포함. 실제 Samsung의 사용자 DB migration은 별도 manual gate다. |
| T-BRIEF-4-01 | modal form footer가 보이는 action row에 남고 로컬 CRUD가 실제로 저장됨 | `MainActivitySmokeTest` (API 26 full app suite) | PASS | 초기 footer-offscreen → scrim dismiss 결함을 bounded scroll content + sticky action row로 수정. 여행·일정·준비·짐·예약·출처 생성과 process recreation 경로 PASS. |
| T-BRIEF-4-02 | AI review / 외부 승인 UI 및 no-auto-apply/no-auto-execute 경계 | `DraftPlannerUiTest`, `ExternalActionsUiTest`, Phase 4 static verifier, API 26 app suite | PASS | 선택한 draft만 Room transaction에 반영, 확인 dialog 이전 Calendar/map/browser/SAF Intent 0건. prompt/response/OAuth credential persistence 없음. |
| T-BRIEF-4-03 | 2.0x font scale Korean CTA bounds | `ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.trippilot.app.PrimaryActionAccessibilityTest --console=plain --no-daemon` | PASS | `PrimaryActionAccessibilityTest`: label top/bottom이 CTA touch target 내부이고 최소 52dp target 유지. |
| T-BRIEF-5-01 | 최신 GUI golden 4종 | `ANDROID_SERIAL=emulator-5556 scripts/run_phase5_screenshot_golden.sh verify` | PASS | API 35, light/1.0x; list empty·summary·draft review·external confirmation 4/4 approved PNG match. CTA accessibility 변경 뒤에도 golden diff 0. |
| T-BRIEF-5-02 | 600dp/840dp light responsive visual review | API 35 emulator; `wm size 1200x1920 @320`, `1680x2400 @320` 후 design journey | PASS | `app/build/reports/qa/adaptive/600dp/`, `840dp/`; 목록·브리핑·준비 3종을 visual review. 작업 뒤 physical 1080x2400/420dpi, light/1.0x로 복원. |
| T-BRIEF-5-03 | 360dp dark/2.0x CTA semantic bound smoke | API 35 emulator; `wm size 1080x1920 @480`, dark, font_scale=2.0 + UI hierarchy | PASS (semantic) | `PrimaryActionAccessibilityTest`와 `uiautomator` bounds에서 title·hero·CTA가 1920px viewport 안에 위치. raw screencap은 `wm` override 전환 중 stale frame을 보여 pixel visual gate로 사용하지 않는다. |
| T-BRIEF-5-04 | design/static security/local-first/verifier suite | `verify_phase0_design.py` ~ `verify_phase5_release.py`, `git diff --check` | PASS | 2026-08-16; contrast/asset provenance, OpenMinis/Grok/credential/raw CLI/localhost ban, no Internet/backup egress, explicit confirmation routes PASS. |
| T-BRIEF-5-05 | JVM unit, lint, debug APK | `JAVA_HOME=... ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon` | PASS | 2026-08-16; BUILD SUCCESSFUL, lint error 0, debug APK 생성. 최초 sandbox Gradle cache lock은 권한 있는 재실행으로 해결했으며 source/build 결함이 아님. |
| T-BRIEF-5-06 | physical Samsung 최신 Trip Briefing visual/manual QA | user-owned Samsung SM-S931N (`R3CY40PXCAP`, Android API 36) | PARTIAL PASS | 2026-08-16; `adb install -r`만 사용하고 uninstall/clear/DB overwrite 없이 기존 `TokyoWeekend`가 cold process recreation 뒤에도 렌더됨. 원래 설정(dark, 0.9x)을 보존한 채 light/dark, temporary 2.0x(복원), 폼 keyboard → Back(IME 닫힘) → Back(sheet 닫힘), TalkBack actual service bind와 목록 semantics를 확인했다. partial sheet에서 action row가 보이지 않는 실기기 결함은 `skipPartiallyExpanded=true`로 수정·재설치 후 keyboard 위 action row를 확인했다. 사용자 DB에 구버전 fixture를 주입하는 것은 보존 원칙에 어긋나므로 literal v1→v2 migration은 이 기기에서 실행하지 않았고, TalkBack 음성 focus traversal gesture도 수행하지 않았다. |
| T-BRIEF-5-07 | form sheet Samsung fix 후 정적/빌드 회귀 | `verify_phase0_design.py` ~ `verify_phase5_release.py`; `JAVA_HOME=... ./gradlew testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon`; `git diff --check` | PASS | 2026-08-16; 모든 verifier PASS, BUILD SUCCESSFUL, lint error 0, whitespace error 0. |
| T-BRIEF-5-08 | 전체 connected instrumentation command | `JAVA_HOME=... ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest --console=plain --no-daemon` | FAIL (vendored upstream) | TripPilot test 전 `alpine-runtime-android`의 `NativePtyBridgeInstrumentedTest#forkptyExecOwnsControllingTerminalAndDeliversKernelWinchWithoutInputLoss`가 API 26에서 kernel SIGWINCH assertion으로 실패했다. TripPilot source/UI 수정과 무관한 vendored native test이며 삭제·완화하지 않았다. |
| T-BRIEF-5-09 | TripPilot app-only instrumentation regression | `JAVA_HOME=... ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest --console=plain --no-daemon` | PASS | 2026-08-16; API 26 18 tests, failures 0/errors 0, expected skip 2 (`DesignJourney` opt-in 및 API<28 capture). `TripFormSheet` expanded 상태/CTA accessibility를 포함한 현재 앱 regression 통과. |
| T-BRIEF-5-10 | Samsung isolated Room v1→v2 fixture | `ANDROID_SERIAL=R3CY40PXCAP ./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.trippilot.app.core.data.TripDatabaseMigrationTest` | PASS (fixture) / INCIDENT (deployment) | 2026-08-17; SM-S931N Android 16에서 1 test PASS. fixture DB 이름은 `migration-test-db`이며 normal TripPilot DB와 다르고 `@After` cleanup도 추가했다. 그러나 Gradle physical instrumentation lifecycle가 target `io.trippilot.app.debug`를 제거해 기존 debug Room data도 잃었다. isolated DB design만으로 user-owned package preservation을 보장하지 못하므로 이 방식은 실제 사용자 프로필에서 금지한다. |
| T-BRIEF-5-11 | data recovery discovery + app availability recovery | Mac Downloads/Documents/Desktop/iCloud candidate paths, Samsung public Download/Documents/Android/media; fresh debug APK install | NO BACKUP FOUND / APP RESTORED EMPTY | 2026-08-17; 공개 파일명 검색에서 `trippilot-backup*.json` 등 candidate를 찾지 못했다. cloud/provider-private 위치까지의 완전한 탐색은 아니다. 삭제된 `io.trippilot.app.debug` 자리에 current debug APK를 빈 상태로 설치하고 `TripPilot`/`새 여행 만들기` UI를 확인했다. `io.trippilot.app.securedebug`는 변경하지 않았다. 원래 TokyoWeekend 상세 데이터는 복구하지 않았다. |

## 2026-08-17 Android design loop

물리 기기에서 `connectedAndroidTest` / uninstall / clear는 금지한다. 자동 캡처와 golden은 `emulator-*`만 허용한다.

| ID | 검증 | 환경 / 명령 | 결과 | 증거 |
|---|---|---|---|---|
| T-LOOP-1-01 | 디자인 계약 JSON, 스킬 금지 문구, 자산 경로 | `python3 scripts/verify_design_contract.py` | PASS | 2026-08-17; content-system 4영역+8 journey, tokens/theme mapping, empty-readiness/reservations/sources runtime 경로 |
| T-LOOP-1-02 | Phase 0 기존 디자인 계약 | `python3 scripts/verify_phase0_design.py` | PASS | 2026-08-17 |
| T-LOOP-4-01 | RUN_STATE fixtures: completed / P1 / P2 / P3 / unapproved / physical | `python3 scripts/test_design_loop_fixtures.py` | PASS | 2026-08-17 |
| T-LOOP-4-02 | 비에뮬레이터 serial 거부 | `ANDROID_SERIAL=R3CY40PXCAP scripts/run_android_design_qa.sh` dry | PASS (guard) | `Refusing non-emulator serial` exit 2; APK 제거 전 중단 |
| T-LOOP-3-01 | Journey 고정 날짜 + golden 05/06 캡처 경로 | `DesignJourneyCaptureTest`, `Phase5ScreenshotGoldenTest` | IMPLEMENTED | fixture `2026-11-01`–`2026-11-03`; 05/06 파일은 첫 emulator `update` 후 승인 |
| T-LOOP-5-01 | 정적 Phase 5 + design-loop 파일 | `python3 scripts/verify_phase5_release.py` | PASS | 2026-08-17; 기존 4 golden 유지. 05/06 PNG는 emulator update 잔여 |
| T-LOOP-5-02 | JVM unit / lint | `JAVA_HOME=... ./gradlew :app:testDebugUnitTest :app:lintDebug --no-daemon` | PASS | 2026-08-17; BUILD SUCCESSFUL, lintDebug PASS |
| T-LOOP-5-03 | androidTest compile | `./gradlew :app:compileDebugAndroidTestKotlin` | PASS | 2026-08-17; DesignTokenContractTest / DesignLayoutMatrixTest / golden 05-06 경로 컴파일 |
| T-LOOP-5-04 | emulator connected / golden verify / 실기기 install -r | API 35 emulator + optional Samsung `adb install -r` | NOT RUN | 이 환경에 adb/emulator 없음. 사용자 기기 instrumentation은 금지 |

## 2026-08-17 populated travel-document GUI

데이터가 있는 목록·브리핑·일정·준비·예약을 Orbit/TripIt식 서류 UI로 읽는 Track 1. golden PNG는 사람 승인 없이 update하지 않았다. 삼성에서는 `adb install -r`만 허용한다.

| ID | 검증 | 환경 / 명령 | 결과 | 증거 |
|---|---|---|---|---|
| T-POP-0-01 | populated 계약·적용/미적용표·PAR-01~17 | `python3 scripts/verify_phase0_design.py`; `git diff --check -- design` | PASS | 2026-08-17; contrast/manifest/parity 유지. `design-direction.md` rule 7, Orbit/TripIt 적용표, screen-map time rail/서류/n-m 와이어프레임 |
| T-POP-1-01 | 목록 featured next-action 계산 | `TripNextActionCalculatorTest` via `:app:testDebugUnitTest` | PASS | 준비 남음 → 첫 일정 → 예약 서류 → 검토 순서. 삼성 `Busan Weekend`는 `다음: 준비 19개 남음`에 해당 |
| T-POP-5-01 | JVM unit, lint, debug APK | `JAVA_HOME=Android Studio JBR ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --no-daemon` | PASS | 2026-08-17; BUILD SUCCESSFUL, lintDebug PASS, `app-debug.apk` 129MB |
| T-POP-5-02 | 정적 승인/로컬 경계 | `python3 scripts/verify_phase5_release.py` | PASS | Calendar/Intent/SAF 확인 UI, INTERNET 없음, golden 자동 update 없음 |
| T-POP-5-03 | 삼성 보존 설치 | SM-S931N (`R3CY40PXCAP`): `adb install -r app-debug.apk`; `am start`; Room 읽기 | PASS (data) / LOCKED (visual) | 2026-08-17; streamed install Success, PID `30589`. `trippilot.db` 유지: `Busan Weekend` / Busan / 2026-08-17–19, prep 11, pack 8, itinerary 1, reservation 1, source 1. 화면 잠금으로 목록·브리핑 육안 탭은 사용자 잠금 해제 후. uninstall/clear/connected test 0건 |
| T-POP-5-04 | screenshot golden update | `scripts/run_phase5_screenshot_golden.sh update` | NOT RUN | 시각 승인 전 자동 update 금지. 기존 4 golden 유지 |

## 2026-08-17 Hallmark 가이드 적용 (T-HMK)

반(反)AI-slop 디자인 규율을 TripPilot에 적용한 Track (dev-plan `implement_20260817_230636.md`). 정적 slop 게이트는 로컬 결정론 검증, 시각·계측 게이트는 emulator/사람 승인 대기.

| ID | 검증 | 환경 / 명령 | 결과 | 증거 |
|---|---|---|---|---|
| T-HMK-0-01 | 가이드 정본·slopGates 스키마·dead code 제거 | `design/hallmark-guide.md`, `design/audit/design-review.json`, `python3 scripts/test_design_loop_fixtures.py` | PASS | 2026-08-17; 6대 규율+게이트 15항+macrostructure 표. slopGates 15항 pending → 정적 6항 pass |
| T-HMK-1-01 | 정적 slop 게이트 3/5/10/12 + 부정 fixture | `python3 scripts/verify_design_contract.py` | PASS | 2026-08-17; 위반 fixture 4종 FAIL 확인. TripPilotApp/DraftPlannerSection 0 위반 |
| T-HMK-2-01 | 핵심 컴포넌트 8-state + showcase preview | `./gradlew :app:compileDebugKotlin`; `TripInteractionState` (PrimaryAction/ChecklistRow/SelectableCard/ConfirmActionSheet) | PASS | 2026-08-17; 상태색은 colorScheme 역할만 사용. Preview 렌더는 Android Studio에서 사용자 확인 |
| T-HMK-3-01 | macrostructure 등록 + gate-4/15 | `python3 scripts/verify_design_contract.py` | PASS | 2026-08-17; 7 페이지+shell 등록. `SummaryMetric` 4연속 → 2×2 그리드 교정. Surface 3연속 fixture FAIL 확인 |
| T-HMK-4-01 | 레이아웃 매트릭스 320/360/414dp | `./gradlew :app:compileDebugAndroidTestKotlin` | PASS (compile) / NOT RUN (emulator) | 2026-08-17; 테스트 클래스 KDoc에 실행 명령 기록. 실행은 T-LOOP-5-04 emulator 확보 후 |
| T-HMK-5-01 | slopGates→RUN_STATE 연동 | `python3 scripts/test_design_loop_fixtures.py` | PASS | 2026-08-17; 정적 게이트 fail/missing → blocked 시나리오 포함 9 시나리오 |
| T-HMK-5-02 | QA 러너 회귀 (dry-path) | `DESIGN_LOOP_SKIP_EMULATOR=1 scripts/run_android_design_qa.sh` | PASS | 2026-08-17; 신규 게이트 자동 포함 확인 |
| T-HMK-6-01 | 초안 반영 LOADING 연결 + 이중 탭 차단 | `DraftUiState.Applying`; `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest` | PASS | 2026-08-17; 반영 버튼 LOADING("…"), 버리기 비활성. 기존 unit/androidTest 회귀 없음 |
| T-HMK-6-02 | 승인 시트 상태 연결 (Calendar LOADING·handoff ERROR) | `calendarWriting` StateFlow; `compileDebugKotlin/AndroidTestKotlin`, `testDebugUnitTest` | PASS | 2026-08-17; Calendar 이중 승인 compareAndSet 차단, handoff 실패 시 "다시 시도" ERROR 유지 |
