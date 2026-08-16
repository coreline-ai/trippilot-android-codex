package io.trippilot.app.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.trippilot.app.R
import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.PackingItemEntity
import io.trippilot.app.core.data.db.PreparationItemEntity
import io.trippilot.app.core.data.db.ReservationEntity
import io.trippilot.app.core.data.db.SourceEvidenceEntity
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.model.ItemOrigin
import io.trippilot.app.core.model.CompletionPolicy
import io.trippilot.app.core.model.PreparationStatus
import io.trippilot.app.core.model.RecheckResult
import io.trippilot.app.core.model.ReservationStatus
import io.trippilot.app.core.model.SourceOwnerType
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.feature.trips.TripViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

private enum class TripSection(val label: String) {
    SUMMARY("개요"), ITINERARY("일정"), READINESS("준비"), RESERVATIONS("예약"), SOURCES("출처")
}

private data class SourceTarget(val ownerType: SourceOwnerType, val ownerId: String, val title: String)

/** Local first: no runtime, OAuth, or network call is made by this surface. */
@Composable
fun TripPilotApp(viewModel: TripViewModel, incomingShareText: String? = null) {
    val trips by viewModel.trips.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val selectedTrip = trips.firstOrNull { it.id == selectedTripId }
    val snackbar = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(incomingShareText) { viewModel.receivePlainTextShare(incomingShareText) }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    Scaffold(
        topBar = { TripPilotTopBar(if (selectedTrip == null) "TripPilot" else selectedTrip.title) },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (selectedTrip == null) {
            TripListScreen(trips, padding, viewModel::selectTrip, viewModel::createTrip)
        } else {
            TripDetailScreen(selectedTrip, padding, viewModel, { viewModel.selectTrip(null) }) {
                coroutineScope.launch { snackbar.showSnackbar(it) }
            }
        }
    }
}

@Composable
private fun TripListScreen(
    trips: List<TripEntity>,
    padding: PaddingValues,
    onTripSelected: (String) -> Unit,
    onCreateTrip: (String, String, String, String, TravelScope) -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(padding).safeDrawingPadding().padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("trip_list_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("내 여행", style = MaterialTheme.typography.displaySmall)
        Text("모든 여행 기록은 이 기기에만 저장됩니다. 로그인이나 네트워크 없이 시작할 수 있어요.")
        if (trips.isEmpty()) {
            Spacer(Modifier.height(16.dp))
            EmptyState("아직 여행이 없습니다", "새 여행을 만들면 일정과 준비 항목을 직접 정리할 수 있어요.", R.drawable.trippilot_empty_trips)
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(trips, key = { it.id }) { trip -> TripCard(trip) { onTripSelected(trip.id) } }
            }
        }
        PrimaryAction("새 여행 만들기", { showCreateDialog = true })
    }
    if (showCreateDialog) {
        TripEditorDialog("새 여행 만들기", "여행 만들기", onDismiss = { showCreateDialog = false }, onConfirm = { title, destination, start, end, scope ->
            onCreateTrip(title, destination, start, end, scope)
            showCreateDialog = false
        })
    }
}

@Composable
private fun TripCard(trip: TripEntity, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .semantics { contentDescription = "${trip.title}, ${trip.destination}, ${trip.startDate}부터 ${trip.endDate}" }
            .testTag("trip_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(trip.title, style = MaterialTheme.typography.titleLarge)
            Text(trip.destination, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text("${trip.startDate} · ${trip.endDate}", style = MaterialTheme.typography.bodyMedium)
            StatusChip(scopeLabel(trip.scope))
        }
    }
}

