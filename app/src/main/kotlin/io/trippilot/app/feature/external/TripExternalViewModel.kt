package io.trippilot.app.feature.external

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.trippilot.app.core.data.TripRepository
import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.external.CalendarWriteCoordinator
import io.trippilot.app.core.external.CalendarWriteResult
import io.trippilot.app.core.reminders.ReadinessReminderCoordinator
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/** UI-facing effects only; all callers are wired from explicit confirmation buttons. */
@HiltViewModel
class TripExternalViewModel @Inject constructor(
    private val repository: TripRepository,
    private val calendarWrites: CalendarWriteCoordinator,
    private val reminders: ReadinessReminderCoordinator,
) : ViewModel() {
    private val mutableMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = mutableMessages.asSharedFlow()

    fun observeCalendarActions(tripId: String) = repository.observeCalendarActions(tripId)
    fun observeReminder(tripId: String) = repository.observeReadinessReminder(tripId)
    fun calendarTargetLabel(): String? = calendarWrites.targetLabel()
    fun notificationPermissionGranted(): Boolean = reminders.notificationPermissionGranted()

    fun writeSelectedToCalendar(trip: TripEntity, selected: List<ItineraryItemEntity>) = viewModelScope.launch {
        when (val result = calendarWrites.executeApproved(trip, selected)) {
            CalendarWriteResult.PermissionRequired -> mutableMessages.emit("Calendar 권한을 먼저 허용하세요. 권한만으로는 일정이 추가되지 않습니다.")
            CalendarWriteResult.NoWritableCalendar -> mutableMessages.emit("쓰기 가능한 Calendar를 찾지 못했습니다. 일정은 변경되지 않았습니다.")
            is CalendarWriteResult.Completed -> mutableMessages.emit(
                "Calendar 반영: ${result.executed}개 추가, ${result.alreadyPresent}개 중복 방지, ${result.failed}개 실패",
            )
        }
    }

    fun setReminderEnabled(tripId: String, enabled: Boolean) = viewModelScope.launch {
        reminders.setEnabled(tripId, enabled)
        mutableMessages.emit(if (enabled) "여행 준비 알림을 켰습니다. D-7부터 D-1까지 하루 한 번만 확인합니다." else "여행 준비 알림을 껐습니다.")
    }
}
