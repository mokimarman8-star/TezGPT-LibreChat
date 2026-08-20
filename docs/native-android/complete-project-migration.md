# Complete LibreChat-to-TezGPT Migration Scope

## What the repository contains

The GitHub repository `mokimarman8-star/LibreChat` is a full self-hosted platform, not a single Android application. It contains a React client, a Node/Express API server, authentication and OAuth strategies, agent and MCP services, file and memory routes, model/provider integrations, administrative routes, and deployment infrastructure. The Android module is therefore one native client target of the complete project; it cannot replace the server process and its data services without porting the backend runtime and databases to Android.

The previous working APK confirmed this distinction. Its original build used the repository’s Capacitor configuration, which pointed the packaged web client at a running LibreChat deployment. That APK preserved the web UI and reused the server’s authentication, chat, files, agents, models, and settings. It did not contain MongoDB, Meilisearch, pgvector, RAG API, provider credentials, or the Node/Express server inside the APK.

## Corrected component map

| Complete-project component | Repository location | Native Android role | Deployment location |
|---|---|---|---|
| Native TezGPT client | `android/app/src/main/java/com/tezgpt/app` and Android resources | Login, session storage, chat streaming, model/endpoint selection, agent foundations, native permissions, future native feature screens | User device |
| Full backend API | `api/`, `packages/api/`, `api/server/` | Called over HTTPS; remains authoritative for auth, chat, agents, tools, memory, files, search, models, admin, and policy | User-owned server/container |
| React web client reference | `client/`, `packages/client/` | Visual and interaction reference for native parity; no WebView runtime in the TezGPT APK | Optional browser client or reference build |
| Authentication and OAuth | `api/server/routes/auth.js`, `api/server/routes/oauth.js`, strategies | Native forms and future provider SDK flows call backend routes | User-owned server plus provider configuration |
| Agents, MCP, skills, tools, approvals | `api/server/routes/agents/`, `api/server/routes/mcp.js`, skills/plugin services | Native API client, run state, progress, approval and artifact UI | User-owned server and configured tools |
| Conversations, memories, files, search | Backend route modules and storage services | Native list, memory, file picker, artifact, and search screens | User-owned server plus MongoDB, Meilisearch, object/file storage |
| Provider/model integrations | Endpoint and provider packages under `api/` and `packages/` | Native model and endpoint selectors send requests to backend | User-owned server with provider keys |
| Data and retrieval services | `mongodb`, `meilisearch`, `vectordb`, `rag_api` in Compose | No direct database access from Android | User-owned server infrastructure |
| Admin panel and observability | `admin-panel`, Langfuse/OTel configuration | Future native admin/telemetry views where appropriate | User-owned server or operator tooling |

## Why a server is still required

A native APK cannot safely contain the project’s provider API keys, MongoDB data, multi-user authorization state, MCP credentials, file indexes, agent execution sandboxes, or administrative secrets. Those responsibilities must remain behind the TezGPT HTTPS API. The Android application is the user-facing native surface; the full LibreChat project remains the deployable server platform that makes the application real.

The correct deployment is therefore:

> **TezGPT Android APK → HTTPS → user-owned LibreChat/TezGPT API → MongoDB, search, vector/RAG, file storage, model providers, agents, and tools.**

## What has been corrected in this project

The native project retains the complete repository source instead of deleting the backend. The Android module no longer uses Capacitor, Cordova, WebView, or remote HTML. It connects directly to the backend over HTTPS using a native Java API client. The recovered legacy endpoint is available as a compatibility default, while production builds can override it with `-PTEZGPT_API_BASE_URL=https://your-owned-domain.example`.

The current native implementation covers the foundation: authentication, encrypted session storage, SSE chat, endpoint/model selection, HTTPS policy, agent streaming and status models, tool discovery, WorkManager synchronization, FileProvider restrictions, and release R8 configuration. Native screens for conversation navigation, approvals, files, memories, MCP management, artifacts, OAuth provider SDKs, and scheduled jobs remain the next parity layers; their server-side routes remain in the full repository.

## Important naming distinction

The **repository/project** remains `LibreChat` because that is the supplied GitHub codebase and its upstream source structure. The **Android product branding and package** are `TezGPT` and `com.tezgpt.app`. The server deployment can be branded TezGPT through the supplied environment, configuration, policy, and deployment files without pretending that the Android APK contains the backend runtime.
