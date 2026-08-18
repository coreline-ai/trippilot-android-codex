package io.trippilot.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startDate ASC, createdAtEpochMs DESC")
    fun observeTrips(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun observeTrip(tripId: String): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun tripById(tripId: String): TripEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrip(entity: TripEntity)

    @Query("UPDATE trips SET title = :title, destination = :destination, startDate = :startDate, endDate = :endDate, scope = :scope, notes = :notes, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :tripId")
    suspend fun updateTrip(
        tripId: String,
        title: String,
        destination: String,
        startDate: String,
        endDate: String,
        scope: io.trippilot.app.core.model.TravelScope,
        notes: String,
        updatedAtEpochMs: Long,
    )

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: String)

    @Query("UPDATE trips SET status = :status, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :tripId")
    suspend fun updateTripStatus(tripId: String, status: io.trippilot.app.core.model.TripStatus, updatedAtEpochMs: Long)

    @Query("SELECT * FROM itinerary_items WHERE tripId = :tripId ORDER BY date, startMinute, sortOrder")
    fun observeItinerary(tripId: String): Flow<List<ItineraryItemEntity>>

    @Query("SELECT * FROM itinerary_items WHERE tripId = :tripId ORDER BY date, startMinute, sortOrder")
    suspend fun itineraryForTrip(tripId: String): List<ItineraryItemEntity>

    @Query("SELECT COUNT(*) FROM itinerary_items WHERE tripId = :tripId AND (date < :startDate OR date > :endDate)")
    suspend fun itineraryOutsideDateRangeCount(tripId: String, startDate: String, endDate: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItinerary(entity: ItineraryItemEntity)

    @Query("UPDATE itinerary_items SET date = :date, startMinute = :startMinute, allDay = :allDay, title = :title, location = :location, notes = :notes, sortOrder = :sortOrder WHERE id = :itemId")
    suspend fun updateItinerary(
        itemId: String,
        date: String,
        startMinute: Int?,
        allDay: Boolean,
        title: String,
        location: String,
        notes: String,
        sortOrder: Int,
    )

    @Query("DELETE FROM itinerary_items WHERE id = :itemId")
    suspend fun deleteItinerary(itemId: String)

    @Query("DELETE FROM calendar_actions WHERE itineraryId = :itemId")
    suspend fun deleteCalendarActionsForItinerary(itemId: String)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCalendarAction(entity: CalendarActionEntity)

    @Query("SELECT * FROM calendar_actions WHERE tripId = :tripId ORDER BY itineraryId")
    fun observeCalendarActions(tripId: String): Flow<List<CalendarActionEntity>>

    @Query("SELECT * FROM calendar_actions WHERE itineraryId = :itineraryId LIMIT 1")
    suspend fun calendarActionForItinerary(itineraryId: String): CalendarActionEntity?

    @Query("UPDATE calendar_actions SET status = :status, failureReason = :failureReason WHERE itineraryId = :itineraryId")
    suspend fun updateCalendarAction(
        itineraryId: String,
        status: io.trippilot.app.core.model.CalendarActionStatus,
        failureReason: String?,
    )

    @Query("SELECT COUNT(*) FROM calendar_actions WHERE itineraryId = :itemId")
    suspend fun calendarActionCount(itemId: String): Int

    @Query("SELECT * FROM readiness_reminders WHERE tripId = :tripId LIMIT 1")
    fun observeReadinessReminder(tripId: String): Flow<ReadinessReminderEntity?>

    @Query("SELECT * FROM readiness_reminders WHERE tripId = :tripId LIMIT 1")
    suspend fun readinessReminderForTrip(tripId: String): ReadinessReminderEntity?

    @Query("SELECT * FROM readiness_reminders WHERE enabled = 1")
    suspend fun enabledReadinessReminders(): List<ReadinessReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReadinessReminder(entity: ReadinessReminderEntity)

    @Query("UPDATE readiness_reminders SET lastNotifiedDate = :date WHERE tripId = :tripId")
    suspend fun updateReminderLastNotified(tripId: String, date: String?)

    @Query("SELECT * FROM preparation_items WHERE tripId = :tripId ORDER BY status, createdAtEpochMs")
    fun observePreparation(tripId: String): Flow<List<PreparationItemEntity>>

    @Query("SELECT * FROM preparation_items WHERE tripId = :tripId")
    suspend fun preparationForTrip(tripId: String): List<PreparationItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPreparation(entity: PreparationItemEntity)

    @Query("SELECT title FROM preparation_items WHERE tripId = :tripId")
    suspend fun preparationTitles(tripId: String): List<String>

    @Query("UPDATE preparation_items SET status = :status WHERE id = :itemId")
    suspend fun updatePreparationStatus(itemId: String, status: io.trippilot.app.core.model.PreparationStatus)

    @Query("DELETE FROM preparation_items WHERE id = :itemId")
    suspend fun deletePreparation(itemId: String)

    @Query("SELECT * FROM packing_items WHERE tripId = :tripId ORDER BY isPacked, createdAtEpochMs")
    fun observePacking(tripId: String): Flow<List<PackingItemEntity>>

    @Query("SELECT * FROM packing_items WHERE tripId = :tripId")
    suspend fun packingForTrip(tripId: String): List<PackingItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPacking(entity: PackingItemEntity)

    @Query("SELECT title FROM packing_items WHERE tripId = :tripId")
    suspend fun packingTitles(tripId: String): List<String>

    @Query("UPDATE packing_items SET isPacked = :packed WHERE id = :itemId")
    suspend fun updatePackingState(itemId: String, packed: Boolean)

    @Query("DELETE FROM packing_items WHERE id = :itemId")
    suspend fun deletePacking(itemId: String)

    @Query("SELECT * FROM reservations WHERE tripId = :tripId ORDER BY dateTime, provider")
    fun observeReservations(tripId: String): Flow<List<ReservationEntity>>

    @Query("SELECT * FROM reservations WHERE tripId = :tripId")
    suspend fun reservationsForTrip(tripId: String): List<ReservationEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertReservation(entity: ReservationEntity)

    @Query("UPDATE reservations SET type = :type, provider = :provider, confirmationCode = :confirmationCode, dateTime = :dateTime, location = :location, url = :url, status = :status, notes = :notes WHERE id = :reservationId")
    suspend fun updateReservation(
        reservationId: String,
        type: String,
        provider: String,
        confirmationCode: String,
        dateTime: String?,
        location: String,
        url: String?,
        status: io.trippilot.app.core.model.ReservationStatus,
        notes: String,
    )

    @Query("SELECT COUNT(*) FROM reservations WHERE tripId = :tripId AND confirmationCode = :confirmationCode")
    suspend fun reservationCodeCount(tripId: String, confirmationCode: String): Int

    @Query("SELECT COUNT(*) FROM reservations WHERE tripId = :tripId AND confirmationCode = :confirmationCode AND id != :reservationId")
    suspend fun reservationCodeCountExcluding(tripId: String, confirmationCode: String, reservationId: String): Int

    @Query("SELECT COUNT(*) FROM reservations WHERE tripId = :tripId AND url = :url")
    suspend fun reservationUrlCount(tripId: String, url: String): Int

    @Query("SELECT COUNT(*) FROM reservations WHERE tripId = :tripId AND url = :url AND id != :reservationId")
    suspend fun reservationUrlCountExcluding(tripId: String, url: String, reservationId: String): Int

    @Query("DELETE FROM reservations WHERE id = :reservationId")
    suspend fun deleteReservation(reservationId: String)

    @Query("SELECT * FROM source_evidence WHERE tripId = :tripId ORDER BY title")
    fun observeSources(tripId: String): Flow<List<SourceEvidenceEntity>>

    @Query("SELECT * FROM source_evidence WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun sourcesForOwner(ownerType: io.trippilot.app.core.model.SourceOwnerType, ownerId: String): List<SourceEvidenceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSource(entity: SourceEvidenceEntity)

    @Query("UPDATE source_evidence SET title = :title, url = :url WHERE id = :sourceId")
    suspend fun updateSource(sourceId: String, title: String, url: String)

    @Query("SELECT COUNT(*) FROM source_evidence WHERE ownerType = :ownerType AND ownerId = :ownerId AND url = :url")
    suspend fun sourceUrlCount(
        ownerType: io.trippilot.app.core.model.SourceOwnerType,
        ownerId: String,
        url: String,
    ): Int

    @Query("SELECT COUNT(*) FROM source_evidence WHERE ownerType = :ownerType AND ownerId = :ownerId AND url = :url AND id != :sourceId")
    suspend fun sourceUrlCountExcluding(
        ownerType: io.trippilot.app.core.model.SourceOwnerType,
        ownerId: String,
        url: String,
        sourceId: String,
    ): Int

    @Query("SELECT COUNT(*) FROM source_evidence WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun sourceCount(ownerType: io.trippilot.app.core.model.SourceOwnerType, ownerId: String): Int

    @Query("DELETE FROM source_evidence WHERE id = :sourceId")
    suspend fun deleteSource(sourceId: String)

    @Query("DELETE FROM source_evidence WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteSourcesForOwner(ownerType: io.trippilot.app.core.model.SourceOwnerType, ownerId: String)

    @Query("UPDATE source_evidence SET lastCheckedAtEpochMs = :checkedAtEpochMs WHERE id = :sourceId")
    suspend fun updateSourceLastChecked(sourceId: String, checkedAtEpochMs: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecheck(entity: EvidenceRecheckEntity)

    @Query("SELECT * FROM evidence_rechecks WHERE evidenceId = :evidenceId ORDER BY checkedDate DESC")
    fun observeRechecks(evidenceId: String): Flow<List<EvidenceRecheckEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPendingShare(entity: PendingReservationShareEntity)

    @Query("SELECT * FROM pending_reservation_shares WHERE tripId = :tripId AND expiresAtEpochMs > :nowEpochMs ORDER BY createdAtEpochMs DESC")
    fun observeActiveShares(tripId: String, nowEpochMs: Long): Flow<List<PendingReservationShareEntity>>

    @Query("DELETE FROM pending_reservation_shares WHERE expiresAtEpochMs <= :nowEpochMs")
    suspend fun deleteExpiredShares(nowEpochMs: Long)

    @Query("DELETE FROM pending_reservation_shares WHERE id = :shareId")
    suspend fun consumeShare(shareId: String)

    @Query("SELECT * FROM safety_memos WHERE tripId = :tripId ORDER BY category, updatedAtEpochMs DESC")
    fun observeSafetyMemos(tripId: String): Flow<List<SafetyMemoEntity>>

    @Query("SELECT * FROM safety_memos WHERE tripId = :tripId")
    suspend fun safetyMemosForTrip(tripId: String): List<SafetyMemoEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSafetyMemo(entity: SafetyMemoEntity)

    @Query("UPDATE safety_memos SET category = :category, title = :title, note = :note, contactLabel = :contactLabel, contactValue = :contactValue, updatedAtEpochMs = :updatedAtEpochMs WHERE id = :memoId")
    suspend fun updateSafetyMemo(
        memoId: String,
        category: io.trippilot.app.core.model.SafetyCategory,
        title: String,
        note: String,
        contactLabel: String?,
        contactValue: String?,
        updatedAtEpochMs: Long,
    )

    @Query("DELETE FROM safety_memos WHERE id = :memoId")
    suspend fun deleteSafetyMemo(memoId: String)
}
