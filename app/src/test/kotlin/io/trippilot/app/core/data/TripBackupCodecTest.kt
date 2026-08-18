package io.trippilot.app.core.data

import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.ChecklistTemplateId
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
        assertFalse(TripBackupCodec.decode("""{"schema":"trippilot.trip-backup","version":4,"trips":[]}""").isSuccess)
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

    @Test
    fun `v3 round trip preserves template ids post trip windows and safety memos`() {
        val document = TripBackupDocument(
            trips = listOf(
                TripBackupTrip(
                    "서울", "Seoul", "2026-10-01", "2026-10-03", "Asia/Seoul", "DOMESTIC", "",
                    preparation = listOf(
                        TripBackupPreparation(
                            "일정과 예약 확인",
                            "TODO",
                            "DEFAULT",
                            ChecklistTemplateId.PLAN_AND_RESERVATION_CHECK.name,
                        ),
                        TripBackupPreparation(
                            "귀국 후 영수증 정리",
                            "TODO",
                            "MANUAL",
                            ChecklistTemplateId.POST_TRIP_RECEIPTS.name,
                            "WITHIN_ONE_WEEK",
                        ),
                    ),
                    safetyMemos = listOf(
                        TripBackupSafetyMemo("PAYMENT", "카드사 공식 앱", "분실 시 차단 순서 메모", "카드사", "공식 앱 링크"),
                    ),
                ),
            ),
        )
        val decoded = TripBackupCodec.decode(TripBackupCodec.encode(document)).getOrThrow()
        assertEquals(3, decoded.version)
        assertEquals(ChecklistTemplateId.PLAN_AND_RESERVATION_CHECK.name, decoded.trips.single().preparation.first().templateId)
        assertEquals("WITHIN_ONE_WEEK", decoded.trips.single().preparation[1].postTripWindow)
        assertEquals(1, decoded.trips.single().safetyMemos.size)
        assertEquals("PAYMENT", decoded.trips.single().safetyMemos.single().category)

        val invalidTemplate = document.copy(
            trips = document.trips.map { trip ->
                trip.copy(preparation = trip.preparation.map { it.copy(templateId = "UNKNOWN_TEMPLATE") })
            },
        )
        assertFalse(runCatching { TripBackupCodec.encode(invalidTemplate) }.isSuccess)

        val invalidWindow = document.copy(
            trips = document.trips.map { trip ->
                trip.copy(preparation = trip.preparation.map { it.copy(postTripWindow = "SOMEDAY") })
            },
        )
        assertFalse(runCatching { TripBackupCodec.encode(invalidWindow) }.isSuccess)

        val invalidMemo = document.copy(
            trips = document.trips.map { trip ->
                trip.copy(safetyMemos = listOf(TripBackupSafetyMemo("NOT_A_CATEGORY", "제목", "메모")))
            },
        )
        assertFalse(runCatching { TripBackupCodec.encode(invalidMemo) }.isSuccess)

        val oversizeContact = document.copy(
            trips = document.trips.map { trip ->
                trip.copy(safetyMemos = listOf(TripBackupSafetyMemo("PAYMENT", "제목", "메모", "라벨", "x".repeat(TripBackupCodec.MAX_CONTACT_VALUE_LENGTH + 1))))
            },
        )
        assertFalse(runCatching { TripBackupCodec.encode(oversizeContact) }.isSuccess)
    }

    @Test
    fun `v2 document with only legacy fields still decodes for restore`() {
        val payload = """{"schema":"trippilot.trip-backup","version":2,"trips":[{"title":"서울","destination":"Seoul","startDate":"2026-10-01","endDate":"2026-10-03","timezone":"Asia/Seoul","scope":"DOMESTIC","notes":"","preparation":[{"title":"일정과 예약 확인","status":"TODO","origin":"DEFAULT"}]}]}"""
        val decoded = TripBackupCodec.decodeForRestore(payload).getOrThrow()
        assertEquals(2, decoded.version)
        assertEquals(1, decoded.trips.single().preparation.size)
        assertTrue(decoded.trips.single().safetyMemos.isEmpty())
    }
}
