package io.trippilot.app.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.trippilot.app.core.data.db.TripDao
import io.trippilot.app.core.data.db.TripPilotDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TripPilotModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TripPilotDatabase = Room.databaseBuilder(
        context,
        TripPilotDatabase::class.java,
        "trippilot.db",
    ).build()

    @Provides
    fun provideTripDao(database: TripPilotDatabase): TripDao = database.tripDao()
}
