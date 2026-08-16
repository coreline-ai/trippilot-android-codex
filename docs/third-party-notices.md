# Third-party notices — 초안

## 현재 앱에 실제 포함된 항목

| 항목 | 용도 | License 확인 상태 |
|---|---|---|
| AndroidX / Jetpack Compose / Room / DataStore / Navigation | Android UI·저장소 기반 | Phase 5 release 전에 dependency report 기준 고지 확정 필요 |
| Hilt | DI 기반 준비 | Phase 5 release 전에 dependency report 기준 고지 확정 필요 |
| Kotlin / kotlinx | Kotlin runtime·coroutine·serialization | Phase 5 release 전에 dependency report 기준 고지 확정 필요 |
| Material Symbols | 기능 아이콘 정책 | Apache-2.0; 실제 binary 포함 방식과 고지를 Phase 5에서 확정 |

## 아직 포함하지 않은 항목

| 항목 | 현재 상태 | 배포 전 필수 검토 |
|---|---|---|
| `alpine-codex-cli-client` | Phase 1 source inspection만 했으며 submodule/composite dependency 없음 | GPL-3.0, bundled CLI, Alpine, PRoot, Python pack, source 제공·ABI·asset reproducibility |
| Codex CLI / Alpine / PRoot / Gateway | TripPilot APK에 미포함 | Phase 4/5에서 exact artifact lock·license·SBOM·notice 결정 |
| unDraw / imagegen raster | v1 미포함 | 사용 승인 전 manifest·license·hash 기록 |

TripPilot repository에는 OAuth token, credential, private CLI binary, signing key, closed-source license file을 저장하지 않는다.
