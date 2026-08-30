#!/bin/bash
# Build AI Keyboard APK inside kali-linux WSL
set -e
export ANDROID_HOME="$HOME/android-sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
PROJECT="/mnt/c/Users/xbxga/Projects/android-builder"

# Match HF Space Testing: Gradle 8.11.1, Android 35, build-tools 35.0.0 & 34.0.0
GRADLE_VERSION="8.11.1"

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
sdkmanager "platforms;android-35" "build-tools;35.0.0" "build-tools;34.0.0" "platform-tools" >/dev/null

echo "== 3. Setting up Gradle ${GRADLE_VERSION} (pinned, like HF Space) =="
if [ ! -d "/opt/gradle/gradle-${GRADLE_VERSION}" ]; then
  cd /tmp
  curl -sL "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o gradle.zip
  unzip -q gradle.zip
  sudo mv gradle-${GRADLE_VERSION} /opt/gradle
  rm gradle.zip
fi
export PATH="/opt/gradle/gradle-${GRADLE_VERSION}/bin:$PATH"

echo "== 4. Building (gradle, first run downloads dependencies) =="
cd "$PROJECT"
chmod +x gradlew
./gradlew assembleDebug --no-daemon -q || ./gradlew assembleDebug --no-daemon

echo "== DONE =="
ls -la "$PROJECT/app/build/outputs/apk/debug/"
