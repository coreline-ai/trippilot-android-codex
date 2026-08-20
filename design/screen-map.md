# TripPilot 화면 맵 및 컴포넌트 계약 — Trip Briefing

## Navigation 구조

```text
Onboarding
 └─ Trips
     ├─ Create / Edit Trip
     └─ Trip Home
         ├─ 다음 행동 / 다음 일정
         ├─ 준비
         ├─ 보관함 (예약 · 출처 · 공유 예약 텍스트)
         └─ 더보기
             ├─ AI 초안 (요청 · 검토 · 선택 반영)
             ├─ 외부 실행 (승인 전 미리보기)
             └─ 문제 대응 · 안전 메모
Settings
 ├─ Codex connection
 ├─ Privacy & external actions
 └─ Backup / reminder preferences

External-confirmation boundaries
 ├─ Calendar selection → ApprovalSheet → Calendar Provider
 ├─ Map / source URL selection → ApprovalSheet → Intent / Custom Tab
 └─ JSON / ICS selection → ApprovalSheet → Storage Access Framework
```

`Trips`는 phone 기본 시작 화면이다. Compact에서는 목록→여행 홈→전용 화면의 Back 흐름을 사용한다. 600dp 이상에서는 선택적으로 list-detail pane을 사용한다. `여정/준비/보관함/도움`을 동등한 고정 탭으로 표시하지 않으며, 일자 선택은 일정 화면 안에서만 쓴다.

## 2026-08-20 운영 홈 레이아웃

여행 홈은 다른 화면을 축소 복제하는 대시보드가 아니다. 첫 viewport에서 사용자가 **지금 처리할 한 가지**를 판단하도록 한다. 큰 cover, 2단 탭, full-width sticky CTA는 사용하지 않는다.

```text
┌────────────────────────────────────┐
│ [목록] 제주 치유 여행         [수정][더보기]│
│ 9/09–9/13 · 제주                        │
│                                            │
│ 오늘 확인할 것                            │
│ 교통편 예약 확인                    [확인] │
│ 항공권 확인번호를 보관함에 추가하세요       │
│                                            │
│ 다음 일정                                  │
│ 9/09 09:50 · 김포공항 출발              › │
│ 준비 6/8 · 다음 여권 유효기간           › │
│ 예약 및 서류 2개                         › │
└────────────────────────────────────┘
```

- 출발 전에는 미완료 준비, 여행 중에는 가장 가까운 일정, 귀국 후에는 사용자가 선택한 정리 항목이 주행동이 된다.
- `일정`, `준비`, `보관함`은 앱 바와 자신의 데이터 본문만 갖는 전용 화면이며 Back으로 여행 홈에 돌아온다.
- `더보기`는 AI 초안, 외부 실행, Safety Hub를 노출한다. 모든 AI·외부 실행의 검토/승인 경계는 기존과 같다.
- 예약과 출처는 보관함 안에서 함께 읽는다. 예약 행은 유형, 상태, 확인번호, 시간/장소를 우선 보이며, 출처는 제목과 재확인 상태를 우선 보인다.
- 600dp 이상에서는 왼쪽에 여행 목록을 고정하고 오른쪽에 선택된 전용 화면을 표시한다. 4개 목적 영역 rail은 만들지 않는다.

### Legacy contract migration

이전 `RouteRibbon`은 고정된 진행 값을 그리던 legacy component다. 이번 GUI에서는 실제 데이터와 semantics를 갖는 `JourneyStageStrip`으로 교체한다. 기존 marker는 디자인 검증과 변경 이력을 위해 이 문서에만 유지하며, 새 UI는 `route_ribbon` testTag나 장식성 완료 상태에 의존하지 않는다.

## Legacy compact wireframes (historical reference)

아래 기록은 2026-08-20 이전 Trip Briefing 구조의 검토 이력이다. 현재 구현과 검증 기준은 위 **운영 홈 레이아웃** 및 `design/audit/content-system.json`을 우선한다.

### 여행 목록

