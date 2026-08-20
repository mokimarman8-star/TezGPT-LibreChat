# LibreChat Native Android UI Inventory

The supplied LibreChat repository contains approximately **1,035 JSX/TSX files** under `client/src`, while the current native module contains only six Java classes and a small set of XML layouts. The existing native module is therefore a foundation, not a complete UI parity conversion.

| Web surface | Primary source families | Native parity target |
|---|---|---|
| Authentication | `client/src/components/Auth`, login/register/password/2FA/OAuth views | Native activities/fragments/dialogs with Android credential fields and provider SDK flows. |
| App shell | `client/src/components/Nav`, layout providers, router and responsive navigation | Native `DrawerLayout`/navigation rail, top app bar, back-stack, tablet/phone behavior. |
| Conversations | `client/src/components/Conversations`, favorites, projects, pinned sections, conversation options | Native RecyclerView with grouping, search, pin/favorite/archive/share/delete actions, paging. |
| Chat landing and messages | `client/src/components/Chat`, `ChatView`, messages, message actions, reasoning/activity views | Native scrolling message list, markdown/code renderer, streaming updates, message action sheets, activity rows. |
| Composer | chat input, attachments, endpoint/model controls, speech and send/stop controls | Native multiline composer, attachment chips, SAF picker, audio controls, model and endpoint bottom sheets. |
| Agents and tools | `client/src/components/Agents`, tool marketplace, MCP, skills, subagents, approvals | Native agent catalog, run timeline, tool risk/approval dialogs, artifacts and subagent navigation. |
| Files and artifacts | file upload, artifact previews, code interpreter, export/share flows | Native document/image picker, upload progress, artifact viewer, secure share/export intents. |
| Search and discovery | message search, conversation search, model/endpoint discovery | Native search screens with debounce, paging, filters, and deep links into conversations. |
| Settings | interface, account, security, data controls, endpoints, presets, memory, speech, plugins | Native preference screens and nested settings navigation using existing server routes. |
| Administration | admin users, roles, groups, config, grants, skills, audit, Langfuse/insights | Native admin area gated by server capabilities and role permissions. |
| Projects, prompts, presets, sharing | project sections, prompt library, presets, shared links | Native list/detail/edit flows with server-backed persistence. |

## Visual parity rules

The native implementation must preserve the existing LibreChat hierarchy, dark/light palettes, typography scale, spacing rhythm, rounded surfaces, message alignment, control labels, icon semantics, and responsive navigation intent. Android-native implementation is allowed to change only the rendering technology and interaction plumbing required by touch, back navigation, accessibility, and platform permissions.

## Current gap

The current `MainActivity` contains login, registration, a minimal chat view, model/endpoint spinners, and logout. It does not yet provide the full shell, conversation drawer, message renderer, composer controls, files, agents, settings, or admin surfaces listed above. The next implementation phase should establish the reusable native shell and navigation before adding feature-specific screens.
