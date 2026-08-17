package io.trippilot.app.core.model

/** Stable IDs are persisted for built-in checklists; labels and hints remain local presentation data. */
enum class ChecklistTemplateId {
    PLAN_AND_RESERVATION_CHECK,
    PAYMENT_METHOD_CHECK,
    TRANSPORT_AND_LODGING_CHECK,
    IDENTITY_DOCUMENT,
    PASSPORT_VALIDITY_CHECK,
    ENTRY_REQUIREMENTS_OFFICIAL_CHECK,
    TRAVEL_DOCUMENT_COPIES,
    TRAVEL_PLAN_SHARING,
    TRAVEL_INSURANCE_CHECK,
    CARD_PAYMENT_PREP,
    CASH_PLAN,
    PAYMENT_LIMIT_CHECK,
    CONNECTIVITY_PLAN,
    DEVICE_CHARGER,
    POWER_BANK,
    ADAPTER_NEED_CHECK,
    OFFLINE_ACCESS_CHECK,
    CLOTHING_PLAN,
    WEATHER_APPROPRIATE_LAYER,
    WALKING_SHOES,
    DAY_BAG,
    REUSABLE_WATER_BOTTLE,
    PERSONAL_MEDICINE,
    MEDICINE_LIST,
    HYGIENE_KIT,
    HEALTH_COVERAGE_CHECK,
    SLEEP_COMFORT_ITEM,
    POST_TRIP_RECEIPTS,
    POST_TRIP_RETURN_CHECK,
}

enum class ChecklistGroup(val label: String) {
    DOCUMENTS_ENTRY("서류 · 입국"),
    MONEY_PAYMENT("돈 · 결제"),
    CONNECTIVITY_ELECTRONICS("통신 · 전자기기"),
    CLOTHING_FIELD("의류 · 현장 용품"),
    HEALTH_HYGIENE("건강 · 위생"),
    DIRECT_ADD("직접 추가"),
    POST_TRIP("귀국 후"),
}

data class ReadinessTemplate(
    val id: ChecklistTemplateId,
    val type: ChecklistType,
    val group: ChecklistGroup,
    val title: String,
    val hint: String,
    val scopes: Set<TravelScope>,
    val optional: Boolean = false,
)

data class ReadinessDisplayMetadata(
    val group: ChecklistGroup,
    val hint: String,
    val optional: Boolean,
)

/**
 * A static, general-purpose local catalog. It deliberately avoids destination-specific law,
 * weather, price, medical, telephone and visa facts; users verify those from their own source.
 */
object ReadinessTemplateCatalog {
    private val allScopes = setOf(TravelScope.AUTO, TravelScope.DOMESTIC, TravelScope.INTERNATIONAL)
    private val domesticAndInternational = setOf(TravelScope.DOMESTIC, TravelScope.INTERNATIONAL)

