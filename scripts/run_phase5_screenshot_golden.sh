#!/usr/bin/env bash
set -euo pipefail

# Deterministic Compose screenshot regression runner. This script deliberately targets an
# emulator only and always resets the debug app data before each run; it must not touch a user's
# physical device or production data.
MODE="${1:-verify}"
if [[ "$MODE" != "verify" && "$MODE" != "update" ]]; then
  echo "Usage: $0 [verify|update]" >&2
  exit 2
fi

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERIAL="${ANDROID_SERIAL:-}"
PACKAGE="io.trippilot.app.debug"
TEST_CLASS="io.trippilot.app.Phase5ScreenshotGoldenTest"
JAVA_HOME="${JAVA_HOME:-}"
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi
GOLDEN_DIR="$ROOT/app/src/androidTest/assets/screenshot-goldens"
ADB="${ADB:-${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb}"

if [[ ! -x "$JAVA_HOME/bin/java" ]]; then
  echo "A usable JDK was not found (set JAVA_HOME)" >&2
  exit 2
fi
if [[ ! -x "$ADB" ]]; then
  echo "Android platform-tools adb not found: $ADB (set ADB or ANDROID_HOME)" >&2
  exit 2
fi

if [[ -z "$SERIAL" ]]; then
  while read -r candidate state _; do
    [[ "$candidate" == emulator-* && "$state" == device ]] || continue
    api_level="$("$ADB" -s "$candidate" shell getprop ro.build.version.sdk < /dev/null | tr -d '\r')"
    if [[ "$api_level" =~ ^[0-9]+$ ]] && (( api_level >= 28 )); then
      SERIAL="$candidate"
      break
    fi
  done < <("$ADB" devices)
fi
if [[ "$SERIAL" != emulator-* ]]; then
  echo "Refusing non-emulator serial: $SERIAL" >&2
  exit 2
fi
"$ADB" -s "$SERIAL" get-state >/dev/null
API_LEVEL="$("$ADB" -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"
if [[ ! "$API_LEVEL" =~ ^[0-9]+$ ]] || (( API_LEVEL < 28 )); then
  echo "Compose dialog screenshots require an API 28+ emulator; got API ${API_LEVEL:-unknown} on $SERIAL" >&2
  exit 2
fi
"$ADB" -s "$SERIAL" shell cmd uimode night no
"$ADB" -s "$SERIAL" shell settings put system font_scale 1.0
"$ADB" -s "$SERIAL" uninstall "$PACKAGE" >/dev/null 2>&1 || true

run_test() {
  local update_argument="${1:-false}"
  (
    cd "$ROOT"
    JAVA_HOME="$JAVA_HOME" ./gradlew assembleDebug assembleDebugAndroidTest --console=plain --no-daemon
  )
  "$ADB" -s "$SERIAL" install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk" >/dev/null
  "$ADB" -s "$SERIAL" install -r "$ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk" >/dev/null
  local result exit_code
  set +e
  result="$("$ADB" -s "$SERIAL" shell am instrument -w -r \
    -e class "$TEST_CLASS" \
    -e updateGoldens "$update_argument" \
    "${PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner" 2>&1)"
  exit_code=$?
  set -e
  printf '%s\n' "$result"
  if (( exit_code != 0 )) || [[ "$result" == *"FAILURES!!!" ]] || [[ "$result" == *"INSTRUMENTATION_FAILED" ]] || [[ "$result" != *"OK (1 test)"* ]]; then
    echo "Screenshot golden instrumentation failed" >&2
    exit 1
  fi
}

if [[ "$MODE" == "update" ]]; then
  run_test true
  for name in 01-trip-list-empty.png 02-trip-summary.png 03-draft-review.png 04-external-confirmation.png; do
    "$ADB" -s "$SERIAL" exec-out run-as "$PACKAGE" cat "files/screenshot-goldens/$name" > "$GOLDEN_DIR/$name"
  done
fi

"$ADB" -s "$SERIAL" uninstall "$PACKAGE" >/dev/null 2>&1 || true
run_test false
