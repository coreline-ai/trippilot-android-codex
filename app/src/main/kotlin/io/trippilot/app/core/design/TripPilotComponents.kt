package io.trippilot.app.core.design

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import io.trippilot.app.R

enum class TripBriefWindowClass { COMPACT, MEDIUM, EXPANDED }

/**
 * The eight interaction states every TripPilot component must define
 * (hallmark-guide.md §3). PRESSED and FOCUSED ride on the Material
 * indication of the idle surface; the remaining states are explicit.
 */
enum class TripInteractionState { IDLE, PRESSED, FOCUSED, DISABLED, LOADING, ERROR, SUCCESS, SELECTED }

/** Token-only color mapping for stateful actions (hallmark-guide.md §3). */
@Composable
private fun interactionColors(state: TripInteractionState): androidx.compose.material3.ButtonColors {
    val scheme = MaterialTheme.colorScheme
    return when (state) {
        TripInteractionState.IDLE, TripInteractionState.PRESSED, TripInteractionState.FOCUSED ->
            ButtonDefaults.buttonColors(
                containerColor = if (isSystemInDarkTheme()) TripPilotBoardingOrangeDark else TripPilotBoardingOrange,
                contentColor = if (isSystemInDarkTheme()) TripPilotOnBoardingOrangeDark else TripPilotOnBoardingOrange,
            )
        // Draft generation and apply run through the AI path, so an in-flight
        // action reads as review-in-progress violet, never as confirmed navy.
        TripInteractionState.LOADING -> ButtonDefaults.buttonColors(
            containerColor = scheme.tertiaryContainer,
            contentColor = scheme.onTertiaryContainer,
        )
        TripInteractionState.ERROR -> ButtonDefaults.buttonColors(
            containerColor = scheme.error,
            contentColor = scheme.onError,
        )
        TripInteractionState.SUCCESS -> ButtonDefaults.buttonColors(
            containerColor = scheme.secondaryContainer,
            contentColor = scheme.onSecondaryContainer,
        )
        TripInteractionState.SELECTED -> ButtonDefaults.buttonColors(
            containerColor = scheme.secondary,
            contentColor = scheme.onSecondary,
        )
        TripInteractionState.DISABLED -> ButtonDefaults.buttonColors(
            containerColor = scheme.surfaceVariant,
            contentColor = scheme.onSurfaceVariant,
        )
    }
}

/** State label that keeps color from being the only signal (hallmark-guide.md §1). */
private fun stateSuffix(state: TripInteractionState): String? = when (state) {
    TripInteractionState.LOADING -> "…"
    TripInteractionState.SUCCESS -> "✓ "
    else -> null
}

/**
 * Shared app frame for the local Trip Briefing experience. It owns the system
 * inset, snackbar, centered reading pane and window classification so screen
 * content can stay focused on a single travel action.
 */
