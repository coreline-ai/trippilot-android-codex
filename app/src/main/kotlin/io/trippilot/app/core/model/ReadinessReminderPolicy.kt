package io.trippilot.app.core.model

import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure scheduling policy only. Phase 2 never requests notification permission,
 * schedules Android work, or performs boot recovery.
 */
object ReadinessReminderPolicy {
    data class Decision(
        val shouldNotifyToday: Boolean,
        val reason: String,
    )

    fun evaluate(
        today: LocalDate,
        tripStart: LocalDate,
        hasIncompleteItems: Boolean,
        alreadyNotifiedOn: LocalDate?,
    ): Decision {
        val daysUntilTrip = java.time.temporal.ChronoUnit.DAYS.between(today, tripStart)
        return when {
            !hasIncompleteItems -> Decision(false, "NO_INCOMPLETE_ITEMS")
            daysUntilTrip !in 1L..7L -> Decision(false, "OUTSIDE_D7_TO_D1")
            alreadyNotifiedOn == today -> Decision(false, "DAILY_LIMIT_REACHED")
            else -> Decision(true, "READY")
        }
    }

    fun localDate(epochMs: Long, zoneId: ZoneId): LocalDate =
        java.time.Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
}
