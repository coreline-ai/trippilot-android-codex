package io.trippilot.app.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.activity.compose.BackHandler
import androidx.compose.ui.unit.dp
import io.trippilot.app.R
import io.trippilot.app.core.data.db.ItineraryItemEntity
import io.trippilot.app.core.data.db.PackingItemEntity
import io.trippilot.app.core.data.db.PreparationItemEntity
import io.trippilot.app.core.data.db.ReservationEntity
import io.trippilot.app.core.data.db.SourceEvidenceEntity
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.data.db.SafetyMemoEntity
import io.trippilot.app.core.model.ItemOrigin
import io.trippilot.app.core.model.ChecklistGroup
import io.trippilot.app.core.model.ChecklistType
import io.trippilot.app.core.model.CompletionPolicy
import io.trippilot.app.core.model.PostTripWindow
import io.trippilot.app.core.model.PreparationStatus
import io.trippilot.app.core.model.ProblemResponseCatalog
import io.trippilot.app.core.model.RecheckResult
import io.trippilot.app.core.model.ReservationStatus
import io.trippilot.app.core.model.SafetyCategory
import io.trippilot.app.core.model.SourceOwnerType
import io.trippilot.app.core.model.TravelScope
import io.trippilot.app.core.model.ReadinessTemplateCatalog
import io.trippilot.app.feature.trips.TripViewModel
import io.trippilot.app.feature.drafts.DraftPlannerSection
import io.trippilot.app.feature.drafts.TripDraftViewModel
import io.trippilot.app.feature.external.ExternalActionsSection
import io.trippilot.app.feature.external.TripExternalViewModel
import io.trippilot.app.feature.external.TripFileViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.launch

/**
 * A trip has one operational home, not four permanently visible destinations.
 * Tools stay reachable, but no longer consume the itinerary/checklist viewport.
 */
private enum class TripDestination {
    HOME,
    ITINERARY,
    READINESS,
    WALLET,
    TOOLS,
    DRAFTS,
    EXTERNAL,
    SAFETY,
}

// Retained only while the old detail implementation is being removed in small,
// reviewable steps. The active shell below routes exclusively through TripDestination.
private enum class TripArea(val label: String) { JOURNEY("여정"), PREPARE("준비"), STORAGE("보관함"), HELP("도움") }
private enum class JourneyPage(val label: String) { SUMMARY("브리핑"), ITINERARY("일정") }
private enum class StoragePage(val label: String) { RESERVATIONS("예약"), SOURCES("출처") }
private enum class HelpPage(val label: String) { DRAFTS("AI 초안"), EXTERNAL("외부 실행") }

private data class SourceTarget(val ownerType: SourceOwnerType, val ownerId: String, val title: String)

