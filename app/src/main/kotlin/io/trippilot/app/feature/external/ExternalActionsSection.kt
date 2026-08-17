package io.trippilot.app.feature.external

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.trippilot.app.core.data.db.CalendarActionEntity
import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.ReadinessReminderEntity
import io.trippilot.app.core.data.db.ReservationEntity
import io.trippilot.app.core.data.db.SourceEvidenceEntity
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.design.ConfirmActionSheet
import io.trippilot.app.core.design.BriefingPanel
import io.trippilot.app.core.external.ExternalHandoff
import io.trippilot.app.core.model.CalendarActionStatus

private sealed interface HandoffRequest {
    val title: String
    val body: String

    data class Map(val place: String) : HandoffRequest {
        override val title = "지도 앱 열기"
        override val body = "‘$place’을(를) 지도 앱으로 보냅니다. TripPilot은 위치를 조회하거나 결과를 저장하지 않습니다."
    }

    data class Link(val url: String) : HandoffRequest {
        override val title = "외부 링크 열기"
        override val body = "다음 링크를 선택한 브라우저/앱으로 엽니다. TripPilot은 페이지를 읽거나 자동으로 로그인하지 않습니다.\n$url"
    }
}

/** Phase 5 surface: every Calendar, Intent, notification, and SAF action starts at a confirm UI. */
@Composable
fun ExternalActionsSection(
    trip: TripEntity,
    itinerary: List<ItineraryItemEntity>,
    reservations: List<ReservationEntity>,
    sources: List<SourceEvidenceEntity>,
    externalViewModel: TripExternalViewModel,
    fileViewModel: TripFileViewModel,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val actions by externalViewModel.observeCalendarActions(trip.id).collectAsState(emptyList())
    val reminder by externalViewModel.observeReminder(trip.id).collectAsState(initial = null)
    val fileWrite by fileViewModel.writeRequest.collectAsState()
    val importReview by fileViewModel.importReview.collectAsState()
    var selectedIds by remember(trip.id, itinerary) { mutableStateOf(itinerary.map { it.id }.toSet()) }
    var showCalendarReview by remember { mutableStateOf(false) }
    var showIcsReview by remember { mutableStateOf(false) }
    var showBackupExportReview by remember { mutableStateOf(false) }
    var showBackupImportReview by remember { mutableStateOf(false) }
    var showReminderReview by remember { mutableStateOf<Boolean?>(null) }
    var handoff by remember { mutableStateOf<HandoffRequest?>(null) }

    val calendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val granted = grants[Manifest.permission.READ_CALENDAR] == true && grants[Manifest.permission.WRITE_CALENDAR] == true
        onMessage(if (granted) "Calendar 권한을 허용했습니다. 미리보기에서 다시 ‘추가’를 눌러야 일정이 저장됩니다." else "Calendar 권한을 허용하지 않았습니다. 일정은 변경되지 않았습니다.")
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            externalViewModel.setReminderEnabled(trip.id, true)
        } else {
            onMessage("알림 권한을 허용하지 않았습니다. 알림은 켜지지 않았습니다.")
        }
    }
    val createJsonDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) fileViewModel.cancelFileWrite() else fileViewModel.writeSelectedDocument(uri)
    }
    val createIcsDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        if (uri == null) fileViewModel.cancelFileWrite() else fileViewModel.writeSelectedDocument(uri)
    }
    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(fileViewModel::inspectSelectedBackup)
    }

    LaunchedEffect(Unit) {
        externalViewModel.messages.collect(onMessage)
    }
    LaunchedEffect(Unit) {
        fileViewModel.messages.collect(onMessage)
    }
    LaunchedEffect(fileWrite?.id) {
        fileWrite?.let { request ->
            if (request.mimeType == "text/calendar") createIcsDocument.launch(request.displayName)
            else createJsonDocument.launch(request.displayName)
        }
    }

    val selectedItinerary = itinerary.filter { it.id in selectedIds }
    val actionByItinerary = actions.associateBy { it.itineraryId }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("external_actions_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BriefingPanel(
            kind = "next_action",
            eyebrow = "도움 / 외부 실행",
            title = "내보내기와 외부 실행",
            body = "TripPilot은 Calendar·지도·브라우저·파일을 자동 실행하지 않습니다. 대상을 고른 뒤, 확인 창에서 직접 승인하세요.",
        )

        CalendarCard(
            itinerary = itinerary,
            selectedIds = selectedIds,
            actions = actionByItinerary,
            onToggle = { id -> selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id },
            onReview = { showCalendarReview = true },
        )
        FileCard(
            selectedCount = selectedItinerary.size,
            onIcs = { showIcsReview = true },
            onBackupExport = { showBackupExportReview = true },
            onBackupImport = { showBackupImportReview = true },
        )
        ReminderCard(reminder, { showReminderReview = true }, { showReminderReview = false })
        HandoffCard(itinerary, reservations, sources, onMap = { handoff = HandoffRequest.Map(it) }, onLink = { handoff = HandoffRequest.Link(it) })
    }

    if (showCalendarReview) {
        val permissionGranted = listOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR).all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        val target = externalViewModel.calendarTargetLabel() ?: "쓰기 가능한 Calendar 확인 필요"
        ConfirmActionSheet(
            title = "Calendar 반영 전 확인",
            body = "선택한 ${selectedItinerary.size}개 일정만 ‘$target’에 추가합니다. 이미 기록된 marker가 있으면 중복 추가하지 않으며, 실패 항목만 나중에 다시 시도할 수 있습니다.",
            confirmLabel = if (permissionGranted) "선택한 일정 추가" else "Calendar 권한 요청",
            onDismiss = { showCalendarReview = false },
            onConfirm = {
                showCalendarReview = false
                if (permissionGranted) externalViewModel.writeSelectedToCalendar(trip, selectedItinerary)
                else calendarPermission.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
            },
        )
    }
    if (showIcsReview) ConfirmActionSheet(
        title = "ICS 파일 내보내기",
        body = "선택한 ${selectedItinerary.size}개 일정의 읽기 전용 .ics 파일을 저장 위치 선택 화면으로 보냅니다. Calendar 권한은 요청하지 않습니다.",
        confirmLabel = "파일 위치 선택", onDismiss = { showIcsReview = false }, onConfirm = {
            showIcsReview = false
            fileViewModel.prepareIcsExport(trip, selectedItinerary)
        },
    )
    if (showBackupExportReview) ConfirmActionSheet(
        title = "로컬 백업 내보내기",
        body = "이 기기에 있는 여행 데이터의 새 사본을 JSON 파일로 저장합니다. AI 원문, OAuth, Calendar 승인 이력, 알림 상태는 포함하지 않습니다.",
        confirmLabel = "파일 위치 선택", onDismiss = { showBackupExportReview = false }, onConfirm = {
            showBackupExportReview = false
            fileViewModel.prepareBackupExport()
        },
    )
    if (showBackupImportReview) ConfirmActionSheet(
        title = "백업 파일 선택",
        body = "선택한 JSON은 먼저 schema·크기·필드를 검사합니다. 검사 후에도 새 사본을 만들기 전 다시 확인합니다.",
        confirmLabel = "파일 선택", onDismiss = { showBackupImportReview = false }, onConfirm = {
            showBackupImportReview = false
            openDocument.launch(arrayOf("application/json", "text/plain"))
        },
    )
    importReview?.let { review -> ConfirmActionSheet(
        title = "백업 가져오기 전 확인",
        body = "여행 ${review.tripCount}개와 로컬 항목 ${review.itemCount}개를 새 사본으로 추가합니다. 기존 여행은 수정하거나 삭제하지 않습니다.",
        confirmLabel = "새 사본으로 가져오기", onDismiss = fileViewModel::cancelImport, onConfirm = fileViewModel::confirmRestoreAsNewCopies,
    ) }
    showReminderReview?.let { enable -> ConfirmActionSheet(
        title = if (enable) "여행 준비 알림 켜기" else "여행 준비 알림 끄기",
        body = if (enable) "사용자 동의 후에만 D-7부터 D-1까지, 미완료 항목이 있을 때 하루 한 번 알립니다. 부팅 뒤에도 이 설정을 기준으로 다시 예약합니다." else "예약된 이 여행의 알림을 취소합니다.",
        confirmLabel = if (enable) "알림 설정" else "알림 끄기",
        onDismiss = { showReminderReview = null },
        onConfirm = {
            showReminderReview = null
            if (enable && Build.VERSION.SDK_INT >= 33 && !externalViewModel.notificationPermissionGranted()) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else externalViewModel.setReminderEnabled(trip.id, enable)
        },
    ) }
    handoff?.let { request -> ConfirmActionSheet(
        title = request.title, body = request.body, confirmLabel = "외부 앱으로 열기", onDismiss = { handoff = null }, onConfirm = {
            handoff = null
            val result = when (request) {
                is HandoffRequest.Map -> ExternalHandoff.openMap(context, request.place)
                is HandoffRequest.Link -> ExternalHandoff.openWebLink(context, request.url)
            }
            result.onFailure { onMessage(it.message ?: "외부 앱을 열지 못했습니다.") }
        },
    ) }
}

