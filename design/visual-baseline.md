# Trip Briefing visual baseline

작성 일시: 2026-08-16 21:25:37 KST

## 현재 기준선

- Design journey capture: `app/build/reports/qa/design-journey/contact-sheet-after.png`
- Versioned screenshot goldens:
  - `app/src/androidTest/assets/screenshot-goldens/01-trip-list-empty.png`
  - `app/src/androidTest/assets/screenshot-goldens/02-trip-summary.png`
  - `app/src/androidTest/assets/screenshot-goldens/03-draft-review.png`
  - `app/src/androidTest/assets/screenshot-goldens/04-external-confirmation.png`
- Capture command: `scripts/run_design_journey_capture.sh`
- Golden command: `scripts/run_phase5_screenshot_golden.sh`

## 변경 후 review sequence

1. 빈 여행 목록
2. 여행 한 건이 있는 목록
3. 여행 브리핑
4. 일자별 일정
5. 그룹형 준비물
6. 예약 문서와 출처
7. AI 초안 검토
8. 외부 실행 승인 sheet

## 합격 기준

- 모든 화면이 Trip Briefing의 compact header, purpose navigation, 하나의 primary action 순서를 지킨다.
- `JourneyStageStrip`은 실제 날짜/상태만 표시하며, 비어 있는 일정에 완료 상태를 꾸며 내지 않는다.
- 준비물은 그룹명·완료 수·항목별 확인 이유가 함께 보인다.
- 일정/예약/출처/AI/외부 실행의 local-only 및 사용자 승인 경계가 visual copy로도 분명하다.
- light/dark, 360dp/600dp/840dp, 2.0x font scale, TalkBack에서 정보가 숨겨지지 않는다.