@Composable
private fun TripDetailScreen(
    trip: TripEntity,
    padding: PaddingValues,
    viewModel: TripViewModel,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val itinerary by viewModel.observeItinerary(trip.id).collectAsState(emptyList())
    val preparation by viewModel.observePreparation(trip.id).collectAsState(emptyList())
    val packing by viewModel.observePacking(trip.id).collectAsState(emptyList())
    val reservations by viewModel.observeReservations(trip.id).collectAsState(emptyList())
    val sources by viewModel.observeSources(trip.id).collectAsState(emptyList())
    val storedShares by viewModel.observeActiveShares(trip.id).collectAsState(emptyList())
    val incomingShare by viewModel.pendingShareText.collectAsState()
    var section by remember(trip.id) { mutableStateOf(TripSection.SUMMARY) }
    var showTripEdit by remember { mutableStateOf(false) }
    var showItineraryAdd by remember { mutableStateOf(false) }
    var itineraryEdit by remember { mutableStateOf<ItineraryItemEntity?>(null) }
    var showPreparationAdd by remember { mutableStateOf(false) }
    var showPackingAdd by remember { mutableStateOf(false) }
    var showReservationAdd by remember { mutableStateOf(false) }
    var reservationEdit by remember { mutableStateOf<ReservationEntity?>(null) }
    var sourceTarget by remember { mutableStateOf<SourceTarget?>(null) }
    var sourceEdit by remember { mutableStateOf<SourceEvidenceEntity?>(null) }
    var recheckSource by remember { mutableStateOf<SourceEvidenceEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().padding(padding).safeDrawingPadding().testTag("trip_detail_screen")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("back_to_trips")) { Text("목록") }
            Column(Modifier.weight(1f)) {
                Text(trip.destination, style = MaterialTheme.typography.titleMedium)
                Text("${trip.startDate} · ${trip.endDate}", style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = { showTripEdit = true }) { Text("수정") }
        }
        RouteRibbon(0, tripDayCount(trip.startDate, trip.endDate), 1, Modifier.padding(horizontal = 20.dp))
        if (incomingShare != null) {
            SurfaceNotice("공유한 예약 텍스트", "이 여행에만 24시간 동안 임시 보관합니다. 자동 분석이나 예약 저장은 하지 않습니다.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.savePendingShare(trip.id) }, modifier = Modifier.testTag("save_shared_text")) { Text("이 여행에 보관") }
                    TextButton(onClick = viewModel::dismissPendingShare) { Text("무시") }
                }
            }
        }
        TabRow(section.ordinal) {
            TripSection.entries.forEach { candidate ->
                Tab(section == candidate, { section = candidate }, text = { Text(candidate.label) }, modifier = Modifier.testTag("trip_section_${candidate.name.lowercase()}"))
            }
        }
        when (section) {
            TripSection.SUMMARY -> TripSummary(trip, itinerary.size, preparation, packing, reservations.size, { viewModel.applyScopeDefaults(trip.id, trip.scope) }, { deleteTarget = "trip" })
            TripSection.ITINERARY -> ItinerarySection(itinerary, { showItineraryAdd = true }, { itineraryEdit = it }, { deleteTarget = "itinerary:$it" }, { sourceTarget = SourceTarget(SourceOwnerType.ITINERARY, it.id, it.title) })
            TripSection.READINESS -> ReadinessSection(preparation, packing, { showPreparationAdd = true }, { showPackingAdd = true }, viewModel::togglePreparation, viewModel::skipPreparation, viewModel::togglePacking, viewModel::deletePreparation, viewModel::deletePacking)
            TripSection.RESERVATIONS -> ReservationSection(reservations, storedShares.map { it.sharedText }, { showReservationAdd = true }, { reservationEdit = it }, { sourceTarget = SourceTarget(SourceOwnerType.RESERVATION, it.id, it.provider) }, viewModel::deleteReservation) { index -> storedShares.getOrNull(index)?.let { viewModel.discardPendingShare(it.id) } }
            TripSection.SOURCES -> SourcesSection(sources, { sourceEdit = it }, { recheckSource = it }, viewModel::deleteSource)
        }
    }
    if (showTripEdit) TripEditorDialog("여행 수정", "저장", onDismiss = { showTripEdit = false }, initial = trip, onConfirm = { title, destination, start, end, scope ->
        viewModel.updateTrip(trip, title, destination, start, end, scope); showTripEdit = false
    })
    if (showItineraryAdd) ItineraryDialog(trip, { showItineraryAdd = false }) { title, date, time, location ->
        viewModel.addItinerary(trip, title, date, time, location); showItineraryAdd = false
    }
    itineraryEdit?.let { item -> ItineraryDialog(trip, { itineraryEdit = null }, initial = item) { title, date, time, location ->
        viewModel.updateItinerary(trip, item.id, title, date, time, location); itineraryEdit = null
    } }
    if (showPreparationAdd) SimpleTextDialog("준비 항목 추가", "준비할 일", { showPreparationAdd = false }) { viewModel.addPreparation(trip.id, it); showPreparationAdd = false }
    if (showPackingAdd) PackingDialog({ showPackingAdd = false }) { title, quantity -> viewModel.addPacking(trip.id, title, quantity); showPackingAdd = false }
    if (showReservationAdd) ReservationDialog({ showReservationAdd = false }) { type, provider, code, url, time, location, status ->
        viewModel.addReservation(trip.id, type, provider, code, url, time, location, status); showReservationAdd = false
    }
    reservationEdit?.let { item -> ReservationDialog({ reservationEdit = null }, initial = item) { type, provider, code, url, time, location, status ->
        viewModel.updateReservation(item, type, provider, code, url, time, location, status); reservationEdit = null
    } }
    sourceTarget?.let { target -> SourceDialog(target.title, { sourceTarget = null }) { title, url ->
        if (target.ownerType == SourceOwnerType.ITINERARY) viewModel.addSource(trip.id, target.ownerId, title, url)
        else viewModel.addReservationSource(trip.id, target.ownerId, title, url)
        sourceTarget = null
    } }
    sourceEdit?.let { source -> SourceDialog("출처 수정", { sourceEdit = null }, initial = source) { title, url ->
        viewModel.updateSource(source, title, url); sourceEdit = null
    } }
    recheckSource?.let { source -> RecheckDialog(source, { recheckSource = null }) { date, result ->
        viewModel.recordRecheck(source.id, date, result); recheckSource = null; onMessage("재확인 결과를 기록했습니다.")
    } }
    deleteTarget?.let { target -> ConfirmActionSheet(
        title = if (target == "trip") "여행을 삭제할까요?" else "일정을 삭제할까요?",
        body = if (target == "trip") "일정, 준비 항목, 예약, 출처도 이 기기에서 함께 삭제됩니다." else "연결한 출처와 기록도 함께 삭제됩니다.",
        confirmLabel = "삭제", onDismiss = { deleteTarget = null }, onConfirm = {
            if (target == "trip") viewModel.deleteTrip(trip.id) else viewModel.deleteItinerary(target.removePrefix("itinerary:"))
            deleteTarget = null
        },
    ) }
}