@Composable
fun TripBriefScaffold(
    snackbarHostState: SnackbarHostState,
    content: @Composable (PaddingValues, TripBriefWindowClass) -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag("trip_brief_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val windowClass = when {
                maxWidth >= 840.dp -> TripBriefWindowClass.EXPANDED
                maxWidth >= 600.dp -> TripBriefWindowClass.MEDIUM
                else -> TripBriefWindowClass.COMPACT
            }
            Box(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
            ) {
                content(padding, windowClass)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TripPilotTopBar(title: String, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier.testTag("top_bar"),
        title = { Text(text = title, style = MaterialTheme.typography.headlineSmall) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

/**
 * Data-bearing replacement for the decorative RouteRibbon. Each stage keeps a text
 * label, state, and optional click target so visual color is never the only signal.
 */
@Composable
fun JourneyStageStrip(
    stages: List<JourneyStage>,
    selectedStageId: String,
    summary: String,
    onStageSelected: (JourneyStage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("journey_stage_strip")
            .semantics { contentDescription = summary },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(vertical = 10.dp)) {
            Text(
                text = summary,
                modifier = Modifier.padding(horizontal = 14.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                stages.forEach { stage ->
                    val selected = stage.id == selectedStageId
                    val container = when {
                        selected -> MaterialTheme.colorScheme.primary
                        stage.state == JourneyStageState.ACTION_REQUIRED -> MaterialTheme.colorScheme.tertiaryContainer
                        stage.state == JourneyStageState.COMPLETE -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val content = when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        stage.state == JourneyStageState.ACTION_REQUIRED -> MaterialTheme.colorScheme.onTertiaryContainer
                        stage.state == JourneyStageState.COMPLETE -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Surface(
                        modifier = Modifier
                            .testTag("journey_stage_${stage.id}")
                            .semantics {
                                contentDescription = "${stage.label}, ${stage.detail}, ${stage.state.label}"
                                this.selected = selected
                            },
                        shape = MaterialTheme.shapes.medium,
                        color = container,
                        contentColor = content,
                        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        onClick = { onStageSelected(stage) },
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(stage.label, style = MaterialTheme.typography.labelLarge)
                            Text(stage.detail, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BriefingPanel(
    kind: String,
    eyebrow: String,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val container = when (kind) {
        "next_action" -> MaterialTheme.colorScheme.primaryContainer
        "draft" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val content = when (kind) {
        "next_action" -> MaterialTheme.colorScheme.onPrimaryContainer
        "draft" -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier.fillMaxWidth().testTag("briefing_panel_$kind"),
        shape = MaterialTheme.shapes.large,
        color = container,
        contentColor = content,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(eyebrow, style = MaterialTheme.typography.labelLarge)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            action?.invoke()
        }
    }
}

/** Keyboard-safe local form container; it does not perform writes by itself. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TripFormSheet(
    title: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
    confirmTag: String? = null,
    content: @Composable (Modifier) -> Unit,
) {
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp * 0.82f).dp
    // Forms contain text fields and a required cancel/confirm row. Starting in the
    // partially-expanded state leaves that action row below the keyboard on tall
    // Samsung screens, so always open the bounded form in its expanded state.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("trip_form_sheet"),
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
            ) {
                content(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onDismiss) { Text("취소") }
                Button(
                    onClick = onConfirm,
                    enabled = confirmEnabled,
                    modifier = if (confirmTag == null) Modifier else Modifier.testTag(confirmTag),
                    shape = TripPilotActionShape,
                ) { Text(confirmLabel) }
            }
        }
    }
}

@Composable
fun StatusChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.testTag("status_$label").clip(MaterialTheme.shapes.extraLarge),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun PrimaryAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    state: TripInteractionState = TripInteractionState.IDLE,
) {
    val resolved = when {
        !enabled -> TripInteractionState.DISABLED
        else -> state
    }
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            // A fixed 52dp height clips the 2.0x accessibility type-scale label.
            // Keep the standard touch-target minimum, while allowing the action to
            // grow when the user's font scale needs another line-height.
            .heightIn(min = 52.dp)
            .testTag("primary_action"),
        enabled = resolved != TripInteractionState.DISABLED,
        shape = TripPilotActionShape,
        colors = interactionColors(resolved),
    ) {
        // The explicit padding participates in measurement, unlike a clipped
        // fixed-height container. At a 2.0x font scale this makes the button
        // taller instead of cutting off the Korean label's descenders.
        val prefix = stateSuffix(resolved)
        Text(
            text = if (prefix == null) label else "$prefix$label",
            modifier = Modifier
                .padding(vertical = 4.dp)
                .semantics { if (resolved == TripInteractionState.LOADING) contentDescription = "$label, 진행 중" },
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    illustration: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("empty_state")
            .semantics { contentDescription = "$title. $body" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(illustration),
            contentDescription = null,
            modifier = Modifier.size(width = 180.dp, height = 120.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun ConfirmActionSheet(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmState: TripInteractionState = TripInteractionState.IDLE,
) {
    AlertDialog(
        modifier = modifier.testTag("approval_sheet"),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_action"),
                enabled = confirmState != TripInteractionState.DISABLED,
                shape = TripPilotActionShape,
                colors = interactionColors(confirmState),
            ) {
                val prefix = stateSuffix(confirmState)
                Text(if (prefix == null) confirmLabel else "$prefix$confirmLabel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun JourneyStageStripPreview() {
    TripPilotTheme {
        JourneyStageStrip(
            stages = listOf(
                JourneyStage("pre", "출발 전", "준비 2개", JourneyStageState.ACTION_REQUIRED),
                JourneyStage("2026-08-16", "DAY 1", "일정 2개", JourneyStageState.PLANNED),
                JourneyStage("post", "귀국 후", "예정", JourneyStageState.UPCOMING),
            ),
            selectedStageId = "pre",
            summary = "출발 전 · 준비 2개 확인 필요",
            onStageSelected = {},
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EmptyStatePreview() {
    TripPilotTheme {
        EmptyState(
            title = "아직 여행이 없습니다",
            body = "새 여행을 만들면 준비할 일을 여행 브리프로 정리합니다.",
            illustration = R.drawable.trippilot_empty_trips,
            modifier = Modifier.padding(24.dp),
        )
    }
}

/**
 * Eight-state showcase for PrimaryAction and ConfirmActionSheet
 * (hallmark-guide.md §3). Previews are state documentation, not screens,
 * so they are exempt from the one-primary-action gate.
 */
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PrimaryActionStatesShowcase() {
    TripPilotTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("PrimaryAction — 8 states", style = MaterialTheme.typography.titleMedium)
            TripInteractionState.entries.forEach { state ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(state.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    PrimaryAction("준비 항목 추가", {}, state = state)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun ConfirmActionSheetStatesShowcase() {
    TripPilotTheme {
        ConfirmActionSheet(
            title = "지도 열기",
            body = "부산역 위치를 지도 앱에서 엽니다.",
            confirmLabel = "지도 열기",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