@Composable
private fun CalendarCard(
    itinerary: List<ItineraryItemEntity>,
    selectedIds: Set<String>,
    actions: Map<String, CalendarActionEntity>,
    onToggle: (String) -> Unit,
    onReview: () -> Unit,
) = Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Calendar", style = MaterialTheme.typography.titleMedium)
        Text("추가할 일정을 고른 뒤 확인합니다. 자동 동기화·수정·삭제는 하지 않습니다.")
        if (itinerary.isEmpty()) Text("내보낼 일정이 없습니다.")
        itinerary.forEach { item ->
            val status = actions[item.id]?.status
            Row(Modifier.fillMaxWidth().semantics { contentDescription = "${item.title} Calendar 선택" }, verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = item.id in selectedIds, onCheckedChange = { onToggle(item.id) }, modifier = Modifier.testTag("calendar_item_${item.id}"))
                Spacer(Modifier.width(4.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.title)
                    Text("${item.date} · ${calendarStatusLabel(status)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        OutlinedButton(onClick = onReview, enabled = selectedIds.isNotEmpty(), modifier = Modifier.fillMaxWidth().testTag("calendar_review")) { Text("선택 일정 미리보기") }
    }
}

@Composable
private fun FileCard(selectedCount: Int, onIcs: () -> Unit, onBackupExport: () -> Unit, onBackupImport: () -> Unit) = Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("파일", style = MaterialTheme.typography.titleMedium)
        Text("SAF 파일 선택기를 사용합니다. 앱은 임의 경로를 읽거나 쓰지 않습니다.")
        OutlinedButton(onClick = onIcs, enabled = selectedCount > 0, modifier = Modifier.fillMaxWidth().testTag("ics_export_review")) { Text("선택 일정 ICS 내보내기") }
        OutlinedButton(onClick = onBackupExport, modifier = Modifier.fillMaxWidth().testTag("backup_export_review")) { Text("로컬 백업 JSON 내보내기") }
        OutlinedButton(onClick = onBackupImport, modifier = Modifier.fillMaxWidth().testTag("backup_import_review")) { Text("백업 JSON 가져오기") }
    }
}

