package io.trippilot.app.integration.codex.contract

import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed interface ContractResult<out T> {
    data class Valid<T>(val value: T) : ContractResult<T>
    data class Invalid(val message: String) : ContractResult<Nothing>
}

/** Strict parser: an unknown key, unsupported schema or malformed value is a rejected draft. */
object TripDraftParser {
    private val json = Json {
        ignoreUnknownKeys = false
        explicitNulls = false
        isLenient = false
    }

    fun parseTripPlan(rawJson: String, tripStart: String, tripEnd: String): ContractResult<TripPlanDraft> =
        parse(rawJson, TripPlanDraft.serializer()) { validateTripPlan(it, tripStart, tripEnd) }

    fun parseWeatherAdvisory(rawJson: String, tripStart: String, tripEnd: String): ContractResult<WeatherAdvisoryDraft> =
        parse(rawJson, WeatherAdvisoryDraft.serializer()) { validateWeatherAdvisory(it, tripStart, tripEnd) }

    fun validateTripPlan(draft: TripPlanDraft, tripStart: String, tripEnd: String): ContractResult<TripPlanDraft> {
        val bounds = dateRange(tripStart, tripEnd) ?: return invalid("여행 기간이 올바르지 않습니다.")
        if (draft.schema != TRIP_PLAN_DRAFT_SCHEMA || draft.version != CONTRACT_VERSION || draft.kind != DraftKind.TRIP_PLAN) {
            return invalid("지원하지 않는 여행 초안 계약입니다.")
        }
        if (!text(draft.title, 100) || !text(draft.destination, 120)) return invalid("초안 제목 또는 목적지가 올바르지 않습니다.")
        if (draft.startDate != tripStart || draft.endDate != tripEnd) return invalid("초안 기간이 요청한 여행 기간과 다릅니다.")
        if (draft.days.size > 30 || draft.reservations.size > 30 || draft.packingSuggestions.size > 60 || draft.preparationSuggestions.size > 60 || draft.sources.size > 90 || draft.assumptions.size > 20) {
            return invalid("초안 항목 수가 허용 범위를 넘었습니다.")
        }
        val itineraryIds = mutableSetOf<String>()
        val allItems = draft.days.flatMap { day ->
            if (!dateInRange(day.date, bounds) || day.items.size > 16) return invalid("일정 날짜 또는 일별 항목 수가 올바르지 않습니다.")
            day.items.map { item ->
                if (!identifier(item.id) || !itineraryIds.add(item.id) || !text(item.title, 140) || !text(item.location, 180, allowBlank = true) || !text(item.notes, 500, allowBlank = true) || item.startMinute !in 0..1439 && item.startMinute != null) {
                    return invalid("일정 항목 형식이 올바르지 않습니다.")
                }
                item
            }
        }
        val reservationIds = mutableSetOf<String>()
        draft.reservations.forEach { reservation ->
            if (!identifier(reservation.id) || !reservationIds.add(reservation.id) || reservation.id in itineraryIds || !text(reservation.provider, 120) || !text(reservation.confirmationCode, 80) || !text(reservation.location, 180, allowBlank = true) || !validDateTime(reservation.dateTime) || !validUrl(reservation.sourceUrl)) {
                return invalid("예약 초안 형식이 올바르지 않습니다.")
            }
        }
        val suggestionIds = mutableSetOf<String>()
        draft.packingSuggestions.forEach {
            if (!identifier(it.id) || !suggestionIds.add(it.id) || !text(it.title, 120) || it.quantity !in 1..99 || !text(it.reason, 240, allowBlank = true)) return invalid("짐 제안 형식이 올바르지 않습니다.")
        }
        draft.preparationSuggestions.forEach {
            if (!identifier(it.id) || !suggestionIds.add(it.id) || !text(it.title, 120) || !text(it.reason, 240, allowBlank = true)) return invalid("준비 제안 형식이 올바르지 않습니다.")
        }
        val sourceIds = mutableSetOf<String>()
        val sourceTargets = itineraryIds + reservationIds
        draft.sources.forEach {
            if (!identifier(it.id) || !sourceIds.add(it.id) || !text(it.title, 160) || !validUrl(it.url) || it.relatedItemId !in sourceTargets) return invalid("출처 후보 형식이 올바르지 않습니다.")
        }
        if (draft.assumptions.any { !text(it, 240) }) return invalid("가정 형식이 올바르지 않습니다.")
        return ContractResult.Valid(draft)
    }

    fun validateWeatherAdvisory(draft: WeatherAdvisoryDraft, tripStart: String, tripEnd: String): ContractResult<WeatherAdvisoryDraft> {
        val bounds = dateRange(tripStart, tripEnd) ?: return invalid("여행 기간이 올바르지 않습니다.")
        if (draft.schema != WEATHER_ADVISORY_DRAFT_SCHEMA || draft.version != CONTRACT_VERSION || draft.kind != DraftKind.WEATHER_ADVISORY) return invalid("지원하지 않는 날씨 참고 계약입니다.")
        if (!text(draft.destination, 120) || !text(draft.summary, 600) || draft.dates.isEmpty() || draft.dates.size > 30 || draft.advisories.size > 20 || draft.assumptions.size > 20) return invalid("날씨 참고 항목 수 또는 필수값이 올바르지 않습니다.")
        if (draft.dates.any { !dateInRange(it, bounds) } || draft.advisories.any { !text(it, 240) } || draft.assumptions.any { !text(it, 240) }) return invalid("날씨 참고 날짜 또는 문구가 올바르지 않습니다.")
        return ContractResult.Valid(draft)
    }

