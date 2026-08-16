package io.trippilot.app.core.data

import androidx.room.withTransaction
import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.PackingItemEntity
import io.trippilot.app.core.data.db.PendingReservationShareEntity
import io.trippilot.app.core.data.db.PreparationItemEntity
import io.trippilot.app.core.data.db.ReservationEntity
import io.trippilot.app.core.data.db.SourceEvidenceEntity
import io.trippilot.app.core.data.db.TripDao
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.data.db.TripPilotDatabase
import io.trippilot.app.core.model.ChecklistType
import io.trippilot.app.core.model.ItemOrigin
import io.trippilot.app.core.model.PreparationStatus
import io.trippilot.app.core.model.ReservationStatus
import io.trippilot.app.core.model.RecheckResult
import io.trippilot.app.core.model.SourceOwnerType
import io.trippilot.app.core.model.TravelScopeTemplates
import io.trippilot.app.core.model.TravelValidators
import io.trippilot.app.core.model.TripInput
import io.trippilot.app.core.model.TripStatus
import io.trippilot.app.core.model.ValidationResult
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class TripRepository @Inject constructor(
    private val database: TripPilotDatabase,
    private val dao: TripDao,
) {
    fun observeTrips(): Flow<List<TripEntity>> = dao.observeTrips()
    fun observeTrip(tripId: String): Flow<TripEntity?> = dao.observeTrip(tripId)
    fun observeItinerary(tripId: String): Flow<List<ItineraryItemEntity>> = dao.observeItinerary(tripId)
    fun observePreparation(tripId: String): Flow<List<PreparationItemEntity>> = dao.observePreparation(tripId)
    fun observePacking(tripId: String): Flow<List<PackingItemEntity>> = dao.observePacking(tripId)
    fun observeReservations(tripId: String): Flow<List<ReservationEntity>> = dao.observeReservations(tripId)
    fun observeSources(tripId: String): Flow<List<SourceEvidenceEntity>> = dao.observeSources(tripId)
    fun observeRechecks(evidenceId: String) = dao.observeRechecks(evidenceId)
    fun observeActiveShares(tripId: String) = dao.observeActiveShares(tripId, System.currentTimeMillis())

    suspend fun createTrip(input: TripInput): ValidationResult {
        val result = TravelValidators.trip(input)
        if (result is ValidationResult.Invalid) return result
        val now = System.currentTimeMillis()
        val tripId = UUID.randomUUID().toString()
        database.withTransaction {
            dao.insertTrip(
                TripEntity(
                    id = tripId,
                    title = input.title.trim(),
                    destination = input.destination.trim(),
                    startDate = input.startDate,
                    endDate = input.endDate,
                    timezone = input.timezone,
                    scope = input.scope,
                    status = TripStatus.DRAFT,
                    notes = input.notes.trim(),
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now,
                ),
            )
            addMissingTemplateItems(tripId, input.scope, now)
        }
        return ValidationResult.Valid
    }

    suspend fun addItinerary(
        trip: TripEntity,
        title: String,
        date: String,
        startMinute: Int?,
        location: String,
        notes: String,
    ): ValidationResult {
        val result = TravelValidators.itinerary(date, startMinute, null, trip.startDate, trip.endDate, title)
        if (result is ValidationResult.Invalid) return result
        dao.insertItinerary(
            ItineraryItemEntity(
                id = UUID.randomUUID().toString(),
                tripId = trip.id,
                date = date,
                startMinute = startMinute,
                endMinute = null,
                allDay = startMinute == null,
                title = title.trim(),
                location = location.trim(),
                notes = notes.trim(),
                sortOrder = (startMinute ?: Int.MAX_VALUE),
            ),
        )
        return ValidationResult.Valid
    }

    suspend fun updateItinerary(
        trip: TripEntity,
        itemId: String,
        title: String,
        date: String,
        startMinute: Int?,
        location: String,
        notes: String,
    ): ValidationResult {
        val result = TravelValidators.itinerary(date, startMinute, null, trip.startDate, trip.endDate, title)
        if (result is ValidationResult.Invalid) return result
        dao.updateItinerary(
            itemId = itemId,
            date = date,
            startMinute = startMinute,
            allDay = startMinute == null,
            title = title.trim(),
            location = location.trim(),
            notes = notes.trim(),
            sortOrder = startMinute ?: Int.MAX_VALUE,
        )
        return ValidationResult.Valid
    }

    suspend fun deleteItinerary(itemId: String) = database.withTransaction {
        dao.deleteSourcesForOwner(SourceOwnerType.ITINERARY, itemId)
        dao.deleteCalendarActionsForItinerary(itemId)
        dao.deleteItinerary(itemId)
    }

    suspend fun updateTrip(existing: TripEntity, input: TripInput): ValidationResult {
        val result = TravelValidators.trip(input)
        if (result is ValidationResult.Invalid) return result
        if (dao.itineraryOutsideDateRangeCount(existing.id, input.startDate, input.endDate) > 0) {
            return ValidationResult.Invalid("기존 일정이 새 여행 기간 밖에 있습니다. 일정을 먼저 수정하거나 삭제하세요.")
        }
        dao.updateTrip(
            tripId = existing.id,
            title = input.title.trim(),
            destination = input.destination.trim(),
            startDate = input.startDate,
            endDate = input.endDate,
            scope = input.scope,
            notes = input.notes.trim(),
            updatedAtEpochMs = System.currentTimeMillis(),
        )
        return ValidationResult.Valid
    }

    suspend fun addPreparation(tripId: String, title: String): ValidationResult = addChecklistItem(
        title = title,
        write = { normalized, now ->
            dao.insertPreparation(
                PreparationItemEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    title = normalized,
                    status = PreparationStatus.TODO,
                    origin = ItemOrigin.MANUAL,
                    createdAtEpochMs = now,
                ),
            )
        },
    )

    suspend fun addPacking(tripId: String, title: String, quantity: Int): ValidationResult {
        if (quantity < 1) return ValidationResult.Invalid("수량은 1 이상이어야 합니다.")
        return addChecklistItem(
        title = title,
        write = { normalized, now ->
            dao.insertPacking(
                PackingItemEntity(
                    id = UUID.randomUUID().toString(),
                    tripId = tripId,
                    title = normalized,
                    quantity = quantity.coerceAtLeast(1),
                    isPacked = false,
                    origin = ItemOrigin.MANUAL,
                    createdAtEpochMs = now,
                ),
            )
        },
        )
    }

    private suspend fun addChecklistItem(
        title: String,
        write: suspend (String, Long) -> Unit,
    ): ValidationResult {
        val normalized = title.trim()
        if (normalized.isEmpty()) return ValidationResult.Invalid("항목 이름을 입력하세요.")
        write(normalized, System.currentTimeMillis())
        return ValidationResult.Valid
    }

    suspend fun togglePreparation(item: PreparationItemEntity) {
        dao.updatePreparationStatus(
            item.id,
            if (item.status == PreparationStatus.DONE) PreparationStatus.TODO else PreparationStatus.DONE,
        )
    }

    suspend fun skipPreparation(itemId: String) = dao.updatePreparationStatus(itemId, PreparationStatus.SKIPPED)

    suspend fun deletePreparation(itemId: String) = dao.deletePreparation(itemId)

    suspend fun togglePacking(item: PackingItemEntity) = dao.updatePackingState(item.id, !item.isPacked)

    suspend fun deletePacking(itemId: String) = dao.deletePacking(itemId)

    suspend fun applyMissingScopeDefaults(tripId: String, scope: io.trippilot.app.core.model.TravelScope) {
        database.withTransaction { addMissingTemplateItems(tripId, scope, System.currentTimeMillis()) }
    }

    private suspend fun addMissingTemplateItems(tripId: String, scope: io.trippilot.app.core.model.TravelScope, now: Long) {
        // The default template never removes existing manual, completed, or AI items.
        val existingPreparation = dao.preparationTitles(tripId).map(String::trim).toSet()
        val existingPacking = dao.packingTitles(tripId).map(String::trim).toSet()
        TravelScopeTemplates.items(scope).forEach { item ->
            when (item.type) {
                ChecklistType.PREPARATION -> if (item.title !in existingPreparation) dao.insertPreparation(
                    PreparationItemEntity(
                        id = UUID.randomUUID().toString(), tripId = tripId, title = item.title,
                        status = PreparationStatus.TODO, origin = ItemOrigin.DEFAULT, createdAtEpochMs = now,
                    ),
                )
                ChecklistType.PACKING -> if (item.title !in existingPacking) dao.insertPacking(
                    PackingItemEntity(
                        id = UUID.randomUUID().toString(), tripId = tripId, title = item.title,
                        quantity = 1, isPacked = false, origin = ItemOrigin.DEFAULT, createdAtEpochMs = now,
                    ),
                )
            }
        }
    }

    suspend fun addReservation(
        tripId: String,
        type: String,
        provider: String,
        confirmationCode: String,
        dateTime: String?,
        location: String,
        url: String?,
        status: ReservationStatus = ReservationStatus.DRAFT,
        notes: String = "",
    ): ValidationResult {
        if (provider.trim().isEmpty() || confirmationCode.trim().isEmpty()) {
            return ValidationResult.Invalid("예약처와 확인번호를 입력하세요.")
        }
        if (!url.isNullOrBlank() && TravelValidators.url(url) is ValidationResult.Invalid) {
            return ValidationResult.Invalid("예약 URL이 올바르지 않습니다.")
        }
        if (dao.reservationCodeCount(tripId, confirmationCode.trim()) > 0) {
            return ValidationResult.Invalid("같은 여행에 이미 있는 확인번호입니다.")
        }
        if (!url.isNullOrBlank() && dao.reservationUrlCount(tripId, url.trim()) > 0) {
            return ValidationResult.Invalid("같은 여행에 이미 있는 예약 URL입니다.")
        }
        dao.insertReservation(
            ReservationEntity(
                id = UUID.randomUUID().toString(), tripId = tripId, type = type.ifBlank { "OTHER" },
                provider = provider.trim(), confirmationCode = confirmationCode.trim(), dateTime = dateTime?.trim()?.ifBlank { null },
                location = location.trim(), url = url?.trim()?.ifBlank { null }, status = status, notes = notes.trim(),
            ),
        )
        return ValidationResult.Valid
    }

    suspend fun updateReservation(
        tripId: String,
        reservationId: String,
        type: String,
        provider: String,
        confirmationCode: String,
        dateTime: String?,
        location: String,
        url: String?,
        status: ReservationStatus,
        notes: String,
    ): ValidationResult {
        if (provider.trim().isEmpty() || confirmationCode.trim().isEmpty()) {
            return ValidationResult.Invalid("예약처와 확인번호를 입력하세요.")
        }
        if (!url.isNullOrBlank() && TravelValidators.url(url) is ValidationResult.Invalid) {
            return ValidationResult.Invalid("예약 URL이 올바르지 않습니다.")
        }
        if (dao.reservationCodeCountExcluding(tripId, confirmationCode.trim(), reservationId) > 0) {
            return ValidationResult.Invalid("같은 여행에 이미 있는 확인번호입니다.")
        }
        if (!url.isNullOrBlank() && dao.reservationUrlCountExcluding(tripId, url.trim(), reservationId) > 0) {
            return ValidationResult.Invalid("같은 여행에 이미 있는 예약 URL입니다.")
        }
        dao.updateReservation(
            reservationId, type.ifBlank { "OTHER" }, provider.trim(), confirmationCode.trim(),
            dateTime?.trim()?.ifBlank { null }, location.trim(), url?.trim()?.ifBlank { null }, status, notes.trim(),
        )
        return ValidationResult.Valid
    }

    suspend fun deleteReservation(reservationId: String) = database.withTransaction {
        dao.deleteSourcesForOwner(SourceOwnerType.RESERVATION, reservationId)
        dao.deleteReservation(reservationId)
    }

    suspend fun addSource(
        tripId: String,
        itineraryId: String,
        url: String,
        title: String,
    ): ValidationResult {
        if (TravelValidators.url(url) is ValidationResult.Invalid) return ValidationResult.Invalid("출처 URL이 올바르지 않습니다.")
        if (title.trim().isEmpty()) return ValidationResult.Invalid("출처 제목을 입력하세요.")
        if (dao.sourceCount(SourceOwnerType.ITINERARY, itineraryId) >= 3) {
            return ValidationResult.Invalid("일정당 출처는 최대 3개입니다.")
        }
        if (dao.sourceUrlCount(SourceOwnerType.ITINERARY, itineraryId, url.trim()) > 0) {
            return ValidationResult.Invalid("같은 일정에 이미 있는 출처 URL입니다.")
        }
        dao.insertSource(
            SourceEvidenceEntity(
                id = UUID.randomUUID().toString(), tripId = tripId, ownerType = SourceOwnerType.ITINERARY,
                ownerId = itineraryId, url = url.trim(), title = title.trim(), lastCheckedAtEpochMs = null,
            ),
        )
        return ValidationResult.Valid
    }

    suspend fun addReservationSource(
        tripId: String,
        reservationId: String,
        url: String,
        title: String,
    ): ValidationResult {
        if (TravelValidators.url(url) is ValidationResult.Invalid) return ValidationResult.Invalid("출처 URL이 올바르지 않습니다.")
        if (title.trim().isEmpty()) return ValidationResult.Invalid("출처 제목을 입력하세요.")
        if (dao.sourceUrlCount(SourceOwnerType.RESERVATION, reservationId, url.trim()) > 0) {
            return ValidationResult.Invalid("같은 예약에 이미 있는 출처 URL입니다.")
        }
        dao.insertSource(
            SourceEvidenceEntity(
                id = UUID.randomUUID().toString(), tripId = tripId, ownerType = SourceOwnerType.RESERVATION,
                ownerId = reservationId, url = url.trim(), title = title.trim(), lastCheckedAtEpochMs = null,
            ),
        )
        return ValidationResult.Valid
    }

    suspend fun deleteSource(sourceId: String) = dao.deleteSource(sourceId)

    suspend fun updateSource(source: SourceEvidenceEntity, title: String, url: String): ValidationResult {
        if (TravelValidators.url(url) is ValidationResult.Invalid) return ValidationResult.Invalid("출처 URL이 올바르지 않습니다.")
        if (title.trim().isEmpty()) return ValidationResult.Invalid("출처 제목을 입력하세요.")
        if (dao.sourceUrlCountExcluding(source.ownerType, source.ownerId, url.trim(), source.id) > 0) {
            return ValidationResult.Invalid("같은 항목에 이미 있는 출처 URL입니다.")
        }
        dao.updateSource(source.id, title.trim(), url.trim())
        return ValidationResult.Valid
    }

    suspend fun recordRecheck(evidenceId: String, checkedDate: String, result: RecheckResult): ValidationResult {
        if (!checkedDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
            return ValidationResult.Invalid("확인일은 YYYY-MM-DD 형식으로 입력하세요.")
        }
        val now = System.currentTimeMillis()
        database.withTransaction {
            dao.upsertRecheck(
                io.trippilot.app.core.data.db.EvidenceRecheckEntity(
                    id = "$evidenceId:$checkedDate", evidenceId = evidenceId, checkedDate = checkedDate,
                    result = result, checkedAtEpochMs = now,
                ),
            )
            dao.updateSourceLastChecked(evidenceId, now)
        }
        return ValidationResult.Valid
    }

    suspend fun storeShareForTrip(tripId: String, sharedText: String): ValidationResult {
        val value = sharedText.trim()
        if (value.isEmpty()) return ValidationResult.Invalid("공유한 텍스트가 비어 있습니다.")
        val now = System.currentTimeMillis()
        dao.deleteExpiredShares(now)
        dao.insertPendingShare(
            PendingReservationShareEntity(
                id = UUID.randomUUID().toString(), tripId = tripId, sharedText = value,
                createdAtEpochMs = now, expiresAtEpochMs = now + 24 * 60 * 60 * 1000L,
            ),
        )
        return ValidationResult.Valid
    }

    suspend fun discardPendingShare(shareId: String) = dao.consumeShare(shareId)

    suspend fun createBackupDocument(): TripBackupDocument {
        val trips = dao.observeTrips().first()
        return TripBackupDocument(trips = trips.map { trip ->
            val itinerary = dao.observeItinerary(trip.id).first()
            val preparation = dao.observePreparation(trip.id).first()
            val packing = dao.observePacking(trip.id).first()
            val reservations = dao.observeReservations(trip.id).first()
            val itineraryIndex = itinerary.mapIndexed { index, item -> item.id to index }.toMap()
            val reservationIndex = reservations.mapIndexed { index, item -> item.id to index }.toMap()
            TripBackupTrip(
                title = trip.title, destination = trip.destination, startDate = trip.startDate, endDate = trip.endDate,
                timezone = trip.timezone, scope = trip.scope.name, notes = trip.notes,
                itinerary = itinerary.map { TripBackupItinerary(it.date, it.startMinute, it.title, it.location, it.notes) },
                preparation = preparation.map { TripBackupPreparation(it.title, it.status.name, it.origin.name) },
                packing = packing.map { TripBackupPacking(it.title, it.quantity, it.isPacked, it.origin.name) },
                reservations = reservations.map { TripBackupReservation(it.type, it.provider, it.confirmationCode, it.dateTime, it.location, it.url, it.status.name, it.notes) },
                sources = dao.observeSources(trip.id).first().mapNotNull { source ->
                    when (source.ownerType) {
                        SourceOwnerType.ITINERARY -> itineraryIndex[source.ownerId]?.let { TripBackupSource("ITINERARY", it, source.url, source.title) }
                        SourceOwnerType.RESERVATION -> reservationIndex[source.ownerId]?.let { TripBackupSource("RESERVATION", it, source.url, source.title) }
                    }
                },
            )
        })
    }

    /** Restores valid user-confirmed backup data as new IDs only; it never overwrites existing records. */
    suspend fun restoreAsNewCopies(document: TripBackupDocument): Result<List<String>> = runCatching {
        val inputs = TripBackupCodec.restoreInputs(document).getOrThrow()
        val restoredTripIds = mutableListOf<String>()
        database.withTransaction {
            document.trips.zip(inputs).forEach { (backup, input) ->
                val now = System.currentTimeMillis()
                val tripId = UUID.randomUUID().toString()
                dao.insertTrip(TripEntity(tripId, input.title.trim(), input.destination.trim(), input.startDate, input.endDate, input.timezone, input.scope, TripStatus.DRAFT, input.notes.trim(), now, now))
                val itineraryIds = backup.itinerary.map { item ->
                    UUID.randomUUID().toString().also { id ->
                        dao.insertItinerary(ItineraryItemEntity(id, tripId, item.date, item.startMinute, null, item.startMinute == null, item.title.trim(), item.location.trim(), item.notes.trim(), item.startMinute ?: Int.MAX_VALUE))
                    }
                }
                val reservationIds = backup.reservations.map { item ->
                    UUID.randomUUID().toString().also { id ->
                        dao.insertReservation(ReservationEntity(id, tripId, item.type, item.provider.trim(), item.confirmationCode.trim(), item.dateTime, item.location.trim(), item.url?.trim()?.ifBlank { null }, ReservationStatus.valueOf(item.status), item.notes.trim()))
                    }
                }
                backup.preparation.forEach { item ->
                    dao.insertPreparation(PreparationItemEntity(UUID.randomUUID().toString(), tripId, item.title.trim(), PreparationStatus.valueOf(item.status), ItemOrigin.valueOf(item.origin), now))
                }
                backup.packing.forEach { item ->
                    dao.insertPacking(PackingItemEntity(UUID.randomUUID().toString(), tripId, item.title.trim(), item.quantity, item.isPacked, ItemOrigin.valueOf(item.origin), now))
                }
                backup.sources.forEach { item ->
                    val ownerId = if (item.ownerType == "ITINERARY") itineraryIds[item.ownerIndex] else reservationIds[item.ownerIndex]
                    dao.insertSource(SourceEvidenceEntity(UUID.randomUUID().toString(), tripId, SourceOwnerType.valueOf(item.ownerType), ownerId, item.url.trim(), item.title.trim(), null))
                }
                restoredTripIds += tripId
            }
        }
        restoredTripIds
    }

    suspend fun deleteTrip(tripId: String) = dao.deleteTrip(tripId)
}
