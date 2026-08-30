# Android Builder Project Context

This folder contains the AI keyboard app (com.letmese.aikeyboard), SwiftKey-style redesign done.

## Current state
- APK build pending. Builds run either:
  - Locally via WSL kali-linux (`./wsl-build.sh`, JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64)
  - Or via GitHub Actions CI (no local SDK on this laptop — ThinkPad W520, 12GB RAM)

## How to build
- Local: `bash wsl-build.sh` (uses WSL kali-linux with JDK 21)
- CI: push to GitHub repo and let the Actions workflow build the APK
- Release build: run `bash tools/generate-keystore.sh` once, then `./gradlew assembleRelease`

## Tools
| Tool | Description |
|---|---|
| `tools/download-apk.sh` | Download the latest APK artifact from GitHub Actions |
| `tools/generate-keystore.sh` | Generate debug keystore for local release builds |
| `.github/workflows/build.yml` | CI: builds debug (push) + release (push) APKs on GitHub runners |
| `.github/workflows/emulator-test.yml` | CI: boots Android emulator, installs APK, runs smoke tests |

## CI features
- Build on push triggers both debug + release APKs automatically
- Manual workflow dispatch lets you pick debug or release
- Release builds are signed with a generated debug keystore (env-var based)
- Emulator test: Android 10, boots, installs, launches, tests IME, captures crash log
- ProGuard/R8 minification enabled for release builds

Any Hermes session (WhatsApp or desktop) working in this folder should read this file first and keep the build status updated here after major progress.
