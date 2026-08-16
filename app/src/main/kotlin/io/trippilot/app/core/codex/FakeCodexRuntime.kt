package io.trippilot.app.core.codex

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

/** Credential-free runtime used by Phase 1 UI and contract tests. */
class FakeCodexRuntime : CodexRuntimePort {
    private val mutableRuntimeStatus = MutableStateFlow(RuntimeStatus.READY)
    private val mutableAuthStatus = MutableStateFlow(AuthStatus.LOGIN_REQUIRED)

    override val runtimeStatus = mutableRuntimeStatus.asStateFlow()
    override val authStatus = mutableAuthStatus.asStateFlow()

    override suspend fun beginLogin() {
        if (mutableRuntimeStatus.value == RuntimeStatus.READY) {
            mutableAuthStatus.value = AuthStatus.LOGIN_IN_PROGRESS
        }
    }

    fun completeLoginForTestOrPreview() {
        mutableAuthStatus.value = AuthStatus.CONNECTED
    }

    fun cancelLoginForTestOrPreview() {
        mutableAuthStatus.value = AuthStatus.CANCELLED
    }

    fun failRuntimeForTestOrPreview() {
        mutableRuntimeStatus.value = RuntimeStatus.ERROR
        mutableAuthStatus.value = AuthStatus.ERROR
    }

    override suspend fun availableModels(): List<CodexModel> =
        if (mutableAuthStatus.value == AuthStatus.CONNECTED) {
            listOf(CodexModel(id = "fake-trip-planner", displayName = "Trip planner (fake)"))
        } else {
            emptyList()
        }

    override fun createPlanStream(request: PlanRequest): Flow<PlanStreamEvent> = flow {
        when {
            mutableRuntimeStatus.value != RuntimeStatus.READY -> emit(
                PlanStreamEvent.Failed(PlanStreamFailure.RUNTIME_UNAVAILABLE),
            )
            mutableAuthStatus.value != AuthStatus.CONNECTED -> emit(
                PlanStreamEvent.Failed(PlanStreamFailure.AUTH_REQUIRED),
            )
            else -> {
                emit(PlanStreamEvent.Started)
                emit(PlanStreamEvent.Completed)
            }
        }
    }

    override suspend fun stop() = Unit

    override suspend fun logout() {
        mutableAuthStatus.value = AuthStatus.LOGIN_REQUIRED
    }
}
