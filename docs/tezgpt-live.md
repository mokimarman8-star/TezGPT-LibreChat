# TezGPT Live backend

TezGPT Live is an optional authenticated control plane for Gemini Live voice and screen sessions. The server mints short-lived, single-use Gemini ephemeral tokens; the browser or Android client then opens the Live API connection with that token. `GEMINI_API_KEY` never leaves the server.

## Environment

Set these values only in the server runtime environment:

```bash
GEMINI_API_KEY=replace-with-server-side-key
TEZGPT_LIVE_MODEL=gemini-3.1-flash-live-preview
TEZGPT_LIVE_SESSION_MINUTES=30
TEZGPT_LIVE_NEW_SESSION_SECONDS=60
```

The server exposes the following authenticated endpoints:

| Endpoint | Purpose |
|---|---|
| `GET /api/live/config` | Reports whether Live is configured and returns bounded media/session capabilities. |
| `POST /api/live/session` | Requests a single-use, short-lived ephemeral token for one Live API session. |

The token is constrained to the configured model, `AUDIO` response modality, and session resumption. The client is expected to send 16 kHz PCM microphone audio and at most one JPEG screen frame per second; the server does not persist raw media by default.

## Security boundary

The route requires the normal LibreChat JWT authentication middleware. It returns `503 LIVE_NOT_CONFIGURED` when the provider key is absent and returns generic errors for provider failures. Device actions are reported as disabled and approval-required. Model output must not be treated as permission to tap, type, execute shell commands, send messages, or perform transactions.

The Android host separately requests `RECORD_AUDIO` and MediaProjection consent at the point of use. Declining screen sharing falls back to voice-only mode. A future AccessibilityService action layer must be user-enabled in Android Settings, use typed schemas and scoped resources, require confirmation for mutating actions, and record an audit event.

## Client build

Build the companion extension/Android host with `TEZGPT_LIVE_ENDPOINT` set to the HTTPS base URL of this LibreChat deployment. Only the base URL is injected into the client bundle; never set `GEMINI_API_KEY` in the extension or Android build environment.
