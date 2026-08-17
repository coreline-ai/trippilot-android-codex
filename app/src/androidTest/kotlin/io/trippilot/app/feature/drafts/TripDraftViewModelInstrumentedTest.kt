package io.trippilot.app.feature.drafts

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.trippilot.app.core.codex.FakeCodexRuntime
import io.trippilot.app.core.codex.FakeCodexScenario
import io.trippilot.app.core.data.TripRepository
import io.trippilot.app.core.data.db.TripPilotDatabase
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.TripInput
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TripDraftViewModelInstrumentedTest {
    private lateinit var database: TripPilotDatabase
    private lateinit var repository: TripRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TripPilotDatabase::class.java).allowMainThreadQueries().build()
        repository = TripRepository(database, database.tripDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun fakeDraftReachesReviewOnlyStateBeforeAnyRoomWrite() = runBlocking {
        repository.createTrip(TripInput("서울", "Seoul", "2026-08-16", "2026-08-18", "Asia/Seoul", TravelScope.DOMESTIC))
        val trip = repository.observeTrips().first().single()
        val viewModel = TripDraftViewModel(repository, FakeCodexRuntime())

        viewModel.createPlan(trip, "도보 중심", io.trippilot.app.integration.codex.contract.TravelCompanion.SOLO, io.trippilot.app.integration.codex.contract.BudgetRange.FLEXIBLE, listOf("음식"))
        val review = withTimeout(5_000) { viewModel.state.filterIsInstance<DraftUiState.Review>().first() }

        assertEquals("Seoul 여행 초안", review.draft.title)
        assertEquals(0, repository.observeItinerary(trip.id).first().size)
        assertEquals(0, repository.observeReservations(trip.id).first().size)
    }

    @Test
    fun stoppedLateCompletionAndWeatherAdvisoryNeverWriteTravelRecords() = runBlocking {
        repository.createTrip(TripInput("서울", "Seoul", "2026-08-16", "2026-08-18", "Asia/Seoul", TravelScope.DOMESTIC))
        val trip = repository.observeTrips().first().single()
        val runtime = FakeCodexRuntime().also { it.setScenarioForTest(FakeCodexScenario.LATE_COMPLETION) }
        val viewModel = TripDraftViewModel(repository, runtime)

        viewModel.createPlan(trip, "도보 중심", io.trippilot.app.integration.codex.contract.TravelCompanion.SOLO, io.trippilot.app.integration.codex.contract.BudgetRange.FLEXIBLE, listOf("음식"))
        withTimeout(5_000) { viewModel.state.filterIsInstance<DraftUiState.Notice>().first() }
        assertEquals(0, repository.observeItinerary(trip.id).first().size)
        assertEquals(0, repository.observeReservations(trip.id).first().size)

        viewModel.createWeatherAdvisory(trip)
        withTimeout(5_000) { viewModel.weather.filterNotNull().first() }
        assertEquals(0, repository.observeItinerary(trip.id).first().size)
        assertEquals(0, repository.observeReservations(trip.id).first().size)
    }
}
