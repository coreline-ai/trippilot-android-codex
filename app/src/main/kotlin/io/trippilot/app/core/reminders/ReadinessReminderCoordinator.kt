package io.trippilot.app.core.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.trippilot.app.R
import io.trippilot.app.MainActivity
import io.trippilot.app.core.data.db.ReadinessReminderEntity
import io.trippilot.app.core.data.db.TripDao
import io.trippilot.app.core.model.PreparationStatus
import io.trippilot.app.core.model.ReadinessReminderPolicy
import io.trippilot.app.core.model.ReadinessReminderSchedule
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * App-private opt-in reminder delivery. The Room reminder row is the source of truth; the Alarm
 * is only a replaceable trigger. No reminder is enabled or sent without a user action in UI.
 */
@Singleton
class ReadinessReminderCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TripDao,
) {
    suspend fun setEnabled(tripId: String, enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val existing = dao.readinessReminderForTrip(tripId)
        dao.upsertReadinessReminder(
            ReadinessReminderEntity(
                id = existing?.id ?: UUID.randomUUID().toString(), tripId = tripId, enabled = enabled,
                lastNotifiedDate = if (enabled) existing?.lastNotifiedDate else null,
            ),
        )
        if (enabled) syncTrip(tripId) else cancel(tripId)
        true
    }

    suspend fun syncTrip(tripId: String, now: ZonedDateTime = ZonedDateTime.now()): Unit = withContext(Dispatchers.IO) {
        val reminder = dao.readinessReminderForTrip(tripId) ?: return@withContext
        if (!reminder.enabled) {
            cancel(tripId)
            return@withContext
        }
        val trip = dao.tripById(tripId) ?: run {
            cancel(tripId)
            return@withContext
        }
        val zone = runCatching { ZoneId.of(trip.timezone) }.getOrDefault(ZoneId.systemDefault())
        val localNow = now.withZoneSameInstant(zone)
        val incomplete = dao.preparationForTrip(tripId).any { it.status == PreparationStatus.TODO } ||
            dao.packingForTrip(tripId).any { !it.isPacked }
        val trigger = ReadinessReminderSchedule.nextTrigger(
            now = localNow,
            tripStart = LocalDate.parse(trip.startDate),
            hasIncompleteItems = incomplete,
            lastNotifiedOn = reminder.lastNotifiedDate?.let(LocalDate::parse),
        )
        if (trigger == null) {
            cancel(tripId)
            return@withContext
        }
        schedule(tripId, trigger)
    }

    suspend fun handleDue(tripId: String, now: ZonedDateTime = ZonedDateTime.now()): Unit = withContext(Dispatchers.IO) {
        val reminder = dao.readinessReminderForTrip(tripId) ?: return@withContext
        if (!reminder.enabled) return@withContext
        val trip = dao.tripById(tripId) ?: return@withContext
        val zone = runCatching { ZoneId.of(trip.timezone) }.getOrDefault(ZoneId.systemDefault())
        val localNow = now.withZoneSameInstant(zone)
        val hasIncomplete = dao.preparationForTrip(tripId).any { it.status == PreparationStatus.TODO } ||
            dao.packingForTrip(tripId).any { !it.isPacked }
        val decision = ReadinessReminderPolicy.evaluate(
            localNow.toLocalDate(), LocalDate.parse(trip.startDate), hasIncomplete, reminder.lastNotifiedDate?.let(LocalDate::parse),
        )
        if (decision.shouldNotifyToday && notificationPermissionGranted()) {
            showNotification(tripId, trip.title)
            dao.updateReminderLastNotified(tripId, localNow.toLocalDate().toString())
        }
        syncTrip(tripId, localNow)
    }

    suspend fun resyncAllAfterBoot(): Unit = withContext(Dispatchers.IO) {
        dao.enabledReadinessReminders().forEach { syncTrip(it.tripId) }
    }

    fun notificationPermissionGranted(): Boolean = Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
        context, Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED

    private fun schedule(tripId: String, trigger: ZonedDateTime) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toInstant().toEpochMilli(), pendingIntent(tripId))
    }

    private fun cancel(tripId: String) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(tripId))
    }

    private fun pendingIntent(tripId: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        tripId.hashCode(),
        Intent(context, ReadinessReminderReceiver::class.java).setAction(ReadinessReminderReceiver.ACTION_DUE).putExtra(ReadinessReminderReceiver.EXTRA_TRIP_ID, tripId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun showNotification(tripId: String, title: String) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "여행 준비 알림", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "사용자가 켠 TripPilot 여행 준비 알림"
                },
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            tripId.hashCode(),
            context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?: Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.notify(
            tripId.hashCode(),
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("$title 준비 항목 확인")
                .setContentText("아직 완료하지 않은 준비 또는 짐 항목이 있습니다.")
                .setContentIntent(openApp)
                .setAutoCancel(true)
                .build(),
        )
    }

    private companion object {
        const val CHANNEL_ID = "trippilot_readiness"
    }
}
