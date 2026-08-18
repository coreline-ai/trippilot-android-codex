# 준비물 gap·문제 대응·귀국 후 요구사항 확정 (2026-08-17)

이 문서는 `dev-plan/implement_20260817_230622.md` Phase 1의 산출물이다. 참조 플래너 분석(`reference-danang-planner-analysis.md`)의 항목을 현행 `ReadinessTemplateCatalog`(29개 template ID)와 전수 대조하고, 문제 대응 7개 카테고리와 귀국 후 3개 시점의 구현 경계를 고정한다.

## 1. 준비물 gap matrix

참조 29개 항목 → 현행 카탈로그 대조. 판정: **적용**(이미 존재) / **보강**(신규 template ID 추가) / **선택**(optional 팩으로 추가) / **제외**(변동 정보·앱 경계 밖).

| 참조 항목 | 판정 | 근거 / 대상 ID |
|---|---|---|
| 여권 상태 확인 | 적용 | `PASSPORT_VALIDITY_CHECK` |
| 입국 조건/비자 | 적용 | `ENTRY_REQUIREMENTS_OFFICIAL_CHECK` |
| 왕복 이동 증빙 | 적용 | `PLAN_AND_RESERVATION_CHECK`가 예약 대조로 커버. 별도 항목 불필요 |
| 숙소 확인서 | 적용 | `TRANSPORT_AND_LODGING_CHECK` |
| 여행 보험 | 적용 | `TRAVEL_INSURANCE_CHECK` |
| 서류 사본 | 선택(적용) | `TRAVEL_DOCUMENT_COPIES` |
| 비상연락처 사본 | **보강** | 신규 `EMERGENCY_CONTACT_COPY` — "여행 중 확인할 비상 연락처를 오프라인으로도 볼 수 있는 곳에 적어 둡니다." 일반 문구만, 번호 불포함 |
| 결제 카드 2장 | **보강(선택)** | 신규 `BACKUP_PAYMENT_METHOD`(optional) — "결제 수단에 문제가 생겼을 때를 대비해 다른 수단을 준비합니다." 매수 고정 없음 |
| 해외 사용 설정 | 적용 | `CARD_PAYMENT_PREP` |
| 현지 결제 수단/소액 현금 | 적용 | `CASH_PLAN`(선택) |
| 카드 분실 연락처 | 적용(확장 해석) | `OFFLINE_ACCESS_CHECK` + Safety Hub 사용자 연락 메모로 커버. 카드사 번호는 하드코딩 금지 → **제외(정적 카탈로그)** |
| eSIM/유심/로밍 | 적용 | `CONNECTIVITY_PLAN` |
| 충전기 | 적용 | `DEVICE_CHARGER` |
| 보조배터리 | 적용 | `POWER_BANK`(선택) |
| 어댑터 | 적용 | `ADAPTER_NEED_CHECK` |
| 오프라인 지도 | **보강(선택)** | 신규 `OFFLINE_MAPS_READY`(optional) — "이동에 필요한 지도를 오프라인에서도 열 수 있게 미리 준비합니다." 지도 앱 자동 실행 없음 |
| 교통 앱 준비 | **보강(선택)** | 신규 `LOCAL_TRANSIT_APP_READY`(optional) — "이동에 쓸 앱·티켓을 미리 확인합니다." |
| 방수/보관 수단 | **보강(선택)** | 신규 `WEATHER_PROOF_STORAGE`(optional) — "물·습기에 민감한 물품의 보관 수단을 확인합니다." |
| 기후에 맞는 의류 | 적용 | `CLOTHING_PLAN`(선택) |
| 얇은 겉옷 | 적용 | `WEATHER_APPROPRIATE_LAYER`(선택) |
| 걷기 편한 신발 | 적용 | `WALKING_SHOES`(선택) |
| 우천/햇빛 대비 | **보강(선택)** | 신규 `RAIN_SUN_PROTECTION`(optional) — 우산·모자 등 개인 선택 문구 |
| 보조 가방 | 적용 | `DAY_BAG`(선택) |
| 평소 복용약 | 적용 | `PERSONAL_MEDICINE` |
| 기본 상비품 | 적용 | `HYGIENE_KIT`(선택) + `MEDICINE_LIST`(선택) |
| 자외선 대비 | 우천/햇빛 항목에 통합 | `RAIN_SUN_PROTECTION`로 통합 판정 |
| 벌레 대비 | **보강(선택)** | 신규 `INSECT_PROTECTION`(optional) — "필요 시 개인 기준으로 대비 물품을 준비합니다." |
| 위생 대비 | 적용 | `HYGIENE_KIT` |
| 물병 | 적용 | `REUSABLE_WATER_BOTTLE`(선택) |

