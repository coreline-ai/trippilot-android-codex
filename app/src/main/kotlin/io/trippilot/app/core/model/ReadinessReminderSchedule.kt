package io.trippilot.app.core.model

import java.time.LocalDate
import java.time.ZonedDateTime

/** Computes the next opt-in alarm trigger without talking to AlarmManager or posting a notification. */
object ReadinessReminderSchedule {
    fun nextTrigger(
        now: ZonedDateTime,
        tripStart: LocalDate,
        hasIncompleteItems: Boolean,
        lastNotifiedOn: LocalDate?,
    ): ZonedDateTime? {
        if (!hasIncompleteItems) return null
        val today = now.toLocalDate()
        val firstEligible = tripStart.minusDays(7)
        val lastEligible = tripStart.minusDays(1)
        if (today.isAfter(lastEligible)) return null

        val targetDate = when {
            today.isBefore(firstEligible) -> firstEligible
            lastNotifiedOn == today -> today.plusDays(1)
            else -> today
        }
        if (targetDate.isAfter(lastEligible)) return null
        val nineAm = targetDate.atTime(9, 0).atZone(now.zone)
        return when {
            targetDate.isAfter(today) -> nineAm
            nineAm.isAfter(now) -> nineAm
            else -> now.plusMinutes(1)
        }
    }
}
