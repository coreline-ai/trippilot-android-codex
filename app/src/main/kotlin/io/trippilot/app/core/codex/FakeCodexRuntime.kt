package io.trippilot.app.core.codex

import io.trippilot.app.integration.codex.contract.CONTRACT_VERSION
import io.trippilot.app.integration.codex.contract.ContractResult
import io.trippilot.app.integration.codex.contract.ReservationAnalysisRequest
import io.trippilot.app.integration.codex.contract.TripDraftParser
import io.trippilot.app.integration.codex.contract.TripPlanningRequest
import io.trippilot.app.integration.codex.contract.WeatherAdvisoryDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/**
 * Credential-free fixtures for the Phase 3 contract and review flow.
 * Fixture JSON is parsed before it is emitted and is never persisted or logged.
 */
class FakeCodexRuntime : CodexRuntimePort {
    private val mutableRuntimeStatus = MutableStateFlow(RuntimeStatus.READY)
    private val mutableAuthStatus = MutableStateFlow(AuthStatus.NOT_REQUIRED)
    private val mutableLoginChallenge = MutableStateFlow<CodexDeviceLoginChallenge?>(null)
    private var fixtureScenario: FakeCodexScenario = FakeCodexScenario.STREAMING
    private var stopRequested = false

    override val runtimeStatus = mutableRuntimeStatus.asStateFlow()
    override val authStatus = mutableAuthStatus.asStateFlow()
    override val loginChallenge = mutableLoginChallenge.asStateFlow()

    override suspend fun beginLogin() = Unit
    override suspend fun cancelLogin() = Unit
    override suspend fun refreshAfterBrowserReturn() = Unit

    fun setScenarioForTest(scenario: FakeCodexScenario) { fixtureScenario = scenario }

    fun completeLoginForTestOrPreview() = Unit
    fun cancelLoginForTestOrPreview() = Unit

    fun failRuntimeForTestOrPreview() {
        mutableRuntimeStatus.value = RuntimeStatus.ERROR
        mutableAuthStatus.value = AuthStatus.ERROR
    }

    override suspend fun availableModels(): List<CodexModel> =
        if (mutableRuntimeStatus.value == RuntimeStatus.READY) listOf(CodexModel(id = "fake-trip-planner", displayName = "Trip planner (test fixture)")) else emptyList()

    override fun createPlanStream(request: TripPlanningRequest): Flow<DraftStreamEvent> = flow {
        when {
            mutableRuntimeStatus.value != RuntimeStatus.READY -> emit(
                DraftStreamEvent.Failed(PlanStreamFailure.RUNTIME_UNAVAILABLE),
            )
            else -> emitPlanFixture(request)
        }
    }

    override fun analyzeReservationStream(request: ReservationAnalysisRequest): Flow<DraftStreamEvent> = flow {
        val planRequest = TripPlanningRequest(
            destination = request.destination, startDate = request.startDate, endDate = request.endDate,
            companion = io.trippilot.app.integration.codex.contract.TravelCompanion.SOLO,
            budget = io.trippilot.app.integration.codex.contract.BudgetRange.FLEXIBLE,
            interests = emptyList(), purpose = request.reservationHint,
        )
        if (mutableRuntimeStatus.value != RuntimeStatus.READY) emit(DraftStreamEvent.Failed(PlanStreamFailure.RUNTIME_UNAVAILABLE))
        else emitReservationFixture(planRequest)
    }

    override fun weatherAdvisoryStream(request: TripPlanningRequest): Flow<DraftStreamEvent> = flow {
        if (mutableRuntimeStatus.value != RuntimeStatus.READY) {
            emit(DraftStreamEvent.Failed(PlanStreamFailure.RUNTIME_UNAVAILABLE))
        } else {
            emit(DraftStreamEvent.Started)
            emit(DraftStreamEvent.Progress(DraftStreamStage.VALIDATING_RESULT))
            when (val result = TripDraftParser.parseWeatherAdvisory(weatherFixture(request), request.startDate, request.endDate)) {
                is ContractResult.Valid -> emit(DraftStreamEvent.WeatherReady(result.value))
                is ContractResult.Invalid -> emit(DraftStreamEvent.Failed(PlanStreamFailure.CONTRACT_REJECTED))
            }
            emit(DraftStreamEvent.Completed)
        }
    }

    override suspend fun stop() { stopRequested = true }

