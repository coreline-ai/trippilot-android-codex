#!/usr/bin/env python3
"""Audit the vendored Codex-only adapter without exposing CLI/OAuth internals to TripPilot."""

from __future__ import annotations

import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/kotlin/io/trippilot/app"
ADAPTER = APP / "integration/codex/alpine"
VENDOR = ROOT / "third_party/alpine-codex-cli-client"


def fail(message: str) -> None:
    print(f"FAIL: {message}")
    raise SystemExit(1)


def text(root: Path) -> str:
    return "\n".join(
        path.read_text(encoding="utf-8")
        for path in sorted(root.rglob("*"))
        if path.suffix in {".kt", ".kts", ".py", ".md", ".json"}
    )


def main() -> None:
    reference = (VENDOR / "REFERENCE.md").read_text(encoding="utf-8")
    vendor = (VENDOR / "VENDORED.md").read_text(encoding="utf-8")
    port = (APP / "core/codex/CodexRuntimePort.kt").read_text(encoding="utf-8")
    app_files = [path for path in APP.rglob("*.kt") if not path.is_relative_to(ADAPTER)]
    public_app = "\n".join(path.read_text(encoding="utf-8") for path in app_files)
    adapter = text(ADAPTER)

    if "c8c7ca9eade2992f40ccc6b3aecf6b9a04f04b35" not in reference or "GPL-3.0-or-later" not in vendor:
        fail("pinned GPL vendor provenance is missing")
    required_vendor_paths = (
        "alpine-runtime-pack-bundled/src/main/assets/alpine-minirootfs.tar.gz.asset",
        "alpine-python-pack-bundled/src/main/python-pack/packages/python3-3.12.14-r0.apk",
        "codex-cli-pack/codex-cli.lock.json",
        "codex_gateway/agent_gateway.py",
    )
    for relative in required_vendor_paths:
        if not (VENDOR / relative).is_file():
            fail(f"required pinned runtime artifact/source is missing: {relative}")
    settings = (ROOT / "settings.gradle.kts").read_text(encoding="utf-8")
    if (VENDOR / "grok-cli-pack").exists() or "grok-cli-pack" in settings or "GrokAgentAdapter" in public_app:
        fail("TripPilot must not package a Grok executable or expose a Grok app path")
    if not ADAPTER.is_dir() or "class AlpineCodexRuntime" not in adapter:
        fail("Codex-only Alpine adapter is missing")
    if "AgentId.GROK" in adapter:
        fail("adapter must select only Codex")
    if "selectAgent(AgentId.CODEX)" not in adapter:
        fail("adapter must force the Codex gateway selection")
    if "conversationId = null" not in adapter or "resumeExisting = false" not in adapter:
        fail("adapter must reject conversation resume/history")
    for marker in ("rawCommand", "rawArgument", "rawExecutable", "auth.json", "localhost", "127.0.0.1"):
        if marker in port or marker in public_app:
            fail(f"public TripPilot boundary/source contains prohibited value: {marker}")
    allowed_sensitive_files = {
        ADAPTER / "runtime/OfficialCodexCliHomeProvisioner.kt",
        ADAPTER / "runtime/UnixDomainSocketGatewayTransport.kt",
    }
    for path in ADAPTER.rglob("*.kt"):
        value = path.read_text(encoding="utf-8")
        if ("auth.json" in value or "localhost" in value or "127.0.0.1" in value) and path not in allowed_sensitive_files:
            fail(f"unexpected credential/UDS implementation outside reviewed carrier: {path.relative_to(ROOT)}")
    provisioner = (ADAPTER / "runtime/OfficialCodexCliHomeProvisioner.kt").read_text(encoding="utf-8")
    if re.search(r"auth\.json.*(?:readText|readBytes|inputStream|bufferedReader)", provisioner, re.DOTALL):
        fail("CLI credential content must not be read")
    carrier = (ADAPTER / "runtime/UnixDomainSocketGatewayTransport.kt").read_text(encoding="utf-8")
    if "LocalSocketAddress.Namespace.FILESYSTEM" not in carrier or "ServerSocket" in carrier:
        fail("gateway carrier must remain a client-only app-private UDS")
    runtime = (ADAPTER / "AlpineCodexRuntime.kt").read_text(encoding="utf-8")
    for marker in ("TripDraftParser.parseTripPlan", "TripDraftParser.parseWeatherAdvisory", "rawResponse.clear()", "conversationId = null"):
        if marker not in runtime:
            fail(f"structured draft/privacy guard missing: {marker}")

    print("PASS: pinned GPL Codex runtime source and supply-chain inputs are present")
    print("PASS: public TripPilot code exposes no credential, raw CLI, localhost, or Grok surface")
    print("PASS: adapter forces Codex, uses client-only UDS, no conversation resume, and clears raw draft text")


if __name__ == "__main__":
    main()
