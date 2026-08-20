# TezGPT official model catalog

TezGPT’s provider catalog is based on the public provider configuration documented in the official [LibreChat-AI/librechat.ai repository](https://github.com/LibreChat-AI/librechat.ai). The catalog contains model identifiers only; no upstream API keys, cookies, OAuth credentials, or private configuration were copied.

## Enabled provider endpoints

The clean TezGPT Render service enables `openAI`, `agents`, `google`, and `anthropic`. The native Android application does not hard-code this list. It requests `/api/endpoints` and `/api/models` after authentication, so the web client and native client receive the same server-side catalog.

## OpenAI

The OpenAI endpoint is configured for user-provided credentials. The available model IDs are:

```text
gpt-5.6,gpt-5.6-terra,gpt-5.6-luna,gpt-5.5,gpt-5.5-pro,chat-latest,gpt-5.4,gpt-5.4-pro,gpt-5.4-mini,gpt-5.4-nano,gpt-5.3-codex,gpt-5.2,gpt-5,gpt-5-codex,gpt-5-mini,gpt-5-nano,o3-pro,o3,o4-mini,gpt-4.1,gpt-4.1-mini,gpt-4.1-nano,o3-mini,o1-pro,o1,gpt-4o,gpt-4o-mini
```

## Anthropic

The Anthropic endpoint is configured for user-provided credentials. The available model IDs are:

```text
claude-fable-5,claude-opus-5,claude-opus-4-8,claude-opus-4-7,claude-sonnet-5,claude-sonnet-4-6,claude-opus-4-6,claude-opus-4-20250514,claude-3-7-sonnet-20250219,claude-3-5-sonnet-20241022,claude-3-5-haiku-20241022
```

## Google Gemini

The Google endpoint is configured for user-provided credentials. The available Gemini API model IDs are:

```text
gemini-3.7-flash,gemini-3.6-flash,gemini-3.5-flash,gemini-3.5-flash-lite,gemini-3.1-pro-preview,gemini-3.1-pro-preview-customtools,gemini-3.1-flash-lite-preview,gemini-2.5-pro,gemini-2.5-flash,gemini-2.5-flash-lite,gemini-2.0-flash,gemini-2.0-flash-lite
```

## Authentication and model visibility

The provider API-key variables are set to `user_provided`, which means TezGPT does not contain or distribute provider secrets. A user must sign in, enter a provider key in TezGPT Settings, and then select a model exposed by that provider. Requests made without authentication to `/api/endpoints` and `/api/models` correctly return HTTP 401.

The model IDs are copied from the official public documentation at the time of configuration. Provider availability, regional restrictions, account entitlements, deprecations, and billing remain controlled by the respective provider. A model appearing in the selector does not grant access to that provider or bypass its billing and permissions.

## Verification

The Render deployment containing this catalog is live at [https://tezgpt.onrender.com](https://tezgpt.onrender.com). The native Android client remains server-driven, so no new APK code change is required for a server-side model catalog update; the existing clean-URL APK automatically receives the updated catalog after login.