```text
┌────────────────────────────────────┐
│ TripPilot                            │
│ 나의 여행                            │
│ 다음 여행의 준비와 예약을 먼저 봅니다 │
│                                    │
│ ┌ 다음 여정 ──────────────────────┐ │
│ │ 서울 주말 여행                   │ │
│ │ 8/18–8/21 · 준비 6/10           │ │
│ │ 다음: 숙소 확인                  │ │
│ └─────────────────────────────────┘ │
│                                    │
│ [새 여행 만들기]                    │
└────────────────────────────────────┘
```

### 여정 / 브리핑

```text
┌────────────────────────────────────┐
│ [목록] 서울 주말 여행          [수정]│
│ 8/18 월 — 8/21 목 · 국내             │
│                                    │
│ 출발 전 ── DAY 1 ── DAY 2 ── 귀국 후 │
│ 오늘 확인: 숙소 예약 번호             │
│                                    │
│ [여정] [준비] [보관함] [도움]          │
│ [브리핑] [일정]                       │
│                                    │
│ 다음 행동                             │
│ 숙소 예약을 확인하세요                │
│                                    │
│ 준비 6/10 · 예약 1 · 출처 재확인 1    │
└────────────────────────────────────┘
```

### 여정 / 일정

```text
┌────────────────────────────────────┐
│ 서울 주말 여행 · 일정                 │
│ DAY 1 · 8월 18일 월                  │
│ [DAY 1] [DAY 2] [DAY 3]              │
│                                    │
│ 09:00 ── 호텔 체크인                  │
│          서울역 · 숙소                │
│          [출처 1]                    │
│ 11:00 ── 성수동 카페                  │
│          개인 메모                    │
│                                    │
│ [+ 일정 추가]                         │
└────────────────────────────────────┘
```

### 준비

```text
┌────────────────────────────────────┐
│ 서울 주말 여행 · 준비                 │
│ 준비 6개 중 3개 완료                  │
│                                    │
│ 서류 · 입국                    1/3    │
│ [✓] 여권 상태                         │
│     유효기간과 입국 조건을 직접 확인  │
│ [ ] 숙소 확인서                       │
│                                    │
│ 통신 · 전자기기                2/4    │
│ [ ] eSIM 또는 로밍                    │
│ [✓] 보조배터리                         │
│                                    │
│ [준비 항목 추가]                      │
└────────────────────────────────────┘
```

### 보관함 / 예약

```text
┌────────────────────────────────────┐
│ 서울 주말 여행 · 보관함               │
│ [예약] [출처]                         │
│                                    │
│ 숙소 · 확인됨                         │
│ Field Hotel Seoul                     │
│ 확인번호 TP-SEOUL-01                  │
│ 8/18 15:00 · 서울                     │
│ 연결 출처 1 · 마지막 확인 없음         │
│                                    │
│ [예약 추가]                           │
└────────────────────────────────────┘
```

### 도움 / AI 초안·외부 실행

```text
┌────────────────────────────────────┐
│ 서울 주말 여행 · 도움                 │
│ [AI 초안] [외부 실행]                 │
│                                    │
│ 초안은 여행 기록이 아닙니다            │
│ [선택됨] 8/18 11:00 성수동 카페 [수정]│
│ [제외됨] 준비: 우산                   │
│                                    │
│ [선택한 2개 항목 반영]                │
└────────────────────────────────────┘
```


### 도움 / Safety Hub (문제 대응 · 안전 메모)

도움 영역 진입 panel에서만 들어가는 별도 화면. 세 번째 tab이 아니며 Back으로 기존 도움 화면에 복귀한다.

```text
┌────────────────────────────────────┐
│ 문제 대응 · 안전 메모      [도움으로]│
│ ⚠ 긴급 서비스가 아닙니다 (고지 패널)│
│ 문제 유형별 일반 순서               │
│ [여권·신분·여행 서류]      [펼치기] │
│ [질병·부상]                [펼치기] │
│ … (7개 카테고리)                   │
│ 내가 저장한 연락 정보·메모           │
│ [카드사 공식 앱 · 메모 · 출처]      │
│ [안전 메모 추가]                    │
└────────────────────────────────────┘
```

