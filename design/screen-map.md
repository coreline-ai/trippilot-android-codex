# TripPilot 화면 맵 및 컴포넌트 계약

## Navigation 구조

```text
Onboarding
 └─ Trips
     ├─ Create / Edit Trip
     └─ Trip Detail
         ├─ Overview
         ├─ Itinerary
         ├─ Readiness (Preparation / Packing)
         ├─ Reservations
         ├─ Sources
         └─ AI Draft
             ├─ Planning request
             ├─ Draft review
             └─ Apply result
Settings
 ├─ Codex connection
 ├─ Privacy & external actions
 └─ Backup / reminder preferences

External-confirmation boundaries
 ├─ Calendar selection → ConfirmActionSheet → Calendar Provider
 ├─ Map / source URL selection → ConfirmActionSheet → Intent / Custom Tab
 └─ JSON / ICS selection → ConfirmActionSheet → Storage Access Framework
```

`Trips`는 phone 기본 시작 화면이다. `TripDetail`의 섹션은 bottom navigation이 아니라 동일 여행 안의 segmented tab/scroll section으로 구현한다. Compact에서는 목록→상세의 순차 back stack을, medium/expanded에서는 선택적으로 list-detail을 사용한다.

## Compact wireframes (360–599dp)

### 여행 목록

```text
┌────────────────────────────────────┐
│ TripPilot                    [설정] │
│ 나의 여행                            │
│ ━━●━━━━○━━━━○  다음 출발까지 12일     │
│                                    │
│ [서울 · 8/18–8/21]                 │
│  준비 6/10 · 다음: 호텔 확인          │
│                                    │
│ [새 여행 만들기]                     │
└────────────────────────────────────┘
```

### 여행 상세 개요

```text
┌────────────────────────────────────┐
│ [뒤로] 서울 여름 여행         [더보기]│
│ 8월 18일 월 — 8월 21일 목 · KST      │
│ ━●━━━━●━━━━●━━━━○                    │
│ 오늘: 성수동 카페 10:00               │
│ [일정] [준비] [예약] [출처] [AI 초안]  │
│                                    │
│ 다음 행동                            │
│ 공항 이동 시간을 확인하세요           │
│ [일정 보기]                          │
└────────────────────────────────────┘
```

### 일정 / 준비

```text
┌────────────────────────────────────┐
│ 8월 18일 월  [<] [>]                │
│ ━●━━━━○━━━━○  1일째                  │
│ 09:00  호텔 체크인                    │
│ 11:00  성수동 카페                    │
│                 [+ 일정 추가]        │
├────────────────────────────────────┤
│ 준비 6 / 10                          │
│ [✓] 여권 확인                        │
│ [ ] 여행자 보험 확인                  │
│ 짐 2 / 5                             │
│ [ ] 충전기 × 1                       │
└────────────────────────────────────┘
```

### AI 초안 검토

```text
┌────────────────────────────────────┐
│ AI 초안 검토                         │
│ 입력: 3박 4일 · 카페와 미술관          │
│ [보라색: 초안은 아직 저장되지 않음]    │
│                                    │
│ [✓] 8/18 11:00 성수동 카페 [수정]     │
│ [ ] 준비: 우산                       │
│ [✓] 출처 후보 2개                    │
│ 가정: 비 예보는 확인이 필요합니다      │
│ [선택한 2개 항목 반영]                │
└────────────────────────────────────┘
```

### Codex 연결 상태

```text
┌────────────────────────────────────┐
│ Codex 연결                           │
│ [보라색 상태] 연결되지 않음            │
│ AI는 여행 초안만 제안합니다.            │
│ 여행 기록과 외부 실행은 직접 승인합니다.│
│ [Codex에서 로그인 시작]               │
│ 자세한 정보: 개인정보 및 외부 전송      │
└────────────────────────────────────┘
```

## Medium/expanded wireframes (600dp 이상)

```text
┌───────────────┬─────────────────────────────────────────┐
│ 나의 여행      │ 서울 여름 여행                            │
│ ━●━━○━━○      │ 8/18–8/21 · KST                           │
│ 서울 여름 여행 │ ━●━━━━●━━━━●━━━━○  오늘 2일째             │
│ 부산 주말 여행 │ [개요] [일정] [준비] [예약] [출처] [AI]    │
│               │ 다음 행동: 공항 이동 시간을 확인하세요      │
│ [+ 새 여행]   │ 09:00 호텔 체크인                          │
│               │ 11:00 성수동 카페                           │
└───────────────┴─────────────────────────────────────────┘
```

- 600dp 이상에서 only list-detail shell을 추가해도 route·tab·action label은 phone 화면과 동일하다.
- 840dp 이상에서 rail을 쓰더라도 primary action을 rail에 중복하지 않는다.
- 모든 compact screen은 vertical scroll 가능하며, 2.0x font scale에서 action은 마지막 문장 뒤에 자연스럽게 내려간다.

## Component contract

