#!/usr/bin/env bash
set -euo pipefail

# This is intentionally an emulator-only generation path. A physical-device benchmark is a later
# release owner approval step; it must not be started by a local development script.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ADB="${ADB:-${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb}"
JAVA_HOME="${JAVA_HOME:-}"
if [[ ! -x "$JAVA_HOME/bin/java" ]]; then JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"; fi
[[ -x "$ADB" && -x "$JAVA_HOME/bin/java" ]] || { echo "Set ADB/ANDROID_HOME and JAVA_HOME" >&2; exit 2; }

SERIAL="${ANDROID_SERIAL:-}"
if [[ -z "$SERIAL" ]]; then
  while read -r candidate state _; do
    [[ "$candidate" == emulator-* && "$state" == device ]] || continue
    api="$("$ADB" -s "$candidate" shell getprop ro.build.version.sdk < /dev/null | tr -d '\r')"
    if [[ "$api" =~ ^[0-9]+$ ]] && (( api >= 33 )); then SERIAL="$candidate"; break; fi
  done < <("$ADB" devices)
fi
[[ "$SERIAL" == emulator-* ]] || { echo "Refusing non-emulator serial: $SERIAL" >&2; exit 2; }
API="$("$ADB" -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"
[[ "$API" =~ ^[0-9]+$ ]] && (( API >= 33 )) || { echo "Baseline Profile generation needs API 33+: $API" >&2; exit 2; }

cd "$ROOT"
JAVA_HOME="$JAVA_HOME" ANDROID_SERIAL="$SERIAL" ./gradlew :app:generateBaselineProfile \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile \
  --console=plain --no-daemon
