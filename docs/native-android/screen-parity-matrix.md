# TezGPT Native Screen Parity Matrix

This matrix is the source of truth for the fully native migration. “Native” means Android Java/XML or AndroidX UI, with no WebView, Capacitor, remote HTML, or browser redirect.

| LibreChat surface | Native destination | Status | Backend/API status |
|---|---|---:|---|
| Login | Native login screen | Native implemented | `/api/auth/login` connected. |
| Registration | Native registration screen | Native implemented | `/api/auth/register` connected. |
| Server configuration | Native settings/configuration form | Native implemented | HTTPS URL persisted locally and build-time default supported. |
| Main app shell | Native DrawerLayout shell | Native implemented | Local navigation connected. |
| New chat | Native chat screen | Native implemented | Local conversation reset and `/api/ask` streaming connected. |
| Chat messages | Native scrollable message column | Native foundation | SSE text streaming connected; rich markdown/code/message actions remain. |
| Model and endpoint selectors | Native spinners | Native implemented | Values currently seeded from config; server-driven discovery remains. |
| Conversation list | Native conversation screen | Native first pass | `/api/convos?limit=50` connected; grouping, paging, search, pin, archive, share, and project sections remain. |
| Agents and tools | Native agents screen | Native first pass | `/api/agents/tools` connected; run timeline, approvals, artifacts, subagents, and steering remain. |
| Files | Native SAF picker and upload screen | Native first pass | `/api/files` multipart upload connected; previews, attachment chips, progress, cancellation, and artifact linking remain. |
| Memory | Native memory list/delete screen | Native first pass | `/api/memories` and delete route connected; create/edit/preferences/agent scopes remain. |
| Settings | Native settings screen | Native first pass | Server URL, theme preference, logout; account, security, endpoints, presets, privacy, speech, plugins, and policy screens remain. |
| OAuth | Native provider SDK flow | Not implemented | Requires provider Android client IDs, redirect configuration, PKCE, and server exchange. |
| Search | Native search screen | Planned | Server route exists; native query, paging, filters, and deep links remain. |
| Artifacts/code interpreter | Native artifact viewer | Planned | Server and file routes exist; native preview/export/share remain. |
| MCP management | Native MCP catalog and approval UI | Planned | Server routes exist; native OAuth/approval and risk controls remain. |
| Admin | Native admin navigation and forms | Planned | Role-gated server routes exist; native admin surface remains. |
| Speech/audio | Native Android capture and playback | Planned | Server STT/TTS routes exist; native capture/playback remains. |

## Acceptance rule

A row may be marked **Native parity complete** only after the Android layout, states, interactions, accessibility labels, loading/error/empty behavior, and server contract have been tested against the corresponding LibreChat web flow. The current build is a working native foundation and first-pass shell, not yet complete parity for every one of LibreChat’s more than one thousand web source files.
