# TezGPT Capability Audit

## Scope

This audit covers the TezGPT repository's `client`, `api`, `packages`, `android`, `config`, and deployment workflow trees. It traces the existing LibreChat-derived implementations rather than replacing missing behavior with placeholder controls.

## Verified Web Capabilities

| Capability | Existing implementation | Live deployment status | Action taken |
|---|---|---|---|
| Email sign-in | `client/src/components/Auth/LoginForm.tsx` and `/api/auth/login` | Enabled | Retained. |
| Registration | `Registration.tsx` and `/api/auth/register` | Enabled | Restored the GitHub Pages in-app Sign up and Login links. |
| Password recovery | `LoginForm.tsx`, `RequestPasswordReset`, and `/api/auth/requestPasswordReset` | Hidden because the live startup config reports `passwordResetEnabled: false` and `emailEnabled: false` | Requires real mail-provider configuration; no fake button is added. |
| Google, GitHub, Discord, Facebook, Apple, OpenID, and SAML | `SocialLoginRender.tsx` and `api/server/routes/oauth.js` | Hidden because all provider flags are false in the live startup config | Source is retained; each provider is shown only after its real server credentials are configured. |
| Model and endpoint selection | Endpoint menus, model selectors, `/api/endpoints`, and `/api/models` | Available after authenticated server configuration resolves | Retained; the server loads the user’s provider-specific catalogs. |
| Provider API-key settings | Provider Keys and API Keys settings views with `/api/keys` and `/api/api-keys` | Available to authenticated users | Retained; keys stay encrypted server-side and are not embedded in the static client. |
| Native Android provider-key controls | Native Java `MainActivity` and `ApiClient` settings flow | Implemented | Retained. Native social sign-in remains a separate secure implementation task. |

## Fixed GitHub Pages Failure

The post-login white screen came from treating the Render API origin as the browser-router base path. A GitHub Pages user entered through `/TezGPT-LibreChat/`, but the authenticated redirect did not remove that repository prefix before React Router navigation.

The client now tracks these two values separately:

| Value | Purpose |
|---|---|
| `https://tezgpt.onrender.com` | HTTPS API origin for authenticated requests |
| `/TezGPT-LibreChat/` | GitHub Pages browser-router base path |

The authenticated redirect is normalized to paths such as `/c/new`. A deployed unauthenticated `/TezGPT-LibreChat/c/new` request now redirects to `/login?redirect_to=%2Fc%2Fnew`, which is safe for the configured router base.

## Required Real Server Configuration

The current live config intentionally does not advertise password recovery or social providers because the corresponding credentials are absent. These features require real third-party setup.

| Requested capability | Required Render environment configuration | External setup required |
|---|---|---|
| Forgot password | `ALLOW_PASSWORD_RESET=true`, plus mail host or service, username, password, and sender address | A mail provider account and verified sender. |
| Google sign-in | `ALLOW_SOCIAL_LOGIN=true`, `GOOGLE_CLIENT_ID`, and `GOOGLE_CLIENT_SECRET` | A Google OAuth web application with callback `https://tezgpt.onrender.com/oauth/google/callback`. |
| GitHub sign-in | `ALLOW_SOCIAL_LOGIN=true`, `GITHUB_CLIENT_ID`, and `GITHUB_CLIENT_SECRET` | A GitHub OAuth application with callback `https://tezgpt.onrender.com/oauth/github/callback`. |
| Discord sign-in | `ALLOW_SOCIAL_LOGIN=true`, `DISCORD_CLIENT_ID`, and `DISCORD_CLIENT_SECRET` | A Discord application with callback `https://tezgpt.onrender.com/oauth/discord/callback`. |
| OpenID or SAML | The corresponding `OPENID_*` or `SAML_*` variables | An identity-provider registration and allowed callback URL. |

`DOMAIN_CLIENT` must remain the GitHub Pages URL and `DOMAIN_SERVER` must remain the Render HTTPS URL. Provider secrets must be stored only in Render’s secret settings, never in the repository, static client, or Android APK.

## Known Validation Boundaries

The repository workflow performs a clean build and deploy. The public sign-up and login routing has been verified after deployment. A full successful login, provider-key submission, model response, password-reset email, or social OAuth callback requires an authenticated account and, for mail/OAuth, the required real provider credentials. No test accounts, mock mail service, fake Google button, or fabricated model catalog have been added.

## Added DeerFlow-Inspired Master-Agent Backend

| Capability | TezGPT implementation | UI impact | Safety boundary |
|---|---|---|---|
| Single master agent | `packages/api/src/agents/masterAgent.ts` and `initialize.ts` compose one runtime catalog and disable delegation tools by default | None | The operator can explicitly re-enable subagents. |
| Short-term memory | Existing conversation messages, run state, code-session state, and artifacts | None | Context windows, summaries, and existing run limits remain authoritative. |
| Long-term memory | Existing permissioned `packages/api/src/agents/memory.ts` with formatted retrieval, token limits, `set_memory`, and `delete_memory` | None | User opt-out, role permissions, configured valid keys, and token limits remain enforced. |
| Tool orchestration | Existing built-in, provider, skills, code, and MCP tool registries are de-duplicated into one catalog | None | Capability checks and MCP access controls remain in force. |
| Sandbox execution | Existing `bash_tool`, `read_file`, file authoring, artifacts, and stateful code sessions are reused | None | The host runtime, environment selection, timeout, file limits, and approval gates remain authoritative. |
| Planning and verification | Master-agent instructions create bounded internal planning and verification behavior | None | Maximum plan steps, tool calls, and execution time are configurable and clamped. |

This integration ports DeerFlow’s architecture patterns rather than copying its source code. See `docs/tezgpt-master-agent.md` for the complete implementation and provenance note.
