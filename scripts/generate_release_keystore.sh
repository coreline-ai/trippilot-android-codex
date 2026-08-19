#!/usr/bin/env bash
set -euo pipefail

# Generates a local release keystore WITHOUT committing it to the repository.
# The output path defaults to ~/.android/trippilot-release.jks so the signing
# template in docs/signing.md can reference it via env vars.
#
# Usage: scripts/generate_release_keystore.sh [output.jks]
# Requires: JDK keytool (JAVA_HOME or PATH)

OUT="${1:-$HOME/.android/trippilot-release.jks}"
KEYTOOL="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}/bin/keytool"
[[ -x "$KEYTOOL" ]] || KEYTOOL="keytool"
command -v "$KEYTOOL" >/dev/null || { echo "keytool not found" >&2; exit 1; }

if [[ -e "$OUT" ]]; then
  echo "Refusing to overwrite existing keystore: $OUT" >&2
  exit 2
fi
mkdir -p "$(dirname "$OUT")"

read -r -p "Keystore password: " -s STORE_PASS; echo
read -r -p "Key alias [trippilot]: " ALIAS; ALIAS="${ALIAS:-trippilot}"
read -r -p "Key password [same as keystore]: " KEY_PASS; KEY_PASS="${KEY_PASS:-$STORE_PASS}"

"$KEYTOOL" -genkeypair -v \
  -keystore "$OUT" \
  -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$STORE_PASS" -keypass "$KEY_PASS" \
  -dname "CN=TripPilot, OU=Mobile, O=Coreline, C=KR"

echo
echo "Keystore written to: $OUT"
echo "Next: copy docs/signing.md's signingConfigs template into app/build.gradle.kts"
echo "and export TRIPPPILOT_KEYSTORE_* env vars (see docs/signing.md)."
