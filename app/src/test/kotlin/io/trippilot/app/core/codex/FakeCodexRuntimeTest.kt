package io.trippilot.app.core.codex

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeCodexRuntimeTest {
    @Test
    fun `login state transitions never expose credentials`() = runBlocking {
        val runtime = FakeCodexRuntime()

        assertEquals(RuntimeStatus.READY, runtime.runtimeStatus.value)
        assertEquals(AuthStatus.LOGIN_REQUIRED, runtime.authStatus.value)

        runtime.beginLogin()
        assertEquals(AuthStatus.LOGIN_IN_PROGRESS, runtime.authStatus.value)

        runtime.completeLoginForTestOrPreview()
        assertEquals(AuthStatus.CONNECTED, runtime.authStatus.value)
        assertEquals("fake-trip-planner", runtime.availableModels().single().id)

        runtime.logout()
        assertEquals(AuthStatus.LOGIN_REQUIRED, runtime.authStatus.value)
        assertTrue(runtime.availableModels().isEmpty())
    }

    @Test
    fun `draft stream is rejected until a connection exists`() = runBlocking {
        val runtime = FakeCodexRuntime()
        val request = PlanRequest(tripId = "trip-1")

        assertEquals(
            listOf(PlanStreamEvent.Failed(PlanStreamFailure.AUTH_REQUIRED)),
            runtime.createPlanStream(request).toList(),
        )

        runtime.beginLogin()
        runtime.completeLoginForTestOrPreview()

        assertEquals(
            listOf(PlanStreamEvent.Started, PlanStreamEvent.Completed),
            runtime.createPlanStream(request).toList(),
        )
    }

    @Test
    fun `runtime failure blocks streaming and changes only runtime state`() = runBlocking {
        val runtime = FakeCodexRuntime()
        runtime.failRuntimeForTestOrPreview()

        assertEquals(RuntimeStatus.ERROR, runtime.runtimeStatus.value)
        assertEquals(AuthStatus.ERROR, runtime.authStatus.value)
        assertEquals(
            listOf(PlanStreamEvent.Failed(PlanStreamFailure.RUNTIME_UNAVAILABLE)),
            runtime.createPlanStream(PlanRequest("trip-1")).toList(),
        )
    }
}