**신규 template ID 7개**: `EMERGENCY_CONTACT_COPY`(필수, 전 scope), `BACKUP_PAYMENT_METHOD`·`OFFLINE_MAPS_READY`·`LOCAL_TRANSIT_APP_READY`·`WEATHER_PROOF_STORAGE`·`RAIN_SUN_PROTECTION`·`INSECT_PROTECTION`(선택). 전부 국가·가격·날씨 사실 없는 일반 문구.

**제외**: 카드사·기관 전화번호, 국가별 반입 기준, 환율 금액, 날씨 예보 자체.

## 2. 문제 대응 7개 카테고리 (ProblemResponseCatalog)

모든 카테고리 공통 골격: **즉시 안전 확보 → 사용자가 저장한 연락처/공식 출처 확인 → 필요한 기록 보존 → 후속 확인**.

| # | 카테고리 | 일반 단계 요약 | 저장 가능 정보 |
|---|---|---|---|
| 1 | 여권·신분·여행 서류 | 분실 시 이동·체크인 영향 확인 → 사본/사진으로 신원 확인 시도 → 분실 신고 필요 여부는 공식 출처 확인 → 남은 일정의 서류 대체 수단 기록 | 사본 위치 메모, 신고 기록, 관련 출처 |
| 2 | 질병·부상 | 즉시 안전 확보와 응급 필요성 판단은 본인/현장이 결정 → 사용자가 저장한 보험·연락 정보 확인 → 방문 기록·처방·지출 보존 → 사후 보장 여부는 증권/공식 출처 확인 | 보험 연락 메모, 진료 기록 메모 |
| 3 | 도난·분실 | 안전 확보 우선 → 카드·서류 등 결제 수단 차단은 해당 기관 공식 채널에서 → 발생 시각·장소·물품 기록 보존 → 신고·보상 필요 여부는 공식 출처 확인 | 분실 목록 메모, 신고 기록 |
| 4 | 카드·현금·결제 | 결제 실패 원인 구분(한도·설정·오류) → 사용자가 저장한 카드사 공식 채널 확인 → 예비 수단 사용 기록 → 귀국 후 정정 항목 기록 | 카드 상태 메모, 예비 수단 메모 |
| 5 | 교통·예약·항공 지연 | 예약 상태를 확인번호로 대조 → 운영사 공식 채널 확인 → 대안 일정·비용 기록 → 관련 출처 보존 | 지연/취소 기록, 변경 예약 |
| 6 | 휴대폰·통신 | 통신 수단별 대체(로밍·Wi‑Fi·공유) 확인 → 기기 분실 시 원격 조치는 공식 서비스에서 → 필요 연락을 다른 수단으로 복구 → 통신사 연락은 공식 채널 | 통신 상태 메모 |
| 7 | 기상·현장 상황 | 안전 우선 판단은 본인/현장 지침 우선 → 일정 조정은 사용자가 기록 → 변경·취소 수수료 등은 운영사 공식 채널 → 날씨 사실은 사용자가 연결한 출처에서 | 일정 변경 기록 |