@Composable
private fun ReminderCard(reminder: ReadinessReminderEntity?, onEnable: () -> Unit, onDisable: () -> Unit) = Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("준비 알림", style = MaterialTheme.typography.titleMedium)
        Text(if (reminder?.enabled == true) "켜짐 — D-7부터 D-1까지 미완료일에 하루 한 번" else "꺼짐 — 알림 권한과 설정을 모두 승인해야 합니다.")
        OutlinedButton(onClick = if (reminder?.enabled == true) onDisable else onEnable, modifier = Modifier.fillMaxWidth().testTag("reminder_review")) {
            Text(if (reminder?.enabled == true) "알림 끄기" else "알림 켜기")
        }
    }
}

@Composable
private fun HandoffCard(
    itinerary: List<ItineraryItemEntity>,
    reservations: List<ReservationEntity>,
    sources: List<SourceEvidenceEntity>,
    onMap: (String) -> Unit,
    onLink: (String) -> Unit,
) = Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("지도와 링크", style = MaterialTheme.typography.titleMedium)
        Text("선택한 대상만 외부 앱으로 전달합니다. TripPilot은 웹 페이지를 조회하거나 내용을 바꾸지 않습니다.")
        itinerary.filter { it.location.isNotBlank() }.forEach { item ->
            TextButton(onClick = { onMap(item.location) }, modifier = Modifier.fillMaxWidth()) { Text("지도: ${item.title} · ${item.location}") }
        }
        reservations.forEach { reservation ->
            if (reservation.location.isNotBlank()) TextButton(onClick = { onMap(reservation.location) }, modifier = Modifier.fillMaxWidth()) { Text("지도: ${reservation.provider}") }
            reservation.url?.let { url -> TextButton(onClick = { onLink(url) }, modifier = Modifier.fillMaxWidth()) { Text("링크: ${reservation.provider}") } }
        }
        sources.forEach { source -> TextButton(onClick = { onLink(source.url) }, modifier = Modifier.fillMaxWidth()) { Text("출처 링크: ${source.title}") } }
    }
}

private fun calendarStatusLabel(status: CalendarActionStatus?): String = when (status) {
    null, CalendarActionStatus.REVIEW_REQUIRED -> "미반영"
    CalendarActionStatus.APPROVED -> "승인됨"
    CalendarActionStatus.EXECUTED -> "반영됨"
    CalendarActionStatus.FAILED -> "실패 — 다시 시도 가능"
    CalendarActionStatus.REJECTED -> "거절됨"
}