/** Local-first app shell; Phase 5 external actions are isolated behind explicit confirmation UI. */
@Composable
fun TripPilotApp(
    viewModel: TripViewModel,
    draftViewModel: TripDraftViewModel,
    externalViewModel: TripExternalViewModel,
    fileViewModel: TripFileViewModel,
    incomingShareText: String? = null,
) {
    val trips by viewModel.trips.collectAsState()
    val selectedTripId by viewModel.selectedTripId.collectAsState()
    val selectedTrip = trips.firstOrNull { it.id == selectedTripId }
    val snackbar = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(incomingShareText) { viewModel.receivePlainTextShare(incomingShareText) }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    TripBriefScaffold(snackbarHostState = snackbar) { padding, windowClass ->
        val showListDetail = windowClass != TripBriefWindowClass.COMPACT
        if (!showListDetail) {
            if (selectedTrip == null) {
                TripListScreen(
                    trips = trips,
                    onTripSelected = viewModel::selectTrip,
                    onCreateTrip = viewModel::createTrip,
                    modifier = Modifier.padding(padding).safeDrawingPadding(),
                )
            } else {
                TripDetailScreen(
                    trip = selectedTrip,
                    viewModel = viewModel,
                    draftViewModel = draftViewModel,
                    externalViewModel = externalViewModel,
                    fileViewModel = fileViewModel,
                    onBackToTrips = { viewModel.selectTrip(null) },
                    onMessage = { coroutineScope.launch { snackbar.showSnackbar(it) } },
                    modifier = Modifier.padding(padding).safeDrawingPadding(),
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .safeDrawingPadding(),
            ) {
                Surface(
                    modifier = Modifier
                        .width(312.dp)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    TripListScreen(
                        trips = trips,
                        onTripSelected = viewModel::selectTrip,
                        onCreateTrip = viewModel::createTrip,
                        modifier = Modifier.fillMaxSize(),
                        inListDetailPane = true,
                    )
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (selectedTrip == null) {
                        TabletEmptyDetail()
                    } else {
                        TripDetailScreen(
                            trip = selectedTrip,
                            viewModel = viewModel,
                            draftViewModel = draftViewModel,
                            externalViewModel = externalViewModel,
                            fileViewModel = fileViewModel,
                            onBackToTrips = { viewModel.selectTrip(null) },
                            onMessage = { coroutineScope.launch { snackbar.showSnackbar(it) } },
                            modifier = Modifier.fillMaxSize(),
                            listDetailPane = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TripListScreen(
    trips: List<TripEntity>,
    onTripSelected: (String) -> Unit,
    onCreateTrip: (String, String, String, String, TravelScope) -> Unit,
    modifier: Modifier = Modifier,
    inListDetailPane: Boolean = false,
) {
    val orderedTrips = remember(trips) { trips.sortedBy { it.startDate } }
    var showCreateDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = if (inListDetailPane) 16.dp else 20.dp)
            .testTag("trip_list_screen"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("TripPilot", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        item {
            Text(if (inListDetailPane) "여행" else "나의 여행", style = MaterialTheme.typography.displaySmall)
        }
        item {
            Text(
                if (orderedTrips.isEmpty()) "여행을 만들면 일정과 준비를 이 기기에 모아 볼 수 있어요."
                else "여행을 선택해 지금 확인할 일을 바로 보세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (orderedTrips.isEmpty()) {
            item {
                SurfaceNotice(
                    title = "아직 여행이 없습니다",
                    body = "제목, 목적지, 기간만 정하면 바로 시작할 수 있어요.",
                ) { PrimaryAction("새 여행 만들기", { showCreateDialog = true }) }
            }
        } else {
            items(orderedTrips, key = { it.id }) { trip ->
                TripCard(trip) { onTripSelected(trip.id) }
            }
            item {
                OutlinedButton(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = TripPilotActionShape,
                ) { Text("새 여행 만들기") }
            }
        }
    }
    if (showCreateDialog) {
        TripEditorDialog("새 여행 만들기", "여행 만들기", onDismiss = { showCreateDialog = false }, onConfirm = { title, destination, start, end, scope ->
            onCreateTrip(title, destination, start, end, scope)
            showCreateDialog = false
        })
    }
}

@Composable
private fun TabletEmptyDetail() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("여행을 선택하세요", style = MaterialTheme.typography.headlineSmall)
        Text(
            "선택한 여행의 다음 행동과 일정을 이 영역에서 확인합니다.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun JourneyHero(trip: TripEntity?, onTripSelected: (String) -> Unit) {
    val title = trip?.title ?: "아직 출발 전이에요"
    val body = trip?.let { "${it.destination} · ${it.startDate} ~ ${it.endDate}" }
        ?: "첫 여정을 만들고 일정과 준비를 한곳에 기록하세요"
    val interaction = if (trip == null) {
        Modifier
    } else {
        Modifier
            .clickable { onTripSelected(trip.id) }
            .semantics { contentDescription = "${trip.title}, ${trip.destination}, ${trip.startDate}부터 ${trip.endDate}" }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(198.dp)
            .then(interaction)
            .testTag("journey_hero"),
        shape = TripPilotHeroShape,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
    ) {
        Box {
            Image(
                painter = painterResource(R.drawable.trippilot_field_route_hero_v1),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Destination monogram: every trip gets a distinct identity mark
            // without any remote imagery (offline-first, design-direction §5).
            trip?.let { current ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .size(56.dp)
                        .background(TripPilotHeroScrimBottom, CircleShape)
                        .border(1.dp, TripPilotHeroText, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        current.destination.trim().firstOrNull()?.toString().orEmpty(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = TripPilotHeroTitle,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to TripPilotHeroScrimTop,
                            0.56f to TripPilotHeroScrimMiddle,
                            1f to TripPilotHeroScrimBottom,
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = if (trip == null) "나만의 여행 기록" else "다음 여정",
                    style = MaterialTheme.typography.labelMedium,
                    color = TripPilotHeroText,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TripPilotHeroTitle,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TripPilotHeroText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    trip.destination.trim().firstOrNull()?.toString().orEmpty(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(trip.title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${trip.destination} · ${trip.startDate} — ${trip.endDate}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StatusChip(scopeLabel(trip.scope))
        }
    }
}

@Composable
private fun TripDetailAppBar(
    trip: TripEntity,
    destination: TripDestination,
    showBack: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onMore: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("trip_brief_header"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (showBack) {
                TextButton(onClick = onBack, modifier = Modifier.testTag("back_to_trips")) {
                    Text(if (destination == TripDestination.HOME) "목록" else "뒤로")
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
            Text(
                trip.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (destination == TripDestination.HOME) {
                TextButton(onClick = onEdit) { Text("수정") }
                TextButton(onClick = onMore, modifier = Modifier.testTag("open_trip_tools")) { Text("더보기") }
            }
        }
        Text(
            "${trip.startDate} — ${trip.endDate} · ${trip.destination}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegacyTripDetailHeader(
    trip: TripEntity,
    isBriefing: Boolean,
    stages: List<JourneyStage>,
    selectedStageId: String,
    onStageSelected: (JourneyStage) -> Unit,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    TripDetailAppBar(
        trip = trip,
        destination = TripDestination.HOME,
        showBack = true,
        onBack = onBack,
        onEdit = onEdit,
        onMore = {},
    )
}

@Composable
private fun TripAreaNavigation(selected: TripArea, onSelected: (TripArea) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TripArea.entries.forEach { candidate ->
                val selectedCandidate = candidate == selected
                if (selectedCandidate) {
                    Button(onClick = { onSelected(candidate) }, modifier = Modifier.weight(1f), shape = TripPilotActionShape) { Text(candidate.label, maxLines = 1) }
                } else {
                    TextButton(onClick = { onSelected(candidate) }, modifier = Modifier.weight(1f)) { Text(candidate.label, maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun <T> SubPageNavigation(
    entries: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: (T) -> String,
    tag: (T) -> String,
    group: String,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.large) {
        Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            entries.forEach { candidate ->
                val selectedCandidate = candidate == selected
                if (selectedCandidate) {
                    Button(onClick = { onSelected(candidate) }, modifier = Modifier.weight(1f).testTag("trip_subpage_${group}_${tag(candidate)}"), shape = TripPilotActionShape) { Text(label(candidate), maxLines = 1) }
                } else {
                    OutlinedButton(onClick = { onSelected(candidate) }, modifier = Modifier.weight(1f).testTag("trip_subpage_${group}_${tag(candidate)}"), shape = TripPilotActionShape) { Text(label(candidate), maxLines = 1) }
                }
            }
        }
    }
}

@Composable
private fun TripDetailScreen(
    trip: TripEntity,
    viewModel: TripViewModel,
    draftViewModel: TripDraftViewModel,
    externalViewModel: TripExternalViewModel,
    fileViewModel: TripFileViewModel,
    onBackToTrips: () -> Unit,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    listDetailPane: Boolean = false,
) {
    val itinerary by viewModel.observeItinerary(trip.id).collectAsState(emptyList())
    val preparation by viewModel.observePreparation(trip.id).collectAsState(emptyList())
    val packing by viewModel.observePacking(trip.id).collectAsState(emptyList())
    val reservations by viewModel.observeReservations(trip.id).collectAsState(emptyList())
    val sources by viewModel.observeSources(trip.id).collectAsState(emptyList())
    val storedShares by viewModel.observeActiveShares(trip.id).collectAsState(emptyList())
    val incomingShare by viewModel.pendingShareText.collectAsState()
    val safetyMemos by viewModel.observeSafetyMemos(trip.id).collectAsState(emptyList())

    var destination by rememberSaveable(trip.id) { mutableStateOf(TripDestination.HOME) }
    var selectedDate by rememberSaveable(trip.id) { mutableStateOf(defaultJourneyStageId(trip).takeIf(::isIsoDate)) }
    var showTripEdit by remember { mutableStateOf(false) }
    var showItineraryAdd by remember { mutableStateOf(false) }
    var itineraryAddDate by remember(trip.id) { mutableStateOf(trip.startDate) }
    var itineraryEdit by remember { mutableStateOf<ItineraryItemEntity?>(null) }
    var showPreparationAdd by remember { mutableStateOf(false) }
    var showPackingAdd by remember { mutableStateOf(false) }
    var showReservationAdd by remember { mutableStateOf(false) }
    var reservationEdit by remember { mutableStateOf<ReservationEntity?>(null) }
    var sourceTarget by remember { mutableStateOf<SourceTarget?>(null) }
    var sourceEdit by remember { mutableStateOf<SourceEvidenceEntity?>(null) }
    var recheckSource by remember { mutableStateOf<SourceEvidenceEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var postTripAddWindow by remember { mutableStateOf<PostTripWindow?>(null) }
    var safetyMemoEdit by remember { mutableStateOf<SafetyMemoEntity?>(null) }
    var showSafetyMemoAdd by remember { mutableStateOf(false) }
    var deleteSafetyMemoTarget by remember { mutableStateOf<String?>(null) }

    fun navigateUp() {
        destination = when (destination) {
            TripDestination.DRAFTS, TripDestination.EXTERNAL, TripDestination.SAFETY -> TripDestination.TOOLS
            TripDestination.TOOLS, TripDestination.ITINERARY, TripDestination.READINESS, TripDestination.WALLET -> TripDestination.HOME
            TripDestination.HOME -> TripDestination.HOME
        }
    }

    BackHandler(enabled = destination != TripDestination.HOME) { navigateUp() }
    BackHandler(enabled = destination == TripDestination.HOME && !listDetailPane) { onBackToTrips() }

    Column(modifier.fillMaxSize().testTag("trip_detail_screen")) {
        TripDetailAppBar(
            trip = trip,
            destination = destination,
            showBack = !listDetailPane || destination != TripDestination.HOME,
            onBack = { if (destination == TripDestination.HOME) onBackToTrips() else navigateUp() },
            onEdit = { showTripEdit = true },
            onMore = { destination = TripDestination.TOOLS },
        )
        Box(Modifier.weight(1f)) {
            when (destination) {
                TripDestination.HOME -> TripHomeScreen(
                    trip = trip,
                    itinerary = itinerary,
                    preparation = preparation,
                    packing = packing,
                    reservations = reservations,
                    sourceCount = sources.size,
                    onOpenItinerary = { date -> selectedDate = date; destination = TripDestination.ITINERARY },
                    onOpenReadiness = { destination = TripDestination.READINESS },
                    onOpenWallet = { destination = TripDestination.WALLET },
                    onOpenTools = { destination = TripDestination.TOOLS },
                )
                TripDestination.ITINERARY -> ItinerarySection(
                    trip = trip,
                    itinerary = itinerary,
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    onAdd = { date -> itineraryAddDate = date; showItineraryAdd = true },
                    onEdit = { itineraryEdit = it },
                    onDelete = { deleteTarget = "itinerary:$it" },
                    onSource = { sourceTarget = SourceTarget(SourceOwnerType.ITINERARY, it.id, it.title) },
                )
                TripDestination.READINESS -> ReadinessSection(
                    scope = trip.scope,
                    preparation = preparation,
                    packing = packing,
                    onAddPreparation = { showPreparationAdd = true },
                    onAddPacking = { showPackingAdd = true },
                    onApplyOptionalPack = { group -> viewModel.applyOptionalReadinessPack(trip.id, trip.scope, group) },
                    onTogglePreparation = viewModel::togglePreparation,
                    onSkipPreparation = viewModel::skipPreparation,
                    onTogglePacking = viewModel::togglePacking,
                    onDeletePreparation = viewModel::deletePreparation,
                    onDeletePacking = viewModel::deletePacking,
                    onApplyPostTripPack = { window -> viewModel.applyPostTripPack(trip.id, window) },
                    onAddPostTrip = { window -> postTripAddWindow = window },
                )
                TripDestination.WALLET -> WalletSection(
                    reservations = reservations,
                    sources = sources,
                    pendingShares = storedShares.map { it.sharedText },
                    incomingShare = incomingShare,
                    onSaveIncomingShare = { viewModel.savePendingShare(trip.id) },
                    onDismissIncomingShare = viewModel::dismissPendingShare,
                    onAddReservation = { showReservationAdd = true },
                    onEditReservation = { reservationEdit = it },
                    onAddReservationSource = { sourceTarget = SourceTarget(SourceOwnerType.RESERVATION, it.id, it.provider) },
                    onDeleteReservation = viewModel::deleteReservation,
                    onDiscardShare = { index -> storedShares.getOrNull(index)?.let { viewModel.discardPendingShare(it.id) } },
                    onEditSource = { sourceEdit = it },
                    onRecheckSource = { recheckSource = it },
                    onDeleteSource = viewModel::deleteSource,
                )
                TripDestination.TOOLS -> TripToolsScreen(
                    onOpenDraft = { destination = TripDestination.DRAFTS },
                    onOpenExternal = { destination = TripDestination.EXTERNAL },
                    onOpenSafety = { destination = TripDestination.SAFETY },
                    onDeleteTrip = { deleteTarget = "trip" },
                )
                TripDestination.DRAFTS -> DraftPlannerSection(trip, draftViewModel)
                TripDestination.EXTERNAL -> ExternalActionsSection(trip, itinerary, reservations, sources, externalViewModel, fileViewModel, onMessage)
                TripDestination.SAFETY -> SafetyHubScreen(
                    trip = trip,
                    memos = safetyMemos,
                    sources = sources,
                    onBack = { destination = TripDestination.TOOLS },
                    onAdd = { showSafetyMemoAdd = true },
                    onEdit = { safetyMemoEdit = it },
                    onDelete = { deleteSafetyMemoTarget = it },
                )
            }
        }
    }

    if (showTripEdit) TripEditorDialog("여행 수정", "저장", onDismiss = { showTripEdit = false }, initial = trip, onConfirm = { title, destinationName, start, end, scope ->
        viewModel.updateTrip(trip, title, destinationName, start, end, scope)
        showTripEdit = false
    })
    if (showItineraryAdd) ItineraryDialog(trip, { showItineraryAdd = false }, initialDate = itineraryAddDate) { title, date, time, location, notes ->
        viewModel.addItinerary(trip, title, date, time, location, notes)
        showItineraryAdd = false
    }
    itineraryEdit?.let { item -> ItineraryDialog(trip, { itineraryEdit = null }, initial = item) { title, date, time, location, notes ->
        viewModel.updateItinerary(trip, item.id, title, date, time, location, notes)
        itineraryEdit = null
    } }
    if (showPreparationAdd) SimpleTextDialog("준비 항목 추가", "준비할 일", { showPreparationAdd = false }) {
        viewModel.addPreparation(trip.id, it)
        showPreparationAdd = false
    }
    if (showPackingAdd) PackingDialog({ showPackingAdd = false }) { title, quantity ->
        viewModel.addPacking(trip.id, title, quantity)
        showPackingAdd = false
    }
    if (showReservationAdd) ReservationDialog({ showReservationAdd = false }) { type, provider, code, url, time, location, status ->
        viewModel.addReservation(trip.id, type, provider, code, url, time, location, status)
        showReservationAdd = false
    }
    reservationEdit?.let { item -> ReservationDialog({ reservationEdit = null }, initial = item) { type, provider, code, url, time, location, status ->
        viewModel.updateReservation(item, type, provider, code, url, time, location, status)
        reservationEdit = null
    } }
    sourceTarget?.let { target -> SourceDialog(target.title, { sourceTarget = null }) { title, url ->
        if (target.ownerType == SourceOwnerType.ITINERARY) viewModel.addSource(trip.id, target.ownerId, title, url)
        else viewModel.addReservationSource(trip.id, target.ownerId, title, url)
        sourceTarget = null
    } }
    sourceEdit?.let { source -> SourceDialog("출처 수정", { sourceEdit = null }, initial = source) { title, url ->
        viewModel.updateSource(source, title, url)
        sourceEdit = null
    } }
    recheckSource?.let { source -> RecheckDialog(source, { recheckSource = null }) { date, result ->
        viewModel.recordRecheck(source.id, date, result)
        recheckSource = null
        onMessage("재확인 결과를 기록했습니다.")
    } }
    deleteTarget?.let { target -> ConfirmActionSheet(
        title = if (target == "trip") "여행을 삭제할까요?" else "일정을 삭제할까요?",
        body = if (target == "trip") "일정, 준비 항목, 예약, 출처도 이 기기에서 함께 삭제됩니다." else "연결한 출처와 기록도 함께 삭제됩니다.",
        confirmLabel = "삭제",
        onDismiss = { deleteTarget = null },
        onConfirm = {
            if (target == "trip") viewModel.deleteTrip(trip.id) else viewModel.deleteItinerary(target.removePrefix("itinerary:"))
            deleteTarget = null
        },
    ) }
    postTripAddWindow?.let { window -> SimpleTextDialog("귀국 후 항목 추가", postTripWindowLabel(window), { postTripAddWindow = null }) { title ->
        viewModel.addPostTripPreparation(trip.id, title, window)
        postTripAddWindow = null
    } }
    if (showSafetyMemoAdd) SafetyMemoDialog(trip.id, null, { showSafetyMemoAdd = false }) { tripId, category, title, note, contactLabel, contactValue ->
        viewModel.addSafetyMemo(tripId, category, title, note, contactLabel, contactValue)
        showSafetyMemoAdd = false
    }
    safetyMemoEdit?.let { memo -> SafetyMemoDialog(trip.id, memo, { safetyMemoEdit = null }) { _, category, title, note, contactLabel, contactValue ->
        viewModel.updateSafetyMemo(memo.copy(category = category, title = title, note = note, contactLabel = contactLabel, contactValue = contactValue))
        safetyMemoEdit = null
    } }
    deleteSafetyMemoTarget?.let { memoId -> ConfirmActionSheet(
        title = "안전 메모를 삭제할까요?",
        body = "이 메모에 연결한 출처도 함께 삭제됩니다. 다른 기록은 변경되지 않습니다.",
        confirmLabel = "삭제",
        onDismiss = { deleteSafetyMemoTarget = null },
        onConfirm = {
            viewModel.deleteSafetyMemo(memoId)
            deleteSafetyMemoTarget = null
        },
    ) }
}

@Composable
private fun LegacyTripDetailScreen(
    trip: TripEntity,
    padding: PaddingValues,
    viewModel: TripViewModel,
    draftViewModel: TripDraftViewModel,
    externalViewModel: TripExternalViewModel,
    fileViewModel: TripFileViewModel,
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
    var area by remember(trip.id) { mutableStateOf(TripArea.JOURNEY) }
    var journeyPage by remember(trip.id) { mutableStateOf(JourneyPage.SUMMARY) }
    var selectedStageId by remember(trip.id) { mutableStateOf(defaultJourneyStageId(trip)) }
    var storagePage by remember(trip.id) { mutableStateOf(StoragePage.RESERVATIONS) }
    var helpPage by remember(trip.id) { mutableStateOf(HelpPage.DRAFTS) }
    var showTripEdit by remember { mutableStateOf(false) }
    var showItineraryAdd by remember { mutableStateOf(false) }
    var itineraryAddDate by remember(trip.id) { mutableStateOf(trip.startDate) }
    var itineraryEdit by remember { mutableStateOf<ItineraryItemEntity?>(null) }
    var showPreparationAdd by remember { mutableStateOf(false) }
    var showPackingAdd by remember { mutableStateOf(false) }
    var showReservationAdd by remember { mutableStateOf(false) }
    var reservationEdit by remember { mutableStateOf<ReservationEntity?>(null) }
    var sourceTarget by remember { mutableStateOf<SourceTarget?>(null) }
    var sourceEdit by remember { mutableStateOf<SourceEvidenceEntity?>(null) }
    var recheckSource by remember { mutableStateOf<SourceEvidenceEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var postTripAddWindow by remember { mutableStateOf<PostTripWindow?>(null) }
    var showSafetyHub by remember(trip.id) { mutableStateOf(false) }
    var safetyMemoEdit by remember { mutableStateOf<SafetyMemoEntity?>(null) }
    var showSafetyMemoAdd by remember { mutableStateOf(false) }
    var deleteSafetyMemoTarget by remember { mutableStateOf<String?>(null) }
    val safetyMemos by viewModel.observeSafetyMemos(trip.id).collectAsState(emptyList())

    val stages = remember(trip, itinerary, preparation, packing) {
        journeyStages(trip, itinerary, preparation, packing)
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .safeDrawingPadding()
            .testTag("trip_detail_screen"),
    ) {
        LegacyTripDetailHeader(
            trip = trip,
            isBriefing = area == TripArea.JOURNEY && journeyPage == JourneyPage.SUMMARY,
            stages = stages,
            selectedStageId = selectedStageId,
            onStageSelected = { stage ->
                selectedStageId = stage.id
                if (stage.id.length == 10 && stage.id[4] == '-') {
                    area = TripArea.JOURNEY
                    journeyPage = JourneyPage.ITINERARY
                }
            },
            onBack = onBack,
            onEdit = { showTripEdit = true },
        )
        if (incomingShare != null) {
            SurfaceNotice("공유한 예약 텍스트", "이 여행에만 24시간 동안 임시 보관합니다. 자동 분석이나 예약 저장은 하지 않습니다.", Modifier.padding(horizontal = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.savePendingShare(trip.id) }, modifier = Modifier.testTag("save_shared_text"), shape = TripPilotActionShape) { Text("이 여행에 보관") }
                    TextButton(onClick = viewModel::dismissPendingShare) { Text("무시") }
                }
            }
        }
        TripAreaNavigation(area, { area = it }, Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        // The header and primary areas stay reachable on short screens. Each body is
        // constrained to the remaining viewport, so its own scroll surface can expose
        // the section action instead of being clipped below the fold.
        Box(Modifier.weight(1f)) {
            when (area) {
                TripArea.JOURNEY -> {
                    Column(Modifier.fillMaxSize()) {
                        SubPageNavigation(JourneyPage.entries.toList(), journeyPage, { journeyPage = it }, { it.label }, { it.name.lowercase() }, "journey", Modifier.padding(horizontal = 20.dp))
                        when (journeyPage) {
                            JourneyPage.SUMMARY -> TripSummary(
                                trip = trip,
                                itineraryCount = itinerary.size,
                                preparation = preparation,
                                packing = packing,
                                reservations = reservations,
                                sourceCount = sources.size,
                                onApplyDefaults = { viewModel.applyScopeDefaults(trip.id, trip.scope) },
                                onOpenDraft = { area = TripArea.HELP; helpPage = HelpPage.DRAFTS },
                                onOpenPrepare = { area = TripArea.PREPARE },
                                onOpenItinerary = { area = TripArea.JOURNEY; journeyPage = JourneyPage.ITINERARY },
                                onOpenReservations = { area = TripArea.STORAGE; storagePage = StoragePage.RESERVATIONS },
                                onDelete = { deleteTarget = "trip" },
                            )
                            JourneyPage.ITINERARY -> ItinerarySection(
                                trip = trip,
                                itinerary = itinerary,
                                selectedDate = selectedStageId.takeIf(::isIsoDate),
                                onDateSelected = { selectedStageId = it },
                                onAdd = { date -> itineraryAddDate = date; showItineraryAdd = true },
                                onEdit = { itineraryEdit = it },
                                onDelete = { deleteTarget = "itinerary:$it" },
                                onSource = { sourceTarget = SourceTarget(SourceOwnerType.ITINERARY, it.id, it.title) },
                            )
                        }
                    }
                }
                TripArea.PREPARE -> ReadinessSection(
                    scope = trip.scope,
                    preparation = preparation,
                    packing = packing,
                    onAddPreparation = { showPreparationAdd = true },
                    onAddPacking = { showPackingAdd = true },
                    onApplyOptionalPack = { group -> viewModel.applyOptionalReadinessPack(trip.id, trip.scope, group) },
                    onTogglePreparation = viewModel::togglePreparation,
                    onSkipPreparation = viewModel::skipPreparation,
                    onTogglePacking = viewModel::togglePacking,
                    onDeletePreparation = viewModel::deletePreparation,
                    onDeletePacking = viewModel::deletePacking,
                    onApplyPostTripPack = { window -> viewModel.applyPostTripPack(trip.id, window) },
                    onAddPostTrip = { window -> postTripAddWindow = window },
                )
                TripArea.STORAGE -> {
                    Column(Modifier.fillMaxSize()) {
                        SubPageNavigation(StoragePage.entries.toList(), storagePage, { storagePage = it }, { it.label }, { it.name.lowercase() }, "storage", Modifier.padding(horizontal = 20.dp))
                        when (storagePage) {
                            StoragePage.RESERVATIONS -> ReservationSection(reservations, storedShares.map { it.sharedText }, { showReservationAdd = true }, { reservationEdit = it }, { sourceTarget = SourceTarget(SourceOwnerType.RESERVATION, it.id, it.provider) }, viewModel::deleteReservation) { index -> storedShares.getOrNull(index)?.let { viewModel.discardPendingShare(it.id) } }
                            StoragePage.SOURCES -> SourcesSection(sources, { sourceEdit = it }, { recheckSource = it }, viewModel::deleteSource)
                        }
                    }
                }
                TripArea.HELP -> {
                    if (showSafetyHub) {
                        SafetyHubScreen(
                            trip = trip,
                            memos = safetyMemos,
                            sources = sources,
                            onBack = { showSafetyHub = false },
                            onAdd = { showSafetyMemoAdd = true },
                            onEdit = { safetyMemoEdit = it },
                            onDelete = { deleteSafetyMemoTarget = it },
                        )
                    } else {
                        Column(Modifier.fillMaxSize()) {
                            // The safety entry panel stays above the two sub-tabs and
                            // never becomes a third tab (gap doc §4).
                            BriefingPanel(
                                kind = "status",
                                eyebrow = "문제 발생 시",
                                title = "문제 대응 · 안전 메모",
                                body = "여행 중 문제가 생기면 일반형 대응 순서와 사용자가 저장한 연락 정보를 오프라인에서 확인합니다.",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                action = {
                                    OutlinedButton(
                                        onClick = { showSafetyHub = true },
                                        modifier = Modifier.testTag("safety_hub_entry"),
                                        shape = TripPilotActionShape,
                                    ) { Text("Safety Hub 열기") }
                                },
                            )
                            SubPageNavigation(HelpPage.entries.toList(), helpPage, { helpPage = it }, { it.label }, { it.name.lowercase() }, "help", Modifier.padding(horizontal = 20.dp))
                            when (helpPage) {
                                HelpPage.DRAFTS -> DraftPlannerSection(trip, draftViewModel)
                                HelpPage.EXTERNAL -> ExternalActionsSection(trip, itinerary, reservations, sources, externalViewModel, fileViewModel, onMessage)
                            }
                        }
                    }
                }
            }
        }
    }
    if (showTripEdit) TripEditorDialog("여행 수정", "저장", onDismiss = { showTripEdit = false }, initial = trip, onConfirm = { title, destination, start, end, scope ->
        viewModel.updateTrip(trip, title, destination, start, end, scope); showTripEdit = false
    })
    if (showItineraryAdd) ItineraryDialog(trip, { showItineraryAdd = false }, initialDate = itineraryAddDate) { title, date, time, location, notes ->
        viewModel.addItinerary(trip, title, date, time, location, notes); showItineraryAdd = false
    }
    itineraryEdit?.let { item -> ItineraryDialog(trip, { itineraryEdit = null }, initial = item) { title, date, time, location, notes ->
        viewModel.updateItinerary(trip, item.id, title, date, time, location, notes); itineraryEdit = null
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
    postTripAddWindow?.let { window -> SimpleTextDialog("귀국 후 항목 추가", postTripWindowLabel(window), { postTripAddWindow = null }) { title ->
        viewModel.addPostTripPreparation(trip.id, title, window); postTripAddWindow = null
    } }
    if (showSafetyMemoAdd) SafetyMemoDialog(trip.id, null, { showSafetyMemoAdd = false }) { tripId, category, title, note, contactLabel, contactValue ->
        viewModel.addSafetyMemo(tripId, category, title, note, contactLabel, contactValue); showSafetyMemoAdd = false
    }
    safetyMemoEdit?.let { memo -> SafetyMemoDialog(trip.id, memo, { safetyMemoEdit = null }) { _, category, title, note, contactLabel, contactValue ->
        viewModel.updateSafetyMemo(memo.copy(category = category, title = title, note = note, contactLabel = contactLabel, contactValue = contactValue)); safetyMemoEdit = null
    } }
    deleteSafetyMemoTarget?.let { memoId -> ConfirmActionSheet(
        title = "안전 메모를 삭제할까요?",
        body = "이 메모에 연결한 출처도 함께 삭제됩니다. 다른 기록은 변경되지 않습니다.",
        confirmLabel = "삭제", onDismiss = { deleteSafetyMemoTarget = null }, onConfirm = {
            viewModel.deleteSafetyMemo(memoId); deleteSafetyMemoTarget = null
        },
    ) }
}

private data class TripHomeAction(
    val eyebrow: String,
    val title: String,
    val detail: String,
    val destination: TripDestination,
)

@Composable
private fun TripHomeScreen(
    trip: TripEntity,
    itinerary: List<ItineraryItemEntity>,
    preparation: List<PreparationItemEntity>,
    packing: List<PackingItemEntity>,
    reservations: List<ReservationEntity>,
    sourceCount: Int,
    onOpenItinerary: (String?) -> Unit,
    onOpenReadiness: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenTools: () -> Unit,
) {
    val today = LocalDate.now().toString()
    val ended = today > trip.endDate
    val inProgress = today >= trip.startDate && today <= trip.endDate
    val readinessPending = preparation.count { it.postTripWindow == null && it.status !in setOf(PreparationStatus.DONE, PreparationStatus.SKIPPED) } +
        packing.count { !it.isPacked }
    val postTripPending = preparation.count { it.postTripWindow != null && it.status !in setOf(PreparationStatus.DONE, PreparationStatus.SKIPPED) }
    val nextItinerary = itinerary
        .filter { it.date >= today }
        .sortedWith(compareBy<ItineraryItemEntity> { it.date }.thenBy { it.startMinute ?: Int.MAX_VALUE })
        .firstOrNull()
    val nextPreparation = preparation.firstOrNull {
        it.postTripWindow == null && it.status !in setOf(PreparationStatus.DONE, PreparationStatus.SKIPPED)
    }
    val action = when {
        ended && postTripPending > 0 -> TripHomeAction("귀국 후", "귀국 후 정리를 확인하세요", "선택한 귀국 후 항목 ${postTripPending}개가 남아 있습니다.", TripDestination.READINESS)
        ended -> TripHomeAction("귀국 후", "정리할 항목을 추가하세요", "필요한 시점의 정리 팩만 직접 선택할 수 있습니다.", TripDestination.READINESS)
        inProgress && nextItinerary != null -> TripHomeAction("지금 이어질 일정", nextItinerary.title, "${nextItinerary.date} · ${formatMinute(nextItinerary.startMinute)}", TripDestination.ITINERARY)
        readinessPending > 0 -> TripHomeAction("오늘 확인할 것", "출발 전 준비를 확인하세요", "${readinessPending}개 남음 · 다음: ${nextPreparation?.title ?: "챙길 물건"}", TripDestination.READINESS)
        itinerary.isEmpty() -> TripHomeAction("다음 단계", "첫 일정을 기록하세요", "시간과 장소를 직접 추가하면 일정표에서 이어서 봅니다.", TripDestination.ITINERARY)
        reservations.isEmpty() -> TripHomeAction("여행 서류", "확정한 예약을 보관하세요", "예약처와 확인번호는 직접 확인한 뒤 기록합니다.", TripDestination.WALLET)
        else -> TripHomeAction("여행 준비", "다음 일정을 확인하세요", "일정, 준비, 예약은 각각의 화면에서 바로 수정할 수 있습니다.", TripDestination.ITINERARY)
    }
    val readinessDetail = when {
        readinessPending == 0 -> "준비할 일과 짐을 모두 확인했습니다"
        nextPreparation != null -> "${readinessPending}개 남음 · 다음: ${nextPreparation.title}"
        else -> "${readinessPending}개 남음 · 챙길 물건을 확인하세요"
    }
    val walletDetail = when {
        reservations.isEmpty() && sourceCount == 0 -> "확정한 예약과 출처를 직접 보관하세요"
        else -> "예약 ${reservations.size}개 · 출처 ${sourceCount}개"
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("trip_briefing_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("여행 홈", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${trip.startDate} — ${trip.endDate} · ${scopeLabel(trip.scope)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (action.destination == TripDestination.ITINERARY) onOpenItinerary(nextItinerary?.date)
                    else when (action.destination) {
                        TripDestination.READINESS -> onOpenReadiness()
                        TripDestination.WALLET -> onOpenWallet()
                        else -> Unit
                    }
                }
                .semantics { contentDescription = "${action.eyebrow}, ${action.title}, ${action.detail}" }
                .testTag("briefing_next_action"),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(action.eyebrow, style = MaterialTheme.typography.labelLarge)
                Text(action.title, style = MaterialTheme.typography.titleLarge)
                Text(action.detail, style = MaterialTheme.typography.bodyMedium)
                Text("확인하기 ›", style = MaterialTheme.typography.labelLarge, color = TripPilotBoardingOrange)
            }
        }
        HomeSummaryRow(
            label = "다음 일정",
            primary = nextItinerary?.title ?: "아직 기록한 일정이 없습니다",
            secondary = nextItinerary?.let { "${it.date} · ${formatMinute(it.startMinute)}${it.location.takeIf(String::isNotBlank)?.let { location -> " · $location" }.orEmpty()}" }
                ?: "시간과 장소를 직접 추가하세요",
            onClick = { onOpenItinerary(nextItinerary?.date) },
            tag = "home_next_itinerary",
        )
        HomeSummaryRow(
            label = "준비",
            primary = if (readinessPending == 0) "준비 완료" else "준비 ${preparationRate(preparation)}% · 짐 ${packingRate(packing)}%",
            secondary = readinessDetail,
            onClick = onOpenReadiness,
            tag = "home_readiness",
        )
        HomeSummaryRow(
            label = "예약 및 서류",
            primary = reservations.firstOrNull()?.provider ?: "보관한 예약이 없습니다",
            secondary = walletDetail,
            onClick = onOpenWallet,
            tag = "home_wallet",
        )
        TextButton(onClick = onOpenTools, modifier = Modifier.align(Alignment.End).testTag("home_tools")) {
            Text("도구 및 안전 메모")
        }
    }
}

@Composable
private fun HomeSummaryRow(
    label: String,
    primary: String,
    secondary: String,
    onClick: () -> Unit,
    tag: String,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(primary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(secondary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TripToolsScreen(
    onOpenDraft: () -> Unit,
    onOpenExternal: () -> Unit,
    onOpenSafety: () -> Unit,
    onDeleteTrip: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("trip_tools_screen"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("도구", style = MaterialTheme.typography.headlineSmall)
        Text("여행 운영과 분리된 검토·내보내기·안전 기능입니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ToolEntry("AI 초안", "제안은 검토하고 선택한 항목만 반영합니다.", onOpenDraft, "open_drafts")
        ToolEntry("내보내기 및 외부 실행", "Calendar, 파일, 링크는 대상 확인 후에만 실행합니다.", onOpenExternal, "open_external_actions")
        HorizontalDivider(Modifier.padding(vertical = 4.dp))
        Text("안전", style = MaterialTheme.typography.titleMedium)
        ToolEntry("문제 대응 · 안전 메모", "일반 대응 순서와 내 연락 메모를 오프라인에서 확인합니다.", onOpenSafety, "safety_hub_entry")
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        TextButton(onClick = onDeleteTrip, modifier = Modifier.testTag("delete_trip")) { Text("이 여행 삭제") }
    }
}

@Composable
private fun ToolEntry(title: String, detail: String, onClick: () -> Unit, tag: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(detail, style = MaterialTheme.typography.bodySmall)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TripSummary(
    trip: TripEntity,
    itineraryCount: Int,
    preparation: List<PreparationItemEntity>,
    packing: List<PackingItemEntity>,
    reservations: List<ReservationEntity>,
    sourceCount: Int,
    onApplyDefaults: () -> Unit,
    onOpenDraft: () -> Unit,
    onOpenPrepare: () -> Unit,
    onOpenItinerary: () -> Unit,
    onOpenReservations: () -> Unit,
    onDelete: () -> Unit,
) {
    val preparationPercent = preparationRate(preparation)
    val packingPercent = packingRate(packing)
    val readinessPending = preparation.count { it.postTripWindow == null && it.status != PreparationStatus.DONE && it.status != PreparationStatus.SKIPPED } +
        packing.count { !it.isPacked }
    // After the end date the briefing points at the post-trip windows the user
    // opted into; an unselected pack stays an offer, never an "incomplete" state.
    val tripEnded = runCatching { LocalDate.now().isAfter(LocalDate.parse(trip.endDate)) }.getOrDefault(false)
    val postTripItems = preparation.filter { it.postTripWindow != null }
    val postTripPending = postTripItems.count { it.status != PreparationStatus.DONE && it.status != PreparationStatus.SKIPPED }
    val nextAction = when {
        tripEnded && postTripItems.isNotEmpty() && postTripPending > 0 -> Triple(
            "귀국 후 정리를 확인하세요",
            "귀국 후 항목 ${postTripPending}개가 아직 남아 있습니다.",
            onOpenPrepare,
        )
        tripEnded -> Triple(
            "귀국 후 팩을 준비하세요",
            "48시간·1주·나중 시점의 선택 팩을 준비 화면에서 직접 추가합니다.",
            onOpenPrepare,
        )
        readinessPending > 0 -> Triple(
            "출발 전 준비를 먼저 확인하세요",
            "준비할 일과 짐에서 ${readinessPending}개가 아직 남아 있습니다.",
            onOpenPrepare,
        )
        itineraryCount == 0 -> Triple(
            "첫 일정의 시간과 장소를 기록하세요",
            "여행 기간에 맞춰 직접 추가한 일정만 여기에 표시합니다.",
            onOpenItinerary,
        )
        reservations.isEmpty() -> Triple(
            "확정한 예약을 보관하세요",
            "예약처와 확인번호는 사용자가 직접 확인한 뒤 기록합니다.",
            onOpenReservations,
        )
        else -> Triple(
            "여행 기록이 준비되었습니다",
            "변경 사항은 일정, 준비, 보관함에서 직접 검토할 수 있습니다.",
            onOpenItinerary,
        )
    }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("trip_briefing_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("여행 브리핑", style = MaterialTheme.typography.headlineSmall)
        Text("${trip.startDate} — ${trip.endDate} · ${trip.destination}", style = MaterialTheme.typography.bodyLarge)
        BriefingPanel(
            kind = "next_action",
            eyebrow = "다음 확인",
            title = nextAction.first,
            body = nextAction.second,
            action = { TextButton(onClick = nextAction.third, modifier = Modifier.testTag("briefing_next_action")) { Text("확인하기") } },
        )
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("briefing_progress"),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("현재 상태", style = MaterialTheme.typography.titleMedium)
                // 2x2 metric grid instead of four stacked rows: the briefing keeps
                // one glanceable status block, not a card-after-card list (hallmark-guide.md §2).
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryMetric("일정", "${itineraryCount}개", Modifier.weight(1f))
                    SummaryMetric("준비", "${preparationPercent}% 완료", Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryMetric("짐", "${packingPercent}% 챙김", Modifier.weight(1f))
                    SummaryMetric("예약", "${reservations.size}개", Modifier.weight(1f))
                }
            }
        }
        val reservation = reservations.firstOrNull()
        if (reservation == null) {
            BriefingPanel(
                kind = "status",
                eyebrow = "여행 서류",
                title = "아직 보관한 예약이 없습니다",
                body = "항공, 숙소, 교통 예약은 확인번호와 함께 보관함에 직접 추가할 수 있습니다.",
            )
        } else {
            DocumentRow(
                title = reservation.provider,
                detail = "${reservation.type} · 확인번호 ${reservation.confirmationCode}",
                supporting = reservation.dateTime ?: reservation.location.ifBlank { "날짜와 장소는 아직 기록하지 않았습니다." },
                onOpen = onOpenReservations,
            )
        }
        BriefingPanel(
            kind = "status",
            eyebrow = "연결 출처",
            title = if (sourceCount == 0) "연결한 출처가 없습니다" else "출처 ${sourceCount}개를 기록했습니다",
            body = "링크는 직접 추가한 텍스트만 보관하며, 앱이 자동으로 열거나 검사하지 않습니다.",
        )
        SurfaceNotice("기본 준비 팩", "${scopeLabel(trip.scope)} 범위에서 누락된 기본 항목만 더합니다. 직접 적거나 완료한 항목은 바꾸지 않습니다.") {
            OutlinedButton(onClick = onApplyDefaults, shape = TripPilotActionShape) { Text("기본 항목 다시 적용") }
        }
        BriefingPanel(
            kind = "draft",
            eyebrow = "AI 초안",
            title = "초안은 검토 후 일부만 반영합니다",
            body = "AI 제안은 자동 저장·외부 실행을 하지 않습니다.",
            action = { TextButton(onClick = onOpenDraft) { Text("초안 검토 열기") } },
        )
        TextButton(onClick = onDelete, modifier = Modifier.testTag("delete_trip")) { Text("이 여행 삭제") }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) = Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
    Text(label, style = MaterialTheme.typography.bodyLarge)
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ItinerarySection(
    trip: TripEntity,
    itinerary: List<ItineraryItemEntity>,
    selectedDate: String?,
    onDateSelected: (String) -> Unit,
    onAdd: (String) -> Unit,
    onEdit: (ItineraryItemEntity) -> Unit,
    onDelete: (String) -> Unit,
    onSource: (ItineraryItemEntity) -> Unit,
) {
    val dates = tripDates(trip)
    val visibleDate = selectedDate?.takeIf { it in dates } ?: dates.firstOrNull().orEmpty()
    val dayItems = itinerary.filter { it.date == visibleDate }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("일정", style = MaterialTheme.typography.headlineSmall)
                Text("선택한 날짜의 시간 흐름을 직접 기록합니다.", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = { onAdd(visibleDate.ifBlank { trip.startDate }) }, modifier = Modifier.testTag("add_itinerary"), shape = TripPilotActionShape) { Text("일정 추가") }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("itinerary_date_selector"),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
        ) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                dates.forEachIndexed { index, date ->
                    val selected = date == visibleDate
                    if (selected) {
                        Button(onClick = { onDateSelected(date) }, modifier = Modifier.testTag("itinerary_date_$date"), shape = TripPilotActionShape) {
                            Text("DAY ${index + 1}\n${date.takeLast(5)}")
                        }
                    } else {
                        OutlinedButton(onClick = { onDateSelected(date) }, modifier = Modifier.testTag("itinerary_date_$date"), shape = TripPilotActionShape) {
                            Text("DAY ${index + 1}\n${date.takeLast(5)}")
                        }
                    }
                }
            }
        }
        BriefingPanel(
            kind = "status",
            eyebrow = "DAY ${(dates.indexOf(visibleDate) + 1).coerceAtLeast(1)}",
            title = if (dayItems.isEmpty()) "아직 이 날의 일정이 없습니다" else "${dayItems.size}개 일정이 있습니다",
            body = if (dayItems.isEmpty()) "시간과 장소를 직접 추가하면 이 날의 타임라인에 표시합니다." else "하루 종일 일정은 시간 일정 뒤에 표시합니다.",
        )
        if (dayItems.isEmpty()) {
            // The status panel already explains the empty day. A second, fixed-height
            // illustration was clipped below the fold on compact phones and made the
            // screen look unfinished, so keep one concise empty-state surface.
            Spacer(Modifier.weight(1f))
        }
        // Keep the header pinned and give the timeline a definite viewport.  Without
        // the weight a nested LazyColumn can be measured inconsistently after process
        // recreation on compact API 26 devices.
        else LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(dayItems, key = { it.id }) { item ->
                TimelineEntry(item, onEdit, onDelete, onSource)
            }
        }
    }
}

@Composable
private fun TimelineEntry(
    item: ItineraryItemEntity,
    onEdit: (ItineraryItemEntity) -> Unit,
    onDelete: (String) -> Unit,
    onSource: (ItineraryItemEntity) -> Unit,
) {
    val outlineRule = MaterialTheme.colorScheme.outlineVariant
    val nodeColor = if (item.allDay) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.fillMaxWidth().testTag("timeline_entry_${item.id}"),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.width(56.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                formatMinute(item.startMinute),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                if (item.allDay) "하루 종일" else "시간 일정",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Document rail: a continuous rule with a node per entry turns the day
        // into a boarding sequence instead of a flat list (design-direction §2).
        Box(
            Modifier
                .width(14.dp)
                .drawBehind {
                    val cx = size.width / 2
                    drawLine(
                        color = outlineRule,
                        start = Offset(cx, 0f),
                        end = Offset(cx, size.height),
                        strokeWidth = 2.dp.toPx(),
                    )
                    drawCircle(
                        color = nodeColor,
                        radius = 5.dp.toPx(),
                        center = Offset(cx, 12.dp.toPx()),
                    )
                },
        )
        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = MaterialTheme.shapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                if (item.location.isNotBlank()) Text(item.location, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (item.notes.isNotBlank()) Text(item.notes, style = MaterialTheme.typography.bodySmall)
                Text("출처는 직접 연결한 링크만 표시합니다.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    TextButton(onClick = { onEdit(item) }) { Text("수정") }
                    TextButton(onClick = { onSource(item) }, modifier = Modifier.testTag("add_source_itinerary")) { Text("출처") }
                    TextButton(onClick = { onDelete(item.id) }) { Text("삭제") }
                }
            }
        }
    }
}

@Composable
private fun ReadinessSection(
    scope: TravelScope,
    preparation: List<PreparationItemEntity>,
    packing: List<PackingItemEntity>,
    onAddPreparation: () -> Unit,
    onAddPacking: () -> Unit,
    onApplyOptionalPack: (ChecklistGroup) -> Unit,
    onTogglePreparation: (PreparationItemEntity) -> Unit,
    onSkipPreparation: (String) -> Unit,
    onTogglePacking: (PackingItemEntity) -> Unit,
    onDeletePreparation: (String) -> Unit,
    onDeletePacking: (String) -> Unit,
    onApplyPostTripPack: (PostTripWindow) -> Unit,
    onAddPostTrip: (PostTripWindow) -> Unit,
) {
    // Post-trip items only count toward their own window progress; the pre-trip
    // completion rate never moves because a post-trip pack was or wasn't added.
    val preTripPreparation = preparation.filter { it.postTripWindow == null }
    val postTripPreparation = preparation.filter { it.postTripWindow != null }
    val preparationGroups = preTripPreparation.groupBy {
        ReadinessTemplateCatalog.displayMetadata(ChecklistType.PREPARATION, it.templateId, it.title).group
    }
    val packingGroups = packing.groupBy {
        ReadinessTemplateCatalog.displayMetadata(ChecklistType.PACKING, it.templateId, it.title).group
    }
    val optionalGroups = ReadinessTemplateCatalog.optionalGroups(scope)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("readiness_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("준비", style = MaterialTheme.typography.headlineSmall)
        Text("기본 항목은 필요한 것만 추가하고, 직접 적은 항목은 그대로 둡니다.", style = MaterialTheme.typography.bodyMedium)
        BriefingPanel(
            kind = "next_action",
            eyebrow = "준비 현황",
            title = "준비 ${preparationRate(preTripPreparation)}% · 짐 ${packingRate(packing)}%",
            body = "각 항목의 짧은 확인 이유를 보고 완료, 건너뜀, 직접 추가를 선택하세요.",
        )
        if (optionalGroups.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("readiness_optional_packs"),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("선택 팩", style = MaterialTheme.typography.titleMedium)
                    Text("필요한 그룹만 추가합니다. 귀국 후 항목도 자동으로 추가하지 않습니다.", style = MaterialTheme.typography.bodySmall)
                    optionalGroups.forEach { group ->
                        OutlinedButton(
                            onClick = { onApplyOptionalPack(group) },
                            modifier = Modifier.fillMaxWidth().testTag("add_optional_pack_${group.name.lowercase()}"),
                            shape = TripPilotActionShape,
                        ) {
                            Text("${group.label} 팩 추가")
                        }
                    }
                }
            }
        }
        Text("준비할 일 · ${preparationRate(preTripPreparation)}%", style = MaterialTheme.typography.titleMedium)
        if (preparationGroups.isEmpty()) {
            EmptyState("준비할 일이 없습니다", "직접 추가하거나 기본 준비 팩을 다시 적용해 보세요.", R.drawable.trippilot_empty_itinerary)
        } else {
            readinessGroupOrder(preparationGroups.keys).forEach { group ->
                val items = preparationGroups[group].orEmpty()
                ChecklistGroupSection(
                    group = group,
                    itemCount = items.size,
                    addLabel = "직접 추가",
                    addTag = if (group == preparationGroups.keys.first()) "add_preparation" else null,
                    onAdd = onAddPreparation,
                ) {
                    items.forEach { item ->
                        val metadata = ReadinessTemplateCatalog.displayMetadata(ChecklistType.PREPARATION, item.templateId, item.title)
                        ChecklistRow(
                            title = item.title,
                            group = metadata.group.label,
                            detail = metadata.hint,
                            state = if (item.status == PreparationStatus.SKIPPED) "건너뜀" else originLabel(item.origin),
                            checked = item.status == PreparationStatus.DONE,
                            onChecked = { onTogglePreparation(item) },
                            onSkip = { onSkipPreparation(item.id) },
                            onDelete = { onDeletePreparation(item.id) },
                        )
                    }
                }
            }
        }
        Text("챙길 물건 · ${packingRate(packing)}%", style = MaterialTheme.typography.titleMedium)
        PostTripWindowSection(postTripPreparation, onApplyPostTripPack, onAddPostTrip, onTogglePreparation, onSkipPreparation, onDeletePreparation)
        if (packingGroups.isEmpty()) {
            EmptyState("챙길 물건이 없습니다", "가방에 넣을 물건을 직접 추가해 보세요.", R.drawable.trippilot_empty_itinerary)
        } else {
            readinessGroupOrder(packingGroups.keys).forEach { group ->
                val items = packingGroups[group].orEmpty()
                ChecklistGroupSection(
                    group = group,
                    itemCount = items.size,
                    addLabel = "직접 추가",
                    addTag = if (group == packingGroups.keys.first()) "add_packing" else null,
                    onAdd = onAddPacking,
                ) {
                    items.forEach { item ->
                        val metadata = ReadinessTemplateCatalog.displayMetadata(ChecklistType.PACKING, item.templateId, item.title)
                        ChecklistRow(
                            title = item.title,
                            group = metadata.group.label,
                            detail = metadata.hint,
                            state = "${item.quantity}개 · ${originLabel(item.origin)}",
                            checked = item.isPacked,
                            onChecked = { onTogglePacking(item) },
                            onSkip = null,
                            onDelete = { onDeletePacking(item.id) },
                        )
                    }
                }
            }
        }
        SurfaceNotice("알림은 직접 켜야 합니다", "미완료 항목 알림은 로컬 opt-in 기능입니다. 실제 권한 요청은 별도로 확인한 뒤에만 진행합니다.") {}
    }
}

@Composable
private fun ChecklistGroupSection(
    group: ChecklistGroup,
    itemCount: Int,
    addLabel: String,
    addTag: String?,
    onAdd: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("checklist_group_${group.name.lowercase()}"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(group.label, style = MaterialTheme.typography.titleMedium)
                    Text("${itemCount}개 항목", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onAdd, modifier = if (addTag == null) Modifier else Modifier.testTag(addTag)) { Text(addLabel) }
            }
            content()
        }
    }
}

private fun postTripWindowLabel(window: PostTripWindow): String = when (window) {
    PostTripWindow.WITHIN_48_HOURS -> "귀국 후 48시간 이내"
    PostTripWindow.WITHIN_ONE_WEEK -> "귀국 후 1주 이내"
    PostTripWindow.LATER -> "귀국 후 나중에"
}

/** Offline problem-response hub. Static guidance stays visually separate from user-owned memos. */
@Composable
private fun SafetyHubScreen(
    trip: TripEntity,
    memos: List<SafetyMemoEntity>,
    sources: List<SourceEvidenceEntity>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (SafetyMemoEntity) -> Unit,
    onDelete: (String) -> Unit,
) {
    var expandedCategory by remember { mutableStateOf<SafetyCategory?>(null) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .testTag("safety_hub_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("문제 대응 · 안전 메모", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack, modifier = Modifier.testTag("safety_hub_back")) { Text("도움으로") }
        }
        SurfaceNotice(
            "긴급 서비스가 아닙니다",
            ProblemResponseCatalog.NOT_EMERGENCY_NOTICE,
            Modifier.testTag("safety_hub_notice"),
        ) {}
        Text("문제 유형별 일반 순서", style = MaterialTheme.typography.titleMedium)
        ProblemResponseCatalog.all().forEach { category ->
            val expanded = expandedCategory == category.id
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("safety_category_${category.id.name.lowercase()}")
                    .semantics { contentDescription = "${category.id.label}, ${if (expanded) "단계 펼침" else "단계 접힘"}" },
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(value = expanded, role = Role.Button, interactionSource = null, indication = null) { expandedCategory = if (expanded) null else category.id },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(category.id.label, style = MaterialTheme.typography.titleMedium)
                        Text(if (expanded) "접기" else "펼치기", style = MaterialTheme.typography.labelLarge)
                    }
                    if (expanded) {
                        category.steps.forEachIndexed { index, step ->
                            Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        Text("내가 저장한 연락 정보·메모", style = MaterialTheme.typography.titleMedium)
        if (memos.isEmpty()) {
            EmptyState("저장한 안전 메모가 없습니다", "카드사 공식 채널, 보험 연락, 숙소 연락 등을 직접 기록해 두면 오프라인에서도 확인할 수 있습니다.", R.drawable.trippilot_empty_sources)
        } else {
            memos.forEach { memo ->
                val memoSources = sources.filter { it.ownerType == SourceOwnerType.SAFETY_MEMO && it.ownerId == memo.id }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("safety_memo_${memo.id}")
                        .semantics { contentDescription = "${memo.category.label}, ${memo.title}" },
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(memo.category.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(memo.title, style = MaterialTheme.typography.titleMedium)
                        if (memo.note.isNotBlank()) Text(memo.note, style = MaterialTheme.typography.bodyMedium)
                        if (!memo.contactLabel.isNullOrBlank() || !memo.contactValue.isNullOrBlank()) {
                            Text(
                                listOfNotNull(memo.contactLabel, memo.contactValue).joinToString(": "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        memoSources.forEach { source ->
                            Text("출처 · ${source.title}", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { onEdit(memo) }, modifier = Modifier.testTag("edit_safety_memo_${memo.id}")) { Text("수정") }
                            TextButton(onClick = { onDelete(memo.id) }, modifier = Modifier.testTag("delete_safety_memo_${memo.id}")) { Text("삭제") }
                        }
                    }
                }
            }
        }
        PrimaryAction("안전 메모 추가", onAdd, Modifier.fillMaxWidth().testTag("add_safety_memo"))
    }
}

/** Local-only safety memo form; values never leave the device. */
@Composable
private fun SafetyMemoDialog(
    tripId: String,
    initial: SafetyMemoEntity?,
    onDismiss: () -> Unit,
    onConfirm: (String, SafetyCategory, String, String, String?, String?) -> Unit,
) {
    var category by remember { mutableStateOf(initial?.category ?: SafetyCategory.DOCUMENTS) }
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var contactLabel by remember { mutableStateOf(initial?.contactLabel ?: "") }
    var contactValue by remember { mutableStateOf(initial?.contactValue ?: "") }
    TripFormSheet(
        title = if (initial == null) "안전 메모 추가" else "안전 메모 수정",
        confirmLabel = "저장",
        onConfirm = { onConfirm(tripId, category, title, note, contactLabel.ifBlank { null }, contactValue.ifBlank { null }) },
        onDismiss = onDismiss,
        confirmEnabled = title.isNotBlank(),
        confirmTag = "confirm_safety_memo",
    ) { fieldModifier ->
        Column(fieldModifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("분류", style = MaterialTheme.typography.labelLarge)
            SafetyCategory.entries.forEach { candidate ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = category == candidate, role = Role.RadioButton, interactionSource = null, indication = null) { category = candidate },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = category == candidate, onClick = { category = candidate })
                    Text(candidate.label, Modifier.padding(start = 6.dp))
                }
            }
            // Children take fillMaxWidth only: fieldModifier carries the sheet's
            // verticalScroll, and a scrollable text field breaks measurement.
            OutlinedTextField(title, { title = it }, label = { Text("제목") }, modifier = Modifier.fillMaxWidth().testTag("safety_memo_title"))
            OutlinedTextField(note, { note = it }, label = { Text("메모") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(contactLabel, { contactLabel = it }, label = { Text("연락처 이름 (예: 카드사 공식 앱)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(contactValue, { contactValue = it }, label = { Text("연락값 (번호·링크, 200자 이내)") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Opt-in post-trip windows. Empty windows stay neutral, never "incomplete". */
@Composable
private fun PostTripWindowSection(
    postTripPreparation: List<PreparationItemEntity>,
    onApplyPack: (PostTripWindow) -> Unit,
    onAddItem: (PostTripWindow) -> Unit,
    onTogglePreparation: (PreparationItemEntity) -> Unit,
    onSkipPreparation: (String) -> Unit,
    onDeletePreparation: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("귀국 후", style = MaterialTheme.typography.titleMedium)
        Text("귀국 후 팩은 자동으로 추가되지 않습니다. 필요한 시점만 골라 담으세요.", style = MaterialTheme.typography.bodySmall)
        PostTripWindow.entries.forEach { window ->
            val items = postTripPreparation.filter { it.postTripWindow == window }
            val done = items.count { it.status == PreparationStatus.DONE || it.status == PreparationStatus.SKIPPED }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_trip_window_${window.name.lowercase()}")
                    .semantics { contentDescription = "${postTripWindowLabel(window)}, ${items.size}개 중 ${done}개 완료" },
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(postTripWindowLabel(window), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (items.isEmpty()) "선택 팩을 추가하지 않으면 표시되지 않습니다."
                                else "${items.size}개 중 ${done}개 완료",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    items.forEach { item ->
                        val metadata = ReadinessTemplateCatalog.displayMetadata(ChecklistType.PREPARATION, item.templateId, item.title)
                        ChecklistRow(
                            title = item.title,
                            group = postTripWindowLabel(item.postTripWindow ?: PostTripWindow.LATER),
                            detail = metadata.hint,
                            state = if (item.status == PreparationStatus.SKIPPED) "건너뜀" else originLabel(item.origin),
                            checked = item.status == PreparationStatus.DONE,
                            onChecked = { onTogglePreparation(item) },
                            onSkip = { onSkipPreparation(item.id) },
                            onDelete = { onDeletePreparation(item.id) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onApplyPack(window) },
                            modifier = Modifier.weight(1f).testTag("add_post_trip_pack_${window.name.lowercase()}"),
                            shape = TripPilotActionShape,
                        ) { Text("이 시점 팩 추가") }
                        TextButton(
                            onClick = { onAddItem(window) },
                            modifier = Modifier.testTag("add_post_trip_item_${window.name.lowercase()}"),
                        ) { Text("직접 추가") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    title: String,
    group: String,
    detail: String,
    state: String,
    checked: Boolean,
    onChecked: () -> Unit,
    onSkip: (() -> Unit)?,
    onDelete: () -> Unit,
    interaction: TripInteractionState = TripInteractionState.IDLE,
) = Column(
    Modifier
        .fillMaxWidth()
        .semantics { contentDescription = "$title, $group, $detail, $state, ${if (checked) "완료" else "미완료"}" },
    verticalArrangement = Arrangement.spacedBy(2.dp),
) {
    Row(verticalAlignment = Alignment.Top) {
        Checkbox(checked = checked, onCheckedChange = { onChecked() }, enabled = interaction != TripInteractionState.DISABLED)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall)
            // Interaction state rides on the utility line so color is never the
            // only signal: LOADING swaps the sentence, ERROR recolors it (hallmark-guide.md §3).
            val stateText = when (interaction) {
                TripInteractionState.LOADING -> "확인 중…"
                else -> state
            }
            val stateColor = when (interaction) {
                TripInteractionState.ERROR -> MaterialTheme.colorScheme.error
                TripInteractionState.SUCCESS -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(stateText, style = MaterialTheme.typography.labelMedium, color = stateColor)
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        onSkip?.let { TextButton(onClick = it) { Text("건너뜀") } }
        TextButton(onClick = onDelete) { Text("삭제") }
    }
}

/** Eight-state showcase for the readiness ChecklistRow (hallmark-guide.md §3). */
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ChecklistRowStatesShowcase() {
    TripPilotTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("ChecklistRow — 8 states", style = MaterialTheme.typography.titleMedium)
            TripInteractionState.entries.forEach { state ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(state.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ChecklistRow(
                        title = "체크카드 해외결제 확인",
                        group = "결제·현금",
                        detail = "카드사 앱에서 해외 결제 허용을 확인합니다",
                        state = "직접 확인 필요",
                        checked = state == TripInteractionState.SUCCESS || state == TripInteractionState.SELECTED,
                        onChecked = {},
                        onSkip = {},
                        onDelete = {},
                        interaction = state,
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletSection(
    reservations: List<ReservationEntity>,
    sources: List<SourceEvidenceEntity>,
    pendingShares: List<String>,
    incomingShare: String?,
    onSaveIncomingShare: () -> Unit,
    onDismissIncomingShare: () -> Unit,
    onAddReservation: () -> Unit,
    onEditReservation: (ReservationEntity) -> Unit,
    onAddReservationSource: (ReservationEntity) -> Unit,
    onDeleteReservation: (String) -> Unit,
    onDiscardShare: (Int) -> Unit,
    onEditSource: (SourceEvidenceEntity) -> Unit,
    onRecheckSource: (SourceEvidenceEntity) -> Unit,
    onDeleteSource: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).testTag("wallet_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("보관함", style = MaterialTheme.typography.headlineSmall)
                Text("예약, 확인번호, 연결 출처를 한곳에서 확인합니다.", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onAddReservation, modifier = Modifier.testTag("add_reservation"), shape = TripPilotActionShape) { Text("예약 추가") }
        }
        incomingShare?.let {
            SurfaceNotice("공유한 예약 텍스트", "이 여행에만 24시간 동안 임시 보관합니다. 자동 분석이나 예약 저장은 하지 않습니다.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onSaveIncomingShare, modifier = Modifier.testTag("save_shared_text"), shape = TripPilotActionShape) { Text("이 여행에 보관") }
                    TextButton(onClick = onDismissIncomingShare) { Text("무시") }
                }
            }
        }
        Text("예약", style = MaterialTheme.typography.titleMedium)
        if (reservations.isEmpty()) {
            Text("아직 보관한 예약이 없습니다. 직접 확인한 예약만 추가하세요.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            reservations.forEach { reservation ->
                ReservationDocumentRow(reservation, onEditReservation, onAddReservationSource, onDeleteReservation)
            }
        }
        if (pendingShares.isNotEmpty()) {
            Text("임시 예약 텍스트", style = MaterialTheme.typography.titleMedium)
            Text("24시간 뒤 자동 삭제됩니다. 분석·예약 생성은 하지 않습니다.", style = MaterialTheme.typography.bodySmall)
            pendingShares.forEachIndexed { index, shared ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(shared, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { onDiscardShare(index) }) { Text("보관 취소") }
                    }
                }
            }
        }
        HorizontalDivider(Modifier.padding(top = 4.dp))
        Text("출처", style = MaterialTheme.typography.titleMedium)
        if (sources.isEmpty()) {
            Text("예약이나 일정에 연결한 출처가 없습니다.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            sources.forEach { source ->
                SourceWalletRow(source, onEditSource, onRecheckSource, onDeleteSource)
            }
        }
    }
}

@Composable
private fun SourceWalletRow(
    source: SourceEvidenceEntity,
    onEdit: (SourceEvidenceEntity) -> Unit,
    onRecheck: (SourceEvidenceEntity) -> Unit,
    onDelete: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("wallet_source_${source.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(source.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${if (source.lastCheckedAtEpochMs == null) "재확인 전" else "재확인 기록 있음"} · ${source.ownerType.name.lowercase()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onRecheck(source) }) { Text("재확인") }
                TextButton(onClick = { onEdit(source) }) { Text("수정") }
                TextButton(onClick = { onDelete(source.id) }) { Text("삭제") }
            }
        }
    }
}

@Composable
private fun ReservationSection(reservations: List<ReservationEntity>, pendingShares: List<String>, onAdd: () -> Unit, onEdit: (ReservationEntity) -> Unit, onSource: (ReservationEntity) -> Unit, onDelete: (String) -> Unit, onDiscardShare: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("여행 서류", style = MaterialTheme.typography.headlineSmall)
                Text("직접 확인한 예약의 확인번호와 출처를 보관합니다.", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onAdd, modifier = Modifier.testTag("add_reservation"), shape = TripPilotActionShape) { Text("예약 추가") }
        }
        if (reservations.isEmpty()) {
            BriefingPanel(
                kind = "status",
                eyebrow = "예약",
                title = "아직 보관한 예약이 없습니다",
                body = "예약처, 확인번호, 시간, 위치, 링크는 직접 확인한 뒤 직접 기록합니다.",
            )
        }
        reservations.forEach { reservation ->
            ReservationDocumentRow(reservation, onEdit, onSource, onDelete)
        }
        if (pendingShares.isNotEmpty()) {
            HorizontalDivider()
            Text("임시 예약 텍스트", style = MaterialTheme.typography.titleMedium)
            Text("24시간 뒤 자동 삭제됩니다. 분석·예약 생성은 하지 않습니다.", style = MaterialTheme.typography.bodySmall)
            pendingShares.forEachIndexed { index, shared ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(shared, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = { onDiscardShare(index) }) { Text("보관 취소") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationDocumentRow(
    reservation: ReservationEntity,
    onEdit: (ReservationEntity) -> Unit,
    onSource: (ReservationEntity) -> Unit,
    onDelete: (String) -> Unit,
) {
    val typeLabel = reservationDocumentTypeLabel(reservation.type)
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("reservation_document_${reservation.id}"),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("$typeLabel · ${reservationStatusLabel(reservation.status)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text("확인번호 ${reservation.confirmationCode}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(reservation.provider, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val metadata = listOfNotNull(
                reservation.dateTime?.takeIf(String::isNotBlank),
                reservation.location.takeIf(String::isNotBlank),
            ).joinToString(" · ")
            if (metadata.isNotBlank()) Text(metadata, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onEdit(reservation) }) { Text("수정") }
                TextButton(onClick = { onSource(reservation) }) { Text("출처 추가") }
                TextButton(onClick = { onDelete(reservation.id) }) { Text("삭제") }
            }
        }
    }
}

private fun reservationDocumentTypeLabel(type: String): String = when (type.uppercase()) {
    "FLIGHT" -> "항공권"
    "LODGING" -> "숙박권"
    "CAR" -> "렌터카권"
    "TRAIN" -> "승차권"
    "TOUR" -> "티켓"
    else -> "서류"
}

private fun reservationStatusLabel(status: ReservationStatus): String = when (status) {
    ReservationStatus.CONFIRMED -> "확정"
    ReservationStatus.DRAFT -> "미확정"
    ReservationStatus.CANCELLED -> "취소"
}

@Composable
private fun DocumentRow(
    title: String,
    detail: String,
    supporting: String,
    onOpen: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag("document_row"),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("여행 서류", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(supporting, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            TextButton(onClick = onOpen) { Text("열기") }
        }
    }
}

@Composable
private fun SourcesSection(sources: List<SourceEvidenceEntity>, onEdit: (SourceEvidenceEntity) -> Unit, onRecheck: (SourceEvidenceEntity) -> Unit, onDelete: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("출처", style = MaterialTheme.typography.headlineSmall)
        Text("일정 또는 예약에 직접 연결한 링크만 보관합니다. 앱은 링크를 자동으로 열거나 검사하지 않습니다.")
        if (sources.isEmpty()) EmptyState("아직 출처가 없습니다", "일정이나 예약에 연결한 출처가 여기 모입니다.", R.drawable.trippilot_empty_itinerary)
        sources.forEach { source ->
            Surface(
                modifier = Modifier.fillMaxWidth().testTag("source_row_${source.id}"),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(source.title, style = MaterialTheme.typography.titleMedium)
                    Text(source.url, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("마지막 재확인: ${source.lastCheckedAtEpochMs?.let { "기록됨" } ?: "아직 없음"}", style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = { onEdit(source) }) { Text("수정") }
                        TextButton(onClick = { onRecheck(source) }) { Text("재확인 기록") }
                        TextButton(onClick = { onDelete(source.id) }) { Text("삭제") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurfaceNotice(title: String, body: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) = Surface(
    modifier = modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    shape = MaterialTheme.shapes.large,
) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium)
        content()
    }
}

@Composable
private fun TripEditorDialog(title: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: (String, String, String, String, TravelScope) -> Unit, initial: TripEntity? = null) {
    var tripTitle by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var destination by remember(initial) { mutableStateOf(initial?.destination.orEmpty()) }
    var startDate by remember(initial) { mutableStateOf(initial?.startDate ?: LocalDate.now().toString()) }
    var endDate by remember(initial) { mutableStateOf(initial?.endDate ?: LocalDate.now().plusDays(2).toString()) }
    var scope by remember(initial) { mutableStateOf(initial?.scope ?: TravelScope.AUTO) }
    val canConfirm = tripTitle.isNotBlank() && destination.isNotBlank() && isIsoDate(startDate) && isIsoDate(endDate) && startDate <= endDate
    TripFormSheet(
        title = title,
        confirmLabel = confirmLabel,
        onDismiss = onDismiss,
        onConfirm = { onConfirm(tripTitle, destination, startDate, endDate, scope) },
        confirmEnabled = canConfirm,
        confirmTag = "confirm_trip",
    ) { contentModifier ->
        Column(contentModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(tripTitle, { tripTitle = it }, label = { Text("여행 제목") }, modifier = Modifier.fillMaxWidth().testTag("trip_title_input"))
            OutlinedTextField(destination, { destination = it }, label = { Text("목적지") }, modifier = Modifier.fillMaxWidth().testTag("trip_destination_input"))
            OutlinedTextField(startDate, { startDate = it }, label = { Text("시작일 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth().testTag("trip_start_input"))
            OutlinedTextField(endDate, { endDate = it }, label = { Text("종료일 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth().testTag("trip_end_input"))
            if (!canConfirm) Text("제목, 목적지, 올바른 날짜 범위를 확인하세요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Text("여행 범위", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TravelScope.entries.forEach { candidate ->
                    if (candidate == scope) Button(onClick = { scope = candidate }, shape = TripPilotActionShape) { Text(scopeLabel(candidate)) }
                    else OutlinedButton(onClick = { scope = candidate }, shape = TripPilotActionShape) { Text(scopeLabel(candidate)) }
                }
            }
        }
    }
}

@Composable
private fun ItineraryDialog(
    trip: TripEntity,
    onDismiss: () -> Unit,
    initial: ItineraryItemEntity? = null,
    initialDate: String = trip.startDate,
    onConfirm: (String, String, String, String, String) -> Unit,
) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }
    var date by remember(initial, initialDate) { mutableStateOf(initial?.date ?: initialDate) }
    var time by remember(initial) { mutableStateOf(initial?.startMinute?.let(::formatMinute).orEmpty()) }
    var location by remember(initial) { mutableStateOf(initial?.location.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    val canConfirm = title.isNotBlank() && isIsoDate(date) && date in tripDates(trip)
    TripFormSheet(
        title = if (initial == null) "일정 추가" else "일정 수정",
        confirmLabel = if (initial == null) "추가" else "저장",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(title, date, time, location, notes) },
        confirmEnabled = canConfirm,
    ) { contentModifier ->
        Column(contentModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("일정 제목") }, modifier = Modifier.fillMaxWidth().testTag("itinerary_title_input"))
            OutlinedTextField(date, { date = it }, label = { Text("날짜 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(time, { time = it }, label = { Text("시각 (HH:mm, 선택)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(location, { location = it }, label = { Text("장소 (선택)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("개인 메모·주의사항 (선택)") }, modifier = Modifier.fillMaxWidth().testTag("itinerary_notes_input"))
            if (!canConfirm) Text("제목과 여행 기간 안의 날짜를 확인하세요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun ReservationDialog(onDismiss: () -> Unit, initial: ReservationEntity? = null, onConfirm: (String, String, String, String, String, String, ReservationStatus) -> Unit) {
    var type by remember(initial) { mutableStateOf(initial?.type ?: "OTHER") }; var provider by remember(initial) { mutableStateOf(initial?.provider.orEmpty()) }; var code by remember(initial) { mutableStateOf(initial?.confirmationCode.orEmpty()) }; var url by remember(initial) { mutableStateOf(initial?.url.orEmpty()) }; var time by remember(initial) { mutableStateOf(initial?.dateTime.orEmpty()) }; var location by remember(initial) { mutableStateOf(initial?.location.orEmpty()) }; var status by remember(initial) { mutableStateOf(initial?.status ?: ReservationStatus.DRAFT) }
    val canConfirm = provider.isNotBlank() && code.isNotBlank() && (url.isBlank() || url.startsWith("http://") || url.startsWith("https://"))
    TripFormSheet(
        title = if (initial == null) "예약 추가" else "예약 수정",
        confirmLabel = "저장",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(type, provider, code, url, time, location, status) },
        confirmEnabled = canConfirm,
    ) { contentModifier ->
        Column(contentModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(type, { type = it }, label = { Text("예약 유형 (예: FLIGHT)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(provider, { provider = it }, label = { Text("예약처") }, modifier = Modifier.fillMaxWidth().testTag("reservation_provider_input"))
            OutlinedTextField(code, { code = it }, label = { Text("확인번호") }, modifier = Modifier.fillMaxWidth().testTag("reservation_code_input"))
            OutlinedTextField(time, { time = it }, label = { Text("시간 (선택)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(location, { location = it }, label = { Text("장소 (선택)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(url, { url = it }, label = { Text("예약 URL (선택)") }, modifier = Modifier.fillMaxWidth())
            Text("상태", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ReservationStatus.entries.forEach { candidate ->
                    if (candidate == status) Button(onClick = { status = candidate }, shape = TripPilotActionShape) { Text(candidate.name) }
                    else OutlinedButton(onClick = { status = candidate }, shape = TripPilotActionShape) { Text(candidate.name) }
                }
            }
            if (!canConfirm) Text("예약처·확인번호와 http/https URL 형식을 확인하세요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SourceDialog(ownerTitle: String, onDismiss: () -> Unit, initial: SourceEvidenceEntity? = null, onConfirm: (String, String) -> Unit) {
    var title by remember(initial) { mutableStateOf(initial?.title.orEmpty()) }; var url by remember(initial) { mutableStateOf(initial?.url.orEmpty()) }
    val canConfirm = title.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))
    TripFormSheet(
        title = "$ownerTitle 출처 추가",
        confirmLabel = "연결",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(title, url) },
        confirmEnabled = canConfirm,
    ) { contentModifier ->
        Column(contentModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("출처 제목") }, modifier = Modifier.fillMaxWidth().testTag("source_title_input"))
            OutlinedTextField(url, { url = it }, label = { Text("https URL") }, modifier = Modifier.fillMaxWidth().testTag("source_url_input"))
            if (!canConfirm) Text("출처 제목과 http/https URL을 입력하세요.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun RecheckDialog(source: SourceEvidenceEntity, onDismiss: () -> Unit, onConfirm: (String, RecheckResult) -> Unit) {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }; var result by remember { mutableStateOf(RecheckResult.UNCHANGED) }
    TripFormSheet(
        title = "${source.title} 재확인",
        confirmLabel = "기록",
        onDismiss = onDismiss,
        onConfirm = { onConfirm(date, result) },
        confirmEnabled = isIsoDate(date),
    ) { contentModifier ->
        Column(contentModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(date, { date = it }, label = { Text("확인일 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            RecheckResult.entries.forEach { candidate ->
                if (candidate == result) Button(onClick = { result = candidate }, modifier = Modifier.fillMaxWidth(), shape = TripPilotActionShape) { Text(recheckLabel(candidate)) }
                else OutlinedButton(onClick = { result = candidate }, modifier = Modifier.fillMaxWidth(), shape = TripPilotActionShape) { Text(recheckLabel(candidate)) }
            }
        }
    }
}

@Composable
private fun SimpleTextDialog(title: String, label: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    TripFormSheet(title, "추가", { onConfirm(value) }, onDismiss, confirmEnabled = value.isNotBlank()) { contentModifier ->
        OutlinedTextField(value, { value = it }, label = { Text(label) }, modifier = contentModifier.testTag("simple_text_input"))
    }
}

@Composable
private fun PackingDialog(onDismiss: () -> Unit, onConfirm: (String, Int) -> Unit) {
    var title by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    TripFormSheet("짐 항목 추가", "추가", { onConfirm(title, quantity.toIntOrNull() ?: 0) }, onDismiss, confirmEnabled = title.isNotBlank() && (quantity.toIntOrNull() ?: 0) >= 1) { contentModifier ->
        Column(contentModifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(title, { title = it }, label = { Text("챙길 물건") }, modifier = Modifier.fillMaxWidth().testTag("packing_title_input"))
        OutlinedTextField(quantity, { quantity = it }, label = { Text("수량 (1 이상)") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun tripDayCount(start: String, end: String): Int = runCatching { (ChronoUnit.DAYS.between(LocalDate.parse(start), LocalDate.parse(end)) + 1).toInt().coerceAtLeast(1) }.getOrDefault(1)
private fun defaultJourneyStageId(trip: TripEntity): String =
    JourneyStageCalculator.defaultSelectedId(trip.startDate, trip.endDate)

private fun journeyStages(
    trip: TripEntity,
    itinerary: List<ItineraryItemEntity>,
    preparation: List<PreparationItemEntity>,
    packing: List<PackingItemEntity>,
): List<JourneyStage> = JourneyStageCalculator.calculate(
    startDate = trip.startDate,
    endDate = trip.endDate,
    itineraryCountByDate = itinerary.groupingBy { it.date }.eachCount(),
    readinessTotal = preparation.count { it.postTripWindow == null } + packing.size,
    readinessPending = preparation.count { it.postTripWindow == null && it.status != PreparationStatus.DONE && it.status != PreparationStatus.SKIPPED } +
        packing.count { !it.isPacked },
    // Only items the user opted into as post-trip count toward the post stage.
    postTripTotal = preparation.count { it.postTripWindow != null },
    postTripPending = preparation.count { it.postTripWindow != null && it.status != PreparationStatus.DONE && it.status != PreparationStatus.SKIPPED },
)

private fun stageSummary(stages: List<JourneyStage>, selectedStageId: String): String =
    JourneyStageCalculator.summary(stages, selectedStageId)

private fun tripDates(trip: TripEntity): List<String> = runCatching {
    val end = LocalDate.parse(trip.endDate)
    generateSequence(LocalDate.parse(trip.startDate)) { date -> date.plusDays(1).takeIf { !it.isAfter(end) } }
        .map(LocalDate::toString)
        .toList()
}.getOrDefault(emptyList())

private fun isIsoDate(value: String): Boolean =
    value.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) && runCatching { LocalDate.parse(value) }.isSuccess

private fun readinessGroupOrder(groups: Set<ChecklistGroup>): List<ChecklistGroup> =
    ChecklistGroup.entries.filter(groups::contains)

private fun formatMinute(value: Int?): String = value?.let { "%02d:%02d".format(it / 60, it % 60) } ?: "하루 종일"
private fun preparationRate(items: List<PreparationItemEntity>): Int = CompletionPolicy.preparationPercent(items.map { it.status })
private fun packingRate(items: List<PackingItemEntity>): Int = CompletionPolicy.packingPercent(items.map { it.isPacked })
private fun scopeLabel(scope: TravelScope): String = when (scope) { TravelScope.AUTO -> "기본"; TravelScope.DOMESTIC -> "국내"; TravelScope.INTERNATIONAL -> "해외" }
private fun originLabel(origin: ItemOrigin): String = when (origin) { ItemOrigin.DEFAULT -> "기본"; ItemOrigin.MANUAL -> "직접 입력"; ItemOrigin.AI -> "AI 제안" }
private fun recheckLabel(result: RecheckResult): String = when (result) { RecheckResult.UNCHANGED -> "변경 없음"; RecheckResult.CHANGED -> "변경됨"; RecheckResult.FAILED -> "확인 실패" }
