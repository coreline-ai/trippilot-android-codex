package io.trippilot.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TripEntity::class,
        ItineraryItemEntity::class,
        PreparationItemEntity::class,
        PackingItemEntity::class,
        ReservationEntity::class,
        SourceEvidenceEntity::class,
        EvidenceRecheckEntity::class,
        CalendarActionEntity::class,
        ReadinessReminderEntity::class,
        PendingReservationShareEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(TripConverters::class)
abstract class TripPilotDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
}
