package io.trippilot.app.tools

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.trippilot.app.core.data.TripRepository
import io.trippilot.app.core.data.db.TripPilotDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** Manual seed tool; see ExampleTripData for the dataset. */
@RunWith(AndroidJUnit4::class)
class ExampleTripSeed {
    @Test
    fun seedCompleteExampleTrips() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, TripPilotDatabase::class.java, "trippilot.db").build()
        ExampleTripData.seed(TripRepository(database, database.tripDao()), database)
        // The seeding instance is closed last; the app reads rows on next launch.
        database.close()
    }
}
