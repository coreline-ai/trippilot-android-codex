package io.trippilot.app.feature.drafts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.trippilot.app.core.data.DraftApplyResult
import io.trippilot.app.core.codex.AuthStatus
import io.trippilot.app.core.codex.RuntimeStatus
import io.trippilot.app.core.data.db.TripEntity
import io.trippilot.app.core.design.BriefingPanel
import io.trippilot.app.core.design.PrimaryAction
import io.trippilot.app.core.design.TripInteractionState
import io.trippilot.app.core.design.TripPilotActionShape
import io.trippilot.app.core.design.TripPilotTheme
import io.trippilot.app.integration.codex.contract.BudgetRange
import io.trippilot.app.integration.codex.contract.ReservationType
import io.trippilot.app.integration.codex.contract.TravelCompanion
import io.trippilot.app.integration.codex.contract.WeatherAdvisoryDraft
import kotlinx.coroutines.launch

/** UI only holds a reviewed parsed draft in memory; it has no direct Room or external-action access. */
@Composable
fun DraftPlannerSection(trip: TripEntity, viewModel: TripDraftViewModel) {
    val state by viewModel.state.collectAsState()
    val weather by viewModel.weather.collectAsState()
    val authStatus by viewModel.authStatus.collectAsState()
    var showPasteDialog by remember(trip.id) { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // A request CTA can be below the fold. When it becomes a review, return the user to the
    // review decision header instead of leaving them mid-list from the request form.
    LaunchedEffect(state) {
        if (state is DraftUiState.Review) scrollState.scrollTo(0)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp).testTag("draft_planner_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        BriefingPanel(
            kind = "draft",
            eyebrow = "도움 / AI 초안",
            title = "여행 초안",
            body = "구조화된 제안은 이 화면에서만 검토합니다. 저장·캘린더·링크 실행은 사용자가 항목별로 확인한 뒤에만 가능합니다.",
        )
        if (authStatus != AuthStatus.NOT_REQUIRED) CodexConnectionCard(viewModel)
        when (val current = state) {
            DraftUiState.Idle -> DraftRequestForm(trip, viewModel, { showPasteDialog = true }, authStatus)
            is DraftUiState.Generating -> GeneratingCard(current, viewModel::stopGeneration)
            is DraftUiState.Review -> DraftReviewCard(current.draft, viewModel, { viewModel.applySelected(trip) }, viewModel::discardReview)
            is DraftUiState.Applying -> DraftReviewCard(current.draft, viewModel, { }, viewModel::discardReview, applying = true)
            is DraftUiState.Notice -> NoticeCard("안내", current.message) { viewModel.dismissMessage() }
            is DraftUiState.Error -> NoticeCard("초안을 반영하지 않았습니다", current.message) { viewModel.dismissMessage() }
            is DraftUiState.Applied -> AppliedCard(current.result, viewModel::dismissMessage)
            DraftUiState.WeatherReady -> DraftRequestForm(trip, viewModel, { showPasteDialog = true }, authStatus)
        }
        WeatherCard(weather, viewModel::clearWeather)
    }
    if (showPasteDialog) {
        ManualJsonDialog(onDismiss = { showPasteDialog = false }) { raw ->
            viewModel.importPastedJson(trip, raw)
            showPasteDialog = false
        }
    }
}

