package io.trippilot.app.tools

import io.trippilot.app.core.data.TripRepository
import io.trippilot.app.core.data.db.CalendarActionEntity
import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.PreparationItemEntity
import io.trippilot.app.core.data.db.ReadinessReminderEntity
import io.trippilot.app.core.data.db.TripPilotDatabase
import io.trippilot.app.core.model.CalendarActionStatus
import io.trippilot.app.core.model.ChecklistGroup
import io.trippilot.app.core.model.ItemOrigin
import io.trippilot.app.core.model.PostTripWindow
import io.trippilot.app.core.model.PreparationStatus
import io.trippilot.app.core.model.RecheckResult
import io.trippilot.app.core.model.ReservationStatus
import io.trippilot.app.core.model.SafetyCategory
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.TripInput
import io.trippilot.app.core.model.ValidationResult
import io.trippilot.app.core.model.TripStatus
import io.trippilot.app.core.data.db.ReservationEntity
import io.trippilot.app.core.data.db.SourceEvidenceEntity
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Fully populated example dataset shared by the manual seed tool and the
 * populated regression tests. Every content type the app can show is covered:
 * timed/all-day/end-time itinerary, three reservation statuses, sources for all
 * three owner types with a recheck record, every preparation origin/status and
 * post-trip window, packing with quantity variance, safety memos with contacts,
 * a reminder row, calendar action history, and a pending share.
 */
object ExampleTripData {
    const val JEJU_TITLE = "제주 치유 여행 4박 5일"
    const val BUSAN_TITLE = "부산 위크엔드 (지난 여행)"
    const val TOKYO_TITLE = "도쿄 겨울 여행"

    suspend fun seed(repository: TripRepository, database: TripPilotDatabase) {
        val dao = database.tripDao()
        listOf(JEJU_TITLE, BUSAN_TITLE, TOKYO_TITLE).forEach { title ->
            repository.observeTrips().first().filter { it.title == title }.forEach { repository.deleteTrip(it.id) }
        }

        seedJeju(repository, database)
        seedPastBusan(repository)
        seedTokyo(repository, database)
    }