- 상태: empty(안내+CTA) / populated / edit(`SafetyMemoDialog` = TripFormSheet 재사용) / 출처 승인(기존 ApprovalSheet)
- testTag: `safety_hub_screen`, `safety_hub_entry`, `safety_category_<id>`, `safety_memo_<id>`, `add_safety_memo`
- 정적 문구와 사용자 메모는 시각적으로 분리. 연락값 tap은 복사만, 외부 열기는 승인 후

### 준비 / 귀국 후 window (준비 화면 하단)

```text
│ 귀국 후                             │
│ [귀국 후 48시간 이내  N개 중 M완료] │
│   … 항목 … [이 시점 팩 추가][직접추가] │
│ [귀국 후 1주 이내] [귀국 후 나중에]  │
```

- window 팩은 opt-in, template ID 기준 idempotent. 미선택 window는 완료/미완료로 표시하지 않는다
- testTag: `post_trip_window_<window>`, `add_post_trip_pack_<window>`, `add_post_trip_item_<window>`

## Legacy medium/expanded wireframes (historical reference)

```text
┌────────────────────┬────────────────────────────────────┐
│ 나의 여행           │ 서울 주말 여행                       │
│                    │ 8/18–8/21 · 국내                    │
│ 서울 주말 여행      │ 출발 전 ─ DAY 1 ─ DAY 2 ─ 귀국 후   │
│ 부산 주말 여행      │ 오늘 확인: 숙소 예약 번호            │
│                    │                                    │
│ [+ 새 여행]         │ [여정] [준비] [보관함] [도움]         │
│                    │ [브리핑] [일정]                      │
│                    │ 준비 6/10 · 예약 1 · 다음 일정 09:00 │
└────────────────────┴────────────────────────────────────┘
```

- 600dp 이상은 list-detail을 추가해도 navigation label·action label·승인 경계가 compact와 같아야 한다.
- 840dp 이상 rail을 써도 primary action을 rail에 중복하지 않는다.
- 2.0x font scale에서 stage strip, 날짜 selector, CTA는 세로로 자연스럽게 확장된다.

## Component contract

| Component | 입력 / 상태 | 의미·접근성 | testTag | 금지 |
|---|---|---|---|---|
| `TripBriefScaffold` | title, selected area/page, content, primary action | safe inset·sticky title·scroll/action order를 한 곳에서 제공 | `trip_brief_scaffold`, `trip_detail_screen` | 탭마다 큰 독립 app bar |
| `TripBriefHeader` | trip, back, edit, compact | 브리핑에서 제목·기간·범위를 모두 읽고, 다른 탭에서는 축소 | `trip_brief_header`, `back_to_trips` | 제목/기간을 숨김 |
| `JourneyStageStrip` | stage list, selected stage, status summary, onStageSelect | 각 stage의 날짜·상태와 전체 요약을 낭독 | `journey_stage_strip`, `journey_stage_<id>` | 데이터 없는 완료 상태, Canvas-only 의미 |
| `TripAreaNavigation` | selected area, onSelected | 여정/준비/보관함/도움 중 하나를 명확히 낭독 | `trip_area_<area>` | 4개 초과 primary tab |
| `SubPageNavigation` | 최대 2 entries, selected | 현재 area에 종속된 하위 목적을 낭독 | `trip_subpage_<group>_<page>` | stage strip과 역할 중복 |
| `BriefingPanel` | kind, title, summary, action | 다음 행동·진행·예약 상태를 읽는 우선순위 panel | `briefing_panel_<kind>` | 동일 card의 반복 stack |
| `TimelineEntry` | date/time/all-day, type, location, notes, source state | 시간·장소·개인 메모·출처 상태를 순서대로 낭독 | `timeline_entry_<id>` | 항목 표시만으로 link 실행 |
| `ChecklistGroup` | group, completion, items, optional | 그룹·완료 수·다음 미완료를 함께 낭독 | `checklist_group_<id>` | 이유 없는 단일 checklist 목록 |
| `DocumentRow` | reservation/source summary, state, actions | 예약처·확인번호·상태·연결 출처를 문서처럼 읽음 | `document_row_<id>` | 카드 안의 중복 action row |
| `ApprovalSheet` | target, consequence, confirm, cancel | 경계 밖 실행 대상·취소 결과를 낭독 | `approval_sheet`, `confirm_<action>` | 로컬 저장에 과도하게 사용 |
| `SelectionReviewRow` | selected, summary, edit state | 초안은 요약으로 시작하고 수정 때만 form 확장 | `draft_selection_<id>`, `edit_draft_<id>` | 초안 form을 기본으로 전부 펼침 |

