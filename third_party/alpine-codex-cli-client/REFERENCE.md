# alpine-codex-cli-client vendor reference pin

- Repository: `https://github.com/coreline-ai/alpine-codex-cli-client`
- Pinned commit: `c8c7ca9eade2992f40ccc6b3aecf6b9a04f04b35`
- Vendor date: 2026-08-16
- License: GPL-3.0-or-later; see [`LICENSE`](LICENSE).
- Decision and security gate: [`docs/runtime-spike.md`](../../docs/runtime-spike.md), [`docs/phase4-gate.md`](../../docs/phase4-gate.md).

TripPilot vendors the minimum reproducible runtime source slice needed for the reference
Device OAuth flow: Alpine/PRoot runtime, Python/Gateway pack, pinned official Codex CLI pack
and runtime bridge. It does **not** vendor the reference `:app`, its Compose/general-chat UI,
terminal/MCP UI, or the `grok-cli-pack` executable. Build artifacts and credential files are
not tracked. The official CLI binary is retrieved and hash-verified by the pinned pack during
the secure arm64 build.

`AlpineCodexRuntime` is TripPilot's only adapter. It forces `AgentId.CODEX`, keeps OAuth
credential ownership in the CLI-owned no-backup guest HOME, exposes no raw command/token/
credential interface, and sends only parsed structured drafts to TripPilot. The retained
shared Gateway source is an explicit release-review risk; it is not a public TripPilot feature
or a Grok fallback.