@Composable
private fun TripSummary(trip: TripEntity, itineraryCount: Int, preparation: List<PreparationItemEntity>, packing: List<PackingItemEntity>, reservationCount: Int, onApplyDefaults: () -> Unit, onDelete: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(trip.title, style = MaterialTheme.typography.headlineMedium)
        StatusChip(scopeLabel(trip.scope))
        Text("${trip.startDate}부터 ${trip.endDate}까지 ${trip.destination}")
        SummaryMetric("일정", "${itineraryCount}개")
        SummaryMetric("준비", "${preparationRate(preparation)}% 완료")
        SummaryMetric("짐", "${packingRate(packing)}% 챙김")
        SummaryMetric("예약", "${reservationCount}개")
        SurfaceNotice("기본 준비 항목", "${scopeLabel(trip.scope)} 범위의 누락 항목만 추가합니다. 직접 적거나 완료한 항목은 삭제하지 않습니다.") {
            OutlinedButton(onClick = onApplyDefaults) { Text("기본 항목 다시 적용") }
        }
        SurfaceNotice("Codex 초안", "AI 연결과 초안 검토는 다음 단계에서 추가됩니다. 현재 화면은 로그인 없이 작동합니다.") {}
        TextButton(onClick = onDelete, modifier = Modifier.testTag("delete_trip")) { Text("이 여행 삭제") }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.titleMedium)
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun ItinerarySection(itinerary: List<ItineraryItemEntity>, onAdd: () -> Unit, onEdit: (ItineraryItemEntity) -> Unit, onDelete: (String) -> Unit, onSource: (ItineraryItemEntity) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("일자별 일정", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = onAdd, modifier = Modifier.testTag("add_itinerary")) { Text("일정 추가") }
        }
        if (itinerary.isEmpty()) EmptyState("아직 일정이 없습니다", "여행 기간 안에서 직접 추가해 보세요.", R.drawable.trippilot_empty_itinerary)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(itinerary, key = { it.id }) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${item.date} · ${formatMinute(item.startMinute)}", style = MaterialTheme.typography.labelLarge)
                        Text(item.title, style = MaterialTheme.typography.titleMedium)
                        if (item.location.isNotBlank()) Text(item.location, style = MaterialTheme.typography.bodyMedium)
                        Row { TextButton(onClick = { onEdit(item) }) { Text("수정") }; TextButton(onClick = { onSource(item) }, modifier = Modifier.testTag("add_source_itinerary")) { Text("출처") }; TextButton(onClick = { onDelete(item.id) }) { Text("삭제") } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadinessSection(preparation: List<PreparationItemEntity>, packing: List<PackingItemEntity>, onAddPreparation: () -> Unit, onAddPacking: () -> Unit, onTogglePreparation: (PreparationItemEntity) -> Unit, onSkipPreparation: (String) -> Unit, onTogglePacking: (PackingItemEntity) -> Unit, onDeletePreparation: (String) -> Unit, onDeletePacking: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("준비와 짐", style = MaterialTheme.typography.headlineSmall)
        Text("준비 ${preparationRate(preparation)}% · 짐 ${packingRate(packing)}%", style = MaterialTheme.typography.titleMedium)
        ChecklistHeader("준비할 일", onAddPreparation)
        if (preparation.isEmpty()) Text("준비 항목이 없습니다.")
        preparation.forEach { ChecklistRow(it.title, originLabel(it.origin), it.status == PreparationStatus.DONE, { onTogglePreparation(it) }, { onSkipPreparation(it.id) }) { onDeletePreparation(it.id) } }
        HorizontalDivider()
        ChecklistHeader("챙길 물건", onAddPacking)
        if (packing.isEmpty()) Text("짐 항목이 없습니다.")
        packing.forEach { ChecklistRow(it.title, "${it.quantity}개 · ${originLabel(it.origin)}", it.isPacked, { onTogglePacking(it) }, null) { onDeletePacking(it.id) } }
        SurfaceNotice("알림은 아직 보내지 않아요", "D-7부터 D-1까지 하루 한 번, 미완료 항목이 있을 때만 알리는 규칙은 로컬에서 준비 중입니다. 실제 알림 권한 요청은 Phase 5에서 별도로 승인받습니다.") {}
    }
}

