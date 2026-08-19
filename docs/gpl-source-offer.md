# GPL-3.0 소스 제공(Source Offer) 및 법적 고지 — 초안

상태: **초안 (배포 owner 법률 검토 전)**. 이 문서는 GPL-3.0 의무를 충족하기 위한 실무 문서를 준비하는 것이며 법률 자문이 아니다.

## 1. GPL 성분의 범위

TripPilot APK에는 다음 GPL-3.0-or-later 성분이 포함된다:

| 성분 | 위치 | 형태 |
|---|---|---|
| Alpine Linux rootfs (Alpine 3.21 minirootfs) | `alpine-runtime-pack-bundled` APK asset | 바이너리 루트파일시스템 |
| PRoot (user-space proot) | `alpine-runtime-android` (NDK `pty_bridge` 포함) | 네이티브/소스 빌드 |
| Codex Gateway (`codex_gateway/` Python) | `codex-gateway-pack-bundled` APK asset | 소스 그대로 번들 |
| Codex CLI (공식 배포 바이너리) | `codex-cli-pack` APK asset (해시 고정) | 바이너리 |
| 로컬 Python 패키지 (게이트웨이 부트스트랩용) | `alpine-python-pack-bundled` APK asset + `sbom.spdx.json` | 바이너리 wheel |
| `codex-runtime-bridge` 등 vendored Kotlin 모듈 | `third_party/alpine-codex-cli-client/` | 소스 (pinned commit `c8c7ca9`) |

앱 본체(`:app`)와 UI 코드는 이 slice를 **별도 프로세스 런타임으로 호출**하지만 APK 하나로 배포되므로 **결합 배포(combined work)** 로 취급해 전체 대응 소스를 제공한다.

## 2. 대응 소스(Corresponding Source) 정의

다음을 모두 포함한다:

1. 이 저장소 전체 — 앱 소스, 빌드 스크립트, vendored 모듈 소스, pinned commit 정보
2. 빌드 방법: `README.md`의 빌드 절차 + `gradle/libs.versions.toml`의 정확한 도구 버전
3. 번들 바이너리의 원본 소스와 빌드 스크립트:
   - Alpine minirootfs: 공식 배포 아티팩트(해시는 `alpine-runtime-pack-bundled`에 고정) 및 패키지 원본 소스 접근 안내
   - PRoot: upstream 소스 + 적용 패치 (있다면 `third_party/`에 보관)
   - Python 패키지: 각 wheel의 원본 소스 URL·해시 — `sbom.spdx.json`에 기록됨
   - Codex CLI: 공식 배포 바이너리 (해시 고정). 소스는 upstream 공개 저장소에서 해당 버전으로 확보 가능
4. 설치 정보(ARMS): APK 설치 자체가 설치 절차에 해당하며 추가 스크립트 불필요

## 3. 소스 제공 방식

- **함께 배포(기본)**: APK와 동일한 취득 경로(배포 페이지)에 소스 스냅샷 아카이브(tag 압축본)를 함께 게시
- **서면 약속(written offer)**: 아래 문구를 배포 페이지·앱 정보에 포함

> **오픈소스 소스 코드 제공 안내**
> 이 애플리케이션에는 GNU General Public License v3.0 이상으로 배포되는 구성요소(Alpine Linux, PRoot, Codex Gateway, Codex CLI)가 포함되어 있습니다. 해당 라이선스에 따라 완전한 대응 소스 코드는 https://github.com/coreline-ai/trippilot-android-codex (릴리스 태그)에서 누구나 내려받을 수 있으며, 3년간 동일 조건으로 제공됩니다. 별도 미디어 요청은 [배포 owner 연락처]로 연락 바랍니다.

## 4. 라이선스 고문 포함 (APK 내)

- `THIRD_PARTY_NOTICES` / assets에 GPL-3.0 전문 + 저작권 고지 포함 (Play 정책상 소스 링크만으로는 부족할 수 있어 전문 포함 권장)
- 앱 내 "오픈소스 라이선스" 화면 또는 설정 링크에서 위 안내문 표시

## 5. 배포 전 체크리스트

- [ ] 릴리스 태그로 소스 아카이브 생성·업로드
- [ ] Codex CLI 해당 버전의 upstream 라이선스·배포조건 재확인 (현재 lock: `codex-cli.lock.json`)
- [ ] `sbom.spdx.json`의 모든 Python 패키지 원본 소스 URL 유효성 확인
- [ ] GPL-3.0 전문 APK 포함
- [ ] 공유 Gateway의 dormant multi-agent 코드 vendor 검토 기록 (`docs/phase4-gate.md` residual)
- [ ] 법률 검토 sign-off

## 6. 연관 문서

- [`third-party-notices.md`](third-party-notices.md) — 포함 성분·라이선스 현황
- [`release-readiness.md`](release-readiness.md) — 배포 게이트
- [`sbom.md`](sbom.md) — 의존성 명세