    fun validateRequest(request: TripPlanningRequest): ContractResult<TripPlanningRequest> {
        val bounds = dateRange(request.startDate, request.endDate) ?: return invalid("여행 기간이 올바르지 않습니다.")
        if (request.schema != TRIP_PLANNING_REQUEST_SCHEMA || request.version != CONTRACT_VERSION || !text(request.destination, 120) || !text(request.purpose, 300) || request.interests.size > 10 || request.interests.any { !text(it, 60) }) {
            return invalid("여행 초안 요청 형식이 올바르지 않습니다.")
        }
        if (bounds.first.isAfter(bounds.second)) return invalid("여행 기간이 올바르지 않습니다.")
        return ContractResult.Valid(request)
    }

    fun validateApprovedSelection(selection: ApprovedDraftSelection, tripStart: String, tripEnd: String): ContractResult<ApprovedDraftSelection> {
        val bounds = dateRange(tripStart, tripEnd) ?: return invalid("여행 기간이 올바르지 않습니다.")
        if (selection.itinerary.size > 120 || selection.reservations.size > 30 || selection.preparation.size > 60 || selection.packing.size > 60 || selection.sources.size > 90) return invalid("선택한 초안 항목 수가 허용 범위를 넘었습니다.")
        val itineraryIds = selection.itinerary.map { it.id }.toSet()
        val reservationIds = selection.reservations.map { it.id }.toSet()
        if (itineraryIds.size != selection.itinerary.size || reservationIds.size != selection.reservations.size) return invalid("중복된 초안 항목이 있습니다.")
        selection.itinerary.forEach {
            if (!identifier(it.id) || !dateInRange(it.date, bounds) || !text(it.title, 140) || !text(it.location, 180, allowBlank = true) || !text(it.notes, 500, allowBlank = true) || it.startMinute !in 0..1439 && it.startMinute != null) return invalid("수정한 일정 형식이 올바르지 않습니다.")
        }
        selection.reservations.forEach {
            if (!identifier(it.id) || !text(it.provider, 120) || !text(it.confirmationCode, 80) || !text(it.location, 180, allowBlank = true) || !validDateTime(it.dateTime) || !validUrl(it.sourceUrl)) return invalid("수정한 예약 형식이 올바르지 않습니다.")
        }
        selection.preparation.forEach { if (!identifier(it.id) || !text(it.title, 120)) return invalid("수정한 준비 항목이 올바르지 않습니다.") }
        selection.packing.forEach { if (!identifier(it.id) || !text(it.title, 120) || it.quantity !in 1..99) return invalid("수정한 짐 항목이 올바르지 않습니다.") }
        val sourceTargets = itineraryIds + reservationIds
        selection.sources.forEach { if (!identifier(it.id) || !text(it.title, 160) || !validUrl(it.url) || it.relatedItemId !in sourceTargets) return invalid("수정한 출처 항목이 올바르지 않습니다.") }
        return ContractResult.Valid(selection)
    }

    private fun <T> parse(rawJson: String, serializer: kotlinx.serialization.KSerializer<T>, validator: (T) -> ContractResult<T>): ContractResult<T> = try {
        if (rawJson.length > MAX_JSON_CHARS) invalid("붙여넣은 JSON이 너무 큽니다.") else validator(json.decodeFromString(serializer, rawJson))
    } catch (_: SerializationException) {
        invalid("지원하지 않는 JSON 계약입니다. 알 수 없는 필드와 필수값을 확인하세요.")
    } catch (_: IllegalArgumentException) {
        invalid("JSON 값 형식이 올바르지 않습니다.")
    }

    private fun validDateTime(value: String?): Boolean = value == null || runCatching { LocalDateTime.parse(value) }.isSuccess
    private fun dateRange(start: String, end: String): Pair<LocalDate, LocalDate>? = runCatching {
        LocalDate.parse(start) to LocalDate.parse(end)
    }.getOrNull()?.takeIf { !it.first.isAfter(it.second) }
    private fun dateInRange(value: String, bounds: Pair<LocalDate, LocalDate>): Boolean = runCatching { LocalDate.parse(value) }.getOrNull()?.let { it in bounds.first..bounds.second } == true
    private fun validUrl(value: String?): Boolean = value == null || runCatching {
        val uri = URI(value)
        uri.scheme in setOf("http", "https") && !uri.host.isNullOrBlank() && value.length <= 2_000
    }.getOrDefault(false)
    private fun identifier(value: String): Boolean = value.matches(Regex("[A-Za-z0-9_-]{1,64}"))
    private fun text(value: String, max: Int, allowBlank: Boolean = false): Boolean = value.length <= max && (allowBlank || value.isNotBlank())
    private fun <T> invalid(message: String): ContractResult<T> = ContractResult.Invalid(message)
}

private const val MAX_JSON_CHARS = 100_000
