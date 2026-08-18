# TripPilot 디자인 방향 — Trip Briefing

## 제품의 한 가지 일

TripPilot은 출발 전에는 **가장 먼저 확인할 준비 항목**, 여행 중에는 **지금 이어질 일정**, 여행 뒤에는 **남아 있는 정리 항목**을 빠르게 판단하게 하는 개인용 여행 브리핑 앱이다. 사진 앨범, 관광 검색기, 자동 실행 Agent가 아니다.

## 시각 논지

**Trip Briefing**은 출발 안내문, 일정표, 예약 서류, 체크리스트를 한 권의 여행 브리프로 읽게 한다. 여행의 상태는 장식이 아니라 사용자가 확인·완료·보류한 로컬 데이터에서 나온다.

- **확정 정보:** Pilot Navy는 사용자 확인이 끝난 여행 기간, 예약, 문서의 기준선이다.
- **현재 단계:** Wayfinding Cyan은 보고 있는 여행 일자와 선택된 단계다.
- **다음 행동:** Boarding Orange는 한 화면에서 바로 할 수 있는 한 가지 행동이다.
- **아직 기록이 아닌 제안:** Stamp Violet은 Codex 초안·검토 대기만 나타낸다.
- **완료:** Moss Green은 체크 완료·확인됨만 나타낸다.

Trip Briefing의 고유 장치는 `JourneyStageStrip`이다. 이 strip은 `출발 전 → DAY 1..N → 귀국 후`를 표시하며, 실제 여행 날짜·선택 일자·일정 수·준비/검토 상태가 있을 때만 그 상태를 나타낸다. 데이터가 없으면 완료처럼 보이는 선이나 점을 그리지 않는다.

## 정보 구조

```text
여행 목록
  └─ 다음 여행 한 건과 다음 행동

여행 상세
  ├─ 여정
  │   ├─ 브리핑: 다음 행동 · 단계 · 준비 · 다음 예약
  │   └─ 일정: 날짜별 time rail · 장소 · 메모 · 출처
  ├─ 준비: 그룹형 준비할 일 · 챙길 물건 · 선택 팩
  ├─ 보관함: 예약 문서 · 출처 · 공유 예약 텍스트
  └─ 도움: AI 초안 검토 · 외부 실행 승인 · 문제 대응(안전 메모)
```

`여정/준비/보관함/도움`은 네 개의 목적 영역만 유지한다. 문제 대응(Safety Hub)은 도움 영역 내부의 별도 화면이며 세 번째 하위 tab이 아니다. 귀국 후 항목은 준비 화면의 48시간/1주/나중 window 그룹으로만 존재하고 자동 추가되지 않는다. `여정/보관함/도움` 내부의 secondary navigation은 두 개 이하로 제한한다. `JourneyStageStrip`은 navigation을 대체하지 않으며, 현재 보고 있는 일자를 표시하거나 일정 화면으로 이동시키는 보조 구조다.

## 레이아웃과 표면

1. **브리핑에서만 큰 표지를 쓴다.** 제목·기간·여행 범위·다음 행동은 브리핑 첫 화면에서만 충분한 높이로 읽는다. 다른 탭은 compact sticky header를 사용한다.
2. **역할 있는 surface만 쓴다.** `BriefingPanel`, `ChecklistGroup`, `DocumentRow`, `TimelineEntry` 외에 같은 정보용 card를 연속으로 쌓지 않는다.
3. **다음 행동은 하나다.** 한 화면의 primary action은 하나이며 Orange는 그 행동에만 쓴다. 수정·삭제·건너뛰기는 text action 또는 overflow다.
4. **진행률에는 이유가 있다.** 단순 퍼센트 대신 미완료 그룹·다음 항목·완료 수를 함께 표시한다.
5. **여백은 읽는 순서다.** 지도, glassmorphism, 임의 gradient, 도시 사진은 쓰지 않는다. 목록 travel cover의 decorative artwork은 텍스트를 보조할 뿐 상태를 표현하지 않는다.
6. **문구는 사용자가 하는 일을 말한다.** `준비 항목 추가`, `선택한 초안 반영`, `파일 위치 선택`처럼 결과가 분명한 동사를 쓴다.