@Composable
private fun DraftRequestForm(trip: TripEntity, viewModel: TripDraftViewModel, onPaste: () -> Unit, authStatus: AuthStatus) {
    var purpose by remember(trip.id) { mutableStateOf("여행의 핵심 동선과 준비 항목을 검토할 수 있는 초안") }
    var interests by remember(trip.id) { mutableStateOf("산책, 음식") }
    var reservationHint by remember(trip.id) { mutableStateOf("숙소 예약 여부를 검토할 수 있는 예시 초안") }
    var companion by remember(trip.id) { mutableStateOf(TravelCompanion.SOLO) }
    var budget by remember(trip.id) { mutableStateOf(BudgetRange.FLEXIBLE) }
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.large) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("초안 요청", style = MaterialTheme.typography.titleLarge)
            Text(
                if (authStatus == AuthStatus.NOT_REQUIRED) {
                    "전송 범위: 목적지, 여행 기간, 동행 유형, 예산, 관심사, 여행 목적. 현재 debug build는 네트워크를 쓰지 않는 fixture입니다."
                } else {
                    "전송 범위: 목적지, 여행 기간, 동행 유형, 예산, 관심사, 여행 목적. Codex 연결 전에는 생성되지 않으며 원문은 저장하지 않습니다."
                },
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedTextField(purpose, { purpose = it }, label = { Text("여행 목적") }, modifier = Modifier.fillMaxWidth().testTag("draft_purpose_input"))
            OutlinedTextField(interests, { interests = it }, label = { Text("관심사 (쉼표로 구분)") }, modifier = Modifier.fillMaxWidth().testTag("draft_interests_input"))
            Text("동행 유형", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(TravelCompanion.entries, companion, { it.name }) { companion = it }
            Text("예산 범위", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(BudgetRange.entries, budget, { it.name }) { budget = it }
            PrimaryAction(
                label = "여행 초안 만들기",
                onClick = { viewModel.createPlan(trip, purpose, companion, budget, interests.split(',')) },
                modifier = Modifier.fillMaxWidth()
                    // A stable Korean label keeps TalkBack and macro-profile automation aligned.
                    .semantics { contentDescription = "여행 초안 만들기" }
                    .testTag("create_fake_draft"),
            )
            HorizontalDivider()
            OutlinedTextField(reservationHint, { reservationHint = it }, label = { Text("예약 분석 메모") }, modifier = Modifier.fillMaxWidth().testTag("reservation_analysis_input"))
            OutlinedButton(onClick = { viewModel.createReservationAnalysis(trip, reservationHint) }, modifier = Modifier.fillMaxWidth().testTag("create_fake_reservation_draft"), shape = TripPilotActionShape) { Text("예약 초안 미리보기") }
            OutlinedButton(onClick = { viewModel.createWeatherAdvisory(trip) }, modifier = Modifier.fillMaxWidth().testTag("create_fake_weather"), shape = TripPilotActionShape) { Text("날씨 참고 보기 (정보성)") }
            TextButton(onClick = onPaste, modifier = Modifier.testTag("paste_draft_json")) { Text("구조화 JSON 직접 붙여넣기") }
        }
    }
}

/** Device OAuth is deliberately a separate, user-confirmed handoff from plan generation. */
@Composable
private fun CodexConnectionCard(viewModel: TripDraftViewModel) {
    val runtimeStatus by viewModel.runtimeStatus.collectAsState()
    val authStatus by viewModel.authStatus.collectAsState()
    val challenge by viewModel.loginChallenge.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showBrowserConfirmation by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.testTag("codex_connection_card"),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Codex 연결", style = MaterialTheme.typography.titleLarge)
            Text(connectionMessage(runtimeStatus, authStatus), style = MaterialTheme.typography.bodySmall)
            when (authStatus) {
                AuthStatus.LOGIN_REQUIRED, AuthStatus.CANCELLED, AuthStatus.ERROR -> Button(
                    onClick = viewModel::beginCodexLogin,
                    modifier = Modifier.fillMaxWidth().testTag("begin_codex_device_login"),
                    shape = TripPilotActionShape,
                ) { Text("OpenAI 계정으로 연결") }
                AuthStatus.LOGIN_IN_PROGRESS -> {
                    challenge?.let {
                        Text("표시 코드: ${it.userCode}", style = MaterialTheme.typography.titleMedium)
                        Text("코드와 로그인 주소는 이 화면을 닫거나 취소하면 저장되지 않습니다.", style = MaterialTheme.typography.bodySmall)
                        Button(
                            onClick = { showBrowserConfirmation = true },
                            modifier = Modifier.fillMaxWidth().testTag("confirm_open_codex_login_browser"),
                            shape = TripPilotActionShape,
                        ) { Text("로그인 브라우저 열기") }
                        OutlinedButton(
                            onClick = viewModel::refreshCodexAfterBrowserReturn,
                            modifier = Modifier.fillMaxWidth().testTag("refresh_codex_login"),
                            shape = TripPilotActionShape,
                        ) { Text("로그인 완료 확인") }
                    }
                    TextButton(onClick = viewModel::cancelCodexLogin, modifier = Modifier.testTag("cancel_codex_login")) { Text("연결 취소") }
                }
                AuthStatus.CONNECTED -> {
                    Text("연결됨 — Codex가 만든 구조화 초안은 검토 후에만 반영됩니다.")
                    TextButton(onClick = viewModel::logoutCodex, modifier = Modifier.testTag("logout_codex")) { Text("연결 해제") }
                }
                AuthStatus.NOT_REQUIRED -> Unit
            }
        }
    }
    if (showBrowserConfirmation) {
        AlertDialog(
            onDismissRequest = { showBrowserConfirmation = false },
            title = { Text("OpenAI 로그인 열기") },
            text = { Text("OpenAI의 공식 로그인 페이지를 외부 브라우저에서 엽니다. 로그인 정보는 TripPilot에 저장되지 않습니다.") },
            confirmButton = {
                Button(
                    onClick = {
                        val url = challenge?.verificationUrl
                        if (url != null) scope.launch {
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        }
                        showBrowserConfirmation = false
                    },
                    modifier = Modifier.testTag("open_codex_login_browser"),
                    shape = TripPilotActionShape,
                ) { Text("브라우저 열기") }
            },
            dismissButton = { TextButton(onClick = { showBrowserConfirmation = false }) { Text("취소") } },
        )
    }
}

