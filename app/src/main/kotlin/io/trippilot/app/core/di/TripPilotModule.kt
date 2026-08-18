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
import io.trippilot.app.core.codex.CodexRuntimePort
import io.trippilot.app.core.codex.FakeCodexRuntime
import io.trippilot.app.BuildConfig
import io.trippilot.app.integration.codex.alpine.AlpineCodexRuntime
import io.trippilot.app.core.external.AndroidCalendarGateway
import io.trippilot.app.core.external.CalendarGateway
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
    ).addMigrations(TripPilotDatabase.MIGRATION_1_2, TripPilotDatabase.MIGRATION_2_3).build()

    @Provides
    fun provideTripDao(database: TripPilotDatabase): TripDao = database.tripDao()

    @Provides
    @Singleton
    fun provideCodexRuntime(
        @ApplicationContext context: Context,
    ): CodexRuntimePort = if (BuildConfig.ALLOW_REAL_CODEX_OAUTH) {
        AlpineCodexRuntime(context)
    } else {
        // Debug and instrumentation builds must stay deterministic and never prompt for OAuth.
        FakeCodexRuntime()
    }

    @Provides
    @Singleton
    fun provideCalendarGateway(gateway: AndroidCalendarGateway): CalendarGateway = gateway
}
