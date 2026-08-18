package io.trippilot.app.core.model

import java.time.LocalDate

enum class TravelScope { AUTO, DOMESTIC, INTERNATIONAL }

enum class TripStatus { DRAFT, PLANNED }

enum class PreparationStatus { TODO, DONE, SKIPPED }

enum class ItemOrigin { DEFAULT, MANUAL, AI }

enum class ReservationStatus { DRAFT, CONFIRMED, CANCELLED }

enum class SourceOwnerType { ITINERARY, RESERVATION, SAFETY_MEMO }

enum class RecheckResult { UNCHANGED, CHANGED, FAILED }

/** Post-trip follow-up windows; null on a preparation item means pre-trip/general. */
enum class PostTripWindow { WITHIN_48_HOURS, WITHIN_ONE_WEEK, LATER }

/** Static problem-response categories; labels carry only general guidance. */
enum class SafetyCategory(val label: String) {
    DOCUMENTS("여권·신분·여행 서류"),
    HEALTH("질병·부상"),
    THEFT_LOSS("도난·분실"),
    PAYMENT("카드·현금·결제"),
    TRANSPORT_DELAY("교통·예약·항공 지연"),
    CONNECTIVITY("휴대폰·통신"),
    WEATHER_FIELD("기상·현장 상황"),
}

enum class CalendarActionStatus { REVIEW_REQUIRED, APPROVED, EXECUTED, FAILED, REJECTED }

data class TripInput(
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val timezone: String,
    val scope: TravelScope,
    val notes: String = "",
)

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val message: String) : ValidationResult
}

object TravelValidators {
    private val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")
    private val urlPattern = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)

    fun trip(input: TripInput): ValidationResult = when {
        input.title.trim().isEmpty() -> ValidationResult.Invalid("여행 제목을 입력하세요.")
        input.destination.trim().isEmpty() -> ValidationResult.Invalid("목적지를 입력하세요.")
        !isIsoDate(input.startDate) || !isIsoDate(input.endDate) -> {
            ValidationResult.Invalid("날짜는 YYYY-MM-DD 형식으로 입력하세요.")
        }
        LocalDate.parse(input.startDate).isAfter(LocalDate.parse(input.endDate)) -> ValidationResult.Invalid("종료일은 시작일보다 빠를 수 없습니다.")
        input.timezone.isBlank() -> ValidationResult.Invalid("시간대를 선택하세요.")
        else -> ValidationResult.Valid
    }

    fun itinerary(date: String, startMinute: Int?, endMinute: Int?, tripStart: String, tripEnd: String, title: String): ValidationResult = when {
        title.trim().isEmpty() -> ValidationResult.Invalid("일정 제목을 입력하세요.")
        !isIsoDate(date) || !isIsoDate(tripStart) || !isIsoDate(tripEnd) -> ValidationResult.Invalid("날짜는 YYYY-MM-DD 형식으로 입력하세요.")
        LocalDate.parse(date).isBefore(LocalDate.parse(tripStart)) || LocalDate.parse(date).isAfter(LocalDate.parse(tripEnd)) -> ValidationResult.Invalid("여행 기간 밖의 일정은 추가할 수 없습니다.")
        startMinute != null && endMinute != null && endMinute < startMinute -> {
            ValidationResult.Invalid("종료 시각은 시작 시각보다 빠를 수 없습니다.")
        }
        else -> ValidationResult.Valid
    }

    fun url(value: String): ValidationResult = if (urlPattern.matches(value.trim())) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid("http 또는 https URL을 입력하세요.")
    }

    private fun isIsoDate(value: String): Boolean = datePattern.matches(value) && runCatching { LocalDate.parse(value) }.isSuccess
}

data class DefaultChecklistItem(
    val templateId: ChecklistTemplateId,
    val title: String,
    val type: ChecklistType,
)

enum class ChecklistType { PREPARATION, PACKING }

object TravelScopeTemplates {
    /** Compatibility view for callers that only need the required local templates. */
    fun items(scope: TravelScope): List<DefaultChecklistItem> =
        ReadinessTemplateCatalog.requiredItems(scope).map { template ->
            DefaultChecklistItem(template.id, template.title, template.type)
        }
}