@Composable
private fun ChecklistHeader(title: String, onAdd: () -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(title, style = MaterialTheme.typography.titleLarge); TextButton(onClick = onAdd, modifier = Modifier.testTag(if (title == "준비할 일") "add_preparation" else "add_packing")) { Text("직접 추가") }
}

@Composable
private fun ChecklistRow(title: String, detail: String, checked: Boolean, onChecked: () -> Unit, onSkip: (() -> Unit)?, onDelete: () -> Unit) = Row(Modifier.fillMaxWidth().semantics { contentDescription = "$title, $detail" }, verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked, { onChecked() }); Spacer(Modifier.width(4.dp)); Column(Modifier.weight(1f)) { Text(title); Text(detail, style = MaterialTheme.typography.bodySmall) }; onSkip?.let { TextButton(onClick = it) { Text("건너뜀") } }; TextButton(onClick = onDelete) { Text("삭제") }
}

@Composable
private fun ReservationSection(reservations: List<ReservationEntity>, pendingShares: List<String>, onAdd: () -> Unit, onEdit: (ReservationEntity) -> Unit, onSource: (ReservationEntity) -> Unit, onDelete: (String) -> Unit, onDiscardShare: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("예약", style = MaterialTheme.typography.headlineSmall); OutlinedButton(onClick = onAdd, modifier = Modifier.testTag("add_reservation")) { Text("예약 추가") }
        }
        if (reservations.isEmpty()) Text("예약은 직접 입력해야 저장됩니다.")
        reservations.forEach { reservation ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(reservation.provider, style = MaterialTheme.typography.titleMedium); Text("${reservation.type} · ${reservation.status}", style = MaterialTheme.typography.bodySmall); Text("확인번호 ${reservation.confirmationCode}")
                    reservation.dateTime?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    if (reservation.location.isNotBlank()) Text(reservation.location, style = MaterialTheme.typography.bodySmall)
                    reservation.url?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Row { TextButton(onClick = { onEdit(reservation) }) { Text("수정") }; TextButton(onClick = { onSource(reservation) }) { Text("출처 추가") }; TextButton(onClick = { onDelete(reservation.id) }) { Text("삭제") } }
                }
            }
        }
        if (pendingShares.isNotEmpty()) {
            HorizontalDivider(); Text("임시 예약 텍스트", style = MaterialTheme.typography.titleMedium); Text("24시간 뒤 자동 삭제됩니다. 분석·예약 생성은 하지 않습니다.", style = MaterialTheme.typography.bodySmall)
            pendingShares.forEachIndexed { index, shared -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp)) { Text(shared, maxLines = 3, overflow = TextOverflow.Ellipsis); TextButton(onClick = { onDiscardShare(index) }) { Text("보관 취소") } }
            } }
        }
    }
}

