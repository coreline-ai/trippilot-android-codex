package io.trippilot.app.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.trippilot.app.core.data.db.TripPilotDatabase
import io.trippilot.app.core.data.db.PendingReservationShareEntity
import io.trippilot.app.core.data.db.CalendarActionEntity
import io.trippilot.app.core.model.CalendarActionStatus
import io.trippilot.app.core.model.RecheckResult
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
import java.util.UUID

class TripRepositoryInstrumentedTest {
    private lateinit var database: TripPilotDatabase
    private lateinit var repository: TripRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TripPilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = TripRepository(database, database.tripDao())
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun tripCrudChecksDuplicatesAndCascadesDependentRecords() = runBlocking {
        assertEquals(
            ValidationResult.Valid,
            repository.createTrip(TripInput("도쿄", "Tokyo", "2026-10-01", "2026-10-03", "Asia/Tokyo", TravelScope.INTERNATIONAL)),
        )
        val trip = repository.observeTrips().first().single()
        assertTrue(repository.observePreparation(trip.id).first().isNotEmpty())
        assertTrue(repository.observePacking(trip.id).first().isNotEmpty())

        assertEquals(ValidationResult.Valid, repository.addItinerary(trip, "하네다 도착", "2026-10-01", 9 * 60, "HND", ""))
        assertTrue(repository.addItinerary(trip, "기간 밖", "2026-10-04", null, "", "") is ValidationResult.Invalid)
        val itinerary = repository.observeItinerary(trip.id).first().single()

        assertEquals(ValidationResult.Valid, repository.addSource(trip.id, itinerary.id, "https://example.com/flight", "항공편"))
        assertTrue(repository.addSource(trip.id, itinerary.id, "https://example.com/flight", "중복") is ValidationResult.Invalid)
        val source = repository.observeSources(trip.id).first().single()
        assertEquals(ValidationResult.Valid, repository.recordRecheck(source.id, "2026-08-16", RecheckResult.UNCHANGED))
        assertEquals(ValidationResult.Valid, repository.recordRecheck(source.id, "2026-08-16", RecheckResult.CHANGED))
        assertEquals(1, repository.observeRechecks(source.id).first().size)
        assertEquals(ValidationResult.Valid, repository.recordRecheck(source.id, "2026-08-17", RecheckResult.UNCHANGED))
        assertEquals(2, repository.observeRechecks(source.id).first().size)
        database.tripDao().insertCalendarAction(
            CalendarActionEntity(UUID.randomUUID().toString(), trip.id, itinerary.id, CalendarActionStatus.REVIEW_REQUIRED, "local-marker", null),
        )

        repository.deleteItinerary(itinerary.id)
        assertTrue(repository.observeSources(trip.id).first().isEmpty())
        assertTrue(repository.observeRechecks(source.id).first().isEmpty())
        assertEquals(0, database.tripDao().calendarActionCount(itinerary.id))

        assertEquals(ValidationResult.Valid, repository.addReservation(trip.id, "FLIGHT", "항공사", "ABC-001", null, "", "https://example.com/booking"))
        assertTrue(repository.addReservation(trip.id, "FLIGHT", "항공사", "ABC-001", null, "", "https://example.com/other") is ValidationResult.Invalid)

        assertEquals(ValidationResult.Valid, repository.storeShareForTrip(trip.id, "예약 링크 https://example.com/booking"))
        val storedShare = repository.observeActiveShares(trip.id).first().single()
        repository.discardPendingShare(storedShare.id)
        assertTrue(repository.observeActiveShares(trip.id).first().isEmpty())
        database.tripDao().insertPendingShare(
            PendingReservationShareEntity(UUID.randomUUID().toString(), trip.id, "expired", 0, 1),
        )
        assertEquals(ValidationResult.Valid, repository.storeShareForTrip(trip.id, "fresh share"))
        assertEquals(1, repository.observeActiveShares(trip.id).first().size)

        repository.deleteTrip(trip.id)
        assertTrue(repository.observeTrips().first().isEmpty())
        assertTrue(repository.observeItinerary(trip.id).first().isEmpty())
        assertTrue(repository.observeSources(trip.id).first().isEmpty())
    }

    @Test
    fun scopeDefaultsOnlyRestoreMissingItemsAndKeepManualAndCompletedItems() = runBlocking {
        assertEquals(
            ValidationResult.Valid,
            repository.createTrip(TripInput("부산", "Busan", "2026-10-01", "2026-10-02", "Asia/Seoul", TravelScope.DOMESTIC)),
        )
        val trip = repository.observeTrips().first().single()
        val completedDefault = repository.observePreparation(trip.id).first().first { it.title == "일정과 예약 확인" }
        repository.togglePreparation(completedDefault)
        assertEquals(ValidationResult.Valid, repository.addPreparation(trip.id, "반려동물 돌봄 부탁"))
        val missing = repository.observePreparation(trip.id).first().first { it.title == "신분증" }
        repository.deletePreparation(missing.id)

        repository.applyMissingScopeDefaults(trip.id, TravelScope.DOMESTIC)
        val result = repository.observePreparation(trip.id).first()
        assertTrue(result.any { it.title == "신분증" })
        assertTrue(result.any { it.title == "반려동물 돌봄 부탁" })
        assertTrue(result.first { it.title == "일정과 예약 확인" }.status.name == "DONE")
    }

    @Test
    fun backupRoundTripCreatesIndependentNewTripWithNestedLocalRecords() = runBlocking {
        assertEquals(ValidationResult.Valid, repository.createTrip(TripInput("상하이", "Shanghai", "2026-12-01", "2026-12-03", "Asia/Shanghai", TravelScope.INTERNATIONAL)))
        val original = repository.observeTrips().first().single()
        assertEquals(ValidationResult.Valid, repository.addItinerary(original, "와이탄", "2026-12-01", 600, "Shanghai", ""))
        val itinerary = repository.observeItinerary(original.id).first().single()
        assertEquals(ValidationResult.Valid, repository.addSource(original.id, itinerary.id, "https://example.com/bund", "여행 안내"))
        assertEquals(ValidationResult.Valid, repository.addReservation(original.id, "HOTEL", "Hotel", "SH-001", null, "Shanghai", "https://example.com/hotel"))

        val encoded = TripBackupCodec.encode(repository.createBackupDocument())
        val restoredIds = repository.restoreAsNewCopies(TripBackupCodec.decodeForRestore(encoded).getOrThrow()).getOrThrow()
        val restored = repository.observeTrip(restoredIds.single()).first()!!
        assertTrue(restored.id != original.id)
        assertEquals(1, repository.observeItinerary(restored.id).first().size)
        assertEquals(1, repository.observeReservations(restored.id).first().size)
        assertEquals(1, repository.observeSources(restored.id).first().size)
    }
}
