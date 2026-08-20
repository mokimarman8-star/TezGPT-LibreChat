# Recovered Working Endpoint

The legacy `capacitor.config.ts` in this project sets the default server origin to `https://chat.librechat.ai` when `LIBRECHAT_SERVER_URL` is not supplied.

The user-provided `TezGPT-debug-final.apk` is the earlier Capacitor/web-runtime APK. Its manifest uses `com.librechat.app.MainActivity` and includes bundled web assets; it is not the current native Java APK. The APK itself did not expose a readable hardcoded endpoint in its dex/assets, but the project’s legacy configuration identifies the origin used by the working build.

A live HTTPS check of `https://chat.librechat.ai/api/config` succeeded. The response reported:

- `serverDomain`: `https://chat.librechat.ai`
- `appTitle`: `LibreChat Demo`
- email login: enabled
- registration: enabled
- social login providers: Apple, Discord, GitHub, Google, and OpenID/Hugging Face
- build commit: `16e4d1419107a31c00fa23442ef07688e64d6366`

This endpoint is a public LibreChat demo server, not the user’s own TezGPT server. It is useful as the recovered legacy compatibility endpoint, but production use should move to a user-owned HTTPS deployment and owner-branded server configuration.
