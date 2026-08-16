package io.trippilot.app.core.model

import java.time.LocalDate
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
}
