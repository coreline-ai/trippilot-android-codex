# Phase 4 Codex runtime Gate — implementation review

- Target reference: [`alpine-codex-cli-client`](https://github.com/coreline-ai/alpine-codex-cli-client) commit `c8c7ca9eade2992f40ccc6b3aecf6b9a04f04b35`
- Integration date: 2026-08-16
- Build variant with real OAuth: `secureDebug` / `release` (`arm64-v8a` only)
- Debug/instrumentation variant: deterministic `FakeCodexRuntime`; OAuth is deliberately disabled.

## Adopted reference flow

```mermaid
sequenceDiagram
    participant U as User
    participant T as TripPilot UI
    participant R as AlpineCodexRuntime
    participant C as Official Codex CLI
    participant B as External Browser

    U->>T: OpenAI account connect
    T->>R: beginLogin()
    R->>C: start app-private runtime and Device OAuth
    C-->>R: transient verification URL + user code
    R-->>T: in-memory challenge only
    U->>T: confirm browser handoff
    T->>B: ACTION_VIEW (official HTTPS URL)
    U->>B: authenticate and approve
    B-->>T: app resumes
    T->>R: refresh existing login only
    R->>C: poll login status
    C-->>R: authenticated + live models
    R-->>T: CONNECTED; no token/account details
```

The adapter copies the reference's core protection model: Alpine/PRoot runs inside the app,
its official CLI owns the credential file under a no-backup guest `HOME`, Gateway traffic is
an authenticated app-private Unix domain socket, and browser URL/code are only process-memory
values. TripPilot neither reads nor stores token, credential content, `auth.json`, raw command,
or raw argument.

## Source and build boundary

| Included | Explicitly excluded |
|---|---|
| Pinned source-only runtime, PRoot/rootfs, local Python package pack, pinned official Codex CLI lock, Gateway pack | Reference `:app`, reference Compose chat UI, terminal UI, `grok-cli-pack` executable, custom OAuth, external provider/fallback |
| `integration/codex/alpine/AlpineCodexRuntime.kt` as the sole adapter | Runtime types in TripPilot feature/UI/Room/DataStore boundary |

The upstream Gateway source is shared with its multi-agent project. TripPilot never stages a
Grok executable, never selects a non-Codex agent, and has no Grok UI/fallback. The retained
shared Gateway source is reviewed as a vendor risk before public distribution; replacing it
with an upstream Codex-only Gateway is a future hardening task.

## Verified implementation guards

- `CodexRuntimePort` has only state, transient challenge, live-model and parsed-draft methods.
- Browser is opened only after a separate confirmation dialog; no redirect handler/token exchange
  is implemented by TripPilot.
- Resume checks a pending login only; it never begins OAuth or replays an AI request.
- Each stream uses `conversationId = null`, `resumeExisting = false`, one selected live Codex
  model, bounded in-memory response accumulation, parser validation, and `rawResponse.clear()`.
- Parsed drafts still enter Phase 3's existing selection/review/one-transaction apply path.
- The adapter has no Room/DataStore/Calendar/Intent/file-write dependency.
- Runtime assets are source-vendored under `third_party/`, and build verifies rootfs/Python/CLI
  locks before packaging.

## Build evidence

| Check | Result |
|---|---|
| `python3 scripts/verify_phase1_independence.py` | PASS |
| `python3 scripts/verify_phase4_gate.py` | PASS |
| `:app:compileDebugKotlin` | PASS |
| `:app:assembleSecureDebug` | PASS; arm64 APK 116 MB, Codex CLI asset 222,231,296 bytes before APK compression |
| Device OAuth browser completion | pending: requires user-owned OpenAI account approval; no credential was requested or recorded |

## Residual gates

1. Run the installed `secureDebug` APK on an arm64 device/emulator through connect → browser
   approval → model list → structured draft → stop/logout. The user must authenticate directly.
2. Do not release before GPL-3.0 source-offer/notice obligations, Alpine/PRoot/CLI terms, signed
   arm64 artifact, and the shared-Gateway vendor review are completed.
3. Weather advisory remains information-only and must not create database or external actions;
   live research freshness needs a separate capability acceptance test.
