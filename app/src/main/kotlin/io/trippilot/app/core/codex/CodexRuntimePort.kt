package io.trippilot.app.core.codex

import io.trippilot.app.integration.codex.contract.ReservationAnalysisRequest
import io.trippilot.app.integration.codex.contract.TripPlanDraft
import io.trippilot.app.integration.codex.contract.TripPlanningRequest
import io.trippilot.app.integration.codex.contract.WeatherAdvisoryDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * TripPilot's narrow boundary to a future CLI-owned Codex runtime.
 *
 * The contract intentionally contains no token, credential-file, command, executable or raw argument access.
 */
interface CodexRuntimePort {
    val runtimeStatus: StateFlow<RuntimeStatus>
    val authStatus: StateFlow<AuthStatus>
    /** Device-code values exist only in process memory until the user cancels or completes login. */
    val loginChallenge: StateFlow<CodexDeviceLoginChallenge?>

    suspend fun beginLogin()
    suspend fun cancelLogin()
    /** Re-checks an already approved Device OAuth attempt; never starts or replays a login. */
    suspend fun refreshAfterBrowserReturn()
    suspend fun availableModels(): List<CodexModel>
    fun createPlanStream(request: TripPlanningRequest): Flow<DraftStreamEvent>
    fun analyzeReservationStream(request: ReservationAnalysisRequest): Flow<DraftStreamEvent>
    fun weatherAdvisoryStream(request: TripPlanningRequest): Flow<DraftStreamEvent>
    suspend fun stop()
    suspend fun logout()
}

/** A transient, display-only challenge produced by the CLI-owned Device OAuth flow. */
data class CodexDeviceLoginChallenge(
    val requestId: String,
    val userCode: String,
    val verificationUrl: String,
    val expiresInSeconds: Int,
    val pollIntervalSeconds: Int,
)

enum class RuntimeStatus {
    UNAVAILABLE,
    PREPARING,
    READY,
    ERROR,
}

enum class AuthStatus {
    NOT_REQUIRED,
    LOGIN_REQUIRED,
    LOGIN_IN_PROGRESS,
    CONNECTED,
    CANCELLED,
    ERROR,
}

data class CodexModel(
    val id: String,
    val displayName: String,
)

/** No raw model chunks cross this boundary. Parsed draft values are transient and review-only. */
sealed interface DraftStreamEvent {
    data object Started : DraftStreamEvent
    data class Progress(val stage: DraftStreamStage) : DraftStreamEvent
    data class TripPlanReady(val draft: TripPlanDraft) : DraftStreamEvent
    data class ReservationReady(val draft: TripPlanDraft) : DraftStreamEvent
    data class WeatherReady(val advisory: WeatherAdvisoryDraft) : DraftStreamEvent
    data object Empty : DraftStreamEvent
    data object Stopped : DraftStreamEvent
    data object Completed : DraftStreamEvent
    data class Failed(val reason: PlanStreamFailure) : DraftStreamEvent
}

enum class DraftStreamStage { VALIDATING, GENERATING, VALIDATING_RESULT }

enum class PlanStreamFailure {
    RUNTIME_UNAVAILABLE,
    AUTH_REQUIRED,
    USER_CANCELLED,
    CONTRACT_REJECTED,
    RUNTIME_ERROR,
}
