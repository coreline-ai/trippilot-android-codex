package io.trippilot.app.core.external

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.TripEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarTarget(val id: Long, val displayName: String)

interface CalendarGateway {
    fun hasWritePermission(): Boolean
    fun defaultWritableTarget(): CalendarTarget?
    fun containsMarker(marker: String): Boolean
    fun insert(trip: TripEntity, item: ItineraryItemEntity, marker: String): Result<Unit>
}

/**
 * The only Calendar Provider implementation. It has no scheduling or automatic execution entry
 * point; [CalendarWriteCoordinator] invokes it only after UI confirmation and permission grant.
 */
@Singleton
class AndroidCalendarGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : CalendarGateway {
    override fun hasWritePermission(): Boolean = listOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR,
    ).all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun defaultWritableTarget(): CalendarTarget? {
        if (!hasWritePermission()) return null
        return runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME),
                "${CalendarContract.Calendars.VISIBLE}=1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL}>=?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars._ID} ASC",
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                CalendarTarget(cursor.getLong(0), cursor.getString(1).orEmpty().ifBlank { "기본 Calendar" })
            }
        }.getOrNull()
    }

    override fun containsMarker(marker: String): Boolean {
        if (!hasWritePermission()) return false
        return runCatching {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                arrayOf(CalendarContract.Events._ID),
                "${CalendarContract.Events.DESCRIPTION} LIKE ?",
                arrayOf("%$marker%"),
                null,
            )?.use { it.moveToFirst() } ?: false
        }.getOrDefault(false)
    }

    override fun insert(trip: TripEntity, item: ItineraryItemEntity, marker: String): Result<Unit> = runCatching {
        check(hasWritePermission()) { "Calendar 권한이 없습니다." }
        val target = requireNotNull(defaultWritableTarget()) { "쓰기 가능한 Calendar를 찾지 못했습니다." }
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, target.id)
            put(CalendarContract.Events.TITLE, item.title)
            put(CalendarContract.Events.DESCRIPTION, "TripPilot marker: $marker\n${item.notes}")
            put(CalendarContract.Events.EVENT_LOCATION, item.location)
            if (item.allDay || item.startMinute == null) {
                val date = LocalDate.parse(item.date)
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.DTSTART, date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                put(CalendarContract.Events.DTEND, date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            } else {
                val zone = ZoneId.of(trip.timezone)
                val start = LocalDate.parse(item.date).atStartOfDay().plusMinutes(item.startMinute.toLong()).atZone(zone)
                put(CalendarContract.Events.ALL_DAY, 0)
                put(CalendarContract.Events.DTSTART, start.toInstant().toEpochMilli())
                put(CalendarContract.Events.DTEND, start.plusHours(1).toInstant().toEpochMilli())
                put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            }
        }
        requireNotNull(context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)) { "Calendar가 이벤트 저장을 거부했습니다." }
    }
}