private fun connectionMessage(runtime: RuntimeStatus, auth: AuthStatus): String = when {
    runtime == RuntimeStatus.PREPARING -> "이 기기에서 Codex 실행 환경을 준비하는 중입니다. 첫 실행에는 시간이 걸릴 수 있습니다."
    runtime == RuntimeStatus.ERROR -> "Codex 실행 환경을 시작하지 못했습니다. 여행 기록은 계속 오프라인으로 사용할 수 있습니다."
    auth == AuthStatus.LOGIN_IN_PROGRESS -> "OpenAI 브라우저 로그인 완료를 기다리고 있습니다."
    auth == AuthStatus.CONNECTED -> "공식 Codex CLI 계정에 연결되었습니다."
    auth == AuthStatus.CANCELLED -> "연결이 취소되었습니다. 필요할 때 다시 시작할 수 있습니다."
    auth == AuthStatus.ERROR -> "연결 상태를 확인하지 못했습니다. 계정 정보나 여행 기록은 저장·변경하지 않았습니다."
    else -> "여행 초안 생성은 Codex 연결 뒤에만 사용할 수 있습니다."
}

@Composable
private fun <T> ChoiceRow(items: List<T>, selected: T, label: (T) -> String, onSelected: (T) -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    items.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { item ->
                if (item == selected) Button(onClick = { onSelected(item) }, shape = TripPilotActionShape) { Text(label(item)) }
                else OutlinedButton(onClick = { onSelected(item) }, shape = TripPilotActionShape) { Text(label(item)) }
            }
        }
    }
}

@Composable
private fun GeneratingCard(state: DraftUiState.Generating, onStop: () -> Unit) = BriefingPanel(
    kind = "info",
    eyebrow = "초안 생성 중",
    title = state.message,
    body = "${state.stage}. 원문 응답은 저장하지 않습니다.",
    action = { OutlinedButton(onClick = onStop, modifier = Modifier.testTag("stop_draft_generation"), shape = TripPilotActionShape) { Text("생성 중지") } },
)

@Composable
private fun DraftReviewCard(review: ReviewDraft, viewModel: TripDraftViewModel, onApply: () -> Unit, onDiscard: () -> Unit, applying: Boolean = false) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .semantics { contentDescription = "구조화된 여행 초안 검토" }
            .testTag("draft_review_screen"),
    ) {
        BriefingPanel(
            kind = "next_action",
            eyebrow = "검토 후 반영",
            title = review.title,
            body = "아래에서 항목을 선택·수정·제외할 수 있습니다. 선택한 값만 하나의 로컬 DB transaction으로 저장됩니다.",
        )
        ReviewItinerary(review, viewModel)
        ReviewReservations(review, viewModel)
        ReviewPreparation(review, viewModel)
        ReviewPacking(review, viewModel)
        ReviewSources(review, viewModel)
        ReviewAssumptions(review.assumptions)
        // LOADING while the single local transaction runs keeps a second tap
        // from applying the same draft twice (hallmark-guide.md §3).
        PrimaryAction(
            "선택한 항목만 여행에 반영",
            onApply,
            Modifier.fillMaxWidth().testTag("apply_selected_draft"),
            state = if (applying) TripInteractionState.LOADING else TripInteractionState.IDLE,
        )
        TextButton(
            onClick = onDiscard,
            enabled = !applying,
            modifier = Modifier.testTag("discard_draft"),
        ) { Text("초안 버리기") }
    }
}

