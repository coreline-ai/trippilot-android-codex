package io.trippilot.app.core.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tripPilotPreferences by preferencesDataStore(name = "trippilot_ui_preferences")

/** Stores only UI choices. Travel records, OAuth and AI content are deliberately excluded. */
@Singleton
class UiPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    private val readinessReminderEnabled = booleanPreferencesKey("readiness_reminder_enabled")

    val isReadinessReminderEnabled: Flow<Boolean> = context.tripPilotPreferences.data.map {
        it[readinessReminderEnabled] ?: false
    }

    suspend fun setReadinessReminderEnabled(enabled: Boolean) {
        context.tripPilotPreferences.edit { it[readinessReminderEnabled] = enabled }
    }
}
