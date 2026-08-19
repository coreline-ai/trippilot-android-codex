# SBOM — TripPilot 0.1.0 (release 구성, 초안)

생성: `./gradlew :app:dependencies --configuration releaseRuntimeClasspath` (2026-08-19, commit 기준).
상태: **초안** — 최종 배포 시 서명 artifact를 다시 기준으로 재생성해야 한다.

## 직접 의존성 — 외부 라이브러리 (17개)

| 모듈 | 버전 |
|---|---|
| `org.jetbrains.kotlin:kotlin-stdlib:2.2.21` | `org.jetbrains.kotlin:kotlin-stdlib:2.2.21` |
| `androidx.compose:compose-bom:2024.09.00` | `androidx.compose:compose-bom:2024.09.00` |
| `androidx.core:core-ktx:1.15.0` | `androidx.core:core-ktx:1.15.0` |
| `androidx.activity:activity-compose:1.10.0` | `androidx.activity:activity-compose:1.10.0` |
| `androidx.lifecycle:lifecycle-runtime-compose:2.8.4 (*)` | `androidx.lifecycle:lifecycle-runtime-compose:2.8.4` |
| `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4` | `androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4` |
| `androidx.navigation:navigation-compose:2.8.5` | `androidx.navigation:navigation-compose:2.8.5` |
| `androidx.compose.foundation:foundation` | `1.7.2` |
| `androidx.compose.material3:material3` | `1.3.0` |
| `androidx.compose.ui:ui` | `1.7.2` |
| `androidx.compose.ui:ui-tooling-preview` | `1.7.2` |
| `androidx.room:room-runtime:2.7.2` | `androidx.room:room-runtime:2.7.2` |
| `androidx.room:room-ktx:2.7.2` | `androidx.room:room-ktx:2.7.2` |
| `androidx.datastore:datastore-preferences:1.1.1` | `androidx.datastore:datastore-preferences:1.1.1` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0 (*)` | `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0` |
| `org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3` | `org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3` |
| `com.google.dagger:hilt-android:2.57.2` | `com.google.dagger:hilt-android:2.57.2` |

외부 AndroidX/Kotlin/kotlinx/Hilt 성분은 Apache-2.0 계열이다. 최종 확인은 서명 artifact의 POM/NOTICE로 수행한다.

## 직접 의존성 — vendored 모듈 (8개, GPL-3.0)

| 모듈 | 비고 |
|---|---|
| `:alpine-runtime-api` | GPL-3.0 (vendored slice) |
| `:alpine-runtime-android` | GPL-3.0 (vendored slice) |
| `:alpine-runtime-host` | GPL-3.0 (vendored slice) |
| `:alpine-runtime-pack-bundled` | GPL-3.0 (vendored slice) |
| `:alpine-python-pack-bundled` | GPL-3.0 (vendored slice) |
| `:codex-cli-pack` | GPL-3.0 (vendored slice) |
| `:codex-gateway-pack-bundled` | GPL-3.0 (vendored slice) |
| `:codex-runtime-bridge` | GPL-3.0 (vendored slice) |

상세 고지는 [`third-party-notices.md`](third-party-notices.md)와 [`gpl-source-offer.md`](gpl-source-offer.md)를 참고.

## 번들 런타임 에셋 (APK 내, 별도 SPDX 존재)

| 에셋 | 근원 SPDX | 항목 수 |
|---|---|---|
| 로컬 Python 패키지 | `alpine-python-pack-bundled/src/main/python-pack/sbom.spdx.json` | 21 packages |
| Alpine 런타임 | `alpine-runtime-pack-bundled/src/main/resources/META-INF/alpine-runtime/sbom.spdx.json` | rootfs + lock |
| Codex CLI | `codex-cli-pack` (`codex-cli.lock.json` 해시 고정) | 공식 바이너리 |

## test-only 도구 (APK 미포함)

`:baselineprofile`(Macrobenchmark, UI Automator), androidTest 런타임(androidx.test) — 최종 검토 기록에만 보존.

## 재생성

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath > docs/dependencies-release.txt
```
