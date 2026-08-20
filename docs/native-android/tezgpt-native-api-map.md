# TezGPT Native Screen and API Map

This map binds the frozen TezGPT Android design to the existing user-owned LibreChat/TezGPT server contract. The Android implementation must preserve the native XML layouts and use these server routes for real behavior.

| Native surface | Existing native layout/code | Server contract |
|---|---|---|
| Server setup | `MainActivity.showConfigurationError()` and Settings | User-owned HTTPS base URL; persisted by `SessionStore` |
| Login | `screen_login.xml`, `MainActivity.showLogin()` | `POST /api/auth/login`, `GET /api/config` for login flags |
| Registration | `screen_register.xml`, `MainActivity.showRegister()` | `POST /api/auth/register` |
| Chat shell | `screen_shell.xml` + `screen_chat.xml` | `GET /api/config`, `GET /api/endpoints`, `GET /api/models` |
| Endpoint selector | `MainActivity.applyEndpointConfig()` | `GET /api/endpoints` |
| Model selector | `MainActivity.applyModelCatalog()` and `updateModelsForEndpoint()` | `GET /api/models` |
| Chat streaming | `MainActivity.sendCurrentMessage()` | `POST /api/ask` with SSE response |
| Conversations | Drawer Conversations destination | `GET /api/convos` |
| Search | Drawer Search destination | `GET /api/search?q=` |
| Agents/tools | Drawer Agents destination | `POST /api/agents/chat`, `GET /api/agents/tools` |
| Files | SAF picker and upload screen | `POST /api/files` multipart |
| Memory | Drawer Memory destination | `GET /api/memories`, `DELETE /api/memories/:key` |
| Settings | Drawer Settings destination | Local server URL/theme/session controls; additional settings remain server-ready |

## Design-preservation rules

The endpoint and model controls are part of the chat design and must remain visible in the same top control row. Their values may be server-driven, but their visual placement, native selector treatment, and surrounding spacing must not be removed or replaced with a new layout.

The login and registration screens remain in-app native screens. Provider flags may change visibility based on `/api/config`, but provider authentication must not silently fall back to a browser. Native provider SDK integration requires explicit provider client IDs and redirect configuration before a provider button can become an active sign-in action.

The server remains the authority for model availability, provider configuration, user permissions, agent tools, memory, files, and authentication. The Android client must not invent fake model names when the server catalog is available.
