# Third-party notices — Phase 5 검토본

## 현재 앱에 실제 포함된 항목

| 항목 | 용도 | License 확인 상태 |
|---|---|---|
| AndroidX / Jetpack Compose / Room / DataStore / Navigation | Android UI·저장소 기반 | 각 artifact의 POM/license를 release dependency report와 함께 재확인. 저장소에는 notice bundle을 생성하지 않음. |
| AndroidX ProfileInstaller | 생성된 Baseline Profile을 설치된 APK에 전달 | Apache-2.0 계열 AndroidX runtime dependency. 실제 서명 artifact의 dependency report와 NOTICE를 배포 직전 재확인. |
| Hilt | DI 기반 준비 | Apache-2.0 계열 dependency로 release dependency report에서 재확인. |
| Kotlin / kotlinx | Kotlin runtime·coroutine·serialization | Apache-2.0 계열 dependency로 release dependency report에서 재확인. |
| Material Symbols | 기능 아이콘 정책 | Apache-2.0; v1은 별도 원격 icon/font binary를 다운로드하지 않음. |
| `alpine-codex-cli-client` source slice | Codex Device OAuth를 위한 Alpine/PRoot·Gateway/CLI staging | GPL-3.0-or-later. Pinned source is in `third_party/alpine-codex-cli-client`; TripPilot source distribution/notice obligations apply. |
| Official Codex CLI | APK asset; official Device OAuth and model runtime | Pinned binary lock is verified at build. Its distribution terms and current release lock require review before public release. |
| Alpine rootfs / PRoot / local Python packages | App-private arm64 runtime and Gateway bootstrap | Runtime lock, SPDX SBOM, vulnerability snapshot and package source artifacts are vendored and verified during build. |

## 아직 포함하지 않은 항목

| 항목 | 현재 상태 | 배포 전 필수 검토 |
|---|---|---|
| unDraw / imagegen raster | v1 미포함 | 사용 승인 전 manifest·license·hash 기록 |
| AndroidX Macrobenchmark / Baseline Profile Gradle plugin / UI Automator | `:baselineprofile`의 test-only 생성·측정 도구 | APK runtime에는 Macrobenchmark/UI Automator가 포함되지 않음. CI/로컬 test artifact의 license 정보를 release 검토 기록에 보존. |

TripPilot repository에는 OAuth token, credential, private CLI binary, signing key를 저장하지 않는다. Release 후보를 외부 배포하기 전에는 GPL-3.0 대응 소스 제공, 실제 생성 APK의 dependency report/SBOM, Codex CLI·Alpine·PRoot·Python package NOTICE, vendor shared-Gateway 검토를 법률 검토와 함께 배포물에 포함해야 한다.
