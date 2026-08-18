package io.trippilot.app.core.design

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JourneyStageCalculatorTest {
    @Test
    fun `one day trip keeps one real day and never invents completion`() {
        val stages = JourneyStageCalculator.calculate(
            startDate = "2026-09-10",
            endDate = "2026-09-10",
            itineraryCountByDate = emptyMap(),
            readinessTotal = 2,
            readinessPending = 2,
        )

        assertEquals(listOf("pre", "2026-09-10", "post"), stages.map { it.id })
        assertEquals(JourneyStageState.EMPTY, stages[1].state)
        assertEquals("일정 없음", stages[1].detail)
    }

    @Test
    fun `multi day trip reflects actual itinerary counts`() {
        val stages = JourneyStageCalculator.calculate(
            startDate = "2026-09-10",
            endDate = "2026-09-12",
            itineraryCountByDate = mapOf("2026-09-10" to 2, "2026-09-12" to 1),
            readinessTotal = 3,
            readinessPending = 0,
        )

        assertEquals(JourneyStageState.COMPLETE, stages.first().state)
        assertEquals("일정 2개", stages.first { it.id == "2026-09-10" }.detail)
        assertEquals(JourneyStageState.EMPTY, stages.first { it.id == "2026-09-11" }.state)
        assertEquals(JourneyStageState.PLANNED, stages.first { it.id == "2026-09-12" }.state)
    }

    @Test
    fun `selection follows before during and after trip dates`() {
        assertEquals("pre", JourneyStageCalculator.defaultSelectedId("2026-09-10", "2026-09-12", LocalDate.parse("2026-09-09")))
        assertEquals("2026-09-11", JourneyStageCalculator.defaultSelectedId("2026-09-10", "2026-09-12", LocalDate.parse("2026-09-11")))
        assertEquals("post", JourneyStageCalculator.defaultSelectedId("2026-09-10", "2026-09-12", LocalDate.parse("2026-09-13")))
    }

    @Test
    fun `empty readiness is stated as empty rather than complete`() {
        val pre = JourneyStageCalculator.calculate("2026-09-10", "2026-09-11", emptyMap(), 0, 0).first()
        assertEquals(JourneyStageState.EMPTY, pre.state)
        assertTrue(pre.detail.contains("없음"))
    }
    @Test
    fun `post stage stays neutral until the user opts into a window pack`() {
        val neutral = JourneyStageCalculator.calculate("2026-11-01", "2026-11-03", emptyMap(), 0, 0)
        val neutralPost = neutral.last()
        assertEquals(JourneyStageState.UPCOMING, neutralPost.state)
        assertEquals("선택 팩", neutralPost.detail)

        val pending = JourneyStageCalculator.calculate("2026-11-01", "2026-11-03", emptyMap(), 0, 0, postTripTotal = 4, postTripPending = 3)
        assertEquals(JourneyStageState.ACTION_REQUIRED, pending.last().state)
        assertEquals("정리 3개 남음", pending.last().detail)

        val done = JourneyStageCalculator.calculate("2026-11-01", "2026-11-03", emptyMap(), 0, 0, postTripTotal = 4, postTripPending = 0)
        assertEquals(JourneyStageState.COMPLETE, done.last().state)
    }
}
