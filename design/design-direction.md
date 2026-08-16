# TripPilot 디자인 방향 — Wayfinding Field Journal

## 제품의 한 가지 일

출발 전에는 **다음에 준비할 일**, 여행 중에는 **지금 이동할 다음 일정**을 한 화면에서 즉시 판단하게 한다. TripPilot은 여행지를 소비하는 사진 앨범이 아니라, 사용자가 확정한 시간·장소·준비 상태를 읽는 개인용 여행 기록장이다.

## 시각 논지

**Wayfinding Field Journal**은 종이 지도나 관광 엽서가 아니라, 현장에서 수정되는 항법 기록에서 출발한다. 앱의 고유 요소인 `RouteRibbon`은 장식용 물결선이 아니다. 날짜 순서, 현재 위치, 완료, 사용자 승인 대기, 다음 행동을 한 줄의 경로로 전달한다.

- **기준선:** Pilot Navy는 시간축과 신뢰 가능한 확정 정보를 표현한다.
- **연결됨:** Wayfinding Cyan은 선택된 날짜·현재 경로·연결 가능한 흐름을 표현한다.
- **다음 행동:** Boarding Orange는 한 화면의 단일 주 행동과 승인 대기 상태에만 쓴다.
- **AI 초안:** Stamp Violet은 외부에서 온 제안이며 아직 여행 기록이 아니라는 경계를 표시한다.
- **완료:** Moss Green은 이미 확인하거나 끝낸 항목에만 쓴다.

이 앱의 한 가지 의도적인 시각적 위험은 **일반적인 여행 카드 그리드를 버리고 RouteRibbon을 상태 탐색 장치로 사용하는 것**이다. 리본의 각 점은 텍스트 날짜·시간·상태와 항상 함께 제공되므로, 색이나 위치만으로 의미를 전달하지 않는다.

## 디자인 원칙

1. **한 화면, 한 행동:** 주 행동은 Boarding Orange 버튼 하나로 제한한다. 파괴적·보조 행동은 text button 또는 overflow로 보낸다.
2. **경로가 구조다:** 날짜, 일정, 예약, 준비 상태의 순서는 표·사진 카드가 아니라 시간축과 리본으로 연결한다.
3. **초안은 기록이 아니다:** AI·공유 예약 입력·Calendar 반영 전 항목은 Stamp Violet 또는 review 상태와 명시적 동사(검토, 반영, 열기)로 구분한다.
4. **여백은 판단 시간이다:** surface 위에 겹친 지도·배경사진·glassmorphism을 쓰지 않는다. 상태 변화만 1dp elevation 또는 color container로 구분한다.
5. **문구는 사용자 행동을 말한다:** “저장”, “반영”, “일정에 추가”, “지도 열기”처럼 결과가 분명한 동사를 쓴다. 기술 용어·모호한 감탄문·마케팅 카피는 배제한다.

## 의도적으로 제거한 일반적 여행 UI

| 제거 | 이유 | 대체 |
|---|---|---|
| 도시 사진 hero·배경 지도 | 라이선스·네트워크·시선 분산을 만들고 현재 할 일을 알려주지 못함 | 날짜·목적지·다음 행동·RouteRibbon |
| 목적지별 큰 카드 grid | 일정의 시간 순서를 숨김 | 여행 보드의 status strip와 최신 여행 우선 목록 |
| 임의의 gradient·glass surface | 정보 상태가 아닌 장식이 됨 | 평면 Cloud Paper surface와 semantic container |
| AI 채팅 버블 | AI가 실행 권한을 가진 것처럼 보일 수 있음 | 요청 → 생성 → 검토 → 선택 반영의 고정 단계 |
| 완료율만 있는 원형 progress | 어느 항목이 막혔는지 설명하지 못함 | Preparation/Packing 분리 목록과 RouteRibbon summary |

## 색상·타입·형태의 사용

