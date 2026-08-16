package io.trippilot.app.core.codex

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

    suspend fun beginLogin()
    suspend fun availableModels(): List<CodexModel>
    fun createPlanStream(request: PlanRequest): Flow<PlanStreamEvent>
    suspend fun stop()
    suspend fun logout()
}

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

data class PlanRequest(
    val tripId: String,
    val requestVersion: Int = 1,
)

sealed interface PlanStreamEvent {
    data object Started : PlanStreamEvent
    data object Completed : PlanStreamEvent
    data class Failed(val reason: PlanStreamFailure) : PlanStreamEvent
}

enum class PlanStreamFailure {
    RUNTIME_UNAVAILABLE,
    AUTH_REQUIRED,
    USER_CANCELLED,
    CONTRACT_REJECTED,
}
