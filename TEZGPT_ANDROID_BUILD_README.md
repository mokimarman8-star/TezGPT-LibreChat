# TezGPT Native Android Build Guide

TezGPT is the native Java Android client in `android/`. It uses the package `com.tezgpt.app` and the application label `TezGPT`. The Android client does not embed the LibreChat web application, does not use WebView, and does not depend on Capacitor or Cordova.

## Configure the server URL

The server URL is injected at build time. The recovered default from the previously working legacy project is `https://chat.librechat.ai`, so the APK no longer opens with an empty-URL configuration error. Do not commit private server credentials or tokens into the Android project. For production, override the default with your own HTTPS TezGPT domain.

```bash
cd /home/ubuntu/LibreChat-native/android
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/home/ubuntu/android-sdk
./gradlew clean test assembleDebug \\
  -PTEZGPT_API_BASE_URL=https://your-owned-tezgpt-domain.example \\
  --no-daemon
```

The URL must use HTTPS. Cleartext HTTP is disabled by the manifest and `network_security_config.xml`. The recovered compatibility endpoint is the public LibreChat demo at `https://chat.librechat.ai`; it is not a user-owned production server. A self-hosted TezGPT deployment should replace it through `-PTEZGPT_API_BASE_URL=...`. The server must expose the authentication, configuration, conversation, chat, agent, and tool routes documented in the project server guide.

## Release build

The release build is R8-shrunk and resource-shrunk but intentionally unsigned in this sandbox.

```bash
./gradlew clean test assembleDebug assembleRelease \
  -PTEZGPT_API_BASE_URL=https://your-real-tezgpt-domain.example \
  --no-daemon
```

Outputs:

- `android/app/build/outputs/apk/debug/app-debug.apk`
- `android/app/build/outputs/apk/release/app-release-unsigned.apk`

Sign the release APK with the owner's private Android keystore before distribution. Keep the keystore, passwords, server secrets, OAuth client secrets, and signing credentials outside Git.

## Verification

Run the native audit before publishing:

```bash
/home/ubuntu/skills/tezgpt-native-android/scripts/audit_native_android.sh /home/ubuntu/LibreChat-native
```

The audit checks for placeholder server URLs, Capacitor/WebView runtime dependencies, and obvious embedded private keys. It is not a substitute for penetration testing, server security review, or production device testing.

## Current native feature scope

The app currently provides native login, registration, logout, encrypted session persistence, HTTPS chat streaming, endpoint selection, model selection, new-chat reset, native error handling, and TezGPT branding. The agent API client, typed run state, tool discovery model, WorkManager synchronization foundation, constrained FileProvider, and security policies are included for the next platform layer.

Conversation history navigation, file picker/upload UI, memory screens, MCP management, approval dialogs, artifact viewing, OAuth providers, voice features, and scheduled-job controls remain planned native UI work. Their server-ready status is tracked in `docs/native-android/tezgpt-capability-matrix.md`.

## Server deployment

Use the TezGPT server deployment files in the repository:

- `TEZGPT_SERVER_README.md`
- `TEZGPT.env.example`
- `docker-compose.tezgpt.yml`
- `librechat.tezgpt.example.yaml`

Replace owner and domain placeholders in policy/help pages before public deployment. Preserve upstream license notices where required by the source project.