## 기존 기능 parity → 신규 화면·테스트 추적표

| ID | 기존 기능 계약 | 신규 화면 / 책임 모델 | 구현 Phase | 최소 검증 |
|---|---|---:|---:|---|
| PAR-01 | 시작 안내·로컬 우선 고지 | Trips/Privacy screen | 1,5 | T-ONBOARD-LOCAL-01 |
| PAR-02 | 여행 보드 CRUD | Trips/Trip editor/Trip brief | 1,4 | T-TRIP-CRUD-01 |
| PAR-03 | 여행 범위·기본 항목 | ReadinessTemplateCatalog/template migration | 3 | T-SCOPE-TEMPLATE-01 |
| PAR-04 | Preparation/Packing 분리 | ChecklistGroup/Readiness screen | 3 | T-READINESS-SPLIT-01 |
| PAR-05 | 준비 완료 알림 | reminder preference/rule/delivery | 5 | T-REMINDER-D7-01 |
| PAR-06 | 일정 CRUD | date selector/TimelineEntry | 2,4 | T-ITINERARY-BOUNDARY-01 |
| PAR-07 | 출처·재확인 | DocumentRow/Sources screen | 2 | T-SOURCE-RECHECK-01 |
| PAR-08 | 지도·브라우저 열기 | ApprovalSheet/Intent handoff | 4,5 | T-EXTERNAL-CONFIRM-01 |
| PAR-09 | 예약 관리 | Reservation document rows | 2,4 | T-RESERVATION-UNIQUE-01 |
| PAR-10 | 공유 예약 입력 | Share intake/document state | 2,4 | T-SHARE-TEXT-ONLY-01 |
| PAR-11 | 예약 AI 초안 | SelectionReviewRow/ReservationDraft | 4 | T-DRAFT-RESERVATION-01 |
| PAR-12 | 여행 AI 계획 | Draft request/review/partial apply | 4 | T-DRAFT-PARTIAL-APPLY-01 |
| PAR-13 | 날씨 참고 | WeatherAdvisoryDraft read-only | 4 | T-WEATHER-READONLY-01 |
| PAR-14 | Calendar approval ledger | ApprovalSheet/CalendarAction | 4,5 | T-CALENDAR-LEDGER-01 |
| PAR-15 | ICS export | selected itinerary review | 4,5 | T-ICS-SELECTED-01 |
| PAR-16 | JSON backup·restore | v1/v2 backup contract | 3,5 | T-BACKUP-COPY-01 |
| PAR-17 | 범용 Agent 대체 | AI draft entry/connection state | 4 | T-AI-NO-CHAT-01 |

## 접근성 / responsive 검토 checklist

- `A11Y-01`: TalkBack 순서는 compact header → journey stage summary → 목적 영역 → 하위 페이지 → content → primary action이다.
- `A11Y-02`: 준비 진행은 `서류·입국 3개 중 1개 완료, 다음 숙소 확인서`처럼 수치·그룹·다음 행동을 함께 말한다.
- `A11Y-03`: icon-only control은 명확한 content description을 가진다.
- `LAYOUT-01`: 360dp, 600dp, 840dp에서 같은 정보 우선순위를 유지한다.
- `TEXT-01`: 1.0x, 1.3x, 2.0x에서 title/date/error/action이 clipping 또는 의미 없는 ellipsis 없이 보인다.
- `MOTION-01`: animator scale 0은 JourneyStageStrip과 content transition을 즉시 완료한다.
