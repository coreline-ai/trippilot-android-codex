package io.trippilot.app.core.external

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.trippilot.app.core.data.TripRepository
import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.TripPilotDatabase
import io.trippilot.app.core.model.CalendarActionStatus
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.TripInput
import io.trippilot.app.core.model.ValidationResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CalendarWriteCoordinatorInstrumentedTest {
    private lateinit var database: TripPilotDatabase
    private lateinit var repository: TripRepository
    private lateinit var gateway: FakeCalendarGateway
    private lateinit var coordinator: CalendarWriteCoordinator

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TripPilotDatabase::class.java).allowMainThreadQueries().build()
        repository = TripRepository(database, database.tripDao())
        gateway = FakeCalendarGateway()
        coordinator = CalendarWriteCoordinator(database.tripDao(), gateway)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun calendarWritesNeedPermissionAndUseLedgerMarkerForIdempotentRetry() = runBlocking {
        assertEquals(ValidationResult.Valid, repository.createTrip(TripInput("서울", "Seoul", "2026-10-01", "2026-10-02", "Asia/Seoul", TravelScope.DOMESTIC)))
        val trip = repository.observeTrips().first().single()
        assertEquals(ValidationResult.Valid, repository.addItinerary(trip, "궁궐", "2026-10-01", 600, "Gyeongbokgung", ""))
        val item = repository.observeItinerary(trip.id).first().single()

        assertEquals(CalendarWriteResult.PermissionRequired, coordinator.executeApproved(trip, listOf(item)))
        assertEquals(0, database.tripDao().calendarActionCount(item.id))

        gateway.permission = true
        assertEquals(CalendarWriteResult.Completed(1, 0, 0), coordinator.executeApproved(trip, listOf(item)))
        assertEquals(1, gateway.insertCount)
        assertEquals(CalendarActionStatus.EXECUTED, database.tripDao().calendarActionForItinerary(item.id)?.status)

        assertEquals(CalendarWriteResult.Completed(0, 1, 0), coordinator.executeApproved(trip, listOf(item)))
        assertEquals(1, gateway.insertCount)
        assertTrue(gateway.markers.single().contains(item.id))
    }

    @Test
    fun failedProviderWriteLeavesRetryableLedgerWithoutPartialSuccess() = runBlocking {
        assertEquals(ValidationResult.Valid, repository.createTrip(TripInput("부산", "Busan", "2026-10-01", "2026-10-02", "Asia/Seoul", TravelScope.DOMESTIC)))
        val trip = repository.observeTrips().first().single()
        repository.addItinerary(trip, "해변", "2026-10-01", null, "Haeundae", "")
        val item = repository.observeItinerary(trip.id).first().single()
        gateway.permission = true
        gateway.failInsert = true

        assertEquals(CalendarWriteResult.Completed(0, 0, 1), coordinator.executeApproved(trip, listOf(item)))
        assertEquals(CalendarActionStatus.FAILED, database.tripDao().calendarActionForItinerary(item.id)?.status)

        gateway.failInsert = false
        assertEquals(CalendarWriteResult.Completed(1, 0, 0), coordinator.executeApproved(trip, listOf(item)))
        assertEquals(CalendarActionStatus.EXECUTED, database.tripDao().calendarActionForItinerary(item.id)?.status)
    }

    private class FakeCalendarGateway : CalendarGateway {
        var permission = false
        var failInsert = false
        var insertCount = 0
        val markers = mutableSetOf<String>()
        override fun hasWritePermission(): Boolean = permission
        override fun defaultWritableTarget(): CalendarTarget? = if (permission) CalendarTarget(1L, "Test calendar") else null
        override fun containsMarker(marker: String): Boolean = marker in markers
        override fun insert(trip: io.trippilot.app.core.data.db.TripEntity, item: ItineraryItemEntity, marker: String): Result<Unit> = runCatching {
            check(!failInsert) { "provider rejected" }
            insertCount++
            markers += marker
        }
    }
}