**금지 문구**(정적 catalog 전체): 국가명 특정, 실제 전화번호, 기관명 단정, 치료·신고 의무·보상 가능 단정, "즉시 안전합니다"류 보장 표현. 표준 안내문: "TripPilot은 긴급 서비스가 아니며 최신 기관 정보가 아닙니다. 변동 사실은 사용자가 저장한 공식 출처에서 직접 확인하세요."

## 3. 귀국 후 시점 (PostTripWindow)

| Window | 항목 | template |
|---|---|---|
| `WITHIN_48_HOURS` | 귀가 확인(지갑·서류·장비), 분실·파손 확인, 대여품/임시 서비스 반납, 즉시 필요한 영수증·기록 확보 | 신규 4개 |
| `WITHIN_ONE_WEEK` | 비용·영수증 정리, 보상/보험 신청 필요 여부 공식 확인, 사진·데이터 백업, 장비 정리 | 신규 4개 |
| `LATER` | 서류 보관, 불필요한 공유·예약 정보 정리, 여행 메모·다음 여행 참고 정리 | 신규 3개 |

기존 `POST_TRIP_RECEIPTS`→`WITHIN_ONE_WEEK` 비용·영수증 정리와 통합 정리, `POST_TRIP_RETURN_CHECK`→`WITHIN_48_HOURS` 반납 확인으로 재매핑한다(기존 행의 templateId는 유지하고 표시 시점만 재계산). 자동 추가 없음 — `귀국 후 팩 추가` 버튼으로만 window별 추가. 미선택 window는 완료율·stage 완료에 포함하지 않는다.

## 4. Safety Hub 화면 계약 (screen-map 반영)

- 진입: 도움 영역 상단 고정 panel `safety_hub_entry`(BriefingPanel `kind="status"`). 선택 시 Help 내부 `showSafetyHub=true`로 전환, Back으로 복귀. 하위 탭은 기존 `AI 초안/외부 실행` 2개 유지
- 표시 순서: ① 비상 서비스 비대체 안내 ② 7개 카테고리(`safety_category_<id>`) ③ 사용자 연락 정보·개인 메모(`safety_memo_list`) ④ 연결 출처 ⑤ 추가/수정/삭제·승인 외부 열기
- 상태: empty(안내+추가 CTA) / populated / edit(TripFormSheet 재사용) / source approval(기존 ApprovalSheet)
- testTag: `safety_hub_screen`, `add_safety_memo`, `safety_memo_<id>`, `delete_safety_memo_<id>` / 연락값은 복사 또는 ApprovalSheet 후 handoff만

## 5. JourneyStage 귀국 후 규칙

- 귀국 후 stage(`post`)의 detail: 선택된 post-trip 항목이 없으면 "선택 팩"(불완전 표시 금지), 있으면 "정리 N개 중 M완료" 식의 이유 동반 표시
- 상태: post-trip 항목 없음 → `UPCOMING`, 있고 미완료 → `ACTION_REQUIRED`, 전부 DONE/SKIPPED → `COMPLETE`
- 종료일 이후 브리핑 CTA: 귀국 후 항목이 있으면 "귀국 후 정리 확인", 없으면 "귀국 후 팩 추가"

## 6. 개인정보·외부 실행 경계

- 안전 메모·연락값은 Room 로컬만. debug log·analytics·AI prompt·알림 본문 미포함
- 연락값 tap = 복사만. 전화·브라우저·지도는 기존 ApprovalSheet 승인 후
- backup v3에 safety memo 포함하되 기존 2MB/항목 수 제한 유지, 연락값 길이 제한(200자)·메모(2000자)·URL은 기존 validator 재사용

## 7. 컴포넌트 계약 추가

- Safety Hub 진입 panel: TalkBack 순서 도움 header → 진입 panel → 하위 탭
- 카테고리 확장 행: 48dp 이상, `semantics contentDescription = "카테고리명, 단계 요약"`
- 귀국 후 window 그룹 헤더: `semantics` "귀국 후 48시간 이내, N개 중 M완료"
- 모든 신규 화면 요소는 `hallmark-guide.md` 게이트 통과 (macrostructure 등록 포함)
