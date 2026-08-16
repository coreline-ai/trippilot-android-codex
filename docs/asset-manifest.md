# TripPilot 에셋 매니페스트

이 문서는 앱에 포함되는 모든 비-Material 에셋의 단일 인벤토리다. 새로운 파일은 포함 전에 아래 필드를 모두 채우고 `scripts/verify_phase0_design.py`를 통과해야 한다. v1은 원격 이미지·런타임 asset download·외부 illustration을 포함하지 않는다.

| 파일 | 용도 | source URL / 생성 방식 | License | Author | Modification | SHA-256 | 승인일 | Android target |
|---|---|---|---|---|---|---|---|---|
| `design/assets/app-mark.svg` | adaptive icon foreground / wordmark seed | TripPilot 팀 자체 제작, 2026-08-16 | Proprietary to TripPilot project | TripPilot | original | `b43fb22a4a3a7f9bc828566c28e0b6986ffc7de8b288aa71fe7bc589f5adecd2` | 2026-08-16 | `mipmap-anydpi-v26` VectorDrawable로 변환 |
| `design/assets/route-ribbon.svg` | RouteRibbon source reference | TripPilot 팀 자체 제작, 2026-08-16 | Proprietary to TripPilot project | TripPilot | original | `ab3102ed7af30defbd38f4585c9437a34c0d35abf01d4e444fbc6fc3341eb1a7` | 2026-08-16 | Compose `RouteRibbon` path reference |
| `design/assets/empty-trips.svg` | 여행 없음 empty state | TripPilot 팀 자체 제작, 2026-08-16 | Proprietary to TripPilot project | TripPilot | original | `4380ac973cc2006bc570e820984c97f9c3d1ef2ee23b629bd6db8ac7e9ac7f49` | 2026-08-16 | `drawable/empty_trips.xml`로 변환 |
| `design/assets/empty-itinerary.svg` | 일정 없음 empty state | TripPilot 팀 자체 제작, 2026-08-16 | Proprietary to TripPilot project | TripPilot | original | `da8dd094a3de096b5e2246c52b99d540bba888a90a979f6a421b65735998943c` | 2026-08-16 | `drawable/empty_itinerary.xml`로 변환 |
| `design/assets/ai-connection-required.svg` | Codex 연결 필요 empty state | TripPilot 팀 자체 제작, 2026-08-16 | Proprietary to TripPilot project | TripPilot | original | `484df8de8625dc021f9b96feffd15b4ffb40b850ca495bf0bad572da0b51be5b` | 2026-08-16 | `drawable/ai_connection_required.xml`로 변환 |

## 표준 아이콘 예외

Material Symbols Rounded는 별도 binary asset으로 저장하지 않는다. [`design/tokens.md`](../design/tokens.md)의 고정 symbol 목록만 사용하며, 사용 시 Apache-2.0 고지를 `docs/third-party-notices.md`에 반영한다.

## 금지 목록

- `<image href="http...">`, 외부 SVG `use`, web font, embedded tracker를 포함한 에셋
- 라이선스·저자·원본 생성 방식·SHA-256 중 하나라도 없는 파일
- 도시 사진·국기·지도 타일·브랜드 로고·unDraw/imagegen raster (별도 승인 전)
