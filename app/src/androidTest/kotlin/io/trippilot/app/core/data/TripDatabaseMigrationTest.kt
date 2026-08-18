package io.trippilot.app.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.trippilot.app.core.data.db.TripPilotDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TripDatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        TripPilotDatabase::class.java,
    )

    @Test
    fun migrationFromV1AddsNullableTemplateIdAndPreservesExistingChecklistFields() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                """
                INSERT INTO trips (id, title, destination, startDate, endDate, timezone, scope, status, notes, createdAtEpochMs, updatedAtEpochMs)
                VALUES ('trip-1', '기존 여행', 'Seoul', '2026-10-01', '2026-10-03', 'Asia/Seoul', 'DOMESTIC', 'DRAFT', '', 10, 11)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO preparation_items (id, tripId, title, status, origin, createdAtEpochMs)
                VALUES ('prep-1', 'trip-1', '일정과 예약 확인', 'DONE', 'DEFAULT', 12)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO packing_items (id, tripId, title, quantity, isPacked, origin, createdAtEpochMs)
                VALUES ('pack-1', 'trip-1', '충전기', 2, 1, 'MANUAL', 13)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 2, true, TripPilotDatabase.MIGRATION_1_2)
        migrated.query("SELECT title, status, origin, templateId FROM preparation_items WHERE id = 'prep-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("일정과 예약 확인", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals("DONE", cursor.getString(cursor.getColumnIndexOrThrow("status")))
            assertEquals("DEFAULT", cursor.getString(cursor.getColumnIndexOrThrow("origin")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("templateId")))
        }
        migrated.query("SELECT title, quantity, isPacked, origin, templateId FROM packing_items WHERE id = 'pack-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("충전기", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(2, cursor.getInt(cursor.getColumnIndexOrThrow("quantity")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("isPacked")))
            assertEquals("MANUAL", cursor.getString(cursor.getColumnIndexOrThrow("origin")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("templateId")))
        }
        migrated.close()
    }

    @Test
    fun migrationFromV2AddsPostTripWindowAndSafetyMemosWithoutTouchingExistingRows() {
        helper.createDatabase(DB_NAME, 2).apply {
            execSQL(
                """
                INSERT INTO trips (id, title, destination, startDate, endDate, timezone, scope, status, notes, createdAtEpochMs, updatedAtEpochMs)
                VALUES ('trip-2', '기존 여행', 'Busan', '2026-11-01', '2026-11-03', 'Asia/Seoul', 'DOMESTIC', 'DRAFT', '', 20, 21)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO preparation_items (id, tripId, title, status, origin, createdAtEpochMs, templateId)
                VALUES ('prep-2', 'trip-2', '귀국 후 영수증 정리', 'TODO', 'MANUAL', 22, 'POST_TRIP_RECEIPTS')
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO packing_items (id, tripId, title, quantity, isPacked, origin, createdAtEpochMs, templateId)
                VALUES ('pack-2', 'trip-2', '충전기', 1, 0, 'DEFAULT', 23, 'DEVICE_CHARGER')
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(DB_NAME, 3, true, TripPilotDatabase.MIGRATION_2_3)
        migrated.query("SELECT title, status, origin, templateId, postTripWindow FROM preparation_items WHERE id = 'prep-2'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("귀국 후 영수증 정리", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals("TODO", cursor.getString(cursor.getColumnIndexOrThrow("status")))
            assertEquals("MANUAL", cursor.getString(cursor.getColumnIndexOrThrow("origin")))
            assertEquals("POST_TRIP_RECEIPTS", cursor.getString(cursor.getColumnIndexOrThrow("templateId")))
            assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("postTripWindow")))
        }
        migrated.query("SELECT title, quantity, isPacked FROM packing_items WHERE id = 'pack-2'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("충전기", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("quantity")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("isPacked")))
        }
        // New table exists and starts empty; the trip foreign key still holds.
        migrated.query("SELECT COUNT(*) FROM safety_memos").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.execSQL(
            """
            INSERT INTO safety_memos (id, tripId, category, title, note, contactLabel, contactValue, createdAtEpochMs, updatedAtEpochMs)
            VALUES ('memo-1', 'trip-2', 'PAYMENT', '카드사 앱', '분실 시 공식 앱에서 차단', '카드사', 'app link', 30, 31)
            """.trimIndent(),
        )
        migrated.query("SELECT category, title, contactValue FROM safety_memos WHERE id = 'memo-1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("PAYMENT", cursor.getString(cursor.getColumnIndexOrThrow("category")))
            assertEquals("카드사 앱", cursor.getString(cursor.getColumnIndexOrThrow("title")))
            assertEquals("app link", cursor.getString(cursor.getColumnIndexOrThrow("contactValue")))
        }
        migrated.close()
    }

    /**
     * The migration fixture is deliberately isolated from the user's TripPilot
     * database. Removing it makes this test safe to run on a physical device
     * without leaving a test-only database beside the real local itinerary.
     */
    @After
    fun removeIsolatedMigrationFixture() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(DB_NAME)
    }

    private companion object {
        const val DB_NAME = "migration-test-db"
    }
}
