package io.trippilot.app.integration.codex.alpine

import android.content.Context
import android.os.Process
import dev.alpine.codexclient.bridge.AgentGatewayChatRequest
import dev.alpine.codexclient.bridge.AgentGatewayClient
import dev.alpine.codexclient.bridge.AgentGatewayStreamControl
import dev.alpine.codexclient.bridge.AgentId
import dev.alpine.codexclient.bridge.AgentTurnEvent
import dev.alpine.codexclient.bridge.CodexRuntimeController
import dev.alpine.codexclient.bridge.CodexRuntimeLifecycle
import dev.alpine.codexclient.bridge.GatewayArtifactStager
import dev.alpine.codexclient.bridge.GatewayLaunchSpec
import dev.alpine.codexclient.bridge.GatewayRequestSigner
import dev.alpine.codexclient.bridge.GatewaySessionLifecycle
import dev.alpine.codexclient.cli.CodexCliArtifactProvider
import dev.alpine.codexclient.gatewaypack.CodexGatewayArtifactProvider
import dev.alpine.pythonpack.bundled.BundledPythonPackageProvider
import dev.alpine.runtime.android.AndroidPrivateDirectoryBind
import dev.alpine.runtime.android.AndroidRuntimeConfiguration
import dev.alpine.runtime.android.DefaultAndroidAlpineRuntimeFactory
import dev.alpine.runtime.api.AlpineRuntimeManager
import dev.alpine.runtime.api.RuntimeLifecycleState
import dev.alpine.runtime.api.RuntimeStartRequest
import dev.alpine.runtime.host.RuntimeHostController
import dev.alpine.runtime.pack.bundled.Alpine321Arm64Pack
import dev.alpine.runtime.pack.bundled.BundledRuntimeArtifactProvider
import io.trippilot.app.core.codex.AuthStatus
import io.trippilot.app.core.codex.CodexDeviceLoginChallenge
import io.trippilot.app.core.codex.CodexModel
import io.trippilot.app.core.codex.CodexRuntimePort
import io.trippilot.app.core.codex.DraftStreamEvent
import io.trippilot.app.core.codex.DraftStreamStage
import io.trippilot.app.core.codex.PlanStreamFailure
import io.trippilot.app.core.codex.RuntimeStatus
import io.trippilot.app.integration.codex.alpine.runtime.AndroidGatewayRuntimeHost
import io.trippilot.app.integration.codex.alpine.runtime.AppPrivatePathPolicy
import io.trippilot.app.integration.codex.alpine.runtime.CodexRuntimePaths
import io.trippilot.app.integration.codex.alpine.runtime.ConfiguredRuntimeStarter
import io.trippilot.app.integration.codex.alpine.runtime.GatewayCapabilityManager
import io.trippilot.app.integration.codex.alpine.runtime.GatewayPythonBootstrapper
import io.trippilot.app.integration.codex.alpine.runtime.OfficialCodexCliHomeProvisioner
import io.trippilot.app.integration.codex.alpine.runtime.UnixDomainSocketGatewayTransport
import io.trippilot.app.integration.codex.contract.ContractResult
import io.trippilot.app.integration.codex.contract.ReservationAnalysisRequest
import io.trippilot.app.integration.codex.contract.TripDraftParser
import io.trippilot.app.integration.codex.contract.TripPlanningRequest
import java.io.File
import java.net.URI
import java.util.concurrent.CompletionStage
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Codex-only implementation of [CodexRuntimePort].
 *
 * It ports the reference project's Device OAuth ownership model: the official CLI owns its
 * credential state under an app-private, no-backup guest HOME; TripPilot receives only a
 * transient user code/verification URL, account state, live models and in-memory parsed drafts.
 * This class never exposes a shell, terminal, gateway transport, token or credential content to
 * TripPilot feature code.
 */
@Singleton
class AlpineCodexRuntime @Inject constructor(context: Context) : CodexRuntimePort {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val operationMutex = Mutex()
    private val mutableRuntimeStatus = MutableStateFlow(RuntimeStatus.UNAVAILABLE)
    private val mutableAuthStatus = MutableStateFlow(AuthStatus.LOGIN_REQUIRED)
    private val mutableLoginChallenge = MutableStateFlow<CodexDeviceLoginChallenge?>(null)
    private var pollingJob: Job? = null
    private var activeStreamControl: AgentGatewayStreamControl? = null
    private var selectedModel: CodexModel? = null

