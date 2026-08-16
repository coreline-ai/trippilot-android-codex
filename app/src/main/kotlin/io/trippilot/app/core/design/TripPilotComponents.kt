package io.trippilot.app.core.design

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import io.trippilot.app.R

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

@Composable
fun RouteRibbon(
    completedDays: Int,
    totalDays: Int,
    selectedDay: Int,
    modifier: Modifier = Modifier,
) {
    val summary = "여행 ${totalDays}일 중 ${selectedDay}일째, 완료 ${completedDays}일"
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val wayfinding = MaterialTheme.colorScheme.secondary
    val primary = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .testTag("route_ribbon")
            .semantics { contentDescription = summary },
    ) {
        val centerY = size.height * 0.52f
        val start = size.width * 0.07f
        val end = size.width * 0.93f
        val path = Path().apply {
            moveTo(start, centerY)
            cubicTo(
                size.width * 0.30f,
                centerY - 24.dp.toPx(),
                size.width * 0.55f,
                centerY + 24.dp.toPx(),
                end,
                centerY - 6.dp.toPx(),
            )
        }
        drawPath(
            path = path,
            color = surfaceVariant,
            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
        )
        val progress = if (totalDays <= 1) 1f else (selectedDay - 1f) / (totalDays - 1f)
        drawPath(
            path = path,
            color = wayfinding,
            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
        )
        val nodeX = start + (end - start) * progress.coerceIn(0f, 1f)
        drawCircle(color = TripPilotMossGreen, radius = 10.dp.toPx(), center = androidx.compose.ui.geometry.Offset(start, centerY))
        drawCircle(color = primary, radius = 12.dp.toPx(), center = androidx.compose.ui.geometry.Offset(nodeX, centerY))
        drawCircle(color = TripPilotBoardingOrange, radius = 10.dp.toPx(), center = androidx.compose.ui.geometry.Offset(end, centerY - 6.dp.toPx()))
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
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("primary_action"),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = TripPilotBoardingOrange,
            contentColor = Color(0xFF381002),
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
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
            painter = painterResource(illustration),
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
) {
    AlertDialog(
        modifier = modifier.testTag("confirm_action_sheet"),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.testTag("confirm_action")) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun EmptyStatePreview() {
    TripPilotTheme {
        EmptyState(
            title = "아직 여행이 없습니다",
            body = "새 여행을 만들면 준비할 일을 경로로 정리합니다.",
            illustration = R.drawable.trippilot_empty_trips,
            modifier = Modifier.padding(24.dp),
        )
    }
}
