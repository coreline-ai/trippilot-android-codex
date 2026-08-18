package io.trippilot.app.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.trippilot.app.core.data.db.TripPilotDatabase
import io.trippilot.app.core.model.PostTripWindow
import io.trippilot.app.core.model.PreparationStatus
import io.trippilot.app.core.model.ReservationStatus
import io.trippilot.app.core.model.SafetyCategory
import io.trippilot.app.tools.ExampleTripData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Full-dataset backup round trip: the complete example dataset must survive
 * encode → decode → restore-as-new-copies with statuses, windows, sources, and
 * safety memos intact. This is the data-persistence guarantee for users.
 */
@RunWith(AndroidJUnit4::class)
class BackupRoundTripInstrumentedTest {
    private lateinit var database: TripPilotDatabase
    private lateinit var repository: TripRepository

    @Before
    fun seed() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TripPilotDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = TripRepository(database, database.tripDao())
        ExampleTripData.seed(repository, database)
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun exampleDatasetSurvivesBackupRoundTrip() = runBlocking {
        val original = repository.observeTrips().first()
        assertEquals(3, original.size)

        val document = repository.createBackupDocument()
        val encoded = TripBackupCodec.encode(document)
        val decoded = TripBackupCodec.decode(encoded).getOrThrow()
        assertEquals(3, decoded.version)

        val restored = repository.restoreAsNewCopies(decoded).getOrThrow()
        assertEquals(3, restored.size)

        // Every original trip reappears with its full content under fresh IDs.
        original.forEach { source ->
            val copy = restored.zip(original).first { (_, o) -> o.id == source.id }.first
            val trip = repository.observeTrip(copy).first()!!
            assertEquals(source.title, trip.title)

            val itinerary = repository.observeItinerary(copy).first()
            assertEquals(repository.observeItinerary(source.id).first().size, itinerary.size)
            if (source.title == ExampleTripData.JEJU_TITLE) assertTrue(itinerary.any { it.allDay })

            val reservations = repository.observeReservations(copy).first()
            assertEquals(repository.observeReservations(source.id).first().size, reservations.size)
            assertTrue(reservations.any { it.status == ReservationStatus.CONFIRMED })

            val preparation = repository.observePreparation(copy).first()
            val sourcePreparation = repository.observePreparation(source.id).first()
            assertEquals(sourcePreparation.size, preparation.size)
            assertEquals(
                sourcePreparation.count { it.status == PreparationStatus.DONE },
                preparation.count { it.status == PreparationStatus.DONE },
            )
            assertEquals(
                sourcePreparation.count { it.postTripWindow != null },
                preparation.count { it.postTripWindow != null },
            )
            if (source.title == ExampleTripData.JEJU_TITLE) assertTrue(preparation.any { it.postTripWindow == PostTripWindow.WITHIN_48_HOURS })

            val packing = repository.observePacking(copy).first()
            assertEquals(repository.observePacking(source.id).first().size, packing.size)

            // v3 safety memos ride along; their sources are re-linked by the user.
            val memos = repository.observeSafetyMemos(copy).first()
            val sourceMemos = repository.observeSafetyMemos(source.id).first()
            assertEquals(sourceMemos.size, memos.size)
            if (source.title == ExampleTripData.JEJU_TITLE) {
                assertTrue(memos.any { it.category == SafetyCategory.PAYMENT && !it.contactValue.isNullOrBlank() })
            }
        }

        // Idempotence of the seed inside one DB: re-seeding replaces, not duplicates.
        ExampleTripData.seed(repository, database)
        assertEquals(3, repository.observeTrips().first().size)
    }
}
