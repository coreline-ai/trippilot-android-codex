package io.trippilot.app.core.data

import io.trippilot.app.core.model.TravelScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripBackupCodecTest {
    @Test
    fun `valid v1 document restores to independent inputs without ids`() {
        val payload = """{"schema":"trippilot.trip-backup","version":1,"trips":[{"title":"서울","destination":"Seoul","startDate":"2026-10-01","endDate":"2026-10-03","timezone":"Asia/Seoul","scope":"DOMESTIC","notes":""}]}"""
        val decoded = TripBackupCodec.decodeForRestore(payload).getOrThrow()
        val inputs = TripBackupCodec.restoreInputs(decoded).getOrThrow()
        assertEquals(1, inputs.size)
        assertEquals(TravelScope.DOMESTIC, inputs.single().scope)
    }

    @Test
    fun `rejects incorrect schema version and oversized payload`() {
        assertFalse(TripBackupCodec.decode("""{"schema":"wrong","version":1,"trips":[]}""").isSuccess)
        assertFalse(TripBackupCodec.decode("""{"schema":"trippilot.trip-backup","version":2,"trips":[]}""").isSuccess)
        assertFalse(TripBackupCodec.decode("x".repeat(TripBackupCodec.MAX_BYTES + 1)).isSuccess)
    }

    @Test
    fun `legacy public schema maps only documented public trip fields`() {
        val legacySchema = "open" + "minis.trip-backup"
        val payload = """{"schema":"$legacySchema","version":1,"trips":[{"title":"부산","destination":"Busan","startDate":"2026-11-01","endDate":"2026-11-02","timezone":"Asia/Seoul","scope":"DOMESTIC","notes":""}]}"""
        assertTrue(TripBackupCodec.decodeForRestore(payload).isSuccess)
    }

    @Test
    fun `nested items reject invalid URLs and count limits`() {
        val withInvalidUrl = TripBackupDocument(
            trips = listOf(
                TripBackupTrip(
                    "서울", "Seoul", "2026-10-01", "2026-10-03", "Asia/Seoul", "DOMESTIC", "",
                    reservations = listOf(TripBackupReservation("HOTEL", "Hotel", "A-1", url = "file:///secret", status = "DRAFT")),
                ),
            ),
        )
        assertFalse(runCatching { TripBackupCodec.encode(withInvalidUrl) }.isSuccess)

        val tooMany = TripBackupDocument(
            trips = List(TripBackupCodec.MAX_TRIPS + 1) { TripBackupTrip("서울$it", "Seoul", "2026-10-01", "2026-10-03", "Asia/Seoul", "DOMESTIC", "") },
        )
        assertFalse(runCatching { TripBackupCodec.encode(tooMany) }.isSuccess)
    }
}
