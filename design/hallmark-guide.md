# TripPilot Hallmark 가이드 — 반(反)AI-slop 디자인 규율

이 문서는 [Hallmark 스킬](../.grok/skills/hallmark/SKILL.md)의 규율을 TripPilot Compose 관행으로 번역한 보완 가이드다. [`design-direction.md`](design-direction.md)과 [`tokens.json`](tokens.json)을 대체하지 않는다. 두 정본과 충돌하면 정본이 우선한다.

시각 참조: [`preview/hallmark-guide-preview.html`](preview/hallmark-guide-preview.html)

---

## 1. 여섯 가지 전역 규율

모든 화면·컴포넌트·PR에 적용된다.

### 1.1 잠긴 토큰 — 렌더 중 즉석 색상 금지

`TripPilotTheme`·컴포넌트 토큰 밖의 hex/sp/dp 리터럴을 금지한다. 새 값이 필요하면 먼저 `tokens.json`에 등록한 뒤 토큰으로 참조한다. `TripPilotTheme.kt`의 색 정의부가 유일한 예외다. 정적 검증: [게이트 3](#게이트-일람).

### 1.2 정직한 문구 — 발명된 콘텐츠 금지

사용자·로컬 데이터가 제공하지 않은 수치·통계·문장을 만들지 않는다.

- 진행률은 퍼센트 하나로 끝내지 않는다: `준비 80%` ❌ → `3개 그룹 중 1개 완료 · 다음: 여권 유효기간` ✅
- AI 초안 문구는 확정처럼 보이면 안 된다: `점심 식당 예약됨` ❌ → `점심 식당 (초안) · 검토 전` ✅
- "신뢰받는 N명", "평점 4.9" 같은 사회증명 문구 전면 금지

### 1.3 재그린 크롬 금지

가짜 브라우저 주소창, 가짜 지도 프레임, 모의 항공권·티켓 코드 블록, 가짜 터미널 창을 그려 넣지 않는다. 외부 링크·지도·Calendar는 대상과 결과를 말하는 `ApprovalSheet` 승인 후 실제 실행으로만 다룬다.

### 1.4 이탤릭 헤더 금지

제목·display 텍스트는 항상 roman(`FontStyle.Normal`). 강조는 weight(700)·semantic 색·밑줄로만 전달한다. 이탤릭은 본문 문단 내 강조에만 허용한다.

### 1.5 반응형 하한

모든 화면은 **320 / 360 / 414 / 600 / 840dp 폭 + fontScale 1.0/1.3/2.0**에서 검증을 통과해야 한다.

- 가로 스크롤 발생 금지
- 버튼·링크·CTA의 클릭 텍스트가 2줄로 늘어나지 않게 한다
- 날짜·시간·확인번호 utility 텍스트를 ellipsis로 의미를 숨기지 않는다 (정적 검증: [게이트 12](#게이트-일람))
- touch target 48dp 이상 (기존 규칙)

### 1.6 pre-emit 자기 비평

새 화면·컴포넌트 PR에는 6축 스탬프를 코멘트로 남긴다. 3 미만 축이 있으면 수정 후 재제출한다.

```
/* Hallmark · critique: P4 H5 E4 S4 R5 V4 */
```

| 축 | 질문 |
|---|---|
| Philosophy | 제품의 한 가지 일(지금 확인할 한 가지)에 봉사하는가 |
| Hierarchy | 한 화면에 primary가 하나이고 시각 순서가 분명한가 |
| Execution | 토큰·간격·타입 규칙이 예외 없이 지켜졌는가 |
| Specificity | 문구가 이 앱·이 데이터를 말하는가 (범용 템플릿 문장 아님) |
| Restraint | 모션·장식·색을 더 줄일 수 없는가 |
| Variety | 기존 화면의 구조 복붙이 아닌가 |

---

## 2. 구조 다양성 — 같은 템플릿의 색 바꾸기 금지

Hallmark의 핵심 기여다. 화면마다 **macrostructure**(문서 구조)를 지정하고, 새 화면은 기존 구조의 무의미한 반복이 되지 않게 한다.

### 화면별 macrostructure 지정표

| 화면 | macrostructure | 핵심 형태 |
|---|---|---|
| 여행 목록 | featured cover rail | 다음 여행 큰 표지 + 나머지 압축 행 |
| 브리핑 | 표지 + 단일 CTA | 큰 표지 1회, compact sticky header, primary 1개 |
| 일정 | time rail 문서 | 좌측 monospace 시각 레일 + 문서 카드 |
| 준비 | 체크리스트 그룹 | 그룹 헤더 + 상태 chip + 48dp 행 |
| 보관함 | 서류 폴더 | 문서 row + 출처 메타 |
| 도움 | 검토 대시보드 | AI 검토 대기·승인 대기 구분 |

등록처: [`audit/content-system.json`](audit/content-system.json). **새 화면 추가 절차 (gate-15, 정적 검증):**

1. `design/audit/content-system.json`의 해당 목적 영역 `pages`에 신규 페이지 항목을 추가한다 — `id`, `label`, `testTag`, `screenTag`, `primaryAction`, **`macrostructure`**, `states`
2. `macrostructure`는 기존 화면과 같은 값을 쓰지 않는다. 정보 구조가 다르면 문서 형태도 달라야 한다
3. `python3 scripts/verify_design_contract.py`로 등록 누락 여부를 확인한다 (모든 page에 macrostructure 없으면 FAIL)
4. 화면의 testTag는 `TripPilotApp.kt`에 실제로 존재해야 한다 (기존 content-system 계약이 함께 검증)

같은 surface가 3연속 이상 나열되지 않게 한다 (정적 검증: 게이트 4). 정보 성격이 다르면 `BriefingPanel`·`ChecklistGroup`·`DocumentRow`·`TimelineEntry` 역할 surface로 분리한다.

---

## 3. 컴포넌트 8-state 규율

모든 상호작용 컴포넌트는 **8 state**의 시각 정의를 토큰으로 갖는다:

```
default · pressed · focused · disabled · loading · error · success · selected
```

- `selected`는 선택 가능 요소(체크 항목, 초안 검토 행, 팩)에만 적용
- `loading`은 실제 비동기 경로(draft 생성·반영)에만 적용 — 정적 화면에 장식으로 넣지 않는다
- disabled는 `outline`/`surfaceVariant` 조합, error는 Signal Red, success는 Moss + `✓` 아이콘
- 신규 컴포넌트에는 8-state를 세로 나열한 `@Preview` 함수를 동반한다 (Hallmark preview wrapper의 Compose 번역)
- 색만으로 상태를 전달하지 않는다: label·icon을 함께 쓴다 (기존 규칙)

### 모션 예산

- 애니메이션은 `transform/opacity`에 대응하는 속성만 (offset·alpha·scale). 레이아웃 속성 애니메이션 금지
- 화면당 모션 3개 이하: stage transition 180ms, content transition 120ms, 여유분 1개
- 무한·반복 애니메이션 전면 금지 (정적 검증: [게이트 10](#게이트-일람))
- 성공 축하 애니메이션 금지 → **silent success**. 확인 다이얼로그보다 undo 스냅바
- reduce-motion·animator scale 0에서는 즉시 완료 (기존 규칙)

---

## 4. 게이트 일람 (slop 게이트 15항)

| # | 게이트 | 검증 수단 |
|---|---|---|
| 1 | 헤더에 이탤릭·장식 글자 | 육안 (design-review) |
| 2 | 발명된 통계·문구 | 육안 |
| 3 | 인라인 hex/sp/dp 리터럴 | **정적** — `verify_design_contract.py` |
| 4 | 동일 surface 3연속 리듬 | **정적** (Phase 3) |
| 5 | primary action 2개 이상 | **정적** (Phase 1) |
| 6 | 8-state 미정의 컴포넌트 | Preview 확인 |
| 7 | 색만으로 상태 전달 | 계측·육안 |
| 8 | 재그린 크롬(fake 브라우저·티켓) | 정적 스캔 대상 아님, 육안 |
| 9 | 320dp/2.0x에서 잘림 | **계측** — `DesignLayoutMatrixTest` |
| 10 | 무한/반복 애니메이션 | **정적** (Phase 1) |
| 11 | 축하 toast (silent success 위반) | 육안 |
| 12 | ellipsis로 날짜·코드 숨김 | **정적** (Phase 1) |
| 13 | 지도·사진·그라디언트 침투 | 정적 스캔 대상 아님, 육안 |
| 14 | AI 초안이 확정처럼 보임 | 육안 |
| 15 | 새 화면의 content-system 등록 누락 | **정적** — `content-system.json` 대조 |

정적 게이트 실패 메시지는 `hallmark-guide.md §gate-N` 형식으로 이 문서를 인용한다.

---

## 5. 검수 워크플로

1. 신규 화면/컴포넌트 작성 시 §1–§3 규율 적용, §4 게이트 자가 점검
2. `python3 scripts/verify_design_contract.py` — 정적 게이트 통과
3. `design/audit/design-review.json`의 해당 화면 `slopGates` 항목 갱신 (`pass`/`fail`)
4. `python3 scripts/sync_design_run_state.py` — RUN_STATE 산출
5. 시각 항목은 golden/journey 캡처 및 사람 승인으로 (`run_android_design_qa.sh`)