    override val runtimeStatus = mutableRuntimeStatus.asStateFlow()
    override val authStatus = mutableAuthStatus.asStateFlow()
    override val loginChallenge = mutableLoginChallenge.asStateFlow()

    private val runtimeRoot = File(appContext.filesDir, RUNTIME_DIRECTORY)
    private val workspaceDirectory = File(runtimeRoot, WORKSPACE_DIRECTORY)
    private val codexWorkspaceDirectory = File(workspaceDirectory, CodexRuntimePaths.PRIVATE_WORKSPACE_DIRECTORY)
    private val stagingDirectory = File(codexWorkspaceDirectory, CodexRuntimePaths.STAGING_DIRECTORY)
    private val gatewayDirectory = File(codexWorkspaceDirectory, CodexRuntimePaths.GATEWAY_DIRECTORY)
    private val socketDirectory = File(workspaceDirectory, CodexRuntimePaths.TRANSPORT_DIRECTORY)

    /* Credentials and gateway capability data are both excluded from Android Backup/D2D. */
    private val codexHomeDirectory = File(appContext.noBackupFilesDir, CODEX_HOME_DIRECTORY)
    private val capabilityDirectory = File(appContext.noBackupFilesDir, CAPABILITY_DIRECTORY)
    private val wrappedCapabilityDirectory = File(appContext.noBackupFilesDir, WRAPPED_CAPABILITY_DIRECTORY)

    private val capabilityManager: GatewayCapabilityManager
    private val gatewayClient: AgentGatewayClient
    private val alpineRuntimeManager: AlpineRuntimeManager
    private val hostController: RuntimeHostController
    private val runtimeController: CodexRuntimeController
    private val configuredStarter: ConfiguredRuntimeStarter

    init {
        runtimeRoot.mkdirs()
        workspaceDirectory.mkdirs()
        AppPrivatePathPolicy.ensureDirectory(workspaceDirectory, codexWorkspaceDirectory)
        AppPrivatePathPolicy.ensureDirectory(workspaceDirectory, stagingDirectory)
        AppPrivatePathPolicy.ensureDirectory(workspaceDirectory, gatewayDirectory)
        AppPrivatePathPolicy.ensureDirectory(workspaceDirectory, socketDirectory)
        AppPrivatePathPolicy.ensureDirectory(appContext.noBackupFilesDir, codexHomeDirectory)
        AppPrivatePathPolicy.ensureDirectory(appContext.noBackupFilesDir, capabilityDirectory)
        AppPrivatePathPolicy.ensureDirectory(appContext.noBackupFilesDir, wrappedCapabilityDirectory)

        alpineRuntimeManager = DefaultAndroidAlpineRuntimeFactory().create(
            context = appContext,
            configuration = AndroidRuntimeConfiguration(
                artifactProvider = BundledRuntimeArtifactProvider(appContext, Alpine321Arm64Pack.create()),
                runtimeDirectoryName = RUNTIME_DIRECTORY,
                workspaceDirectoryName = WORKSPACE_DIRECTORY,
                privateDirectoryBinds = listOf(
                    AndroidPrivateDirectoryBind(CODEX_HOME_DIRECTORY, CodexRuntimePaths.GUEST_HOME),
                    AndroidPrivateDirectoryBind(CAPABILITY_DIRECTORY, CodexRuntimePaths.GUEST_SECURITY),
                ),
            ),
        )
        hostController = RuntimeHostController(alpineRuntimeManager)
        capabilityManager = GatewayCapabilityManager(
            context = appContext,
            capabilityDirectory = capabilityDirectory,
            wrappedDirectory = wrappedCapabilityDirectory,
        )
        gatewayClient = AgentGatewayClient(
            GatewayRequestSigner(capabilityManager),
            UnixDomainSocketGatewayTransport(
                socketFile = File(socketDirectory, CodexRuntimePaths.GATEWAY_SOCKET_FILE),
                expectedPeerUid = Process.myUid(),
            ),
        )
        runtimeController = CodexRuntimeController(
            runtimeHost = AndroidGatewayRuntimeHost(alpineRuntimeManager, hostController),
            stager = GatewayArtifactStager(::stageGatewayLaunch),
            gatewayClient = gatewayClient,
            homeDirectory = CodexRuntimePaths.GUEST_HOME,
            sessionLifecycle = object : GatewaySessionLifecycle {
                override fun onGatewayStartFailed() = cleanupAfterRuntimeStop()
                override fun onRuntimeStopped() = cleanupAfterRuntimeStop()
            },
        )
        val pythonBootstrapper = GatewayPythonBootstrapper(
            runtimeController = hostController,
            stagePackagePack = { BundledPythonPackageProvider(appContext).stage(stagingDirectory, CodexRuntimePaths.GUEST_STAGING) },
            stageGateway = { CodexGatewayArtifactProvider(appContext).stage(gatewayDirectory, CodexRuntimePaths.GUEST_GATEWAY) },
        )
        configuredStarter = ConfiguredRuntimeStarter(
            gatewayLifecycle = { runtimeController.currentState().lifecycle },
            gatewayHealthy = gatewayClient::isRuntimeHealthy,
            stopStaleGateway = runtimeController::stop,
            startAlpine = { hostController.start(RuntimeStartRequest(environment = mapOf("HOME" to CodexRuntimePaths.GUEST_HOME))) },
            preparePython = pythonBootstrapper::prepare,
            startGateway = runtimeController::start,
        )
    }

