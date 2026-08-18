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
        SafetyMemoEntity::class,
    ],
    version = 3,
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

        /** Non-destructive v2 → v3 migration: nullable window column + safety memo table. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE preparation_items ADD COLUMN postTripWindow TEXT")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `safety_memos` (" +
                        "`id` TEXT NOT NULL, `tripId` TEXT NOT NULL, `category` TEXT NOT NULL, " +
                        "`title` TEXT NOT NULL, `note` TEXT NOT NULL, `contactLabel` TEXT, `contactValue` TEXT, " +
                        "`createdAtEpochMs` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), FOREIGN KEY(`tripId`) REFERENCES `trips`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )",
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_safety_memos_tripId` ON `safety_memos` (`tripId`)")
            }
        }
    }
}
