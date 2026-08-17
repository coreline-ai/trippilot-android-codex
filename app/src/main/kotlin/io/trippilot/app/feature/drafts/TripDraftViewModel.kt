package io.trippilot.app.feature.drafts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.trippilot.app.core.codex.CodexRuntimePort
import io.trippilot.app.core.codex.AuthStatus
import io.trippilot.app.core.codex.RuntimeStatus
import io.trippilot.app.core.codex.DraftStreamEvent
import io.trippilot.app.core.codex.DraftStreamStage
import io.trippilot.app.core.data.DraftApplyResult
import io.trippilot.app.core.data.TripRepository
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.integration.codex.contract.ApprovedDraftSelection
import io.trippilot.app.integration.codex.contract.ApprovedItineraryItem
import io.trippilot.app.integration.codex.contract.BudgetRange
import io.trippilot.app.integration.codex.contract.ContractResult
import io.trippilot.app.integration.codex.contract.DraftPackingSuggestion
import io.trippilot.app.integration.codex.contract.DraftPreparationSuggestion
import io.trippilot.app.integration.codex.contract.DraftReservation
import io.trippilot.app.integration.codex.contract.ReservationAnalysisRequest
import io.trippilot.app.integration.codex.contract.ReservationType
import io.trippilot.app.integration.codex.contract.SourceCandidate
import io.trippilot.app.integration.codex.contract.TravelCompanion
import io.trippilot.app.integration.codex.contract.TripDraftParser
import io.trippilot.app.integration.codex.contract.TripPlanDraft
import io.trippilot.app.integration.codex.contract.TripPlanningRequest
import io.trippilot.app.integration.codex.contract.WeatherAdvisoryDraft
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TripDraftViewModel @Inject constructor(
    private val repository: TripRepository,
    private val runtime: CodexRuntimePort,
) : ViewModel() {
    private val mutableState = MutableStateFlow<DraftUiState>(DraftUiState.Idle)
    val state: StateFlow<DraftUiState> = mutableState.asStateFlow()

    private val mutableWeather = MutableStateFlow<WeatherAdvisoryDraft?>(null)
    val weather: StateFlow<WeatherAdvisoryDraft?> = mutableWeather.asStateFlow()

    /** These values are runtime-only and are never copied into Room or DataStore. */
    val runtimeStatus: StateFlow<RuntimeStatus> = runtime.runtimeStatus
    val authStatus: StateFlow<AuthStatus> = runtime.authStatus
    val loginChallenge = runtime.loginChallenge

    private var streamJob: Job? = null
    private var streamGeneration = 0

    fun createPlan(
        trip: TripEntity,
        purpose: String,
        companion: TravelCompanion,
        budget: BudgetRange,
        interests: List<String>,
    ) {
        val request = TripPlanningRequest(
            destination = trip.destination,
            startDate = trip.startDate,
            endDate = trip.endDate,
            companion = companion,
            budget = budget,
            interests = interests.map(String::trim).filter(String::isNotEmpty),
            purpose = purpose.trim(),
        )
        when (val validation = TripDraftParser.validateRequest(request)) {
            is ContractResult.Invalid -> {
                mutableState.value = DraftUiState.Error(validation.message)
                return
            }
            is ContractResult.Valid -> Unit
        }
        collectPlanStream(trip, runtime.createPlanStream(request), isReservationOnly = false)
    }

    fun createReservationAnalysis(trip: TripEntity, reservationHint: String) {
        val request = ReservationAnalysisRequest(
            destination = trip.destination,
            startDate = trip.startDate,
            endDate = trip.endDate,
            reservationHint = reservationHint.trim(),
        )
        if (request.reservationHint.isBlank() || request.reservationHint.length > 300) {
            mutableState.value = DraftUiState.Error("예약 분석 메모는 1~300자로 입력하세요.")
            return
        }
        collectPlanStream(trip, runtime.analyzeReservationStream(request), isReservationOnly = true)
    }

    fun createWeatherAdvisory(trip: TripEntity) {
        val request = defaultRequest(trip)
        val generation = ++streamGeneration
        streamJob?.cancel()
        mutableWeather.value = null
        mutableState.value = DraftUiState.Generating("날씨 참고를 준비하는 중", DraftStreamStage.VALIDATING)
        streamJob = viewModelScope.launch {
            var terminal = false
            runtime.weatherAdvisoryStream(request).collect { event ->
                if (generation != streamGeneration || terminal) return@collect
                when (event) {
                    DraftStreamEvent.Started -> mutableState.value = DraftUiState.Generating("날씨 참고를 준비하는 중", DraftStreamStage.GENERATING)
                    is DraftStreamEvent.Progress -> mutableState.value = DraftUiState.Generating("날씨 참고를 준비하는 중", event.stage)
                    is DraftStreamEvent.WeatherReady -> mutableWeather.value = event.advisory
                    DraftStreamEvent.Empty -> { terminal = true; mutableState.value = DraftUiState.Notice("표시할 날씨 참고가 없습니다.") }
                    DraftStreamEvent.Stopped -> { terminal = true; mutableState.value = DraftUiState.Notice("날씨 참고 생성을 중지했습니다.") }
                    is DraftStreamEvent.Failed -> { terminal = true; mutableState.value = DraftUiState.Error(streamFailureText(event.reason.name)) }
                    DraftStreamEvent.Completed -> if (mutableWeather.value != null) mutableState.value = DraftUiState.WeatherReady
                    is DraftStreamEvent.TripPlanReady, is DraftStreamEvent.ReservationReady -> Unit
                }
            }
        }
    }

    fun stopGeneration() {
        streamGeneration++
        streamJob?.cancel()
        viewModelScope.launch { runtime.stop() }
        mutableState.value = DraftUiState.Notice("초안 생성을 중지했습니다. 기존 여행 기록은 변경되지 않았습니다.")
    }

    fun beginCodexLogin() {
        viewModelScope.launch { runtime.beginLogin() }
    }

    fun cancelCodexLogin() {
        viewModelScope.launch { runtime.cancelLogin() }
    }

    fun refreshCodexAfterBrowserReturn() {
        viewModelScope.launch { runtime.refreshAfterBrowserReturn() }
    }

    fun logoutCodex() {
        viewModelScope.launch { runtime.logout() }
    }

    fun clearWeather() { mutableWeather.value = null }
    fun dismissMessage() { mutableState.value = DraftUiState.Idle }
    fun discardReview() { mutableState.value = DraftUiState.Idle }

    fun setItinerarySelected(id: String, selected: Boolean) = updateReview { review ->
        review.copy(itinerary = review.itinerary.map { if (it.item.id == id) it.copy(selected = selected) else it })
    }
    fun updateItinerary(id: String, title: String? = null, date: String? = null, time: String? = null, location: String? = null, notes: String? = null) = updateReview { review ->
        review.copy(itinerary = review.itinerary.map {
            if (it.item.id != id) it else it.copy(item = it.item.copy(
                title = title ?: it.item.title,
                date = date ?: it.item.date,
                startMinute = time?.let(::parseMinute) ?: it.item.startMinute,
                location = location ?: it.item.location,
                notes = notes ?: it.item.notes,
            ))
        })
    }
    fun setReservationSelected(id: String, selected: Boolean) = updateReview { review ->
        review.copy(reservations = review.reservations.map { if (it.item.id == id) it.copy(selected = selected) else it })
    }
    fun updateReservation(id: String, type: ReservationType? = null, provider: String? = null, code: String? = null, time: String? = null, location: String? = null, url: String? = null) = updateReview { review ->
        review.copy(reservations = review.reservations.map {
            if (it.item.id != id) it else it.copy(item = it.item.copy(
                type = type ?: it.item.type,
                provider = provider ?: it.item.provider,
                confirmationCode = code ?: it.item.confirmationCode,
                dateTime = time ?: it.item.dateTime,
                location = location ?: it.item.location,
                sourceUrl = url ?: it.item.sourceUrl,
            ))
        })
    }
    fun setPreparationSelected(id: String, selected: Boolean) = updateReview { review ->
        review.copy(preparation = review.preparation.map { if (it.item.id == id) it.copy(selected = selected) else it })
    }
    fun updatePreparation(id: String, title: String) = updateReview { review ->
        review.copy(preparation = review.preparation.map { if (it.item.id == id) it.copy(item = it.item.copy(title = title)) else it })
    }
    fun setPackingSelected(id: String, selected: Boolean) = updateReview { review ->
        review.copy(packing = review.packing.map { if (it.item.id == id) it.copy(selected = selected) else it })
    }
    fun updatePacking(id: String, title: String? = null, quantity: String? = null) = updateReview { review ->
        review.copy(packing = review.packing.map {
            if (it.item.id != id) it else it.copy(item = it.item.copy(title = title ?: it.item.title, quantity = quantity?.toIntOrNull() ?: it.item.quantity))
        })
    }
    fun setSourceSelected(id: String, selected: Boolean) = updateReview { review ->
        review.copy(sources = review.sources.map { if (it.item.id == id) it.copy(selected = selected) else it })
    }
    fun updateSource(id: String, title: String? = null, url: String? = null) = updateReview { review ->
        review.copy(sources = review.sources.map { if (it.item.id == id) it.copy(item = it.item.copy(title = title ?: it.item.title, url = url ?: it.item.url)) else it })
    }

    fun importPastedJson(trip: TripEntity, rawJson: String) {
        when (val parsed = TripDraftParser.parseTripPlan(rawJson, trip.startDate, trip.endDate)) {
            is ContractResult.Valid -> mutableState.value = DraftUiState.Review(ReviewDraft.from(trip.id, parsed.value))
            is ContractResult.Invalid -> mutableState.value = DraftUiState.Error(parsed.message)
        }
    }

    fun applySelected(trip: TripEntity) {
        val review = (mutableState.value as? DraftUiState.Review)?.draft ?: return
        if (review.tripId != trip.id) {
            mutableState.value = DraftUiState.Error("다른 여행의 초안입니다. 다시 열어 주세요.")
            return
        }
        // Applying blocks a second tap while the single local transaction runs,
        // so the same draft can never be applied twice (hallmark-guide.md §3 LOADING).
        mutableState.value = DraftUiState.Applying(review)
        viewModelScope.launch {
            when (val result = repository.applyApprovedDraft(trip, review.toApprovedSelection())) {
                is DraftApplyResult.Rejected -> mutableState.value = DraftUiState.Error(result.message)
                is DraftApplyResult.Applied -> mutableState.value = DraftUiState.Applied(result)
            }
        }
    }

    private fun collectPlanStream(trip: TripEntity, stream: kotlinx.coroutines.flow.Flow<DraftStreamEvent>, isReservationOnly: Boolean) {
        val generation = ++streamGeneration
        streamJob?.cancel()
        mutableState.value = DraftUiState.Generating("구조화된 초안을 만드는 중", DraftStreamStage.VALIDATING)
        streamJob = viewModelScope.launch {
            var terminal = false
            stream.collect { event ->
                if (generation != streamGeneration || terminal) return@collect
                when (event) {
                    DraftStreamEvent.Started -> mutableState.value = DraftUiState.Generating("구조화된 초안을 만드는 중", DraftStreamStage.GENERATING)
                    is DraftStreamEvent.Progress -> mutableState.value = DraftUiState.Generating("구조화된 초안을 만드는 중", event.stage)
                    is DraftStreamEvent.TripPlanReady -> if (!isReservationOnly) mutableState.value = DraftUiState.Review(ReviewDraft.from(trip.id, event.draft))
                    is DraftStreamEvent.ReservationReady -> mutableState.value = DraftUiState.Review(ReviewDraft.from(trip.id, event.draft))
                    DraftStreamEvent.Empty -> { terminal = true; mutableState.value = DraftUiState.Notice("제안된 항목이 없습니다. 기존 여행 기록은 변경되지 않았습니다.") }
                    DraftStreamEvent.Stopped -> { terminal = true; mutableState.value = DraftUiState.Notice("초안 생성을 중지했습니다. 기존 여행 기록은 변경되지 않았습니다.") }
                    is DraftStreamEvent.Failed -> { terminal = true; mutableState.value = DraftUiState.Error(streamFailureText(event.reason.name)) }
                    DraftStreamEvent.Completed -> Unit
                    is DraftStreamEvent.WeatherReady -> Unit
                }
            }
        }
    }

    private fun updateReview(transform: (ReviewDraft) -> ReviewDraft) {
        val current = mutableState.value as? DraftUiState.Review ?: return
        mutableState.value = DraftUiState.Review(transform(current.draft))
    }

    private fun defaultRequest(trip: TripEntity) = TripPlanningRequest(
        destination = trip.destination, startDate = trip.startDate, endDate = trip.endDate,
        companion = TravelCompanion.SOLO, budget = BudgetRange.FLEXIBLE,
        interests = emptyList(), purpose = "여행 기간 중 참고할 날씨 정보",
    )

    private fun parseMinute(value: String): Int? {
        if (value.isBlank()) return null
        val parts = value.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return (hour * 60 + minute).takeIf { hour in 0..23 && minute in 0..59 }
    }

    private fun streamFailureText(reason: String): String = when (reason) {
        "CONTRACT_REJECTED" -> "초안 계약을 확인할 수 없어 반영하지 않았습니다. 기존 여행 기록은 변경되지 않았습니다."
        "RUNTIME_UNAVAILABLE" -> "테스트 runtime을 사용할 수 없습니다. 기존 여행 기록은 변경되지 않았습니다."
        else -> "초안을 만들지 못했습니다. 기존 여행 기록은 변경되지 않았습니다."
    }
}

