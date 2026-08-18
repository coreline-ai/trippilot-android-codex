package io.trippilot.app.core.model

/**
 * Static, general-purpose problem-response guidance. It is deliberately NOT an
 * emergency service: no country names, phone numbers, agencies, or claims about
 * duties, treatments, or compensation. Every category follows the same skeleton
 * (secure safety → check user-stored contacts/official sources → preserve
 * records → follow up), and all mutable facts are left for the user to verify
 * from their own saved sources.
 */
object ProblemResponseCatalog {
    const val NOT_EMERGENCY_NOTICE =
        "TripPilot은 긴급 서비스가 아니며 최신 기관 정보가 아닙니다. 긴급 상황에서는 현장의 안전 지침과 공식 기관 안내를 우선하세요. 변동 사실은 사용자가 저장한 공식 출처에서 직접 확인하세요."

    data class Category(
        val id: SafetyCategory,
        val steps: List<String>,
    )

    private val categories = listOf(
        Category(
            SafetyCategory.DOCUMENTS,
            listOf(
                "즉시 안전 확보: 남은 이동·체크인에 영향이 있는지 먼저 확인합니다.",
                "사본·사진 등 사용자가 저장한 대체 확인 수단을 확인합니다.",
                "분실 신고·재발급 필요 여부는 공식 출처에서 직접 확인합니다.",
                "남은 일정의 서류 대체 수단과 확인 결과를 기록으로 남깁니다.",
            ),
        ),
        Category(
            SafetyCategory.HEALTH,
            listOf(
                "즉시 안전 확보: 응급 필요성 판단은 본인과 현장 지침이 우선합니다.",
                "사용자가 저장한 보험·연락 정보를 확인합니다.",
                "방문 기록·처방·지출 등 필요한 기록을 보존합니다.",
                "사후 보장·청구 여부는 본인 증권과 공식 출처에서 직접 확인합니다.",
            ),
        ),
        Category(
            SafetyCategory.THEFT_LOSS,
            listOf(
                "즉시 안전 확보: 물품보다 사람의 안전이 우선입니다.",
                "카드·서류 등 결제·신원 수단의 차단은 해당 기관 공식 채널에서 진행합니다.",
                "발생 시각·장소·물품 목록을 기록으로 보존합니다.",
                "신고·보상 필요 여부는 공식 출처에서 직접 확인합니다.",
            ),
        ),
        Category(
            SafetyCategory.PAYMENT,
            listOf(
                "결제 실패 원인을 구분합니다: 한도·이용 설정·일시 오류.",
                "사용자가 저장한 카드사 공식 채널로 확인합니다.",
                "예비 결제 수단 사용 내역을 기록으로 남깁니다.",
                "귀국 후 정정·청구 항목을 정리합니다.",
            ),
        ),
        Category(
            SafetyCategory.TRANSPORT_DELAY,
            listOf(
                "예약 상태를 확인번호와 직접 대조합니다.",
                "운영사 공식 채널에서 변경·대안을 확인합니다.",
                "대안 일정과 추가 비용을 기록으로 보존합니다.",
                "관련 출처를 저장해 두고 다시 확인합니다.",
            ),
        ),
        Category(
            SafetyCategory.CONNECTIVITY,
            listOf(
                "대체 통신 수단(로밍·Wi‑Fi·다른 기기 공유)을 확인합니다.",
                "기기 분실 시 원격 조치는 해당 공식 서비스에서 진행합니다.",
                "필요한 연락을 다른 수단으로 복구합니다.",
                "통신사 연락은 공식 채널에서 직접 확인합니다.",
            ),
        ),
        Category(
            SafetyCategory.WEATHER_FIELD,
            listOf(
                "안전 우선: 현장 지침과 공식 안내를 따릅니다.",
                "일정 조정 내용을 사용자가 직접 기록합니다.",
                "변경·취소 수수료 등 조건은 운영사 공식 채널에서 확인합니다.",
                "날씨 사실은 사용자가 연결한 출처에서 다시 확인합니다.",
            ),
        ),
    )

    fun all(): List<Category> = categories

    fun find(id: SafetyCategory): Category = categories.first { it.id == id }
}
