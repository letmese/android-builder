#!/bin/bash
# Build AI Keyboard APK inside kali-linux WSL
set -e
export ANDROID_HOME="$HOME/android-sdk"
PROJECT="/mnt/c/Users/xbxga/Projects/android-builder"

echo "== 1. Installing cmdline-tools if missing =="
if [ ! -d "$ANDROID_HOME/cmdline-tools" ]; then
  mkdir -p "$ANDROID_HOME"
  cd /tmp
  curl -sL -o cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q -o cmdtools.zip -d "$ANDROID_HOME/cmdline-tools-tmp"
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  mv "$ANDROID_HOME/cmdline-tools-tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

echo "== 2. Accepting licenses & installing SDK =="
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools" >/dev/null

echo "== 3. Building (gradle, first run downloads dependencies) =="
cd "$PROJECT"
chmod +x gradlew
./gradlew assembleDebug --no-daemon -q || ./gradlew assembleDebug --no-daemon

echo "== DONE =="
ls -la "$PROJECT/app/build/outputs/apk/debug/"
