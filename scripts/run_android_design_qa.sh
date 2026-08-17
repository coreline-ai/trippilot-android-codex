#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERIAL="${ANDROID_SERIAL:-}"
JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
ADB="${ADB:-${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb}"
OUT="$ROOT/app/build/reports/qa/design-loop"
SKIP_EMULATOR="${DESIGN_LOOP_SKIP_EMULATOR:-0}"

mkdir -p "$OUT"

if [[ "$SERIAL" != emulator-* && "$SKIP_EMULATOR" != "1" ]]; then
  echo "Refusing non-emulator serial: ${SERIAL:-empty}" >&2
  exit 2
fi

python3 "$ROOT/scripts/verify_phase0_design.py"
python3 "$ROOT/scripts/verify_design_contract.py"

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "A usable JDK was not found (set JAVA_HOME)" >&2
  exit 2
fi

(
  cd "$ROOT"
  JAVA_HOME="$JAVA_HOME" ./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug --console=plain --no-daemon
)

EVIDENCE="$OUT/evidence.json"
cat > "$EVIDENCE" <<'JSON'
{
  "tokenContract": "pass",
  "unitLintBuild": "pass",
  "goldenVerify": "skip",
  "captures": [],
  "physicalInstrumentation": false
}
JSON

if [[ "$SKIP_EMULATOR" == "1" ]]; then
  python3 "$ROOT/scripts/sync_design_run_state.py" --evidence "$EVIDENCE" --out-dir "$OUT" || true
  echo "Design loop static stages passed; emulator stages skipped (DESIGN_LOOP_SKIP_EMULATOR=1)"
  exit 0
fi

if [[ ! -x "$ADB" ]]; then
  echo "adb not found: $ADB" >&2
  exit 2
fi

ORIGINAL_NIGHT="$("$ADB" -s "$SERIAL" shell cmd uimode night 2>/dev/null | tr -d '\r' || true)"
ORIGINAL_FONT="$("$ADB" -s "$SERIAL" shell settings get system font_scale | tr -d '\r')"
ORIGINAL_SIZE="$("$ADB" -s "$SERIAL" shell wm size | tr -d '\r')"
ORIGINAL_DENSITY="$("$ADB" -s "$SERIAL" shell wm density | tr -d '\r')"
restore() {
  [[ -n "${ORIGINAL_NIGHT:-}" ]] && "$ADB" -s "$SERIAL" shell cmd uimode night no >/dev/null || true
  [[ -n "${ORIGINAL_FONT:-}" ]] && "$ADB" -s "$SERIAL" shell settings put system font_scale 1.0 >/dev/null || true
  "$ADB" -s "$SERIAL" shell wm size reset >/dev/null || true
  "$ADB" -s "$SERIAL" shell wm density reset >/dev/null || true
}
trap restore EXIT

"$ADB" -s "$SERIAL" shell cmd uimode night no
"$ADB" -s "$SERIAL" shell settings put system font_scale 1.0

(
  cd "$ROOT"
  JAVA_HOME="$JAVA_HOME" ANDROID_SERIAL="$SERIAL" ./gradlew :app:connectedDebugAndroidTest --console=plain --no-daemon
)
"$ROOT/scripts/run_phase5_screenshot_golden.sh" verify
python3 "$ROOT/scripts/sync_design_run_state.py" --evidence "$EVIDENCE" --out-dir "$OUT"
echo "Design loop finished: $OUT/RUN_STATE.json"