| Component | 입력 / 상태 | 의미·접근성 | testTag | 금지 |
|---|---|---|---|---|
| `TripPilotTopBar` | title, back, overflow, inset | heading 1, icon마다 contentDescription | `top_bar` | title truncate로 목적지 숨김 |
| `RouteRibbon` | days, selectedDay, statusNodes, onDaySelect | 요약 `여행 4일 중 2일째`; 노드마다 날짜·상태 label | `route_ribbon`, `route_day_<id>` | Canvas-only 의미 전달 |
| `StatusChip` | semantic status, label | label을 항상 노출; color는 보조 | `status_<status>` | filter/CTA 겸임 |
| `PrimaryAction` | label, enabled, loading, onClick | 48dp 이상, loading도 action명 유지 | `primary_action` | 한 screen에 2개 이상 |
| `EmptyState` | asset, title, body, action | illustration decorative, text가 의미 보유 | `empty_<kind>` | 사진 배경·자동 동작 |
| `ConfirmActionSheet` | target, consequence, confirm, cancel | 경계 밖 실행 대상·취소 결과를 낭독 | `confirm_<action>` | 로컬 저장에 과도하게 사용 |

## 기존 기능 parity → 신규 화면·테스트 추적표

`PAR-*`은 기존 기능 매트릭스의 적용/변형 적용 행에 대한 최소 검증 ID다. Phase 2 이후 `docs/test-matrix.md`에 실제 명령·결과를 누적한다.

| ID | 기존 기능 계약 | 신규 화면 / 책임 모델 | 구현 Phase | 최소 검증 |
|---|---|---|---:|---|
| PAR-01 | 시작 안내·로컬 우선 고지 | `OnboardingScreen`, `PrivacyScreen` | 0,2,5 | T-ONBOARD-LOCAL-01 |
| PAR-02 | 여행 보드 CRUD | `TripsScreen`, `TripEditor`, `Trip` | 2 | T-TRIP-CRUD-01 |
| PAR-03 | 여행 범위·기본 항목 | `TripScopeSelector`, `TravelScope`, template rule | 2 | T-SCOPE-TEMPLATE-01 |
| PAR-04 | Preparation/Packing 분리 | `ReadinessScreen`, item models | 2 | T-READINESS-SPLIT-01 |
| PAR-05 | 준비 완료 알림 | reminder preference/rule/delivery | 2,5 | T-REMINDER-D7-01 |
| PAR-06 | 일정 CRUD | `ItineraryScreen`, `ItineraryItem` | 2 | T-ITINERARY-BOUNDARY-01 |
| PAR-07 | 출처·재확인 | `SourcesScreen`, `SourceEvidence`, recheck | 2,5 | T-SOURCE-RECHECK-01 |
| PAR-08 | 지도·브라우저 열기 | `ConfirmActionSheet`, map/Custom Tab handoff | 5 | T-EXTERNAL-CONFIRM-01 |
| PAR-09 | 예약 관리 | `ReservationsScreen`, `Reservation` | 2 | T-RESERVATION-UNIQUE-01 |
| PAR-10 | 공유 예약 입력 | `ShareIntakeScreen`, `PendingReservationShare` | 2,5 | T-SHARE-TEXT-ONLY-01 |
| PAR-11 | 예약 AI 초안 | `ReservationDraftReview`, `ReservationDraft` | 3,4 | T-DRAFT-RESERVATION-01 |
| PAR-12 | 여행 AI 계획 | `PlanRequestScreen`, `DraftReviewScreen`, `TripPlanDraft` | 3,4 | T-DRAFT-PARTIAL-APPLY-01 |
| PAR-13 | 날씨 참고 | `WeatherAdvisoryPanel`, `WeatherAdvisoryDraft` | 4 | T-WEATHER-READONLY-01 |
| PAR-14 | Calendar 승인 ledger | `CalendarReviewScreen`, `CalendarAction` | 5 | T-CALENDAR-LEDGER-01 |
| PAR-15 | ICS 내보내기 | `IcsExportSheet`, selected itinerary | 5 | T-ICS-SELECTED-01 |
| PAR-16 | JSON 백업·복원 | `BackupScreen`, backup contract | 2,5 | T-BACKUP-COPY-01 |
| PAR-17 | 범용 Agent 바로가기 대체 | `AiDraftEntryScreen`, connection state | 3,4 | T-AI-NO-CHAT-01 |

의도적 미적용 항목(과거 chat 검색, Place table, generic pending action, browser automation)은 이 화면 맵과 테스트 목록에 넣지 않는다.

## 접근성 / responsive 검토 checklist

- `A11Y-01`: TalkBack 순서는 top bar → 여행 제목/기간 → RouteRibbon 요약 → 날짜 탭 → 다음 행동 → content → primary action이다.
- `A11Y-02`: progress는 `준비 6개 중 10개 완료`처럼 수치와 의미를 함께 말한다.
- `A11Y-03`: 모든 icon-only control은 명확한 content description을 가진다.
- `LAYOUT-01`: 360dp, 600dp, 840dp의 와이어프레임에서 정보와 action의 역할이 유지된다.
- `TEXT-01`: 1.0x, 1.3x, 2.0x에서 title/date/error/action이 clipping 또는 의미 없는 ellipsis 없이 보인다.
- `MOTION-01`: animator scale 0은 RouteRibbon과 content transition을 즉시 완료한다.
