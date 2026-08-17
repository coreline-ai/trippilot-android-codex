package io.trippilot.app.core.external

import io.trippilot.app.core.data.db.CalendarActionEntity
import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.TripDao
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.model.CalendarActionStatus
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CalendarWriteResult {
    data object PermissionRequired : CalendarWriteResult
    data object NoWritableCalendar : CalendarWriteResult
    data class Completed(val executed: Int, val alreadyPresent: Int, val failed: Int) : CalendarWriteResult
}

/** Persists an approval ledger around, but never before, an explicit UI-confirmed Calendar write. */
@Singleton
class CalendarWriteCoordinator @Inject constructor(
    private val dao: TripDao,
    private val gateway: CalendarGateway,
) {
    fun targetLabel(): String? = gateway.defaultWritableTarget()?.displayName

    suspend fun executeApproved(trip: TripEntity, selected: List<ItineraryItemEntity>): CalendarWriteResult {
        if (selected.isEmpty()) return CalendarWriteResult.Completed(0, 0, 0)
        if (!gateway.hasWritePermission()) return CalendarWriteResult.PermissionRequired
        if (gateway.defaultWritableTarget() == null) return CalendarWriteResult.NoWritableCalendar

        var executed = 0
        var alreadyPresent = 0
        var failed = 0
        selected.forEach { item ->
            if (item.tripId != trip.id) {
                failed++
                return@forEach
            }
            val marker = markerFor(trip.id, item.id)
            val existing = dao.calendarActionForItinerary(item.id)
            if (existing?.status == CalendarActionStatus.EXECUTED) {
                alreadyPresent++
                return@forEach
            }
            if (existing == null) {
                dao.insertCalendarAction(
                    CalendarActionEntity(
                        id = UUID.randomUUID().toString(), tripId = trip.id, itineraryId = item.id,
                        status = CalendarActionStatus.REVIEW_REQUIRED, eventMarker = marker, failureReason = null,
                    ),
                )
            }
            dao.updateCalendarAction(item.id, CalendarActionStatus.APPROVED, null)
            val outcome = runCatching {
                if (gateway.containsMarker(marker)) false else {
                    gateway.insert(trip, item, marker).getOrThrow()
                    true
                }
            }
            outcome.onSuccess { inserted ->
                dao.updateCalendarAction(item.id, CalendarActionStatus.EXECUTED, null)
                if (inserted) executed++ else alreadyPresent++
            }.onFailure { error ->
                dao.updateCalendarAction(item.id, CalendarActionStatus.FAILED, error.message?.take(160) ?: "Calendar 저장 실패")
                failed++
            }
        }
        return CalendarWriteResult.Completed(executed, alreadyPresent, failed)
    }

    fun markerFor(tripId: String, itineraryId: String): String = "trippilot:$tripId:$itineraryId"
}
