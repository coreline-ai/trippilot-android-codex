package io.trippilot.app.integration.codex.contract

import kotlinx.serialization.Serializable

/**
 * Versioned, provider-neutral data that may be sent to a future Codex runtime.
 * It deliberately has no database IDs, credentials, prompt text, or executable action.
 */
@Serializable
data class TripPlanningRequest(
    val schema: String = TRIP_PLANNING_REQUEST_SCHEMA,
    val version: Int = CONTRACT_VERSION,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val companion: TravelCompanion,
    val budget: BudgetRange,
    val interests: List<String>,
    val purpose: String,
)

@Serializable
data class ReservationAnalysisRequest(
    val schema: String = RESERVATION_ANALYSIS_REQUEST_SCHEMA,
    val version: Int = CONTRACT_VERSION,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val reservationHint: String,
)

@Serializable
enum class TravelCompanion { SOLO, COUPLE, FAMILY, FRIENDS, BUSINESS }

@Serializable
enum class BudgetRange { LOW, MODERATE, HIGH, FLEXIBLE }

@Serializable
enum class ReservationType { FLIGHT, HOTEL, TRAIN, ACTIVITY, RESTAURANT, OTHER }

@Serializable
enum class DraftKind { TRIP_PLAN, WEATHER_ADVISORY }

@Serializable
data class TripPlanDraft(
    val schema: String,
    val version: Int,
    val kind: DraftKind,
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val days: List<DraftDay>,
    val reservations: List<DraftReservation>,
    val packingSuggestions: List<DraftPackingSuggestion>,
    val preparationSuggestions: List<DraftPreparationSuggestion>,
    val sources: List<SourceCandidate>,
    val assumptions: List<String>,
)

@Serializable
data class DraftDay(
    val date: String,
    val items: List<DraftItineraryItem>,
)

@Serializable
data class DraftItineraryItem(
    val id: String,
    val title: String,
    val startMinute: Int? = null,
    val location: String = "",
    val notes: String = "",
)

@Serializable
data class DraftReservation(
    val id: String,
    val type: ReservationType,
    val provider: String,
    val confirmationCode: String,
    val dateTime: String? = null,
    val location: String = "",
    val sourceUrl: String? = null,
)

@Serializable
data class DraftPackingSuggestion(
    val id: String,
    val title: String,
    val quantity: Int = 1,
    val reason: String = "",
)

@Serializable
data class DraftPreparationSuggestion(
    val id: String,
    val title: String,
    val reason: String = "",
)

/** A source can only be applied when its linked selected itinerary or reservation is applied. */
@Serializable
data class SourceCandidate(
    val id: String,
    val title: String,
    val url: String,
    val relatedItemId: String,
)

/** Information-only. It must never contain an action or change a travel record. */
@Serializable
data class WeatherAdvisoryDraft(
    val schema: String,
    val version: Int,
    val kind: DraftKind,
    val destination: String,
    val dates: List<String>,
    val summary: String,
    val advisories: List<String>,
    val assumptions: List<String>,
)

const val CONTRACT_VERSION = 1
const val TRIP_PLANNING_REQUEST_SCHEMA = "trippilot.trip-planning-request"
const val RESERVATION_ANALYSIS_REQUEST_SCHEMA = "trippilot.reservation-analysis-request"
const val TRIP_PLAN_DRAFT_SCHEMA = "trippilot.trip-plan-draft"
const val WEATHER_ADVISORY_DRAFT_SCHEMA = "trippilot.weather-advisory-draft"

/** The only parsed, in-memory values that the user explicitly approves for local persistence. */
data class ApprovedDraftSelection(
    val itinerary: List<ApprovedItineraryItem>,
    val reservations: List<DraftReservation>,
    val preparation: List<DraftPreparationSuggestion>,
    val packing: List<DraftPackingSuggestion>,
    val sources: List<SourceCandidate>,
)

data class ApprovedItineraryItem(
    val id: String,
    val date: String,
    val title: String,
    val startMinute: Int?,
    val location: String,
    val notes: String,
)
