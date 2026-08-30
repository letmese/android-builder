#!/bin/bash
# download-apk.sh — Fetch latest APK artifact from GitHub Actions
# Usage: bash download-apk.sh [debug|release] [output-dir]
#
# Downloads the latest successful build's APK artifact from GitHub,
# saves it to the current dir (or your chosen dir) as "aikeyboard.apk".

set -euo pipefail

BUILD_TYPE="${1:-debug}"
OUT_DIR="${2:-.}"

REPO="letmese/android-builder"
ARTIFACT_NAME="app-${BUILD_TYPE}-apk"
OUTPUT_FILE="${OUT_DIR}/aikeyboard-${BUILD_TYPE}.apk"

echo "== Fetching latest successful ${BUILD_TYPE} build from ${REPO} =="

# Get auth token from git credential manager
TOKEN=$(printf 'protocol=https\nhost=github.com\n\n' | git credential fill 2>/dev/null | grep '^password=' | cut -d= -f2)
if [ -z "$TOKEN" ]; then
  echo "! Could not get GitHub token."
  echo "  Run: gh auth login  (or push once to let git-credential-manager save your token)"
  exit 1
fi

# Find the artifact by name
echo "  Searching for artifact: ${ARTIFACT_NAME}..."
ARTIFACTS_URL="https://api.github.com/repos/${REPO}/actions/artifacts?per_page=10"
ARTIFACT_DATA=$(curl -s -H "Authorization: token ${TOKEN}" "${ARTIFACTS_URL}")
DL_URL=$(echo "${ARTIFACT_DATA}" | python3 -c "
import json,sys
d = json.load(sys.stdin)
for a in d.get('artifacts', []):
    if a['name'] == '${ARTIFACT_NAME}' and not a.get('expired', True):
        print(a['archive_download_url'])
        break
" 2>/dev/null)

if [ -z "$DL_URL" ]; then
  echo "! No ${ARTIFACT_NAME} artifact found. Has a build run yet?"
  echo "  Trigger one at: https://github.com/${REPO}/actions/workflows/build.yml"
  exit 1
fi

echo "== Downloading APK from GitHub..."
mkdir -p "$OUT_DIR"
TMP_DIR=$(mktemp -d)
TMP_ZIP="${TMP_DIR}/artifact.zip"

curl -sL -H "Authorization: token ${TOKEN}" "${DL_URL}" -o "$TMP_ZIP"
unzip -qo "$TMP_ZIP" -d "$TMP_DIR"

# Find the .apk inside the extracted artifact
FOUND=$(find "$TMP_DIR" -name "*.apk" 2>/dev/null | head -1)
if [ -n "$FOUND" ]; then
   mv "$FOUND" "$OUTPUT_FILE"
   rm -rf "$TMP_DIR"
   SIZE=$(stat -c%s "$OUTPUT_FILE" 2>/dev/null || stat -f%z "$OUTPUT_FILE" 2>/dev/null || echo "?")
   echo "✅ Saved to: ${OUTPUT_FILE} (${SIZE} bytes)"
else
   echo "! APK not found in artifact zip"
   ls -la "$TMP_DIR"
   rm -rf "$TMP_DIR"
   exit 1
fi