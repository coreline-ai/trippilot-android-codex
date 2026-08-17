package io.trippilot.app.core.external

import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.TripEntity
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Local RFC 5545 writer. It accepts already selected local itinerary records only and never
 * opens a calendar, browser, network connection, or file URI by itself.
 */
object IcsCodec {
    const val MIME_TYPE = "text/calendar"

    fun encode(trip: TripEntity, selected: List<ItineraryItemEntity>): Result<String> = runCatching {
        require(selected.isNotEmpty()) { "내보낼 일정을 하나 이상 선택하세요." }
        require(selected.all { it.tripId == trip.id }) { "다른 여행의 일정은 함께 내보낼 수 없습니다." }
        val zone = ZoneId.of(trip.timezone)
        buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//TripPilot//Wayfinding Field Journal//KO")
            appendLine("CALSCALE:GREGORIAN")
            selected.sortedWith(compareBy<ItineraryItemEntity> { it.date }.thenBy { it.startMinute ?: Int.MAX_VALUE }.thenBy { it.id })
                .forEach { item -> appendEvent(this, trip, item, zone) }
            appendLine("END:VCALENDAR")
        }.let(::foldLines)
    }

    private fun appendEvent(target: StringBuilder, trip: TripEntity, item: ItineraryItemEntity, zone: ZoneId) {
        val date = LocalDate.parse(item.date)
        target.appendLine("BEGIN:VEVENT")
        target.appendLine("UID:${escape("trippilot-${trip.id}-${item.id}")}")
        target.appendLine("DTSTAMP:${DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").format(java.time.Instant.now().atZone(ZoneOffset.UTC))}")
        if (item.allDay || item.startMinute == null) {
            target.appendLine("DTSTART;VALUE=DATE:${date.format(DateTimeFormatter.BASIC_ISO_DATE)}")
            target.appendLine("DTEND;VALUE=DATE:${date.plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)}")
        } else {
            val start = date.atStartOfDay().plusMinutes(item.startMinute.toLong()).atZone(zone).withZoneSameInstant(ZoneOffset.UTC)
            val end = start.plusHours(1)
            val format = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            target.appendLine("DTSTART:${format.format(start)}")
            target.appendLine("DTEND:${format.format(end)}")
        }
        target.appendLine("SUMMARY:${escape(item.title)}")
        if (item.location.isNotBlank()) target.appendLine("LOCATION:${escape(item.location)}")
        val note = listOf("TripPilot exported itinerary", item.notes.takeIf { it.isNotBlank() })
            .filterNotNull()
            .joinToString("\\n")
        target.appendLine("DESCRIPTION:${escape(note)}")
        target.appendLine("END:VEVENT")
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")

    /** RFC 5545 line folding: keep every physical line at or below 75 UTF-8 octets. */
    internal fun foldLines(unfolded: String): String = unfolded
        .lineSequence()
        .filter { it.isNotEmpty() }
        .joinToString("\r\n", postfix = "\r\n") { foldLine(it) }

    private fun foldLine(line: String): String {
        val chunks = mutableListOf<String>()
        var current = StringBuilder()
        var bytes = 0
        line.forEach { character ->
            val charBytes = character.toString().toByteArray(StandardCharsets.UTF_8).size
            val limit = if (chunks.isEmpty()) 75 else 74 // continuation consumes one leading space.
            if (bytes + charBytes > limit && current.isNotEmpty()) {
                chunks += current.toString()
                current = StringBuilder()
                bytes = 0
            }
            current.append(character)
            bytes += charBytes
        }
        if (current.isNotEmpty()) chunks += current.toString()
        return chunks.mapIndexed { index, chunk -> if (index == 0) chunk else " $chunk" }.joinToString("\r\n")
    }
}