정본 값은 [`tokens.json`](tokens.json)과 [`tokens.md`](tokens.md)다. 앱 구현은 이 파일의 semantic role을 Compose `ColorScheme`과 component token으로 옮긴다.

- 한국어 본문과 제목은 시스템 `sans-serif`를 사용한다. 외부 web font와 런타임 font download는 없다.
- 날짜, 시간, 예약 확인번호만 `monospace` utility style을 사용해 스캔성을 높인다.
- Surface radius는 20dp, interactive container와 action은 14dp, 상태 chip은 pill로 고정한다.
- 최소 터치 영역은 48dp이며, 작은 아이콘 단독 버튼도 touch target을 줄이지 않는다.
- system animator scale이 0이거나 reduce-motion이 요청되면 route transition을 즉시 전환한다.

## RouteRibbon 정보 계약

| 표현 | 데이터 의미 | 텍스트 대체 | 상호작용 |
|---|---|---|---|
| 기준선 | 여행 기간과 날짜 순서 | `여행 기간 3일 중 2일째` | 없음 |
| Cyan 구간 | 현재 선택된 날짜 또는 진행 중인 itinerary 범위 | `현재 보고 있는 8월 18일` | 날짜 탭과 동기화 |
| Moss 노드 | 준비·일정·Calendar action이 완료됨 | `완료: 여권 확인` | 해당 항목으로 이동 |
| Orange 노드 | 다음 사용자 행동 또는 승인 대기 | `확인 필요: 공항 이동 일정 검토` | review/상세로 이동 |
| Violet 노드 | AI 초안·검토 대기 | `AI 초안 3개 항목 검토 필요` | AI 초안 검토로 이동 |
| Outline 노드 | 아직 계획되지 않은 미래 날짜 | `8월 19일 일정 없음` | 날짜 탭으로 이동 |

RouteRibbon은 Canvas만으로 의미를 전달하지 않는다. Compose semantics에는 전체 요약과 개별 노드의 설명을 제공하고, 키보드/TalkBack 흐름에서는 같은 날짜 탭과 목록을 통해 모든 행동을 수행할 수 있어야 한다.

## 컴포넌트 사용 금지 규칙

- `StatusChip`은 상태를 짧게 읽는 용도다. filter, navigation, primary CTA 역할을 겸하지 않는다.
- `PrimaryAction`은 한 screen state에 1개만 표시한다. loading일 때 문구를 유지한 채 진행 상태만 덧붙인다.
- `ConfirmActionSheet`는 Calendar/브라우저/지도/파일 쓰기 같은 경계 밖 action에만 쓴다. 로컬 폼 저장에는 쓰지 않는다.
- empty state illustration은 안내 문구와 행동을 보조할 뿐, 정보보다 크게 보이지 않게 한다.

## 디자인 비평 기록 (`frontend-design`)

### 초기 검토

처음 방향에는 “여행 앱”이라는 이유만으로 목적지 사진·지도 미리보기·여행 카드가 들어갈 위험이 있었다. 이는 다른 여행 앱과 구분되지 않고, 준비·승인·시간 순서를 보여줘야 하는 제품의 단일 목적도 흐린다.

### 수정 결정

사진·지도·카드 중심 접근을 제거하고, RouteRibbon을 화면 간 공통 구조로 제한했다. 리본 이외의 surface는 조용한 Cloud Paper와 명확한 label을 사용한다. 이 선택은 시각적 차별화를 한 요소에 집중하면서도 Compose에서 접근성과 screenshot test로 검증 가능하다.

### 재검토 결과

RouteRibbon이 장식으로 변하는 것을 막기 위해, 모든 노드에 도메인 상태·텍스트 대체·동작 목적을 명시했다. 따라서 별도 illustration이나 raster texture는 Phase 0에서 불필요하다고 판정한다. v1에는 `imagegen` 및 unDraw 산출물을 포함하지 않는다.