## 준비물 정보 원칙

참조 플래너의 장점인 “항목 + 왜 확인하는지” 구조를 적용하되, TripPilot은 지역·시간 의존 사실을 자동으로 단정하지 않는다.

- 기본 템플릿 그룹은 서류·입국, 결제·현금, 통신·전자기기, 의류·현장 용품, 건강·위생이다.
- `INTERNATIONAL` 기본 팩은 일반형 필요 항목을 로컬로 추가한다. 비자 기간, 전압, 환율, 날씨, 현지 병원·전화번호는 `공식 출처에서 직접 확인`으로 남긴다.
- 기본 항목은 stable `templateId`로 catalog의 그룹·짧은 확인 이유에 연결한다. 수동/AI 항목은 원래 사용자가 입력한 텍스트를 우선한다.
- 선택 팩과 귀국 후 팩은 사용자가 명시적으로 추가한다. 불필요한 항목이 기본 진행률을 낮추면 안 된다.

## AI 및 외부 경계

- AI 초안은 보라색 review state로만 표현하고, 여행 기록/Calendar/링크 실행 권한이 있는 것처럼 보이면 안 된다.
- AI가 제안한 일정·예약·준비물은 항목별 검토 후 선택한 것만 한 번의 로컬 transaction으로 반영한다.
- 지도·브라우저·Calendar·파일은 target과 결과를 말하는 `ApprovalSheet` 후에만 실행한다.
- 링크와 출처는 보관·재확인할 수 있으나, 화면을 표시하는 것만으로 열거나 검사하지 않는다.

## 타입·형태·자산

- 한국어 제목·본문은 시스템 sans-serif, 날짜·시간·확인번호는 monospace utility style을 사용한다.
- surface 20dp, action/input 14dp, chip pill, travel cover 28dp를 유지한다.
- 아이콘은 Material Symbols Rounded(500/outline/24)만 쓴다.
- 외부 web font, 원격 이미지, 도시 사진, 지도 타일은 추가하지 않는다. 새 시각 자산은 자체 Compose vector/VectorDrawable만 허용한다.

## 이전 Field Route Journal에서의 변경

| 기존 부분 적용 | Trip Briefing 결정 |
|---|---|
| 색상 semantic role | 유지. 상태 label/icon과 함께만 사용. |
| 목록의 field-route artwork | decorative travel cover로만 재검토. 화면 전체의 테마로 확장하지 않음. |
| `RouteRibbon`의 고정 progress | 실제 데이터 입력의 `JourneyStageStrip`으로 교체. |
| 상세 탭의 큰 반복 header | 브리핑 전용 cover + 나머지 compact sticky header로 분리. |
| 범용 Material card/button 반복 | 역할 있는 panel/row/group surface로 교체. |
| AlertDialog 입력 | keyboard-safe modal bottom sheet로 전환. |

## 접근성·motion 계약

> 반(反)AI-slop 세부 규율과 slop 게이트 15항은 [Hallmark 가이드](hallmark-guide.md)를 따른다.

- `JourneyStageStrip`은 전체 진행 요약과 각 stage의 날짜·상태를 semantics로 제공한다. Canvas만으로 의미를 전달하지 않는다.
- TalkBack 순서는 compact header → stage summary → purpose navigation → subpage → content → primary action이다.
- 모든 touch control은 48dp 이상이며, 색만으로 완료/오류/AI 상태를 전달하지 않는다.
- 1.0x/1.3x/2.0x font scale에서 action, 날짜, 확인번호를 ellipsis로 숨기지 않는다.
- animator scale 0 또는 reduce motion 상태에서는 stage/content transition을 즉시 완료한다.
