package io.trippilot.app.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.trippilot.app.core.model.CalendarActionStatus
import io.trippilot.app.core.model.ItemOrigin
import io.trippilot.app.core.model.PreparationStatus
import io.trippilot.app.core.model.RecheckResult
import io.trippilot.app.core.model.ReservationStatus
import io.trippilot.app.core.model.SourceOwnerType
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.TripStatus

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val timezone: String,
    val scope: TravelScope,
    val status: TripStatus,
    val notes: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)

@Entity(
    tableName = "itinerary_items",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class ItineraryItemEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val date: String,
    val startMinute: Int?,
    val endMinute: Int?,
    val allDay: Boolean,
    val title: String,
    val location: String,
    val notes: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "preparation_items",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class PreparationItemEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val title: String,
    val status: PreparationStatus,
    val origin: ItemOrigin,
    val createdAtEpochMs: Long,
    val templateId: String? = null,
)

@Entity(
    tableName = "packing_items",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class PackingItemEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val title: String,
    val quantity: Int,
    val isPacked: Boolean,
    val origin: ItemOrigin,
    val createdAtEpochMs: Long,
    val templateId: String? = null,
)

@Entity(
    tableName = "reservations",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class ReservationEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val type: String,
    val provider: String,
    val confirmationCode: String,
    val dateTime: String?,
    val location: String,
    val url: String?,
    val status: ReservationStatus,
    val notes: String,
)

@Entity(
    tableName = "source_evidence",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId"), Index(value = ["ownerType", "ownerId"])],
)
data class SourceEvidenceEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val ownerType: SourceOwnerType,
    val ownerId: String,
    val url: String,
    val title: String,
    val lastCheckedAtEpochMs: Long?,
)

@Entity(
    tableName = "evidence_rechecks",
    foreignKeys = [ForeignKey(
        entity = SourceEvidenceEntity::class,
        parentColumns = ["id"],
        childColumns = ["evidenceId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["evidenceId", "checkedDate"], unique = true)],
)
data class EvidenceRecheckEntity(
    @PrimaryKey val id: String,
    val evidenceId: String,
    val checkedDate: String,
    val result: RecheckResult,
    val checkedAtEpochMs: Long,
)

@Entity(
    tableName = "calendar_actions",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class CalendarActionEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val itineraryId: String,
    val status: CalendarActionStatus,
    val eventMarker: String,
    val failureReason: String?,
)

@Entity(
    tableName = "readiness_reminders",
    foreignKeys = [ForeignKey(
        entity = TripEntity::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class ReadinessReminderEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val enabled: Boolean,
    val lastNotifiedDate: String?,
)

@Entity(tableName = "pending_reservation_shares", indices = [Index("tripId")])
data class PendingReservationShareEntity(
    @PrimaryKey val id: String,
    val tripId: String,
    val sharedText: String,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
)
