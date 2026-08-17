package io.trippilot.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 2,
    exportSchema = true,
)
@TypeConverters(TripConverters::class)
abstract class TripPilotDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        /** Non-destructive v1 → v2 migration. Existing item values remain untouched. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE preparation_items ADD COLUMN templateId TEXT")
                database.execSQL("ALTER TABLE packing_items ADD COLUMN templateId TEXT")
            }
        }
    }
}
