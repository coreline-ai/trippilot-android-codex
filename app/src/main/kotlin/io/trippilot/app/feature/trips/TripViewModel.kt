package io.trippilot.app.feature.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.trippilot.app.core.data.TripRepository
import io.trippilot.app.core.data.db.PackingItemEntity
import io.trippilot.app.core.data.db.PreparationItemEntity
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.model.RecheckResult
import io.trippilot.app.core.model.ChecklistGroup
import io.trippilot.app.core.model.ReservationStatus
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.TripInput
import io.trippilot.app.core.model.ValidationResult
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

@HiltViewModel
class TripViewModel @Inject constructor(
    private val repository: TripRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val trips = repository.observeTrips().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedTripId: StateFlow<String?> = savedStateHandle.getStateFlow("selected_trip_id", null)

    private val mutablePendingShareText = MutableStateFlow<String?>(null)
    val pendingShareText: StateFlow<String?> = mutablePendingShareText.asStateFlow()

    private val mutableMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = mutableMessage.asSharedFlow()

    fun selectTrip(tripId: String?) { savedStateHandle["selected_trip_id"] = tripId }
    fun observeTrip(tripId: String) = repository.observeTrip(tripId)
    fun observeItinerary(tripId: String) = repository.observeItinerary(tripId)
    fun observePreparation(tripId: String) = repository.observePreparation(tripId)
    fun observePacking(tripId: String) = repository.observePacking(tripId)
    fun observeReservations(tripId: String) = repository.observeReservations(tripId)
    fun observeSources(tripId: String) = repository.observeSources(tripId)
    fun observeActiveShares(tripId: String) = repository.observeActiveShares(tripId)

    fun createTrip(title: String, destination: String, startDate: String, endDate: String, scope: TravelScope) =
        viewModelScope.launch {
            report(repository.createTrip(TripInput(title, destination, startDate, endDate, "Asia/Seoul", scope)))
        }

    fun updateTrip(trip: TripEntity, title: String, destination: String, startDate: String, endDate: String, scope: TravelScope) =
        viewModelScope.launch {
            report(repository.updateTrip(trip, TripInput(title, destination, startDate, endDate, trip.timezone, scope, trip.notes)))
        }

    fun addItinerary(
        trip: TripEntity,
        title: String,
        date: String,
        hour: String,
        location: String,
        notes: String = "",
    ) = viewModelScope.launch {
        val minute = parseTime(hour)
        if (hour.isNotBlank() && minute == null) {
            mutableMessage.emit("시각은 HH:mm 형식으로 입력하세요.")
            return@launch
        }
        report(repository.addItinerary(trip, title, date, minute, location, notes))
    }

    fun deleteItinerary(itemId: String) = viewModelScope.launch { repository.deleteItinerary(itemId) }

    fun updateItinerary(
        trip: TripEntity,
        itemId: String,
        title: String,
        date: String,
        hour: String,
        location: String,
        notes: String = "",
    ) = viewModelScope.launch {
        val minute = parseTime(hour)
        if (hour.isNotBlank() && minute == null) {
            mutableMessage.emit("시각은 HH:mm 형식으로 입력하세요.")
            return@launch
        }
        report(repository.updateItinerary(trip, itemId, title, date, minute, location, notes))
    }

    fun addPreparation(tripId: String, title: String) = viewModelScope.launch { report(repository.addPreparation(tripId, title)) }
    fun addPacking(tripId: String, title: String, quantity: Int = 1) = viewModelScope.launch { report(repository.addPacking(tripId, title, quantity)) }
    fun addReservation(
        tripId: String,
        type: String,
        provider: String,
        confirmationCode: String,
        url: String,
        dateTime: String = "",
        location: String = "",
        status: ReservationStatus = ReservationStatus.DRAFT,
    ) = viewModelScope.launch {
        report(repository.addReservation(tripId, type, provider, confirmationCode, dateTime.ifBlank { null }, location, url.ifBlank { null }, status))
    }
    fun updateReservation(
        item: io.trippilot.app.core.data.db.ReservationEntity,
        type: String,
        provider: String,
        confirmationCode: String,
        url: String,
        dateTime: String,
        location: String,
        status: ReservationStatus,
    ) = viewModelScope.launch {
        report(repository.updateReservation(item.tripId, item.id, type, provider, confirmationCode, dateTime.ifBlank { null }, location, url.ifBlank { null }, status, item.notes))
    }
    fun addSource(tripId: String, itineraryId: String, title: String, url: String) = viewModelScope.launch {
        report(repository.addSource(tripId, itineraryId, url, title))
    }
    fun addReservationSource(tripId: String, reservationId: String, title: String, url: String) = viewModelScope.launch {
        report(repository.addReservationSource(tripId, reservationId, url, title))
    }
    fun deleteSource(sourceId: String) = viewModelScope.launch { repository.deleteSource(sourceId) }
    fun updateSource(source: io.trippilot.app.core.data.db.SourceEvidenceEntity, title: String, url: String) = viewModelScope.launch {
        report(repository.updateSource(source, title, url))
    }
    fun recordRecheck(sourceId: String, date: String, result: RecheckResult) = viewModelScope.launch {
        report(repository.recordRecheck(sourceId, date, result))
    }
    fun togglePreparation(item: PreparationItemEntity) = viewModelScope.launch { repository.togglePreparation(item) }
    fun skipPreparation(itemId: String) = viewModelScope.launch { repository.skipPreparation(itemId) }
    fun togglePacking(item: PackingItemEntity) = viewModelScope.launch { repository.togglePacking(item) }
    fun deletePreparation(itemId: String) = viewModelScope.launch { repository.deletePreparation(itemId) }
    fun deletePacking(itemId: String) = viewModelScope.launch { repository.deletePacking(itemId) }
    fun deleteReservation(itemId: String) = viewModelScope.launch { repository.deleteReservation(itemId) }
    fun applyScopeDefaults(tripId: String, scope: TravelScope) = viewModelScope.launch {
        repository.applyMissingScopeDefaults(tripId, scope)
        mutableMessage.emit("누락된 기본 항목만 추가했습니다.")
    }
    // Stable per-trip flow: recreating the cold Room flow on every recomposition
    // can resubscribe past an invalidation and leave a just-inserted memo invisible.
    private val safetyMemoFlows = mutableMapOf<String, kotlinx.coroutines.flow.Flow<List<io.trippilot.app.core.data.db.SafetyMemoEntity>>>()
    fun observeSafetyMemos(tripId: String): kotlinx.coroutines.flow.Flow<List<io.trippilot.app.core.data.db.SafetyMemoEntity>> =
        safetyMemoFlows.getOrPut(tripId) { repository.observeSafetyMemos(tripId) }
    fun addSafetyMemo(tripId: String, category: io.trippilot.app.core.model.SafetyCategory, title: String, note: String, contactLabel: String?, contactValue: String?) = viewModelScope.launch {
        report(repository.addSafetyMemo(tripId, category, title, note, contactLabel, contactValue))
    }
    fun updateSafetyMemo(memo: io.trippilot.app.core.data.db.SafetyMemoEntity) = viewModelScope.launch { report(repository.updateSafetyMemo(memo)) }
    fun deleteSafetyMemo(memoId: String) = viewModelScope.launch { repository.deleteSafetyMemo(memoId) }
    fun addSafetyMemoSource(memoId: String, tripId: String, title: String, url: String) = viewModelScope.launch {
        report(repository.addSafetyMemoSource(memoId, tripId, title, url))
    }
    fun applyPostTripPack(tripId: String, window: io.trippilot.app.core.model.PostTripWindow) = viewModelScope.launch {
        repository.applyPostTripPack(tripId, window)
    }
    fun addPostTripPreparation(tripId: String, title: String, window: io.trippilot.app.core.model.PostTripWindow) = viewModelScope.launch {
        report(repository.addPostTripPreparation(tripId, title, window))
    }
    fun applyOptionalReadinessPack(tripId: String, scope: TravelScope, group: ChecklistGroup) = viewModelScope.launch {
        repository.applyOptionalReadinessPack(tripId, scope, group)
        mutableMessage.emit("${group.label} 선택 팩의 누락 항목만 추가했습니다.")
    }
    fun deleteTrip(tripId: String) = viewModelScope.launch {
        repository.deleteTrip(tripId)
        savedStateHandle["selected_trip_id"] = null
    }

    fun receivePlainTextShare(value: String?) {
        val text = value?.trim()?.takeIf(String::isNotEmpty) ?: return
        if (mutablePendingShareText.value != text) mutablePendingShareText.value = text
    }

    fun savePendingShare(tripId: String) = viewModelScope.launch {
        report(repository.storeShareForTrip(tripId, mutablePendingShareText.value.orEmpty()))
        mutablePendingShareText.value = null
    }

    fun dismissPendingShare() { mutablePendingShareText.value = null }
    fun discardPendingShare(shareId: String) = viewModelScope.launch { repository.discardPendingShare(shareId) }

    private fun parseTime(value: String): Int? {
        val raw = value.trim()
        if (raw.isEmpty()) return null
        val parts = raw.split(":")
        val hours = parts.firstOrNull()?.toIntOrNull() ?: return null
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return if (hours in 0..23 && minutes in 0..59) hours * 60 + minutes else null
    }

    private suspend fun report(result: ValidationResult) {
        when (result) {
            ValidationResult.Valid -> Unit
            is ValidationResult.Invalid -> mutableMessage.emit(result.message)
        }
    }
}
