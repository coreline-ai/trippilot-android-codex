package io.trippilot.app.core.model

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadinessReminderPolicyTest {
    private val today = LocalDate.parse("2026-08-16")

    @Test
    fun `allows one incomplete reminder from D7 through D1`() {
        assertTrue(ReadinessReminderPolicy.evaluate(today, today.plusDays(7), true, null).shouldNotifyToday)
        assertTrue(ReadinessReminderPolicy.evaluate(today, today.plusDays(1), true, null).shouldNotifyToday)
    }

    @Test
    fun `blocks completed passed outside range and repeated day`() {
        assertFalse(ReadinessReminderPolicy.evaluate(today, today.plusDays(7), false, null).shouldNotifyToday)
        assertFalse(ReadinessReminderPolicy.evaluate(today, today, true, null).shouldNotifyToday)
        assertFalse(ReadinessReminderPolicy.evaluate(today, today.plusDays(8), true, null).shouldNotifyToday)
        assertFalse(ReadinessReminderPolicy.evaluate(today, today.plusDays(2), true, today).shouldNotifyToday)
    }

    @Test
    fun `schedule reaches D7, respects daily limit, and stops after departure`() {
        val zone = ZoneId.of("Asia/Seoul")
        val start = LocalDate.of(2026, 10, 10)
        val beforeWindow = ZonedDateTime.of(2026, 10, 1, 12, 0, 0, 0, zone)
        assertEquals(
            ZonedDateTime.of(2026, 10, 3, 9, 0, 0, 0, zone),
            ReadinessReminderSchedule.nextTrigger(beforeWindow, start, true, null),
        )

        val afterNine = ZonedDateTime.of(2026, 10, 3, 10, 0, 0, 0, zone)
        assertEquals(afterNine.plusMinutes(1), ReadinessReminderSchedule.nextTrigger(afterNine, start, true, null))
        assertEquals(
            ZonedDateTime.of(2026, 10, 4, 9, 0, 0, 0, zone),
            ReadinessReminderSchedule.nextTrigger(afterNine, start, true, LocalDate.of(2026, 10, 3)),
        )
        assertEquals(
            null,
            ReadinessReminderSchedule.nextTrigger(ZonedDateTime.of(2026, 10, 10, 9, 0, 0, 0, zone), start, true, null),
        )
    }
}
