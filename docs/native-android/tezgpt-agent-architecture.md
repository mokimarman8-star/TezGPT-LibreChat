# TezGPT native Android and agent-platform architecture

## Purpose

This document turns the audited TezGPT repository into an implementation contract for a fully native Java Android client and a Manus-like agent platform. The client preserves the established UI direction; the server remains the trust boundary for model providers, tools, memory, files, long-running runs, and privileged actions.

## Existing capability inventory

The repository already contains substantial server and web feature surfaces for authentication, conversations, messages, files, agents, MCP, tools, OAuth, search, memories, endpoints, model selection, settings, and insights. The current Android module is still a Capacitor/WebView wrapper: it uses `BridgeActivity`, `capacitor-android`, generated web assets, and `activity_main.xml` with a WebView. This is the primary blocker for the native migration.

## Target components

| Component | Responsibility | Native/server boundary |
|---|---|---|
| Android UI | Screens, navigation, rendering, user approval, accessibility, device capabilities | Native Java/XML; no remote HTML |
| Session layer | Login, refresh, logout, secure device state | Android Keystore plus server session APIs |
| API client | Typed REST, SSE/streaming, cancellation, retry, errors | HTTPS; provider secrets remain server-side |
| Chat state | Conversations, drafts, message branches, streaming reconciliation | ViewModels plus server conversation APIs |
| Agent run client | Run status, plan steps, progress, approvals, artifacts | Native event renderer over durable server runs |
| Tool/MCP gateway | Tool registry, schemas, authorization, sandbox, audit | Server trust boundary |
| Memory/indexing | User/project memory, files, retrieval, citations, deletion | Server indexes and policy layer |
| Job system | Long-running runs, schedules, retries, notifications | Server queue; Android WorkManager for sync |
| Provider adapters | Model capability discovery, routing, fallbacks, usage | Server-side provider integrations |

## Native Java package plan

```text
com.tezgpt.app
├── api
│   ├── ApiClient
│   ├── ApiError
│   ├── StreamEvent
│   └── models
├── auth
├── chat
├── conversations
├── endpoints
├── files
├── agents
├── tools
├── memory
├── oauth
├── notifications
├── storage
├── settings
└── ui
    ├── auth
    ├── chat
    ├── conversations
    ├── agents
    ├── files
    └── settings
```

## Agent run model

A run is durable and resumable. The server owns the authoritative state; Android renders a locally cached projection.

```text
planned -> running -> waiting_for_approval -> running
                         |                    |
                         v                    v
                      paused              completed
                         |                    |
                         v                    v
                      cancelled            failed
```

Each run contains a user, tenant, conversation, agent, plan, ordered or dependency-aware steps, token/cost/time/tool budgets, checkpoints, progress events, approval requests, tool results, generated artifacts, citations, usage, and audit metadata. Android stores only the minimum event cursor and UI state needed to reconnect safely.

## Tool and MCP policy

Every tool publishes a versioned schema, risk class, required scope, timeout, result limit, approval rule, and audit policy. High-risk tools include destructive file operations, external communication, financial actions, credentials, code execution, browser form submission, and changes to external systems. These require explicit user approval and server-side revalidation.

Remote instructions and tool output are treated as untrusted data. They cannot override server policies, user approval, tenant boundaries, or tool scopes. MCP connections require explicit user authorization, revocation, per-server state, scoped variables, and output redaction.

## Multi-model orchestration

Provider adapters expose model capabilities, context limits, modalities, tool support, reasoning support, cost, and latency metadata. A route planner can select a primary model, fallbacks, parallel research models, critic/judge models, and a synthesizer. The run record exposes which models were used and why, without exposing provider secrets.

## Memory and file governance

Separate transient conversation context, user memories, project/workspace memories, indexed file content, and generated artifacts. Every record has owner/tenant scope, provenance, visibility, retention, deletion, export, and citation behavior. Memory is inspectable and disable-able. File processing is asynchronous, bounded, cancellable, and isolated from the Android process.

## Native UI parity contract

Port the existing web screen hierarchy and design tokens into XML resources. Preserve login, sidebar, composer, message bubbles, model selectors, settings, dialogs, loading/empty/error states, light/dark behavior, and accessibility semantics. Verify each screen using reference screenshots and interaction checklists. Do not add new marketing or navigation surfaces under the guise of native conversion.

## Security boundaries

Provider keys, OAuth client secrets, tool credentials, signing keys, and admin privileges never ship in Android. Tokens use Android Keystore-backed storage. Production networking requires HTTPS. Incoming intents, deep links, files, and tool results are validated. Logs and notifications redact secrets. Native permissions are requested at point of use.

## First implementation slices

1. Replace the Capacitor launcher with a native Java `MainActivity` and configuration/error state.
2. Add typed API/session storage and native login/registration.
3. Port the chat shell, conversation list, composer, streaming messages, model/endpoint selection, and theme resources.
4. Port files and settings.
5. Add agent run progress, approval, tool result, and artifact views.
6. Add memory, MCP/tool configuration, background jobs, notifications, and advanced integrations.