    override suspend fun beginLogin() = operationMutex.withLock {
        if (mutableLoginChallenge.value != null) return
        if (!ensureRuntimeReady()) return
        when (val account = runCatching { gatewayClient.account(AgentId.CODEX) }.getOrNull()) {
            null -> stableRuntimeError()
            else -> if (account.authenticated) {
                finishAuthenticated()
            } else {
                val login = runCatching { gatewayClient.startDeviceLogin(AgentId.CODEX) }.getOrElse {
                    mutableAuthStatus.value = AuthStatus.ERROR
                    return
                }
                val challenge = login.toSafeChallengeOrNull()
                if (challenge == null) {
                    runCatching { gatewayClient.cancelLogin(AgentId.CODEX, login.requestId) }
                    mutableAuthStatus.value = AuthStatus.ERROR
                    return
                }
                mutableLoginChallenge.value = challenge
                mutableAuthStatus.value = AuthStatus.LOGIN_IN_PROGRESS
                pollLogin(challenge.requestId, challenge.pollIntervalSeconds)
            }
        }
    }

    override suspend fun cancelLogin() = operationMutex.withLock {
        val challenge = mutableLoginChallenge.value ?: return
        pollingJob?.cancel()
        runCatching { gatewayClient.cancelLogin(AgentId.CODEX, challenge.requestId) }
        mutableLoginChallenge.value = null
        mutableAuthStatus.value = AuthStatus.CANCELLED
    }

    override suspend fun refreshAfterBrowserReturn() {
        val challenge = mutableLoginChallenge.value ?: return
        pollLoginStatus(challenge.requestId, challenge.pollIntervalSeconds)
    }

    override suspend fun availableModels(): List<CodexModel> {
        if (mutableAuthStatus.value != AuthStatus.CONNECTED || mutableRuntimeStatus.value != RuntimeStatus.READY) return emptyList()
        return runCatching { gatewayClient.models(AgentId.CODEX).map { CodexModel(it.id, it.displayName) } }
            .getOrElse { emptyList() }
    }

    override fun createPlanStream(request: TripPlanningRequest): Flow<DraftStreamEvent> = structuredStream(
        prompt = tripPlanPrompt(request),
        onCompleted = { raw ->
            when (val parsed = TripDraftParser.parseTripPlan(raw, request.startDate, request.endDate)) {
                is ContractResult.Valid -> DraftStreamEvent.TripPlanReady(parsed.value)
                is ContractResult.Invalid -> DraftStreamEvent.Failed(PlanStreamFailure.CONTRACT_REJECTED)
            }
        },
    )

    override fun analyzeReservationStream(request: ReservationAnalysisRequest): Flow<DraftStreamEvent> = structuredStream(
        prompt = reservationPrompt(request),
        onCompleted = { raw ->
            when (val parsed = TripDraftParser.parseTripPlan(raw, request.startDate, request.endDate)) {
                is ContractResult.Valid -> DraftStreamEvent.ReservationReady(parsed.value)
                is ContractResult.Invalid -> DraftStreamEvent.Failed(PlanStreamFailure.CONTRACT_REJECTED)
            }
        },
    )

