package io.trippilot.app.feature.external

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.trippilot.app.core.data.TripBackupCodec
import io.trippilot.app.core.data.TripBackupDocument
import io.trippilot.app.core.data.TripRepository
import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.external.IcsCodec
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FileWriteRequest(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val content: String,
)

data class BackupImportReview(
    val document: TripBackupDocument,
    val tripCount: Int,
    val itemCount: Int,
)

/**
 * Holds file bytes only in volatile ViewModel memory between an explicit confirmation and the
 * system SAF picker. It never stores URIs, raw backup content, or imports in Room/DataStore.
 */
@HiltViewModel
class TripFileViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TripRepository,
) : ViewModel() {
    private val mutableWriteRequest = MutableStateFlow<FileWriteRequest?>(null)
    val writeRequest: StateFlow<FileWriteRequest?> = mutableWriteRequest.asStateFlow()

    private val mutableImportReview = MutableStateFlow<BackupImportReview?>(null)
    val importReview: StateFlow<BackupImportReview?> = mutableImportReview.asStateFlow()

    private val mutableMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = mutableMessages.asSharedFlow()

    fun prepareBackupExport() = viewModelScope.launch {
        runCatching { TripBackupCodec.encode(repository.createBackupDocument()) }
            .onSuccess { content ->
                mutableWriteRequest.value = FileWriteRequest(
                    id = UUID.randomUUID().toString(), displayName = "trippilot-backup.json",
                    mimeType = "application/json", content = content,
                )
            }
            .onFailure { mutableMessages.emit(it.message ?: "백업을 만들지 못했습니다.") }
    }

    fun prepareIcsExport(trip: TripEntity, selected: List<ItineraryItemEntity>) = viewModelScope.launch {
        IcsCodec.encode(trip, selected)
            .onSuccess { content ->
                mutableWriteRequest.value = FileWriteRequest(
                    id = UUID.randomUUID().toString(), displayName = "${safeFileName(trip.title)}-itinerary.ics",
                    mimeType = IcsCodec.MIME_TYPE, content = content,
                )
            }
            .onFailure { mutableMessages.emit(it.message ?: "ICS 파일을 만들지 못했습니다.") }
    }

    fun writeSelectedDocument(uri: Uri) = viewModelScope.launch {
        val request = mutableWriteRequest.value ?: return@launch
        runCatching {
            requireNotNull(context.contentResolver.openOutputStream(uri, "w")) { "선택한 파일에 쓸 수 없습니다." }
                .bufferedWriter(Charsets.UTF_8).use { it.write(request.content) }
        }.onSuccess {
            mutableMessages.emit("파일을 저장했습니다.")
        }.onFailure {
            mutableMessages.emit(it.message ?: "파일 저장을 취소했거나 실패했습니다.")
        }
        mutableWriteRequest.value = null
    }

    fun cancelFileWrite() { mutableWriteRequest.value = null }

    fun inspectSelectedBackup(uri: Uri) = viewModelScope.launch {
        val payload = runCatching {
            requireNotNull(context.contentResolver.openInputStream(uri)) { "선택한 파일을 읽을 수 없습니다." }
                .use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        require(output.size() <= TripBackupCodec.MAX_BYTES) { "백업 파일이 2MB 제한을 초과합니다." }
                    }
                    output.toString(Charsets.UTF_8.name())
                }
        }.getOrElse {
            mutableMessages.emit(it.message ?: "백업 파일을 읽지 못했습니다.")
            return@launch
        }
        TripBackupCodec.decodeForRestore(payload)
            .onSuccess { document ->
                mutableImportReview.value = BackupImportReview(
                    document = document,
                    tripCount = document.trips.size,
                    itemCount = document.trips.sumOf { it.itinerary.size + it.preparation.size + it.packing.size + it.reservations.size + it.sources.size },
                )
            }
            .onFailure { mutableMessages.emit(it.message ?: "지원하지 않거나 손상된 백업입니다.") }
    }

    fun confirmRestoreAsNewCopies() = viewModelScope.launch {
        val review = mutableImportReview.value ?: return@launch
        repository.restoreAsNewCopies(review.document)
            .onSuccess { ids -> mutableMessages.emit("기존 데이터를 바꾸지 않고 여행 ${ids.size}개를 새 사본으로 가져왔습니다.") }
            .onFailure { mutableMessages.emit(it.message ?: "백업 가져오기에 실패했습니다.") }
        mutableImportReview.value = null
    }

    fun cancelImport() { mutableImportReview.value = null }

    private fun safeFileName(value: String): String = value
        .replace(Regex("[^a-zA-Z0-9가-힣_-]+"), "-")
        .trim('-')
        .ifBlank { "trippilot" }
}
