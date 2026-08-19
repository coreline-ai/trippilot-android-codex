# TripPilot release readiness — Phase 5

## 빌드/서명

- 현재 applicationId: `io.trippilot.app`; debug는 `.debug`, real OAuth 검증용 secureDebug는 `.securedebug` suffix를 사용한다.
- `release`는 `assembleRelease`로 unsigned release APK를 생성한다. signing key, keystore path, alias, password는 저장소·DataStore·백업·logcat에 넣지 않는다. 배포 담당자가 CI secret 또는 로컬 secure keystore로 서명한다.
- `versionCode=1`, `versionName=0.1.0`은 첫 internal candidate 기준이다. 외부 배포 전 versionCode는 단조 증가해야 한다.
- `debug` APK는 local/fake runtime 검증용이며 ABI filter가 없다. `secureDebug`/release는 native Alpine runtime·official Codex CLI를 포함하고 `arm64-v8a`만 지원한다. runtime asset hash·packaging은 build에서 검증됐고 physical-device OAuth/startup은 별도 release Gate다.

## 개인정보/배포 체크

- `allowBackup=false`, `fullBackupContent=false`, Android 12+ cloud backup/D2D exclude, `usesCleartextTraffic=false`를 manifest에 적용했다.
- `debug`에는 network permission이 없고 analytics SDK·광고 SDK·자체 backend는 모든 variant에 없다. `secureDebug`/release에만 user-started official Codex OAuth/runtime을 위한 `INTERNET` permission이 있다. OAuth credential 저장은 없다.
- 현재 화면은 사용자가 여행 계획을 스크린샷으로 공유할 수 있도록 `FLAG_SECURE`를 전역 적용하지 않는다. TripPilot UI는 transient Device Code만 보이며 실제 credential/password 입력은 external browser가 소유한다.
- release 배포 전 dependency report/SBOM, Android Data safety declaration, third-party notice/legal source-offer 여부를 실제 artifact 기준으로 검토해야 한다.

## Baseline Profile와 screenshot 회귀

- 후보 사용자 여정: cold start → 여행 목록 → 로컬 여행 생성·상세 → fake 구조화 초안 검토다. profile 생성 흐름은 OAuth·Calendar·지도/브라우저·SAF를 호출하지 않는다.
- `:baselineprofile` test-only module은 AndroidX Baseline Profile plugin/Macrobenchmark/UI Automator를 쓰고, 앱은 `profileinstaller`만 runtime으로 포함한다. 생성된 source profile은 `app/src/main/generated/baselineProfiles/`에 보관한다.
- API 35 emulator에서 profile 생성과 5회 cold-start Macrobenchmark를 실제로 PASS했다. 측정값·기기·제한은 [`performance-baseline.md`](performance-baseline.md)에 기록한다. emulator 결과는 물리 기기 성능 또는 profile 적용의 보증이 아니다.
- unsigned release APK에는 `assets/dexopt/baseline.prof`가 포함된다. release 병합 시 non-minified profile의 일부 synthetic rule을 찾지 못하는 D8 warning이 관찰되었으므로, signed arm64 artifact에서의 적용 여부는 반드시 다시 확인한다.
- deterministic golden은 빈 목록, 여행 요약, AI 초안 검토, 외부 실행 확인 네 화면을 API 28+ emulator에서 고정 light mode/1.0x font scale로 비교한다. `scripts/run_phase5_screenshot_golden.sh verify`가 non-emulator를 거부하고 CI/local static verifier에 연결된다.
- signed AAB·arm64 실기기·실제 provider 권한과 배포 설치 경로에서의 profile 적용은 별도 release Gate로 남는다.

## 2026-08-16 Trip Briefing GUI 검증 업데이트

- `TripFormSheet`는 높이가 제한된 scroll content와 고정 action row를 사용한다. 이전에는 footer가 화면 밖에 놓여 test tap이 scrim dismiss로 전달될 수 있었고, 현재 `MainActivitySmokeTest`의 실제 로컬 CRUD 경로로 회귀를 막는다.
- `PrimaryAction`은 52dp 최소 touch target을 지키되, 2.0x 글꼴의 한국어 label이 내부에서 잘리지 않도록 content measurement에 여백을 포함한다. `PrimaryActionAccessibilityTest`가 label bounds가 CTA 내부에 남는지 확인한다.
- API 35 emulator에서 Trip Briefing 8-screen journey, 4-screen golden verify, 600dp/840dp light responsive 캡처를 PASS했다. API 26 full app suite는 20 tests, expected API<28 capture skip 2건, failure 0건이다. 세부 명령과 경로는 [`test-matrix.md`](test-matrix.md)의 `T-BRIEF-*` 행을 따른다.
- `wm` override 중 raw screencap은 stale frame이 될 수 있어 360dp/dark/2.0x의 자동 증거는 Compose/UI semantics bounds와 dedicated CTA test로 한정했다. 이것은 physical visual QA 대체가 아니다.

### 2026-08-16 Samsung 보존 모드 수동 QA