    private val all = listOf(
        ReadinessTemplate(ChecklistTemplateId.PLAN_AND_RESERVATION_CHECK, ChecklistType.PREPARATION, ChecklistGroup.DOCUMENTS_ENTRY, "일정과 예약 확인", "출발 전 시간·확인번호를 직접 대조합니다.", allScopes),
        ReadinessTemplate(ChecklistTemplateId.PAYMENT_METHOD_CHECK, ChecklistType.PREPARATION, ChecklistGroup.MONEY_PAYMENT, "결제 수단 확인", "사용할 결제 수단과 개인 한도를 확인합니다.", allScopes),
        ReadinessTemplate(ChecklistTemplateId.DEVICE_CHARGER, ChecklistType.PACKING, ChecklistGroup.CONNECTIVITY_ELECTRONICS, "충전기", "필요한 기기 수에 맞춰 챙깁니다.", allScopes),
        ReadinessTemplate(ChecklistTemplateId.PERSONAL_MEDICINE, ChecklistType.PACKING, ChecklistGroup.HEALTH_HYGIENE, "개인 의약품", "평소 복용·휴대하는 물품을 개인 기준으로 챙깁니다.", allScopes),

        ReadinessTemplate(ChecklistTemplateId.TRANSPORT_AND_LODGING_CHECK, ChecklistType.PREPARATION, ChecklistGroup.DOCUMENTS_ENTRY, "교통·숙소 예약 확인", "교통편과 숙소의 일정·확인번호를 다시 봅니다.", domesticAndInternational),
        ReadinessTemplate(ChecklistTemplateId.IDENTITY_DOCUMENT, ChecklistType.PREPARATION, ChecklistGroup.DOCUMENTS_ENTRY, "신분증", "이동과 체크인에 필요한 본인 확인 수단을 챙깁니다.", setOf(TravelScope.DOMESTIC)),

        ReadinessTemplate(ChecklistTemplateId.PASSPORT_VALIDITY_CHECK, ChecklistType.PREPARATION, ChecklistGroup.DOCUMENTS_ENTRY, "여권 유효기간 확인", "여권의 현재 상태는 공식 안내 기준으로 직접 확인합니다.", setOf(TravelScope.INTERNATIONAL)),
        ReadinessTemplate(ChecklistTemplateId.ENTRY_REQUIREMENTS_OFFICIAL_CHECK, ChecklistType.PREPARATION, ChecklistGroup.DOCUMENTS_ENTRY, "입국 요건 공식 출처 확인", "필요 서류와 입국 요건은 방문 국가의 공식 출처에서 직접 확인합니다.", setOf(TravelScope.INTERNATIONAL)),
        ReadinessTemplate(ChecklistTemplateId.TRAVEL_INSURANCE_CHECK, ChecklistType.PREPARATION, ChecklistGroup.HEALTH_HYGIENE, "여행자 보험 확인", "가입 여부와 보장 범위는 본인 증권으로 직접 확인합니다.", setOf(TravelScope.INTERNATIONAL)),
        ReadinessTemplate(ChecklistTemplateId.ADAPTER_NEED_CHECK, ChecklistType.PACKING, ChecklistGroup.CONNECTIVITY_ELECTRONICS, "어댑터 필요 여부 확인", "사용할 충전 환경은 숙소·기기 안내에서 직접 확인합니다.", setOf(TravelScope.INTERNATIONAL)),

        ReadinessTemplate(ChecklistTemplateId.TRAVEL_DOCUMENT_COPIES, ChecklistType.PREPARATION, ChecklistGroup.DOCUMENTS_ENTRY, "여행 서류 사본 준비", "분실 대비가 필요한 서류만 개인 판단으로 준비합니다.", setOf(TravelScope.INTERNATIONAL), optional = true),
        ReadinessTemplate(ChecklistTemplateId.TRAVEL_PLAN_SHARING, ChecklistType.PREPARATION, ChecklistGroup.DOCUMENTS_ENTRY, "여행 일정 공유 여부 확인", "필요한 경우에만 믿을 수 있는 사람과 직접 공유합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.CARD_PAYMENT_PREP, ChecklistType.PREPARATION, ChecklistGroup.MONEY_PAYMENT, "결제 카드 사용 설정 확인", "해외·온라인 사용 설정은 카드사 공식 채널에서 직접 확인합니다.", setOf(TravelScope.INTERNATIONAL), optional = true),
        ReadinessTemplate(ChecklistTemplateId.CASH_PLAN, ChecklistType.PREPARATION, ChecklistGroup.MONEY_PAYMENT, "현금 사용 계획", "필요한 경우에만 개인 예산과 보관 방법을 정합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.PAYMENT_LIMIT_CHECK, ChecklistType.PREPARATION, ChecklistGroup.MONEY_PAYMENT, "결제 한도 확인", "결제 수단별 한도는 본인 계정에서 직접 확인합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.CONNECTIVITY_PLAN, ChecklistType.PREPARATION, ChecklistGroup.CONNECTIVITY_ELECTRONICS, "통신 사용 계획", "로밍·유심·Wi‑Fi 등 사용할 방식을 직접 선택합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.POWER_BANK, ChecklistType.PACKING, ChecklistGroup.CONNECTIVITY_ELECTRONICS, "보조 배터리", "이동 중 충전이 필요할 때만 챙깁니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.OFFLINE_ACCESS_CHECK, ChecklistType.PREPARATION, ChecklistGroup.CONNECTIVITY_ELECTRONICS, "오프라인 접근 수단 확인", "필요한 예약·연락 정보를 기기에서 열 수 있는지 직접 확인합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.CLOTHING_PLAN, ChecklistType.PREPARATION, ChecklistGroup.CLOTHING_FIELD, "의류 계획", "방문 일정과 개인 활동에 맞는 옷을 직접 정합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.WEATHER_APPROPRIATE_LAYER, ChecklistType.PACKING, ChecklistGroup.CLOTHING_FIELD, "겉옷·보온 용품", "필요 여부는 출발 전 직접 확인한 예보와 활동 기준으로 정합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.WALKING_SHOES, ChecklistType.PACKING, ChecklistGroup.CLOTHING_FIELD, "걷기 편한 신발", "이동량과 개인 발 상태에 맞게 선택합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.DAY_BAG, ChecklistType.PACKING, ChecklistGroup.CLOTHING_FIELD, "작은 가방", "낮 동안 필요한 물품을 따로 보관할 때 챙깁니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.REUSABLE_WATER_BOTTLE, ChecklistType.PACKING, ChecklistGroup.CLOTHING_FIELD, "물병", "개인 사용 필요에 따라 챙깁니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.MEDICINE_LIST, ChecklistType.PREPARATION, ChecklistGroup.HEALTH_HYGIENE, "복용 물품 목록 확인", "개인 복용 물품과 휴대 필요량을 직접 확인합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.HYGIENE_KIT, ChecklistType.PACKING, ChecklistGroup.HEALTH_HYGIENE, "위생 용품", "개인에게 필요한 위생 물품만 챙깁니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.HEALTH_COVERAGE_CHECK, ChecklistType.PREPARATION, ChecklistGroup.HEALTH_HYGIENE, "건강 관련 보장 확인", "필요 시 본인 보험 또는 서비스 약관을 직접 확인합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.SLEEP_COMFORT_ITEM, ChecklistType.PACKING, ChecklistGroup.HEALTH_HYGIENE, "개인 수면 용품", "장거리 이동이나 숙박에서 필요할 때만 추가합니다.", allScopes, optional = true),

        ReadinessTemplate(ChecklistTemplateId.POST_TRIP_RECEIPTS, ChecklistType.PREPARATION, ChecklistGroup.POST_TRIP, "귀국 후 영수증 정리", "필요한 기록만 귀국 후 직접 정리합니다.", allScopes, optional = true),
        ReadinessTemplate(ChecklistTemplateId.POST_TRIP_RETURN_CHECK, ChecklistType.PREPARATION, ChecklistGroup.POST_TRIP, "귀국 후 반납·정리 확인", "대여품·개인 물품의 반납 여부를 직접 확인합니다.", allScopes, optional = true),
    )

    fun requiredItems(scope: TravelScope): List<ReadinessTemplate> = items(scope, optional = false)
    fun optionalItems(scope: TravelScope): List<ReadinessTemplate> = items(scope, optional = true)
    fun optionalGroups(scope: TravelScope): List<ChecklistGroup> = optionalItems(scope).map { it.group }.distinct()
    fun optionalItems(scope: TravelScope, group: ChecklistGroup): List<ReadinessTemplate> =
        optionalItems(scope).filter { it.group == group }

    fun find(id: String?): ReadinessTemplate? = id?.let { raw ->
        all.firstOrNull { it.id.name == raw }
    }

    fun isKnownTemplateId(id: String?): Boolean = id == null || find(id) != null

    /** Maps safe v1 built-in labels at display time without changing legacy data rows. */
    fun displayMetadata(type: ChecklistType, templateId: String?, title: String): ReadinessDisplayMetadata =
        find(templateId)?.let { ReadinessDisplayMetadata(it.group, it.hint, it.optional) }
            ?: all.firstOrNull { it.type == type && it.title.equals(title.trim(), ignoreCase = true) }
                ?.let { ReadinessDisplayMetadata(it.group, it.hint, it.optional) }
            ?: ReadinessDisplayMetadata(ChecklistGroup.DIRECT_ADD, "직접 추가한 항목입니다.", optional = false)

    private fun items(scope: TravelScope, optional: Boolean): List<ReadinessTemplate> = all.filter { template ->
        template.optional == optional && scopeMatches(template.scopes, scope)
    }

    private fun scopeMatches(scopes: Set<TravelScope>, scope: TravelScope): Boolean = when (scope) {
        TravelScope.AUTO -> TravelScope.AUTO in scopes
        else -> scope in scopes || TravelScope.AUTO in scopes
    }
}