    override fun weatherAdvisoryStream(request: TripPlanningRequest): Flow<DraftStreamEvent> = structuredStream(
        prompt = weatherPrompt(request),
        onCompleted = { raw ->
            when (val parsed = TripDraftParser.parseWeatherAdvisory(raw, request.startDate, request.endDate)) {
                is ContractResult.Valid -> DraftStreamEvent.WeatherReady(parsed.value)
                is ContractResult.Invalid -> DraftStreamEvent.Failed(PlanStreamFailure.CONTRACT_REJECTED)
            }
        },
    )

    override suspend fun stop() {
        activeStreamControl?.let { control -> runCatching { control.stop(gatewayClient) } }
    }

    override suspend fun logout() = operationMutex.withLock {
        pollingJob?.cancel()
        mutableLoginChallenge.value = null
        activeStreamControl = null
        selectedModel = null
        if (mutableRuntimeStatus.value == RuntimeStatus.READY) runCatching { gatewayClient.logout(AgentId.CODEX) }
        mutableAuthStatus.value = AuthStatus.LOGIN_REQUIRED
    }

    private fun structuredStream(
        prompt: String,
        onCompleted: (String) -> DraftStreamEvent,
    ): Flow<DraftStreamEvent> = flow {
        if (mutableRuntimeStatus.value != RuntimeStatus.READY) {
            emit(DraftStreamEvent.Failed(PlanStreamFailure.RUNTIME_UNAVAILABLE))
            return@flow
        }
        if (mutableAuthStatus.value != AuthStatus.CONNECTED) {
            emit(DraftStreamEvent.Failed(PlanStreamFailure.AUTH_REQUIRED))
            return@flow
        }
        val model = selectedModel ?: availableModels().firstOrNull()?.also { selectedModel = it }
        if (model == null) {
            emit(DraftStreamEvent.Failed(PlanStreamFailure.RUNTIME_UNAVAILABLE))
            return@flow
        }
        val rawResponse = StringBuilder()
        val control = gatewayClient.newStreamControl(AgentId.CODEX)
        activeStreamControl = control
        try {
            emit(DraftStreamEvent.Started)
            emit(DraftStreamEvent.Progress(DraftStreamStage.GENERATING))
            var terminalFailure: PlanStreamFailure? = null
            gatewayClient.stream(
                AgentGatewayChatRequest(
                    agentId = AgentId.CODEX,
                    conversationId = null,
                    model = model.id,
                    text = prompt,
                    resumeExisting = false,
                ),
                control,
            ).collect { event ->
                when (event) {
                    is AgentTurnEvent.Delta -> {
                        if (rawResponse.length + event.text.length > MAX_STRUCTURED_RESPONSE_CHARS) {
                            terminalFailure = PlanStreamFailure.CONTRACT_REJECTED
                            control.stop(gatewayClient)
                        } else rawResponse.append(event.text)
                    }
                    is AgentTurnEvent.Failed -> terminalFailure = PlanStreamFailure.RUNTIME_ERROR
                    else -> Unit
                }
            }
            when (terminalFailure) {
                PlanStreamFailure.CONTRACT_REJECTED -> emit(DraftStreamEvent.Failed(PlanStreamFailure.CONTRACT_REJECTED))
                PlanStreamFailure.RUNTIME_ERROR -> emit(DraftStreamEvent.Failed(PlanStreamFailure.RUNTIME_ERROR))
                else -> {
                    emit(DraftStreamEvent.Progress(DraftStreamStage.VALIDATING_RESULT))
                    emit(onCompleted(rawResponse.toString()))
                    emit(DraftStreamEvent.Completed)
                }
            }
        } catch (_: CancellationException) {
            runCatching { control.stop(gatewayClient) }
            emit(DraftStreamEvent.Stopped)
        } catch (_: Exception) {
            emit(DraftStreamEvent.Failed(PlanStreamFailure.RUNTIME_ERROR))
        } finally {
            rawResponse.clear()
            if (activeStreamControl === control) activeStreamControl = null
        }
    }

