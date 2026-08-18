package io.trippilot.app.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.trippilot.app.core.data.db.TripPilotDatabase
import io.trippilot.app.core.data.db.PreparationItemEntity
import io.trippilot.app.core.data.db.PendingReservationShareEntity
import io.trippilot.app.core.data.db.CalendarActionEntity
import io.trippilot.app.core.model.CalendarActionStatus
import io.trippilot.app.core.model.RecheckResult
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.TripInput
import io.trippilot.app.core.model.ValidationResult
import io.trippilot.app.core.model.ItemOrigin
import io.trippilot.app.core.model.ChecklistGroup
import io.trippilot.app.core.model.PreparationStatus
import io.trippilot.app.core.model.ReadinessTemplateCatalog
import io.trippilot.app.integration.codex.contract.ApprovedDraftSelection
import io.trippilot.app.integration.codex.contract.ApprovedItineraryItem
import io.trippilot.app.integration.codex.contract.DraftPackingSuggestion
import io.trippilot.app.integration.codex.contract.DraftPreparationSuggestion
import io.trippilot.app.integration.codex.contract.DraftReservation
import io.trippilot.app.integration.codex.contract.ReservationType
import io.trippilot.app.integration.codex.contract.SourceCandidate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

    @Test
    fun approvedDraftIsAtomicIdempotentAndNeverCreatesCalendarActions() = runBlocking {
        assertEquals(ValidationResult.Valid, repository.createTrip(TripInput("도쿄", "Tokyo", "2026-10-01", "2026-10-03", "Asia/Tokyo", TravelScope.INTERNATIONAL)))
        val trip = repository.observeTrips().first().single()
        val selection = ApprovedDraftSelection(
            itinerary = listOf(ApprovedItineraryItem("walk", "2026-10-01", "시부야 산책", 600, "Shibuya", "여유 있게 이동")),
            reservations = listOf(DraftReservation("hotel", ReservationType.HOTEL, "Trip Hotel", "TP-2026", "2026-10-01T15:00", "Shibuya", "https://example.com/hotel")),
            preparation = listOf(DraftPreparationSuggestion("hours", "운영 시간 재확인", "변동 가능")),
            packing = listOf(DraftPackingSuggestion("adapter", "충전 어댑터", 1, "충전")),
            sources = listOf(
                SourceCandidate("walk-source", "산책 안내", "https://example.com/walk", "walk"),
                SourceCandidate("hotel-source", "숙소 안내", "https://example.com/hotel", "hotel"),
            ),
        )

        val first = repository.applyApprovedDraft(trip, selection) as DraftApplyResult.Applied
        assertEquals(1, first.itineraryAdded)
        assertEquals(1, first.reservationAdded)
        assertEquals(1, first.preparationAdded)
        assertEquals(1, first.packingAdded)
        assertEquals(2, first.sourceAdded)
        assertEquals(1, repository.observeItinerary(trip.id).first().size)
        assertEquals(1, repository.observeReservations(trip.id).first().size)
        assertTrue(repository.observePreparation(trip.id).first().any { it.title == "운영 시간 재확인" && it.origin == ItemOrigin.AI })
        assertTrue(repository.observePacking(trip.id).first().any { it.title == "충전 어댑터" && it.origin == ItemOrigin.AI })
        assertEquals(0, database.tripDao().calendarActionCount(repository.observeItinerary(trip.id).first().single().id))

        val repeated = repository.applyApprovedDraft(trip, selection) as DraftApplyResult.Applied
        assertEquals(0, repeated.itineraryAdded + repeated.reservationAdded + repeated.preparationAdded + repeated.packingAdded + repeated.sourceAdded)
        assertEquals(1, repository.observeItinerary(trip.id).first().size)
        assertEquals(1, repository.observeReservations(trip.id).first().size)
        assertEquals(2, repository.observeSources(trip.id).first().size)

        val invalid = selection.copy(itinerary = listOf(selection.itinerary.single().copy(date = "2026-09-30")))
        assertTrue(repository.applyApprovedDraft(trip, invalid) is DraftApplyResult.Rejected)
        assertEquals(1, repository.observeItinerary(trip.id).first().size)
        assertEquals(1, repository.observeReservations(trip.id).first().size)
    }

    @Test
    fun readinessTemplatesAreIdempotentAndPreserveExistingManualAiDoneAndSkippedRows() = runBlocking {
        assertEquals(
            ValidationResult.Valid,
            repository.createTrip(TripInput("제주", "Jeju", "2026-10-01", "2026-10-03", "Asia/Seoul", TravelScope.INTERNATIONAL)),
        )
        val trip = repository.observeTrips().first().single()
        val completed = repository.observePreparation(trip.id).first().first { it.origin == ItemOrigin.DEFAULT }
        repository.togglePreparation(completed)
        val skipped = repository.observePreparation(trip.id).first().first { it.id != completed.id }
        repository.skipPreparation(skipped.id)
        assertEquals(ValidationResult.Valid, repository.addPreparation(trip.id, "반려동물 돌봄 부탁"))
        database.tripDao().insertPreparation(
            PreparationItemEntity(
                id = UUID.randomUUID().toString(),
                tripId = trip.id,
                title = "AI가 제안한 개인 메모",
                status = PreparationStatus.TODO,
                origin = ItemOrigin.AI,
                createdAtEpochMs = 1L,
                templateId = null,
            ),
        )

        repository.applyMissingScopeDefaults(trip.id, TravelScope.INTERNATIONAL)
        val afterFirstApply = repository.observePreparation(trip.id).first()
        repository.applyMissingScopeDefaults(trip.id, TravelScope.INTERNATIONAL)
        val afterSecondApply = repository.observePreparation(trip.id).first()
        assertEquals(afterFirstApply.size, afterSecondApply.size)
        assertEquals(PreparationStatus.DONE, afterSecondApply.first { it.id == completed.id }.status)
        assertEquals(PreparationStatus.SKIPPED, afterSecondApply.first { it.id == skipped.id }.status)
        assertTrue(afterSecondApply.any { it.title == "반려동물 돌봄 부탁" && it.origin == ItemOrigin.MANUAL })
        assertTrue(afterSecondApply.any { it.title == "AI가 제안한 개인 메모" && it.origin == ItemOrigin.AI })
        assertTrue(afterSecondApply.any { it.templateId == "PASSPORT_VALIDITY_CHECK" })

        repository.applyOptionalReadinessPack(trip.id, TravelScope.INTERNATIONAL, ChecklistGroup.MONEY_PAYMENT)
        val optionalCount = repository.observePreparation(trip.id).first().size
        repository.applyOptionalReadinessPack(trip.id, TravelScope.INTERNATIONAL, ChecklistGroup.MONEY_PAYMENT)
        assertEquals(optionalCount, repository.observePreparation(trip.id).first().size)
        assertTrue(ReadinessTemplateCatalog.optionalItems(TravelScope.INTERNATIONAL, ChecklistGroup.MONEY_PAYMENT).isNotEmpty())
    }
    @Test
    fun safetyMemoObserveEmitsAfterInsert() = runBlocking {
        val input = TripInput("안전", "Busan", "2026-11-01", "2026-11-02", "Asia/Seoul", TravelScope.DOMESTIC)
        repository.createTrip(input)
        val trip = repository.observeTrips().first().single()

        // Subscribe first: the emission after insert proves live invalidation works.
        val emissions = mutableListOf<Int>()
        val job = kotlinx.coroutines.GlobalScope.launch {
            repository.observeSafetyMemos(trip.id).collect { emissions += it.size }
        }
        kotlinx.coroutines.delay(200)
        repository.addSafetyMemo(trip.id, io.trippilot.app.core.model.SafetyCategory.PAYMENT, "카드사", "메모", null, null)
        kotlinx.coroutines.delay(1_000)
        job.cancel()
        assertTrue("expected emission after insert, got $emissions", emissions.any { it == 1 })
    }
}