sealed interface DraftUiState {
    data object Idle : DraftUiState
    data class Generating(val message: String, val stage: DraftStreamStage) : DraftUiState
    data class Review(val draft: ReviewDraft) : DraftUiState
    data class Applying(val draft: ReviewDraft) : DraftUiState
    data class Notice(val message: String) : DraftUiState
    data class Error(val message: String) : DraftUiState
    data class Applied(val result: DraftApplyResult.Applied) : DraftUiState
    data object WeatherReady : DraftUiState
}

data class ReviewDraft(
    val tripId: String,
    val title: String,
    val itinerary: List<Selectable<ApprovedItineraryItem>>,
    val reservations: List<Selectable<DraftReservation>>,
    val preparation: List<Selectable<DraftPreparationSuggestion>>,
    val packing: List<Selectable<DraftPackingSuggestion>>,
    val sources: List<Selectable<SourceCandidate>>,
    val assumptions: List<String>,
) {
    fun toApprovedSelection(): ApprovedDraftSelection {
        val selectedItinerary = itinerary.filter { it.selected }.map { it.item }
        val selectedReservations = reservations.filter { it.selected }.map { it.item }
        val selectedTargetIds = selectedItinerary.map { it.id }.toSet() + selectedReservations.map { it.id }.toSet()
        return ApprovedDraftSelection(
            itinerary = selectedItinerary,
            reservations = selectedReservations,
            preparation = preparation.filter { it.selected }.map { it.item },
            packing = packing.filter { it.selected }.map { it.item },
            // A source never becomes orphaned. Excluding its linked suggestion excludes it too.
            sources = sources.filter { it.selected && it.item.relatedItemId in selectedTargetIds }.map { it.item },
        )
    }

    companion object {
        fun from(tripId: String, draft: TripPlanDraft) = ReviewDraft(
            tripId = tripId,
            title = draft.title,
            itinerary = draft.days.flatMap { day -> day.items.map { Selectable(ApprovedItineraryItem(it.id, day.date, it.title, it.startMinute, it.location, it.notes)) } },
            reservations = draft.reservations.map(::Selectable),
            preparation = draft.preparationSuggestions.map(::Selectable),
            packing = draft.packingSuggestions.map(::Selectable),
            sources = draft.sources.map(::Selectable),
            assumptions = draft.assumptions,
        )
    }
}

data class Selectable<T>(val item: T, val selected: Boolean = true)