    private suspend fun ensureRuntimeReady(): Boolean {
        if (runtimeController.currentState().lifecycle == CodexRuntimeLifecycle.RUNNING && runCatching(gatewayClient::isRuntimeHealthy).getOrDefault(false)) {
            mutableRuntimeStatus.value = RuntimeStatus.READY
            return true
        }
        mutableRuntimeStatus.value = RuntimeStatus.PREPARING
        return runCatching {
            when (alpineRuntimeManager.currentState().lifecycle) {
                RuntimeLifecycleState.NOT_INSTALLED -> hostController.install().await()
                RuntimeLifecycleState.REPAIR_REQUIRED -> hostController.repair().await()
                else -> Unit
            }
            configuredStarter.start().await()
            gatewayClient.selectAgent(AgentId.CODEX)
            mutableRuntimeStatus.value = RuntimeStatus.READY
            true
        }.getOrElse {
            stableRuntimeError()
            false
        }
    }

    private fun pollLogin(requestId: String, seconds: Int) {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            delay(seconds.coerceIn(MIN_POLL_SECONDS, MAX_POLL_SECONDS) * 1_000L)
            while (mutableLoginChallenge.value?.requestId == requestId) {
                pollLoginStatus(requestId, seconds)
                if (mutableLoginChallenge.value?.requestId != requestId) break
                delay(seconds.coerceIn(MIN_POLL_SECONDS, MAX_POLL_SECONDS) * 1_000L)
            }
        }
    }

    private suspend fun pollLoginStatus(requestId: String, seconds: Int) = operationMutex.withLock {
        if (mutableLoginChallenge.value?.requestId != requestId || mutableRuntimeStatus.value != RuntimeStatus.READY) return
        val status = runCatching { gatewayClient.loginStatus(AgentId.CODEX, requestId) }.getOrElse {
            // Browser completion can race one UDS poll. Reconcile via account state without
            // issuing a new login or replaying any travel request.
            if (runCatching { gatewayClient.account(AgentId.CODEX) }.getOrNull()?.authenticated == true) finishAuthenticated()
            return
        }
        when (status.state) {
            "pending" -> Unit
            "authenticated", "completed" -> finishAuthenticated()
            "cancelled" -> finishLogin(AuthStatus.CANCELLED)
            "expired", "failed" -> finishLogin(AuthStatus.ERROR)
            else -> finishLogin(AuthStatus.ERROR)
        }
    }

    private suspend fun finishAuthenticated() {
        val models = runCatching { gatewayClient.models(AgentId.CODEX) }.getOrElse {
            mutableAuthStatus.value = AuthStatus.ERROR
            return
        }
        val selected = models.firstOrNull { it.isDefault } ?: models.firstOrNull()
        if (selected == null) {
            mutableAuthStatus.value = AuthStatus.ERROR
            return
        }
        selectedModel = CodexModel(selected.id, selected.displayName)
        finishLogin(AuthStatus.CONNECTED)
    }

    private fun finishLogin(status: AuthStatus) {
        pollingJob?.cancel()
        mutableLoginChallenge.value = null
        mutableAuthStatus.value = status
    }

    private fun stableRuntimeError() {
        pollingJob?.cancel()
        mutableLoginChallenge.value = null
        selectedModel = null
        mutableRuntimeStatus.value = RuntimeStatus.ERROR
        mutableAuthStatus.value = AuthStatus.ERROR
    }

    private fun cleanupAfterRuntimeStop() {
        runCatching { capabilityManager.clearAfterRuntimeStop() }
        val socket = File(socketDirectory, CodexRuntimePaths.GATEWAY_SOCKET_FILE)
        if (socket.exists() && socket.isFile) runCatching { socket.delete() }
    }

    private fun stageGatewayLaunch(): GatewayLaunchSpec {
        val cli = CodexCliArtifactProvider(appContext).stage(stagingDirectory, CodexRuntimePaths.GUEST_STAGING)
        CodexGatewayArtifactProvider(appContext).stage(gatewayDirectory, CodexRuntimePaths.GUEST_GATEWAY)
        OfficialCodexCliHomeProvisioner.provision(codexHomeDirectory)
        val capabilityFile = capabilityManager.rotateAndStage()
        return GatewayLaunchSpec(
            codexExecutable = cli.guestExecutablePath,
            gatewayRootDirectory = CodexRuntimePaths.GUEST_GATEWAY,
            homeDirectory = CodexRuntimePaths.GUEST_HOME,
            workspaceDirectory = "/workspace",
            capabilityFile = capabilityFile,
            socketPath = File(socketDirectory, CodexRuntimePaths.GATEWAY_SOCKET_FILE).canonicalPath,
            expectedPeerUid = Process.myUid(),
        )
    }

    private fun dev.alpine.codexclient.bridge.AgentLogin.toSafeChallengeOrNull(): CodexDeviceLoginChallenge? {
        val url = verificationUrl ?: return null
        val code = userCode ?: return null
        if (requestId.isBlank() || requestId.length > MAX_LOGIN_REQUEST_ID_CHARS || code.length !in 1..MAX_USER_CODE_CHARS || code.any(Char::isISOControl)) return null
        if (!isOfficialCodexLoginUrl(url)) return null
        val poll = (pollIntervalSeconds ?: DEFAULT_POLL_SECONDS).coerceIn(MIN_POLL_SECONDS, MAX_POLL_SECONDS)
        val expires = (expiresInSeconds ?: DEFAULT_EXPIRY_SECONDS).coerceIn(MIN_EXPIRY_SECONDS, MAX_EXPIRY_SECONDS)
        return CodexDeviceLoginChallenge(requestId, code, url, expires, poll)
    }

    private fun isOfficialCodexLoginUrl(value: String): Boolean = runCatching {
        val uri = URI(value)
        uri.scheme == "https" && uri.userInfo == null && uri.fragment == null &&
            uri.host?.lowercase()?.let { host -> host == "auth.openai.com" || host.endsWith(".openai.com") || host == "chatgpt.com" || host.endsWith(".chatgpt.com") } == true
    }.getOrDefault(false)

    private fun tripPlanPrompt(request: TripPlanningRequest): String = """
        Return exactly one JSON value that conforms to schema trippilot.trip-plan-draft version 1.
        Do not use Markdown, code fences, tools, browsing instructions, or extra text.
        destination=${request.destination}
        startDate=${request.startDate}
        endDate=${request.endDate}
        companion=${request.companion}
        budget=${request.budget}
        interests=${request.interests.joinToString("|")}
        purpose=${request.purpose}
        Suggest only reviewable itinerary, reservation, preparation, packing, source and assumption values.
    """.trimIndent()

    private fun reservationPrompt(request: ReservationAnalysisRequest): String = """
        Return exactly one JSON value that conforms to schema trippilot.trip-plan-draft version 1.
        Do not use Markdown, code fences, tools, browsing instructions, or extra text.
        destination=${request.destination}
        startDate=${request.startDate}
        endDate=${request.endDate}
        reservationHint=${request.reservationHint}
        Return only reservation candidates, linked source candidates, and assumptions. Do not execute links.
    """.trimIndent()

    private fun weatherPrompt(request: TripPlanningRequest): String = """
        Return exactly one JSON value that conforms to schema trippilot.weather-advisory-draft version 1.
        Do not use Markdown, code fences, tools, browsing instructions, or extra text.
        destination=${request.destination}
        startDate=${request.startDate}
        endDate=${request.endDate}
        This is informational only. Do not modify itinerary, reservations, preparation, packing, or external apps.
    """.trimIndent()

    private companion object {
        const val RUNTIME_DIRECTORY = "alpine-codex-runtime"
        const val WORKSPACE_DIRECTORY = "workspace"
        const val CODEX_HOME_DIRECTORY = "trippilot-codex-home"
        const val CAPABILITY_DIRECTORY = "trippilot-codex-capability"
        const val WRAPPED_CAPABILITY_DIRECTORY = "trippilot-codex-session"
        const val MAX_STRUCTURED_RESPONSE_CHARS = 65_536
        const val MAX_LOGIN_REQUEST_ID_CHARS = 512
        const val MAX_USER_CODE_CHARS = 128
        const val MIN_POLL_SECONDS = 2
        const val MAX_POLL_SECONDS = 60
        const val DEFAULT_POLL_SECONDS = 5
        const val MIN_EXPIRY_SECONDS = 60
        const val MAX_EXPIRY_SECONDS = 1_800
        const val DEFAULT_EXPIRY_SECONDS = 900
    }
}

private suspend fun <T> CompletionStage<T>.await(): T = suspendCancellableCoroutine { continuation ->
    whenComplete { value, error ->
        if (error == null) continuation.resume(value) else continuation.resumeWithException(error)
    }
    continuation.invokeOnCancellation { toCompletableFuture().cancel(true) }
}
