# TezGPT Native Design Freeze

This document freezes the current user-approved TezGPT Android direction. Future implementation must preserve the visual structure, palette, navigation semantics, and model/chat interaction style. The implementation may change only where required to express the same behavior with native Android Java/XML components.

## Non-negotiable runtime rules

The app must remain a fully native Java Android application. It must not use WebView, Capacitor, Cordova, remote HTML, browser-based chat rendering, or an external browser redirect for ordinary app flows.

## Frozen visual reference

| Surface | Frozen native reference |
|---|---|
| Brand | TezGPT wordmark and green accent `#10A37F` |
| Light background | White `#FFFFFF` with light surface `#F7F7F8` |
| Dark/text palette | Existing `values-night` colors and `#202123` primary text direction |
| App shell | DrawerLayout with a compact top bar, drawer button, title, and new-chat action |
| Drawer | TezGPT title, Chat, Conversations, Search, Agents, Files, Memory, Settings, and Logout destinations |
| Chat | Toolbar, provider/endpoint selector, model selector, scrollable message area, empty state, composer, send action, and progress state |
| Authentication | In-app email/password login and registration screens with the existing TezGPT spacing, fields, green primary action, and error placement |
| Controls | Existing native buttons, input backgrounds, message bubbles, selectors, margins, and typography remain the baseline |

## Functional preservation

The model and endpoint controls are not decorative. They must load the user-owned server’s real `/api/endpoints` and `/api/models` data. Selecting an endpoint must show that endpoint’s own models. Authentication must call the server’s real `/api/auth/login`, `/api/auth/register`, `/api/auth/logout`, and `/api/auth/refresh` routes. Files, conversations, agents, memory, and search must continue using native Android controls connected to the TezGPT backend.

The official LibreChat website is not a runtime dependency for this app. It may be used only as a reference for behavior that already exists in the user-owned project. No external web page may replace a missing native screen.

## Change-control rule

Before adding a new screen or changing an existing layout, compare it with the current XML resources and this freeze document. Do not rename or remove model controls, endpoint controls, the sign-in surface, or the drawer destinations without explicit user approval.
