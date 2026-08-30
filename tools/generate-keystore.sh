#!/bin/bash
# generate-keystore.sh — Create a debug keystore for local release builds
# Usage: bash generate-keystore.sh
#
# Creates ~/.android/debug.keystore with the standard debug credentials.
# Required: Java JDK (keytool) installed.

set -euo pipefail

KEYSTORE_DIR="$HOME/.android"
KEYSTORE_FILE="${KEYSTORE_DIR}/debug.keystore"

if [ -f "$KEYSTORE_FILE" ]; then
  echo "✅ Keystore already exists at ${KEYSTORE_FILE}"
  exit 0
fi

echo "== Generating debug keystore =="
mkdir -p "$KEYSTORE_DIR"
keytool -genkey -v -keystore "$KEYSTORE_FILE" \
  -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"

echo "✅ Created: ${KEYSTORE_FILE}"
echo "   alias: androiddebugkey"
echo "   password: android"
echo ""
echo "Now run: ./gradlew assembleRelease"