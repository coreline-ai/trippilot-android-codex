#!/usr/bin/env bash
set -euo pipefail

# Deliberately emulator-only design audit. It clears only the debug package and creates one
# local fixture through Compose controls; physical devices and real OAuth are never touched.
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SERIAL="${ANDROID_SERIAL:-emulator-5556}"
PACKAGE="io.trippilot.app.debug"
TEST_CLASS="io.trippilot.app.DesignJourneyCaptureTest"
JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
ADB="${ADB:-${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb}"
OUT="$ROOT/app/build/reports/qa/design-journey"

[[ "$SERIAL" == emulator-* ]] || { echo "Refusing non-emulator serial: $SERIAL" >&2; exit 2; }
[[ -x "$JAVA_HOME/bin/java" ]] || { echo "JDK not found: $JAVA_HOME" >&2; exit 2; }
[[ -x "$ADB" ]] || { echo "adb not found: $ADB" >&2; exit 2; }
"$ADB" -s "$SERIAL" get-state >/dev/null
API_LEVEL="$("$ADB" -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"
[[ "$API_LEVEL" =~ ^[0-9]+$ ]] && (( API_LEVEL >= 28 )) || { echo "API 28+ emulator required" >&2; exit 2; }

restore() {
  "$ADB" -s "$SERIAL" shell cmd uimode night no >/dev/null || true
  "$ADB" -s "$SERIAL" shell settings put system font_scale 1.0 >/dev/null || true
  "$ADB" -s "$SERIAL" shell wm size reset >/dev/null || true
  "$ADB" -s "$SERIAL" shell wm density reset >/dev/null || true
}
trap restore EXIT
"$ADB" -s "$SERIAL" shell cmd uimode night no
"$ADB" -s "$SERIAL" shell settings put system font_scale 1.0
"$ADB" -s "$SERIAL" uninstall "$PACKAGE" >/dev/null 2>&1 || true

(
  cd "$ROOT"
  JAVA_HOME="$JAVA_HOME" ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain --no-daemon
)
"$ADB" -s "$SERIAL" install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk" >/dev/null
"$ADB" -s "$SERIAL" install -r "$ROOT/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk" >/dev/null
"$ADB" -s "$SERIAL" shell am instrument -w -r \
  -e class "$TEST_CLASS" \
  -e captureDesignJourney true \
  "${PACKAGE}.test/androidx.test.runner.AndroidJUnitRunner"

mkdir -p "$OUT"
for name in \
  01-list-featured.png 02-summary.png 03-itinerary.png 04-readiness.png \
  05-reservations.png 06-sources.png 07-draft-review.png 08-external-confirmation.png; do
  "$ADB" -s "$SERIAL" exec-out run-as "$PACKAGE" cat "files/design-journey/$name" > "$OUT/$name"
done

echo "Design journey screenshots: $OUT"
