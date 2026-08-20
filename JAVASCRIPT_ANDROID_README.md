# LibreChat Android App — JavaScript Web Client Wrapper

This Android project packages the existing LibreChat web client without recreating or redesigning its UI. The same React/JavaScript client, routes, authentication flow, chat interface, model and endpoint selectors, settings, file handling, and other web features are built into the Android package through Capacitor.

## Build the real APK

From the repository root:

```bash
npm ci
LIBRECHAT_SERVER_URL=https://chat.librechat.ai npm run mobile:sync
cd android
./gradlew assembleDebug
```

The debug APK is generated at:

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

For a release build:

```bash
cd android
./gradlew assembleRelease
```

The release output is unsigned unless a signing configuration is supplied.

## Use another LibreChat deployment

Set `LIBRECHAT_SERVER_URL` to the HTTPS URL of the user’s LibreChat deployment before running `npm run mobile:sync`. The value is used by `capacitor.config.ts`; no API key, provider key, password, or personal access token is embedded in the APK.

## Android capabilities

The manifest contains internet and network-state permissions plus runtime-capable permissions for camera, microphone, notifications, vibration, and Android media/file access. `NativePermissionsPlugin.java` exposes `NativePermissions.request()` and `NativePermissions.status()` to the JavaScript client. On a native Android runtime, the web client requests these permissions before rendering; in a normal browser, no permission request is made and the web UI behaves as before.

The app remains a real LibreChat client rather than an informational shell. It connects to the configured backend, preserves the existing web UI, and uses the same server-side authentication, chat, model selection, endpoint selection, file, agent, and settings flows that the web client supports. System permission dialogs are Android-native and are the only additional UI introduced by the wrapper.

## Security

The repository and Android package do not contain the GitHub token that was posted in chat. That token should be revoked immediately if it has not already been revoked.