    override suspend fun logout() {
        mutableAuthStatus.value = AuthStatus.NOT_REQUIRED
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<DraftStreamEvent>.emitPlanFixture(request: TripPlanningRequest) {
        stopRequested = false
        emit(DraftStreamEvent.Started)
        emit(DraftStreamEvent.Progress(DraftStreamStage.VALIDATING))
        when (fixtureScenario) {
            FakeCodexScenario.ERROR -> { emit(DraftStreamEvent.Failed(PlanStreamFailure.RUNTIME_ERROR)); return }
            FakeCodexScenario.CONTRACT_VIOLATION -> {
                // Exercise the same strict parser path that rejects an unknown fixture field.
                when (TripDraftParser.parseTripPlan(contractViolationFixture(request), request.startDate, request.endDate)) {
                    is ContractResult.Invalid -> emit(DraftStreamEvent.Failed(PlanStreamFailure.CONTRACT_REJECTED))
                    is ContractResult.Valid -> emit(DraftStreamEvent.Failed(PlanStreamFailure.RUNTIME_ERROR))
                }
                return
            }
            FakeCodexScenario.EMPTY -> { emit(DraftStreamEvent.Empty); emit(DraftStreamEvent.Completed); return }
            else -> Unit
        }
        emit(DraftStreamEvent.Progress(DraftStreamStage.GENERATING))
        if (fixtureScenario == FakeCodexScenario.STOPPED || stopRequested) { emit(DraftStreamEvent.Stopped); return }
        emit(DraftStreamEvent.Progress(DraftStreamStage.VALIDATING_RESULT))
        when (val result = TripDraftParser.parseTripPlan(planFixture(request), request.startDate, request.endDate)) {
            is ContractResult.Valid -> emit(DraftStreamEvent.TripPlanReady(result.value))
            is ContractResult.Invalid -> { emit(DraftStreamEvent.Failed(PlanStreamFailure.CONTRACT_REJECTED)); return }
        }
        if (fixtureScenario == FakeCodexScenario.LATE_COMPLETION) {
            emit(DraftStreamEvent.Stopped)
            // Deliberately malformed lifecycle: the ViewModel must ignore events after a terminal stop.
            when (val late = TripDraftParser.parseTripPlan(planFixture(request), request.startDate, request.endDate)) {
                is ContractResult.Valid -> emit(DraftStreamEvent.TripPlanReady(late.value))
                is ContractResult.Invalid -> Unit
            }
        }
        emit(DraftStreamEvent.Completed)
    }

    private suspend fun kotlinx.coroutines.flow.FlowCollector<DraftStreamEvent>.emitReservationFixture(request: TripPlanningRequest) {
        emit(DraftStreamEvent.Started)
        emit(DraftStreamEvent.Progress(DraftStreamStage.GENERATING))
        when (val result = TripDraftParser.parseTripPlan(planFixture(request, reservationOnly = true), request.startDate, request.endDate)) {
            is ContractResult.Valid -> emit(DraftStreamEvent.ReservationReady(result.value))
            is ContractResult.Invalid -> { emit(DraftStreamEvent.Failed(PlanStreamFailure.CONTRACT_REJECTED)); return }
        }
        emit(DraftStreamEvent.Completed)
    }
}

enum class FakeCodexScenario { NORMAL, EMPTY, CONTRACT_VIOLATION, STREAMING, STOPPED, LATE_COMPLETION, ERROR }

private fun planFixture(request: TripPlanningRequest, reservationOnly: Boolean = false): String {
    val itinerary = if (reservationOnly) "[]" else "[{\"date\":\"${request.startDate}\",\"items\":[{\"id\":\"day1-arrival\",\"title\":\"도착 후 동네 산책\",\"startMinute\":900,\"location\":\"${escape(request.destination)} 중심\",\"notes\":\"이동 시간을 고려한 여유 일정\"}]}]"
    val packing = if (reservationOnly) "[]" else "[{\"id\":\"pack-adapter\",\"title\":\"충전 어댑터\",\"quantity\":1,\"reason\":\"기기 충전\"}]"
    val preparation = if (reservationOnly) "[]" else "[{\"id\":\"prep-hours\",\"title\":\"운영 시간 재확인\",\"reason\":\"변동 가능성\"}]"
    val sources = if (reservationOnly) "[{\"id\":\"source-reservation\",\"title\":\"예약 안내\",\"url\":\"https://example.com/reservation\",\"relatedItemId\":\"reservation-stay\"}]" else "[{\"id\":\"source-arrival\",\"title\":\"동네 안내\",\"url\":\"https://example.com/guide\",\"relatedItemId\":\"day1-arrival\"},{\"id\":\"source-reservation\",\"title\":\"예약 안내\",\"url\":\"https://example.com/reservation\",\"relatedItemId\":\"reservation-stay\"}]"
    return """{"schema":"trippilot.trip-plan-draft","version":$CONTRACT_VERSION,"kind":"TRIP_PLAN","title":"${escape(request.destination)} 여행 초안","destination":"${escape(request.destination)}","startDate":"${request.startDate}","endDate":"${request.endDate}","days":$itinerary,"reservations":[{"id":"reservation-stay","type":"HOTEL","provider":"예시 숙소","confirmationCode":"FAKE-${request.startDate.replace("-", "")}","dateTime":"${request.startDate}T15:00","location":"${escape(request.destination)}","sourceUrl":"https://example.com/reservation"}],"packingSuggestions":$packing,"preparationSuggestions":$preparation,"sources":$sources,"assumptions":["가격과 운영 정보는 예약 전에 직접 확인해야 합니다."]}"""
}

private fun weatherFixture(request: TripPlanningRequest): String = """{"schema":"trippilot.weather-advisory-draft","version":$CONTRACT_VERSION,"kind":"WEATHER_ADVISORY","destination":"${escape(request.destination)}","dates":["${request.startDate}"],"summary":"테스트용 날씨 참고입니다. 실제 날씨가 아닙니다.","advisories":["출발 전 공식 예보를 직접 확인하세요."],"assumptions":["네트워크나 날씨 서비스를 호출하지 않은 fixture입니다."]}"""

private fun contractViolationFixture(request: TripPlanningRequest): String = planFixture(request).dropLast(1) + ",\"unexpected\":true}"

private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"")
