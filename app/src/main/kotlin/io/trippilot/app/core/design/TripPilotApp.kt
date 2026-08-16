package io.trippilot.app.core.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.trippilot.app.R
import io.trippilot.app.core.codex.AuthStatus
import io.trippilot.app.core.codex.FakeCodexRuntime
import io.trippilot.app.core.codex.RuntimeStatus
import kotlinx.coroutines.launch

@Composable
fun TripPilotApp() {
    val runtime = remember { FakeCodexRuntime() }
    val runtimeStatus by runtime.runtimeStatus.collectAsState()
    val authStatus by runtime.authStatus.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TripPilotTopBar(title = "TripPilot") },
    ) { padding ->
        PhaseOneRuntimeScreen(
            padding = padding,
            runtimeStatus = runtimeStatus,
            authStatus = authStatus,
            onBeginFakeLogin = { scope.launch { runtime.beginLogin() } },
            onCompleteFakeLogin = runtime::completeLoginForTestOrPreview,
            onLogout = { scope.launch { runtime.logout() } },
        )
    }
}

@Composable
private fun PhaseOneRuntimeScreen(
    padding: PaddingValues,
    runtimeStatus: RuntimeStatus,
    authStatus: AuthStatus,
    onBeginFakeLogin: () -> Unit,
    onCompleteFakeLogin: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("phase_one_runtime_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("여행을 기록하는 경로", style = MaterialTheme.typography.displaySmall)
        Text(
            "로컬 여행 기능은 로그인 없이 사용할 수 있습니다. AI는 검토 가능한 초안만 제안합니다.",
            style = MaterialTheme.typography.bodyLarge,
        )
        RouteRibbon(completedDays = 1, totalDays = 4, selectedDay = 2)
        StatusChip(label = statusLabel(runtimeStatus, authStatus), modifier = Modifier.testTag("runtime_status"))
        EmptyState(
            title = if (authStatus == AuthStatus.CONNECTED) "초안 연결 준비됨" else "Codex 연결이 필요합니다",
            body = if (authStatus == AuthStatus.CONNECTED) {
                "여행 계획은 초안으로만 전달되며, 반영 전 직접 검토합니다."
            } else {
                "로그인을 시작해도 TripPilot은 인증 정보나 대화 원문을 저장하지 않습니다."
            },
            illustration = R.drawable.trippilot_ai_connection_required,
        )
        when (authStatus) {
            AuthStatus.LOGIN_REQUIRED, AuthStatus.CANCELLED, AuthStatus.ERROR -> PrimaryAction(
                label = "Codex에서 로그인 시작",
                onClick = onBeginFakeLogin,
                enabled = runtimeStatus == RuntimeStatus.READY,
            )
            AuthStatus.LOGIN_IN_PROGRESS -> PrimaryAction(
                label = "미리보기 로그인 완료 상태 보기",
                onClick = onCompleteFakeLogin,
            )
            AuthStatus.CONNECTED -> PrimaryAction(label = "미리보기 연결 해제", onClick = onLogout)
            AuthStatus.NOT_REQUIRED -> Unit
        }
    }
}

private fun statusLabel(runtimeStatus: RuntimeStatus, authStatus: AuthStatus): String = when {
    runtimeStatus == RuntimeStatus.ERROR -> "런타임 오류"
    runtimeStatus == RuntimeStatus.PREPARING -> "런타임 준비 중"
    runtimeStatus == RuntimeStatus.UNAVAILABLE -> "런타임을 사용할 수 없음"
    authStatus == AuthStatus.CONNECTED -> "연결됨"
    authStatus == AuthStatus.LOGIN_IN_PROGRESS -> "로그인 진행 중"
    authStatus == AuthStatus.CANCELLED -> "로그인 취소됨"
    else -> "로그인 필요"
}