@Composable
private fun SourcesSection(sources: List<SourceEvidenceEntity>, onEdit: (SourceEvidenceEntity) -> Unit, onRecheck: (SourceEvidenceEntity) -> Unit, onDelete: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("수동 출처", style = MaterialTheme.typography.headlineSmall)
        Text("일정 또는 예약 화면에서 URL을 직접 추가할 수 있습니다. 앱은 링크를 열거나 검사하지 않습니다.")
        if (sources.isEmpty()) EmptyState("아직 출처가 없습니다", "일정이나 예약에 연결한 출처가 여기 모입니다.", R.drawable.trippilot_empty_itinerary)
        sources.forEach { source -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(source.title, style = MaterialTheme.typography.titleMedium); Text(source.url, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("마지막 확인: ${source.lastCheckedAtEpochMs?.let { "기록됨" } ?: "없음"}", style = MaterialTheme.typography.bodySmall)
                Row { TextButton(onClick = { onEdit(source) }) { Text("수정") }; TextButton(onClick = { onRecheck(source) }) { Text("재확인 기록") }; TextButton(onClick = { onDelete(source.id) }) { Text("삭제") } }
            }
        } }
    }
}

@Composable
private fun SurfaceNotice(title: String, body: String, content: @Composable () -> Unit) = Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(body, style = MaterialTheme.typography.bodyMedium); content() }
}

