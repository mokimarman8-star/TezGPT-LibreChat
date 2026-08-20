# TezGPT Native Android Capability Matrix

**Package:** `com.tezgpt.app`  
**Application label:** `TezGPT`  
**Client architecture:** Native Java Android; no WebView, Capacitor, Cordova, or remote HTML runtime.

## Current implementation

| Capability | Native Android status | Server requirement or limitation |
|---|---|---|
| Native launcher and Android lifecycle | Implemented | None beyond normal Android packaging. |
| Login, registration, logout, and encrypted session persistence | Implemented | TezGPT server must expose the documented `/api/auth/*` routes. Session data is stored with Android Keystore-backed encryption. |
| HTTPS chat streaming | Implemented | Build with a real HTTPS `TEZGPT_API_BASE_URL`; server must expose `/api/ask`. |
| Endpoint and model selection | Implemented | The server must expose compatible endpoint/model configuration. |
| SSE response handling | Implemented | Server must return `text/event-stream` data compatible with the client parser. |
| TezGPT branding | Implemented | Owner details in policy pages should be filled before public distribution. |
| Native agent streaming client | Implemented as platform foundation | Server must expose `/api/agents/chat`. The current screen still presents the established chat UI; a richer run panel is planned. |
| Typed agent run state | Implemented | Server responses should provide run status, progress, events, and artifacts. |
| Durable agent status worker | Implemented as WorkManager foundation | A production notification and resume policy still needs to be connected to product UX. |
| Tool discovery model and API | Implemented as platform foundation | Server must expose `/api/agents/tools`; tool execution remains server-controlled. |
| Human-in-the-loop approvals | Model-ready, native approval UI planned | Approval checkpoints must be emitted by the server and presented in a dedicated native dialog or run panel. |
| Secure FileProvider | Implemented | File upload/picker UI still needs to be connected to server upload endpoints. |
| HTTPS-only network policy | Implemented | Cleartext HTTP is blocked. Use a valid certificate chain on the TezGPT server. |
| Release R8 shrinking | Implemented | Release APK is currently unsigned and must be signed with the owner's Android keystore. |
| Local conversation reset | Implemented | Full conversation sidebar and history navigation are planned. |

## Server-ready, native UI planned

| Capability | Current state | Next native milestone |
|---|---|---|
| Conversation sidebar and search | Server route exists in the upstream API surface; not yet exposed in the Android UI | Add a RecyclerView drawer with paging, search, rename, archive, and delete actions. |
| File picker and upload | Android permissions and constrained FileProvider are present | Add Storage Access Framework picker, upload progress, cancellation, and attachment chips. |
| Memory management | Architecture documented; no final native screen | Add memory list, per-item delete, scope controls, and consent settings. |
| MCP/custom tool management | Tool discovery foundation exists | Add server-managed tool catalog, risk labels, scope controls, and approval flows. |
| Multi-model orchestration | Endpoint/model selectors exist | Add a server-provided route plan, fallback state, and cost/latency telemetry. |
| Scheduled and background jobs | WorkManager dependency and worker foundation exist | Add job creation, recurrence controls, notification actions, and server webhook reconciliation. |
| Agent artifacts | Agent model includes artifact references | Add native artifact viewer and secure share/export flow. |
| OAuth providers | Not implemented natively | Add provider-specific Android client IDs, redirect URIs, signing fingerprints, and server configuration for Google/GitHub/Discord. |
| Voice input/output | Permissions are declared; no native voice UX | Add Android speech recognition, upload/audio playback, and server TTS/STT contracts. |
| Offline queue | Not implemented | Add encrypted local queue and explicit retry/conflict policy. |

## Release gates

The project is ready for a controlled developer build when the following checks pass:

1. `./gradlew clean test assembleDebug assembleRelease --no-daemon`
2. `/home/ubuntu/skills/tezgpt-native-android/scripts/audit_native_android.sh /home/ubuntu/LibreChat-native`
3. The server is reachable over HTTPS and the APK is built with `-PTEZGPT_API_BASE_URL=https://your-real-domain.example`.
4. The release APK is signed with the owner's keystore and tested against the production server.

The project should not be described as a complete Manus replacement yet. It is a working native chat client with a secure agent-platform foundation; the remaining capabilities above require additional native screens and corresponding server contracts.