    private suspend fun seedJeju(repository: TripRepository, database: TripPilotDatabase) {
        val dao = database.tripDao()
        repository.createTrip(TripInput(JEJU_TITLE, "제주", "2026-09-09", "2026-09-13", "Asia/Seoul", TravelScope.DOMESTIC, "친구와 함께하는 느긋한 힐링 여행"))
        val trip = repository.observeTrips().first().first { it.title == JEJU_TITLE }

        fun ok(result: ValidationResult) = check(result is ValidationResult.Valid) { "insert failed: $result" }

        // Timed itinerary with personal notes.
        listOf(
            Triple("2026-09-09", 9, "김포공항 출발") to Pair("김포공항 국내선", "모바일 탑승권 미리 확인"),
            Triple("2026-09-09", 12, "제주공항 도착 · 렌터카 픽업") to Pair("제주국제공항", "보증금 결제 카드는 시그니처 카드"),
            Triple("2026-09-09", 15, "함덕 해수욕장 산책") to Pair("함덕해수욕장", "해질녘 사진 찍기"),
            Triple("2026-09-10", 10, "성산일출봉 등반") to Pair("서귀포시 성산읍", "완등 1시간 30분 예상, 물 챙기기"),
            Triple("2026-09-10", 14, "우도 배편 일주") to Pair("성산포항", "배표 왕복으로 구매"),
            Triple("2026-09-11", 11, "카페 투어 (애월)") to Pair("애월 한담해안도로", "대기 없는 시간대 방문"),
            Triple("2026-09-13", 11, "공항 반납 · 귀가") to Pair("제주국제공항", "주유 후 반납"),
        ).forEach { (when_, place) ->
            ok(repository.addItinerary(trip, when_.third, when_.first, when_.second * 60, place.first, place.second))
        }

        // All-day item and an end-time item exercise the remaining itinerary shapes.
        dao.insertItinerary(
            ItineraryItemEntity(
                UUID.randomUUID().toString(), trip.id, "2026-09-12", null, null, true,
                "제주 올레길 12코스 (자유일정)", "구좌읍", "날씨 보고 코스 결정", Int.MAX_VALUE,
            ),
        )
        val dinner = dao.itineraryForTrip(trip.id).first { it.title == "카페 투어 (애월)" }.let { cafe ->
            ItineraryItemEntity(
                UUID.randomUUID().toString(), trip.id, "2026-09-11", 1110, 1200, false,
                "흑돼지 저녁", "제주시 노형동", "예약한 식당 확인", 5,
            )
        }
        dao.insertItinerary(dinner)

        // Reservations: confirmed pair plus draft and cancelled documents.
        ok(repository.addReservation(trip.id, "FLIGHT", "대한항공 KE1225", "KE1225-9A", "2026-09-09 09:50", "김포공항 (GMP) → 제주 (CJU)", "https://example.com/flight/KE1225", ReservationStatus.CONFIRMED))
        val hotel = repository.observeReservations(trip.id).first().let { list ->
            ok(repository.addReservation(trip.id, "LODGING", "애월 바다뷰 호텔", "JEJU-88012", "2026-09-09 15:00 체크인", "제주시 애월읍", "https://example.com/hotel/88012", ReservationStatus.CONFIRMED))
            list
        }
        ok(repository.addReservation(trip.id, "CAR", "제주렌터카", "RTC-2026-0912", "2026-09-09 12:30 픽업", "제주공항 1번 게이트", "https://example.com/renta/0912", ReservationStatus.CONFIRMED))
        ok(repository.addReservation(trip.id, "TOUR", "카약 투어 (미확정)", "KAYAK-PENDING", null, "표선 해변", null, ReservationStatus.DRAFT))
        ok(repository.addReservation(trip.id, "FLIGHT", "변경 전 항공편", "KE1224-OLD", "2026-09-09 07:10", "김포공항 → 제주", null, ReservationStatus.CANCELLED))

        // Sources for every owner type + a recheck history record.
        val sunrise = dao.itineraryForTrip(trip.id).first { it.title.contains("성산") }
        ok(repository.addSource(trip.id, sunrise.id, "https://example.com/guide/seongsan", "성산일출봉 공식 안내"))
        val hotelRow = repository.observeReservations(trip.id).first().first { it.provider.contains("호텔") }
        ok(repository.addReservationSource(trip.id, hotelRow.id, "https://example.com/hotel/88012/confirm", "호텔 예약 확인 페이지"))
        ok(repository.recordRecheck(
            repository.observeSources(trip.id).first().first { it.title.contains("성산") }.id,
            "2026-08-30", RecheckResult.UNCHANGED,
        ))

        // Preparation: required + three more optional packs + all post-trip windows + manual.
        repository.applyMissingScopeDefaults(trip.id, TravelScope.DOMESTIC)
        listOf(ChecklistGroup.MONEY_PAYMENT, ChecklistGroup.DOCUMENTS_ENTRY, ChecklistGroup.CONNECTIVITY_ELECTRONICS, ChecklistGroup.HEALTH_HYGIENE).forEach { group ->
            repository.applyOptionalReadinessPack(trip.id, TravelScope.DOMESTIC, group)
        }
        PostTripWindow.entries.forEach { repository.applyPostTripPack(trip.id, it) }
        ok(repository.addPostTripPreparation(trip.id, "친구들에게 선물 배부", PostTripWindow.LATER))
        // AI-origin row: the visible trace of an applied draft suggestion.
        dao.insertPreparation(
            PreparationItemEntity(
                UUID.randomUUID().toString(), trip.id, "우천 대비 접이식 우산 (AI 초안 반영)",
                PreparationStatus.TODO, ItemOrigin.AI, System.currentTimeMillis(),
            ),
        )

        // Status variety: some done, one skipped, 48h window finished.
        suspend fun complete(part: (PreparationItemEntity) -> Boolean) {
            repository.observePreparation(trip.id).first().filter(part).forEach {
                if (it.status == PreparationStatus.TODO) repository.togglePreparation(it)
            }
        }
        complete { it.title in setOf("일정과 예약 확인", "교통·숙소 예약 확인", "비상 연락처 오프라인 사본") }
        complete { it.postTripWindow == PostTripWindow.WITHIN_48_HOURS }
        repository.skipPreparation(repository.observePreparation(trip.id).first().first { it.status == PreparationStatus.TODO && it.postTripWindow == null }.id)

        // Packing: quantity variance and partial completion.
        repository.applyOptionalReadinessPack(trip.id, TravelScope.DOMESTIC, ChecklistGroup.CLOTHING_FIELD)
        repository.addPacking(trip.id, "선물용 귤 3박스", 3)
        repository.observePacking(trip.id).first().take(2).forEach { if (!it.isPacked) repository.togglePacking(it) }

        // Safety memos with contact values, one carrying a source.
        repository.addSafetyMemo(trip.id, SafetyCategory.PAYMENT, "카드사 해외이상거래 차단", "분실 시 앱에서 잠금 후 재발급 신청", "카드사 공식 앱", "app://card-official")
        repository.addSafetyMemo(trip.id, SafetyCategory.TRANSPORT_DELAY, "항공사 지연 안내", "결항 시 다음 편 재예약은 공식 앱에서", "대한항공 앱", "https://example.com/airline/koreanair")
        repository.observeSafetyMemos(trip.id).first().first { it.category == SafetyCategory.PAYMENT }.let { memo ->
            repository.addSafetyMemoSource(memo.id, trip.id, "카드사 분실 신고 안내", "https://example.com/card/lost")
        }

        // Reminder row (opt-in flag recorded locally) and calendar action history.
        dao.upsertReadinessReminder(ReadinessReminderEntity(UUID.randomUUID().toString(), trip.id, true, null))
        dao.insertCalendarAction(CalendarActionEntity(UUID.randomUUID().toString(), trip.id, sunrise.id, CalendarActionStatus.EXECUTED, "seongsan", null))
        dao.insertCalendarAction(CalendarActionEntity(UUID.randomUUID().toString(), trip.id, dinner.id, CalendarActionStatus.FAILED, "dinner", "쓰기 가능한 캘린더 없음"))

        // Pending shared reservation text (24h TTL surface).
        repository.storeShareForTrip(trip.id, "대한항공 KE1225 2026-09-09 09:50 GMP→CJU 확인번호 KE1225-9A")
    }

