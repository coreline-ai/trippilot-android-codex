# TripPilot 성능 기준선 — Phase 5

## 목적과 범위

생성된 Baseline Profile은 앱 시작, 빈 여행 목록, 로컬 여행 생성·상세, fake 구조화 초안 검토 요청을 대상으로 한다. 초안 검토 화면의 시각적 완료는 deterministic golden UI test가 별도로 확인한다. 이 여정은 네트워크·Codex OAuth·Calendar·지도/브라우저·SAF 파일 실행을 호출하지 않는다. AI 원문과 OAuth 정보도 생성·측정 결과에 기록하지 않는다.

## 재현 명령

API 33 이상 **에뮬레이터**에서만 아래 스크립트를 실행한다. 두 스크립트는 물리 기기 serial을 거부한다.

```bash
ANDROID_SERIAL=emulator-5556 scripts/run_baseline_profile.sh
ANDROID_SERIAL=emulator-5556 scripts/run_startup_benchmark.sh
```

- 생성 결과: `app/src/main/generated/baselineProfiles/baseline-prof.txt`, `startup-prof.txt`
- 측정 원문: `baselineprofile/build/outputs/connected_android_test_additional_output/` (빌드 산출물이며 저장소·백업에 포함하지 않음)
- 앱에는 `androidx.profileinstaller`만 runtime으로 포함한다. Macrobenchmark·UI Automator·생성 모듈은 test-only다.
- 2026-08-16 unsigned release APK에는 `assets/dexopt/baseline.prof`(17,578 bytes)와 `baseline.profm`(477 bytes)가 포함됨을 확인했다.

## 2026-08-16 로컬 측정 기록

| 항목 | 결과 |
|---|---|
| 생성 기기 | `Phone_API_35` emulator, API 35 (`sdk_gphone64_arm64`) |
| profile 생성 | PASS — `scripts/run_baseline_profile.sh` |
| 생성 규칙 | 각 파일 19,836줄, 그중 `io.trippilot.app` 규칙 1,631개 |
| cold start 측정 | PASS — `scripts/run_startup_benchmark.sh`, 5회 |
| `timeToInitialDisplayMs` | min 359.9ms / median 403.7ms / max 617.4ms |

이 숫자는 **에뮬레이터 신호일 뿐 실제 기기 SLA나 출시 성능 주장이 아니다**. Macrobenchmark JSON도 emulator 경고와 `run-from-apk` context를 기록한다. unsigned release 병합 중 non-minified profile의 synthetic rule 일부를 찾지 못했다는 D8 경고가 있었지만 build는 성공하고 위 binary profile asset은 생성됐다. 서명된 배포 후보가 정해지면 arm64 실기기에서 동일 여정을 재측정하고, cold/warm start·thermal 상태·profile 적용을 별도 release evidence로 남긴다.

## APK 직접 배포 한계

생성 profile은 앱 source에 포함되며 `ProfileInstaller`가 설치 경로에 전달한다. 그러나 ART의 실제 컴파일 시점·정도는 Android 버전, 설치 방식, 기기 상태에 따라 달라진다. 따라서 직접 sideload APK 또는 signed AAB가 실제 대상 기기에서 profile을 적용했다고 가정하지 않는다. release owner는 signed artifact 설치 뒤 profile 상태와 시작 성능을 물리 기기에서 확인해야 한다.
