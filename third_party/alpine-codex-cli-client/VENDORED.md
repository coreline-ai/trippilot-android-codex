# Vendored Alpine Codex runtime source

- Origin: `/Volumes/Eprojects/project_202608/alpine-codex-cli-client`
- Pinned source commit: `c8c7ca9eade2992f40ccc6b3aecf6b9a04f04b35`
- Imported: 2026-08-16
- License: GPL-3.0-or-later; see `LICENSE`.

This is a source-only, Codex runtime slice used by TripPilot's `:codex-alpine-runtime`
integration. The upstream application module, its Compose chat UI, and the `grok-cli-pack`
executable are deliberately not vendored. The upstream Gateway source is retained unchanged
for reproducibility of the official Codex Device OAuth path; TripPilot always selects Codex
and exposes no Grok model, UI, or fallback.

Build artifacts and the official Codex CLI executable are not checked in. The `codex-cli-pack`
module verifies the pinned upstream CLI archive and binary before packaging it.