@Composable
private fun ReviewItinerary(review: ReviewDraft, viewModel: TripDraftViewModel) = ReviewGroup("일정", review.itinerary.size) {
    review.itinerary.forEach { selection ->
        val item = selection.item
        SelectableCard(item.id, selection.selected, { viewModel.setItinerarySelected(item.id, it) }, "${item.date} · ${item.title}") {
            OutlinedTextField(item.title, { viewModel.updateItinerary(item.id, title = it) }, label = { Text("일정 제목") }, modifier = Modifier.fillMaxWidth().testTag("draft_itinerary_title_${item.id}"))
            OutlinedTextField(item.date, { viewModel.updateItinerary(item.id, date = it) }, label = { Text("날짜 (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(item.startMinute?.let(::formatMinute).orEmpty(), { viewModel.updateItinerary(item.id, time = it) }, label = { Text("시각 (HH:mm, 선택)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(item.location, { viewModel.updateItinerary(item.id, location = it) }, label = { Text("장소") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(item.notes, { viewModel.updateItinerary(item.id, notes = it) }, label = { Text("메모") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReviewReservations(review: ReviewDraft, viewModel: TripDraftViewModel) = ReviewGroup("예약", review.reservations.size) {
    review.reservations.forEach { selection ->
        val item = selection.item
        SelectableCard(item.id, selection.selected, { viewModel.setReservationSelected(item.id, it) }, "${item.type} · ${item.provider}") {
            Text("예약 유형", style = MaterialTheme.typography.labelLarge)
            ChoiceRow(ReservationType.entries, item.type, { it.name }) { viewModel.updateReservation(item.id, type = it) }
            OutlinedTextField(item.provider, { viewModel.updateReservation(item.id, provider = it) }, label = { Text("예약처") }, modifier = Modifier.fillMaxWidth().testTag("draft_reservation_provider_${item.id}"))
            OutlinedTextField(item.confirmationCode, { viewModel.updateReservation(item.id, code = it) }, label = { Text("확인번호") }, modifier = Modifier.fillMaxWidth().testTag("draft_reservation_code_${item.id}"))
            OutlinedTextField(item.dateTime.orEmpty(), { viewModel.updateReservation(item.id, time = it.ifBlank { null }) }, label = { Text("시간 (YYYY-MM-DDTHH:mm, 선택)") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(item.location, { viewModel.updateReservation(item.id, location = it) }, label = { Text("장소") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(item.sourceUrl.orEmpty(), { viewModel.updateReservation(item.id, url = it.ifBlank { null }) }, label = { Text("출처 URL (선택)") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReviewPreparation(review: ReviewDraft, viewModel: TripDraftViewModel) = ReviewGroup("준비 항목", review.preparation.size) {
    review.preparation.forEach { selection ->
        val item = selection.item
        SelectableCard(item.id, selection.selected, { viewModel.setPreparationSelected(item.id, it) }, item.reason.ifBlank { "준비 제안" }) {
            OutlinedTextField(item.title, { viewModel.updatePreparation(item.id, it) }, label = { Text("준비할 일") }, modifier = Modifier.fillMaxWidth().testTag("draft_preparation_title_${item.id}"))
        }
    }
}

@Composable
private fun ReviewPacking(review: ReviewDraft, viewModel: TripDraftViewModel) = ReviewGroup("짐", review.packing.size) {
    review.packing.forEach { selection ->
        val item = selection.item
        SelectableCard(item.id, selection.selected, { viewModel.setPackingSelected(item.id, it) }, item.reason.ifBlank { "짐 제안" }) {
            OutlinedTextField(item.title, { viewModel.updatePacking(item.id, title = it) }, label = { Text("챙길 물건") }, modifier = Modifier.fillMaxWidth().testTag("draft_packing_title_${item.id}"))
            OutlinedTextField(item.quantity.toString(), { viewModel.updatePacking(item.id, quantity = it) }, label = { Text("수량") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReviewSources(review: ReviewDraft, viewModel: TripDraftViewModel) = ReviewGroup("출처 후보", review.sources.size) {
    Text("링크는 열리지 않습니다. 연결된 일정 또는 예약을 함께 반영할 때만 출처로 저장됩니다.", style = MaterialTheme.typography.bodySmall)
    review.sources.forEach { selection ->
        val item = selection.item
        SelectableCard(item.id, selection.selected, { viewModel.setSourceSelected(item.id, it) }, "연결: ${item.relatedItemId}") {
            OutlinedTextField(item.title, { viewModel.updateSource(item.id, title = it) }, label = { Text("출처 제목") }, modifier = Modifier.fillMaxWidth().testTag("draft_source_title_${item.id}"))
            OutlinedTextField(item.url, { viewModel.updateSource(item.id, url = it) }, label = { Text("https URL") }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ReviewAssumptions(assumptions: List<String>) = ReviewGroup("가정", assumptions.size) {
    Text("가정은 정보용으로만 보여주며 저장하거나 실행하지 않습니다.", style = MaterialTheme.typography.bodySmall)
    if (assumptions.isEmpty()) Text("표시할 가정이 없습니다.") else assumptions.forEach { Text("• $it") }
}

@Composable
private fun ReviewGroup(title: String, count: Int, content: @Composable () -> Unit) = Surface(
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape = MaterialTheme.shapes.large,
) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$title ($count)", style = MaterialTheme.typography.titleLarge)
        content()
    }
}

@Composable
private fun SelectableCard(
    id: String,
    selected: Boolean,
    onSelected: (Boolean) -> Unit,
    summary: String,
    interaction: TripInteractionState = TripInteractionState.IDLE,
    content: @Composable () -> Unit,
) {
    var editing by remember(id) { mutableStateOf(false) }
    val disabled = interaction == TripInteractionState.DISABLED
    Surface(
        color = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        // ERROR draws a border in addition to the text so color is never the
        // only signal (hallmark-guide.md §3).
        border = if (interaction == TripInteractionState.ERROR) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        } else {
            null
        },
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Checkbox(
                    checked = selected,
                    onCheckedChange = {
                        editing = false
                        onSelected(it)
                    },
                    enabled = !disabled,
                    modifier = Modifier.semantics { contentDescription = "$summary 선택" }.testTag("draft_selection_$id"),
                )
                Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(summary, style = MaterialTheme.typography.titleMedium)
                    val statusText = when (interaction) {
                        TripInteractionState.LOADING -> "검증 중…"
                        TripInteractionState.ERROR -> "검증 실패 · 항목을 다시 확인하세요"
                        else -> if (selected) "반영 예정 · 필요하면 수정하세요" else "이번 반영에서 제외됨"
                    }
                    val statusColor = when (interaction) {
                        TripInteractionState.ERROR -> MaterialTheme.colorScheme.error
                        TripInteractionState.LOADING -> MaterialTheme.colorScheme.onTertiaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)
                }
            }
            if (selected) {
                TextButton(
                    onClick = { editing = !editing },
                    enabled = !disabled,
                    modifier = Modifier.testTag("edit_draft_$id"),
                ) { Text(if (editing) "수정 완료" else "수정") }
                if (editing) content()
            }
        }
    }
}

@Composable
private fun WeatherCard(weather: WeatherAdvisoryDraft?, onClear: () -> Unit) {
    if (weather == null) return
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.testTag("weather_advisory")) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("날씨 참고 (정보성)", style = MaterialTheme.typography.titleLarge)
            Text(weather.summary)
            weather.advisories.forEach { Text("• $it") }
            Text("이 정보는 일정·예약·준비·짐·외부 실행을 변경하지 않습니다.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onClear) { Text("닫기") }
        }
    }
}

@Composable
private fun NoticeCard(title: String, message: String, onClose: () -> Unit) = Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(message); TextButton(onClick = onClose) { Text("확인") } }
}

@Composable
private fun AppliedCard(result: DraftApplyResult.Applied, onClose: () -> Unit) = Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("선택한 초안을 반영했습니다", style = MaterialTheme.typography.titleLarge)
        Text("일정 ${result.itineraryAdded} · 예약 ${result.reservationAdded} · 준비 ${result.preparationAdded} · 짐 ${result.packingAdded} · 출처 ${result.sourceAdded}")
        result.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
        Text("캘린더·지도·브라우저·파일은 실행되지 않았습니다.", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onClose) { Text("완료") }
    }
}

@Composable
private fun ManualJsonDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var json by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("구조화 JSON 붙여넣기") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("과거 채팅을 읽지 않습니다. 붙여넣은 원문은 검토 뒤 저장하지 않습니다.")
                OutlinedTextField(json, { json = it }, label = { Text("TripPlanDraft JSON") }, modifier = Modifier.fillMaxWidth().testTag("manual_draft_json"), minLines = 6)
            }
        },
        confirmButton = { Button(onClick = { onConfirm(json) }, modifier = Modifier.testTag("confirm_manual_draft"), shape = TripPilotActionShape) { Text("검증 후 미리보기") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

private fun formatMinute(value: Int): String = "%02d:%02d".format(value / 60, value % 60)

/** Eight-state showcase for the draft SelectableCard (hallmark-guide.md §3). */
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun SelectableCardStatesShowcase() {
    TripPilotTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("SelectableCard — 8 states", style = MaterialTheme.typography.titleMedium)
            TripInteractionState.entries.forEach { state ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(state.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SelectableCard(
                        id = "showcase_${state.name}",
                        selected = state == TripInteractionState.SELECTED || state == TripInteractionState.PRESSED,
                        onSelected = {},
                        summary = "11:30 점심 식당 (초안)",
                        content = { Text("초안 상세 내용", style = MaterialTheme.typography.bodySmall) },
                        interaction = state,
                    )
                }
            }
        }
    }
}
