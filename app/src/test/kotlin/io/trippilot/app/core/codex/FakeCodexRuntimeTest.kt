package io.trippilot.app.core.codex

import io.trippilot.app.integration.codex.contract.BudgetRange
import io.trippilot.app.integration.codex.contract.TravelCompanion
import io.trippilot.app.integration.codex.contract.TripPlanningRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeCodexRuntimeTest {
    private val request = TripPlanningRequest(
        destination = "도쿄", startDate = "2026-10-01", endDate = "2026-10-03",
        companion = TravelCompanion.SOLO, budget = BudgetRange.FLEXIBLE,
        interests = listOf("음식"), purpose = "검토용 여행 초안",
    )

    @Test
    fun `fake runtime is credential-free and exposes only a fixture model`() = runBlocking {
        val runtime = FakeCodexRuntime()

        assertEquals(RuntimeStatus.READY, runtime.runtimeStatus.value)
        assertEquals(AuthStatus.NOT_REQUIRED, runtime.authStatus.value)
        runtime.beginLogin()
        runtime.cancelLogin()
        runtime.refreshAfterBrowserReturn()
        assertEquals(AuthStatus.NOT_REQUIRED, runtime.authStatus.value)
        assertEquals(null, runtime.loginChallenge.value)
        assertEquals("fake-trip-planner", runtime.availableModels().single().id)
    }

    @Test
    fun `stream emits parsed draft without raw response content`() = runBlocking {
        val events = FakeCodexRuntime().createPlanStream(request).toList()

        assertTrue(events.first() is DraftStreamEvent.Started)
        assertTrue(events.any { it is DraftStreamEvent.Progress })
        assertTrue(events.any { it is DraftStreamEvent.TripPlanReady })
        assertEquals(DraftStreamEvent.Completed, events.last())
    }

    @Test
    fun `empty contract failure stop and late completion fixtures are explicit`() = runBlocking {
        val runtime = FakeCodexRuntime()
        runtime.setScenarioForTest(FakeCodexScenario.EMPTY)
        assertTrue(runtime.createPlanStream(request).toList().contains(DraftStreamEvent.Empty))

        runtime.setScenarioForTest(FakeCodexScenario.CONTRACT_VIOLATION)
        assertEquals(
            DraftStreamEvent.Failed(PlanStreamFailure.CONTRACT_REJECTED),
            runtime.createPlanStream(request).toList().last(),
        )

        runtime.setScenarioForTest(FakeCodexScenario.STOPPED)
        assertTrue(runtime.createPlanStream(request).toList().contains(DraftStreamEvent.Stopped))

        runtime.setScenarioForTest(FakeCodexScenario.LATE_COMPLETION)
        val lateEvents = runtime.createPlanStream(request).toList()
        val stoppedIndex = lateEvents.indexOf(DraftStreamEvent.Stopped)
        assertTrue(stoppedIndex >= 0 && lateEvents.drop(stoppedIndex + 1).any { it is DraftStreamEvent.TripPlanReady })
    }

    @Test
    fun `runtime error never becomes a draft`() = runBlocking {
        val runtime = FakeCodexRuntime()
        runtime.failRuntimeForTestOrPreview()

        assertEquals(
            listOf(DraftStreamEvent.Failed(PlanStreamFailure.RUNTIME_UNAVAILABLE)),
            runtime.createPlanStream(request).toList(),
        )
    }
}