@Composable
private fun TripEditorDialog(title: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (String, String, String, String, TravelScope) -> Unit, initial: TripEntity? = null) {
    var tripTitle by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var destination by remember(initial) { mutableStateOf(initial?.destination.orEmpty()) }
    var startDate by remember(initial) { mutableStateOf(initial?.startDate ?: LocalDate.now().toString()) }
    var endDate by remember(initial) { mutableStateOf(initial?.endDate ?: LocalDate.now().plusDays(2).toString()) }
    var scope by remember(initial) { mutableStateOf(initial?.scope ?: TravelScope.AUTO) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(tripTitle, { tripTitle = it }, label = { Text("여행 제목") }, modifier = Modifier.fillMaxWidth().testTag("trip_title_input"))
        OutlinedTextField(destination, { destination = it }, label = { Text("목적지") }, modifier = Modifier.fillMaxWidth().testTag("trip_destination_input"))
        OutlinedTextField(startDate, { startDate = it }, label = { Text("시작일 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(endDate, { endDate = it }, label = { Text("종료일 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Text("여행 범위", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { TravelScope.entries.forEach { candidate -> if (candidate == scope) Button(onClick = { scope = candidate }) { Text(scopeLabel(candidate)) } else OutlinedButton(onClick = { scope = candidate }) { Text(scopeLabel(candidate)) } } }
    } }, confirmButton = { Button(onClick = { onConfirm(tripTitle, destination, startDate, endDate, scope) }, modifier = Modifier.testTag("confirm_trip")) { Text(confirmLabel) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@Composable
private fun ItineraryDialog(trip: TripEntity, onDismiss: () -> Unit, initial: ItineraryItemEntity? = null, onConfirm: (String, String, String, String) -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }; var date by remember(initial) { mutableStateOf(initial?.date ?: trip.startDate) }; var time by remember(initial) { mutableStateOf(initial?.startMinute?.let(::formatMinute).orEmpty()) }; var location by remember(initial) { mutableStateOf(initial?.location.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "일정 추가" else "일정 수정") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("일정 제목") }, modifier = Modifier.fillMaxWidth().testTag("itinerary_title_input")); OutlinedTextField(date, { date = it }, label = { Text("날짜 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(time, { time = it }, label = { Text("시각 (HH:mm, 선택)") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(location, { location = it }, label = { Text("장소 (선택)") }, modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { Button(onClick = { onConfirm(title, date, time, location) }) { Text("추가") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@Composable
private fun ReservationDialog(onDismiss: () -> Unit, initial: ReservationEntity? = null, onConfirm: (String, String, String, String, String, String, ReservationStatus) -> Unit) {
    var type by remember(initial) { mutableStateOf(initial?.type ?: "OTHER") }; var provider by remember(initial) { mutableStateOf(initial?.provider.orEmpty()) }; var code by remember(initial) { mutableStateOf(initial?.confirmationCode.orEmpty()) }; var url by remember(initial) { mutableStateOf(initial?.url.orEmpty()) }; var time by remember(initial) { mutableStateOf(initial?.dateTime.orEmpty()) }; var location by remember(initial) { mutableStateOf(initial?.location.orEmpty()) }; var status by remember(initial) { mutableStateOf(initial?.status ?: ReservationStatus.DRAFT) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("예약 추가") }, text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(type, { type = it }, label = { Text("예약 유형 (예: FLIGHT)") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(provider, { provider = it }, label = { Text("예약처") }, modifier = Modifier.fillMaxWidth().testTag("reservation_provider_input")); OutlinedTextField(code, { code = it }, label = { Text("확인번호") }, modifier = Modifier.fillMaxWidth().testTag("reservation_code_input")); OutlinedTextField(time, { time = it }, label = { Text("시간 (선택)") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(location, { location = it }, label = { Text("장소 (선택)") }, modifier = Modifier.fillMaxWidth()); OutlinedTextField(url, { url = it }, label = { Text("예약 URL (선택)") }, modifier = Modifier.fillMaxWidth()); Text("상태", style = MaterialTheme.typography.labelLarge); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { ReservationStatus.entries.forEach { candidate -> if (candidate == status) Button(onClick = { status = candidate }) { Text(candidate.name) } else OutlinedButton(onClick = { status = candidate }) { Text(candidate.name) } } }
    } }, confirmButton = { Button(onClick = { onConfirm(type, provider, code, url, time, location, status) }) { Text("저장") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@Composable
private fun SourceDialog(ownerTitle: String, onDismiss: () -> Unit, initial: SourceEvidenceEntity? = null, onConfirm: (String, String) -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }; var url by remember(initial) { mutableStateOf(initial?.url.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("$ownerTitle 출처 추가") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(title, { title = it }, label = { Text("출처 제목") }, modifier = Modifier.fillMaxWidth().testTag("source_title_input")); OutlinedTextField(url, { url = it }, label = { Text("https URL") }, modifier = Modifier.fillMaxWidth().testTag("source_url_input")) } }, confirmButton = { Button(onClick = { onConfirm(title, url) }) { Text("연결") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@Composable
private fun RecheckDialog(source: SourceEvidenceEntity, onDismiss: () -> Unit, onConfirm: (String, RecheckResult) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }; var result by remember { mutableStateOf(RecheckResult.UNCHANGED) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("${source.title} 재확인") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(date, { date = it }, label = { Text("확인일 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth()); RecheckResult.entries.forEach { candidate -> if (candidate == result) Button(onClick = { result = candidate }, modifier = Modifier.fillMaxWidth()) { Text(recheckLabel(candidate)) } else OutlinedButton(onClick = { result = candidate }, modifier = Modifier.fillMaxWidth()) { Text(recheckLabel(candidate)) } } } }, confirmButton = { Button(onClick = { onConfirm(date, result) }) { Text("기록") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@Composable
private fun SimpleTextDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { OutlinedTextField(value, { value = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth().testTag("simple_text_input")) }, confirmButton = { Button(onClick = { onConfirm(value) }) { Text("추가") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@Composable
private fun PackingDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("짐 항목 추가") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("챙길 물건") }, modifier = Modifier.fillMaxWidth().testTag("packing_title_input"))
        OutlinedTextField(quantity, { quantity = it }, label = { Text("수량 (1 이상)") }, modifier = Modifier.fillMaxWidth())
    } }, confirmButton = { Button(onClick = { onConfirm(title, quantity.toIntOrNull() ?: 0) }) { Text("추가") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

private fun tripDayCount(start: String, end: String): Int = runCatching { (ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.parse(end)) + 1).toInt().coerceAtLeast(1) }.getOrDefault(1)
private fun formatMinute(value: Int?): String = value?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "하루 종일"
private fun preparationRate(items: List<PreparationItemEntity>): Int = CompletionPolicy.preparationPercent(items.map { it.status })
private fun packingRate(items: List<PackingItemEntity>): Int = CompletionPolicy.packingPercent(items.map { it.isPacked })
private fun scopeLabel(scope: TravelScope): String = when (scope) { TravelScope.AUTO -> "기본"; TravelScope.DOMESTIC -> "국내"; TravelScope.INTERNATIONAL -> "해외" }
private fun originLabel(origin: ItemOrigin): String = when (origin) { ItemOrigin.DEFAULT -> "기본"; ItemOrigin.MANUAL -> "직접 입력"; ItemOrigin.AI -> "AI 제안" }
private fun recheckLabel(result: RecheckResult): String = when (result) { RecheckResult.UNCHANGED -> "변경 없음"; RecheckResult.CHANGED -> "변경됨"; RecheckResult.FAILED -> "확인 실패" }
