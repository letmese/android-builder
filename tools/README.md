# Tools

Helper scripts for the Android Builder project.

| Script | What it does |
|---|---|
| `download-apk.sh` | Downloads the latest APK artifact from GitHub Actions |
| `generate-keystore.sh` | Creates a debug keystore for local release builds |

## download-apk.sh

```bash
bash tools/download-apk.sh [debug|release] [output-dir]
```

Fetches the latest successful CI build's APK from GitHub. Requires `git-credential-manager` to be logged in (run `git push` once).

## generate-keystore.sh

```bash
bash tools/generate-keystore.sh
```

Creates `~/.android/debug.keystore` so you can run `./gradlew assembleRelease` locally. Requires JDK (`keytool`).