    private suspend fun seedPastBusan(repository: TripRepository) {
        repository.createTrip(TripInput(BUSAN_TITLE, "부산", "2026-08-14", "2026-08-16", "Asia/Seoul", TravelScope.DOMESTIC, "이미 다녀온 여행 — 귀국 후 정리 중"))
        val trip = repository.observeTrips().first().first { it.title == BUSAN_TITLE }
        repository.addItinerary(trip, "광안리 해변 산책", "2026-08-15", 17 * 60, "광안리", "야경 명소")
        repository.addReservation(trip.id, "TRAIN", "KTX 407", "KTX-407-B3", "2026-08-14 06:25", "서울역 → 부산역", null, ReservationStatus.CONFIRMED)
        repository.applyMissingScopeDefaults(trip.id, TravelScope.DOMESTIC)
        repository.applyPostTripPack(trip.id, PostTripWindow.WITHIN_ONE_WEEK)
        // One item still open so the ended trip shows the post-trip follow-up CTA.
    }

    private suspend fun seedTokyo(repository: TripRepository, database: TripPilotDatabase) {
        repository.createTrip(TripInput(TOKYO_TITLE, "도쿄", "2026-12-24", "2026-12-27", "Asia/Tokyo", TravelScope.INTERNATIONAL, "크리스마스 도쿄 야경 투어"))
        val trip = repository.observeTrips().first().first { it.title == TOKYO_TITLE }
        repository.applyMissingScopeDefaults(trip.id, TravelScope.INTERNATIONAL)
        repository.addReservation(trip.id, "FLIGHT", "JL098", "JL098-X2", "2026-12-24 09:20", "인천 (ICN) → 하네다 (HND)", "https://example.com/flight/JL098", ReservationStatus.CONFIRMED)
        repository.addSafetyMemo(trip.id, SafetyCategory.HEALTH, "여행자 보험 연락", "현지 진료 시 증권 확인 후 청구", "보험사 앱", "https://example.com/insurer/app")
        // Completed post-trip window + PLANNED status: the finished-journey shape
        // where the post stage reads COMPLETE instead of ACTION_REQUIRED.
        repository.applyPostTripPack(trip.id, PostTripWindow.LATER)
        repository.observePreparation(trip.id).first()
            .filter { it.postTripWindow == PostTripWindow.LATER && it.status == PreparationStatus.TODO }
            .forEach { repository.togglePreparation(it) }
        database.tripDao().updateTripStatus(trip.id, TripStatus.PLANNED, System.currentTimeMillis())
    }
}
