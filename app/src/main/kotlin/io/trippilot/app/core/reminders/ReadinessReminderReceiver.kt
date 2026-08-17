package io.trippilot.app.core.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Direct app receiver only: boot resyncs opted-in Room rows, due alarms re-evaluate before notifying. */
@AndroidEntryPoint
class ReadinessReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var coordinator: ReadinessReminderCoordinator

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DUE -> intent.getStringExtra(EXTRA_TRIP_ID)?.let { tripId -> coordinator.handleDue(tripId) }
                    Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> coordinator.resyncAllAfterBoot()
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DUE = "io.trippilot.app.action.READINESS_REMINDER_DUE"
        const val EXTRA_TRIP_ID = "trip_id"
    }
}