- user-owned Samsung SM-S931N (`R3CY40PXCAP`, Android API 36)에서 debug APK를 `adb install -r`로만 업데이트했다. uninstall, clear data, backup restore, DB overwrite는 하지 않았으며, 기존 `TokyoWeekend`가 force-stop/cold launch 뒤에도 목록에 남아 있음을 UI hierarchy와 screenshot으로 확인했다.
- 원래 device 설정은 dark mode와 font scale `0.9`였다. temporary light mode와 `2.0x` 글꼴을 캡처한 뒤 원래 값으로 복원했다. dark/2.0x에서는 날짜가 두 줄로 wrap되지만 CTA·hero·날짜가 screen bounds 안에 남았고, light mode에서도 동일 여행 카드와 CTA를 육안 확인했다.
- 실제 blank form에서 keyboard를 열어 첫 Back은 IME만 닫고 둘째 Back은 저장 없이 form을 닫는 흐름을 확인했다. partial sheet에서는 tall Samsung에서 action row가 초기 viewport 밖에 남는 결함을 발견하여 `TripFormSheet`를 `skipPartiallyExpanded=true`로 변경했다. 재설치 후 keyboard 전/후 모두 cancel/confirm row가 보이는 것을 확인했다.
- TalkBack을 임시로 actual service bind하여 Compose accessibility hierarchy가 `TripPilot`, `나의 여정`, 기존 여행의 설명 label, `새 여행 만들기`를 노출하는 것을 확인한 뒤, 원래 disabled 설정으로 복원했다. 사용자 음성 출력·gesture focus traversal은 사용자 보조기능을 오래 점유하지 않기 위해 수행하지 않았다.
- literal v1 DB fixture를 user-owned DB에 주입해 migration하는 검증은 기존 데이터를 위험하게 만들 수 있어 실행하지 않았다. v1→v2 migration은 instrumented fixture로 PASS했고, physical device에서는 in-place update 뒤 현존 데이터 보존으로 제한해 확인했다.

### 2026-08-17 physical instrumentation data-preservation incident

- Samsung SM-S931N에서 `TripDatabaseMigrationTest`를 `:app:connectedDebugAndroidTest`로 단독 실행해 isolated `migration-test-db`의 v1→v2 migration은 PASS했다. 하지만 Gradle의 physical instrumentation deployment lifecycle가 target `io.trippilot.app.debug`를 제거했고, 이 package에 있던 existing local Room data도 함께 사라졌다. `migration-test-db`를 normal database와 분리하거나 `@After`로 delete하는 것만으로는 package uninstall을 막을 수 없었다.
- 즉시 재설치·재생성을 중단하고, Mac의 일반 문서 경로 및 Samsung 공개 저장소에서 `trippilot-backup`/backup schema 파일을 read-only로 탐색했다. 접근 가능한 범위에서는 backup을 찾지 못했다. cloud provider-private 위치 또는 사용자가 별도로 보관한 파일은 아직 검토 대상이다.
- 사용자의 지시에 따라 current debug APK를 **빈 상태로만** 다시 설치해 실행 가능 상태를 복구했다. `secureDebug` package는 변경하지 않았고, 원래 TokyoWeekend의 상세 데이터는 임의로 추정·재생성하지 않았다.
- 앞으로 user-owned device의 package에는 Gradle connected instrumentation을 실행하지 않는다. physical migration smoke가 필요하면 (1) disposable device/profile, 또는 (2) production debug package와 다른 test-only applicationId를 사용하며, install/uninstall lifecycle와 data isolation을 사전에 확인해야 한다.

### 아직 닫히지 않은 release Gate

- physical Samsung의 literal v1 DB migration fixture, 새 여행 생성부터 기본 팩·수동 항목·예약·출처·승인 화면까지의 end-to-end smoke, TalkBack 사용자 gesture/음성 traversal은 남아 있다. 첫 항목은 user-owned DB 보존 원칙 때문에 별도 disposable test profile에서만 수행한다.
- original debug Room content의 복구는 사용자가 제공하는 JSON backup 또는 별도 보관 파일이 있을 때만 진행한다. 빈 debug app을 이미 재설치했으므로, 근거 없는 데이터 복원·재생성은 금지한다.
- real Device OAuth success/cancel/return/live model/stream/logout은 사용자 브라우저 로그인 승인이 필요한 별도 Gate다. credential, token, auth.json은 요청·저장·로그 출력하지 않는다.
- signed arm64 release/AAB, 실제 Calendar account·notification permission·외부 app handoff, 배포 artifact Data safety/SBOM/legal 검토는 계속 release owner가 수행한다.

## 2026-08-19 릴리즈 문서 준비 (로컬 완료)

| 항목 | 문서 | 상태 |
|---|---|---|
| GPL-3.0 소스 제공 안내·체크리스트 | `gpl-source-offer.md` | 초안 — 법률 검토 대기 |
| SBOM (release 구성 직접 의존성 25종 + 번들 SPDX) | `sbom.md` | 초안 — 서명 artifact 재생성 대기 |
| Play Data safety 선언 매핑 | `data-safety.md` | 초안 — 콘솔 정책 재확인 대기 |
| Signing 키 생성·설정 가이드 | `signing.md`, `scripts/generate_release_keystore.sh` | 준비 — 키 생성은 owner 결정 |
| 태블릿 가로(2160×1080) 대표 캡처 | `docs/demo/tablet-landscape/` | 완료 — list·브리핑·귀국후 window |
