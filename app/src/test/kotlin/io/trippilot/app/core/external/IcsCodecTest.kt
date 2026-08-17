package io.trippilot.app.core.external

import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.TripStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IcsCodecTest {
    private val trip = TripEntity(
        id = "trip-id", title = "도쿄 여행", destination = "Tokyo", startDate = "2026-10-01", endDate = "2026-10-03",
        timezone = "Asia/Tokyo", scope = TravelScope.INTERNATIONAL, status = TripStatus.DRAFT, notes = "", createdAtEpochMs = 0, updatedAtEpochMs = 0,
    )

    @Test
    fun `writes selected all-day and timed events using CRLF folded RFC5545 lines`() {
        val events = listOf(
            ItineraryItemEntity("all", trip.id, "2026-10-01", null, null, true, "하루 일정", "Tokyo", "", Int.MAX_VALUE),
            ItineraryItemEntity("timed", trip.id, "2026-10-02", 9 * 60, null, false, "긴 제목 ".repeat(30), "Shibuya", "메모", 9 * 60),
        )
        val ics = IcsCodec.encode(trip, events).getOrThrow()
        assertTrue(ics.contains("BEGIN:VCALENDAR\r\n"))
        assertTrue(ics.contains("DTSTART;VALUE=DATE:20261001"))
        assertTrue(ics.contains("DTSTART:20261002T000000Z"))
        assertTrue(ics.contains("\r\n "))
        ics.split("\r\n").filter(String::isNotEmpty).forEach { line ->
            assertTrue("line exceeds 75 octets: $line", line.toByteArray().size <= 75)
        }
    }

    @Test
    fun `rejects an empty selection and a record from another trip`() {
        assertTrue(IcsCodec.encode(trip, emptyList()).isFailure)
        val foreign = ItineraryItemEntity("foreign", "other-trip", "2026-10-01", null, null, true, "다른 여행", "", "", Int.MAX_VALUE)
        assertFalse(IcsCodec.encode(trip, listOf(foreign)).isSuccess)
    }
}
