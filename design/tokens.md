# TripPilot Trip Briefing 디자인 토큰

이 문서는 [`tokens.json`](tokens.json)의 사람이 읽는 설명이다. 구현 시 화면별 hex·spacing·radius·animation duration 하드코딩을 금지하고, Compose theme와 component token으로만 참조한다.

## 색상 역할

| 역할 | Light | Dark | 사용 |
|---|---:|---:|---|
| Pilot Navy / `primary` | `#10243F` | `#B8CCE8` | app bar, 신뢰 가능한 기준선, 주요 텍스트 |
| Wayfinding Cyan / `wayfinding` | `#16A8B8` | `#72D8E1` | 현재 날짜, 연결된 경로, 선택 상태 |
| Boarding Orange / `boarding` | `#F26A3D` | `#FFB59A` | 단일 주 행동, 승인 필요 |
| Stamp Violet / `ai` | `#6C63D9` | `#C5BFFF` | AI 초안 및 검토 중 |
| Moss Green / `success` | `#25785C` | `#7ED7AF` | 완료·확인됨 |
| Signal Red / `error` | `#B3261E` | `#FFB4AB` | 오류·파괴적 확인 |
| Cloud Paper / `surface` | `#F5F8FA` | `#0E151C` | 기본 배경 |
| Surface Variant | `#E0E7ED` | `#25313B` | 분리된 정보 영역 |

각 solid semantic color는 대응하는 `on*` foreground와 함께만 사용한다. outline은 border/disabled only이며 일반 본문에 사용하지 않는다.

## 대비 기준

- 일반 본문·아이콘·상태 label은 WCAG AA 4.5:1 이상이다.
- 18sp bold 이상 display text는 3:1 이상일 수 있으나, TripPilot의 정본 pair는 일관성을 위해 4.5:1 이상을 목표로 한다.
- 성공·오류·AI 상태는 색만으로 전달하지 않고 label과 icon/문장으로 같이 표시한다.
- 실제 ratio는 `python3 scripts/verify_phase0_design.py`가 `tokens.json`의 light/dark pair마다 계산한다.

## 글자·간격

| 토큰 | 값 | 규칙 |
|---|---|---|
| Display | 28sp / 36sp / 700 | 여행 제목·주요 상태, 2줄 초과 금지 |
| Title | 22sp / 28sp / 700 | 화면 제목, 2줄이면 버튼 아래로 이동 |
| Section | 17sp / 24sp / 700 | section heading |
| Body | 16sp / 24sp / 400 | 기본 설명·폼 label |
| Label | 14sp / 20sp / 600 | status chip, 보조 행동 |
| Utility | 13sp / 18sp / 500 monospace | 날짜·시간·예약 코드 |
| spacing | 4, 8, 12, 16, 24, 32, 40dp | 이 값 외 임의 gap 금지 |

font scale 1.0x, 1.3x, 2.0x에서 text는 줄바꿈될 수 있어야 하며, 날짜·시간·오류·주 행동은 ellipsis로 의미를 숨기지 않는다. 제목이 2줄을 넘는 경우 body scroll을 허용하고 action을 off-screen으로 밀어내지 않는다.

## Layout·edge-to-edge

- Compact: 360–599dp, horizontal padding 20dp, single-column navigation.
- Medium: 600–839dp, 2-pane은 선택 향상; drawer가 아니라 list-detail hierarchy를 유지.
- Expanded: 840dp 이상, navigation rail + list-detail을 허용하고 content max width 720dp를 유지.
- status/navigation bar inset은 앱 shell에서만 처리한다. 각 screen이 중복 padding을 추가하지 않는다.
- touch target은 48dp 이상이다.

## Shape·elevation·motion

| 역할 | 값 | 규칙 |
|---|---:|---|
| surface | 20dp | summary, section container |
| action/input | 14dp | button, input, selectable row |
| travel cover | 28dp | 브리핑 첫 화면과 목록의 featured journey container |
| status chip | 999dp | 상태 전달만 수행 |
| raised | 1dp | 선택된 summary만; card stack 금지 |
| modal | 6dp | confirm sheet만 |
| stage transition | 180ms | 실제 여행 단계·선택 일자 상태 보간 |
| content transition | 120ms | section 전환 |

system animator scale이 0이거나 접근성 reduce-motion 상태에서는 모든 transition을 생략한다. JourneyStageStrip, icon, button을 반복·무한 애니메이션하지 않는다.

## Material Symbols 고정 목록

모든 기능 아이콘은 **Material Symbols Rounded, weight 500, fill 0, optical size 24**로 통일한다.

| 의미 | Symbol |
|---|---|
| 여행 만들기 | `add` |
| 일정 | `calendar_month` |
| 준비 확인 | `checklist` |
| 짐 | `luggage` |
| 예약 | `confirmation_number` |
| 출처 | `link` |
| AI 초안 | `auto_awesome` |
| 승인·반영 | `task_alt` |
| 지도 열기 | `map` |
| 브라우저 열기 | `open_in_new` |
| 공유 입력 | `ios_share` |
| 백업 | `archive` |
| 알림 | `notifications` |
| 설정·개인정보 | `tune` |
| 삭제 | `delete` |
| 오류 | `error_outline` |

Material Symbols 외 third-party icon pack을 추가하지 않는다. 기능 의미가 이미 text label로 충분한 경우 아이콘을 중복하지 않는다.
