# Notask Flow Frontend

> Vue 3 web frontend

[中文](README.md) | English

---

## Description

The web app serves two kinds of users: end users with their personal/team spaces (`/app/*`), and platform administrators via the admin console (`/admin/*`, a separate session system). Document collaboration is built on TipTap + Yjs; it also builds the WebView collab editor kernel for Android (see "Android Collab Kernel" below) and ships with bilingual UI (zh-CN / en-US) and 5 themes.

## Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Vue 3.4 (Composition API + `<script setup>`) |
| Language | TypeScript 5.5 |
| Build | Vite 5.4 |
| UI | Element Plus 2.8 (auto-imported on demand), Tailwind CSS 3.4 |
| State | Pinia 3 (+ persistedstate plugin) |
| Rich Text | TipTap 3.22 |
| Collaboration | Yjs 13.6, y-protocols, y-indexeddb |
| Charts | ECharts 5.5 |
| Office Preview | @vue-office (docx / excel / pdf) |

## Project Structure

```
frontend/
├── src/
│   ├── api/               # HTTP layer: http.ts (axios instance), adminHttp.ts, modules/ (15 business modules)
│   ├── collab/            # Collab providers: NoteCollabProvider, SpaceEventProvider
│   ├── android-collab-kernel/  # Android collab kernel entry (library-mode build)
│   ├── components/        # Components: common / files / notes / personal / projects / shared / team
│   ├── composables/       # usePermission, useSpaceRealtimeEvents, useTheme
│   ├── i18n/              # Lightweight in-house i18n + locales (zh-CN / en-US)
│   ├── layouts/           # AppLayout, AdminLayout, AuthLayout
│   ├── router/            # Routes and global guards
│   ├── stores/            # 9 Pinia stores
│   ├── styles/            # main.css + themes/ (5 theme CSS files)
│   ├── utils/             # avatar, collabWs, date, device, markdown, sanitize, etc.
│   ├── views/             # Pages: admin (10) / auth / files / notes / projects / spaces / stats / ...
│   ├── App.vue / main.ts
│   └── auto-imports.d.ts / components.d.ts   # generated type declarations
├── public/                # Static assets
├── vite.config.ts         # Dev proxy + auto-import + chunking
├── vite.android-collab.config.ts   # Android collab kernel build config
└── tailwind.config.ts     # All colors mapped to CSS variables
```

## Prerequisites

- Node.js ≥ 18 (20+ recommended)
- Package manager: **npm only** (`package-lock.json` is committed; do not mix pnpm/yarn)
- Backend services: local development requires the [backend](../backend/README_EN.md) (:8080) and collab-ws (:8081) running

## Quick Start

```bash
cp .env.example .env   # or .env.local, edit as needed
npm install
npm run dev
```

Visit `http://localhost:3000`.

## Commands

| Command | Purpose |
|---------|---------|
| `npm run dev` | Start dev server (port 3000, with `/api` and `/ws` proxies) |
| `npm run build` | Type-check with `vue-tsc`, then build for production into `dist/` |
| `npm run build:android-collab` | Build the Android collab editor kernel into the Android assets directory |
| `npm run preview` | Preview the `dist/` build locally |
| `npm run lint` | ESLint check (note: eslint is not currently in devDependencies) |

## API Proxy Configuration

CORS is handled in development by Vite proxies (`vite.config.ts`) and in production by Nginx (see [`deploy/prod/nginx.conf`](../deploy/prod/nginx.conf)); no application code changes are needed:

| Proxy Path | Target | Purpose |
|------------|--------|---------|
| `/api` | `VITE_DEV_API_PROXY_TARGET` (default `http://localhost:8080`) | Backend REST API |
| `/ws` | `VITE_DEV_COLLAB_WS_PROXY_TARGET` (default `http://localhost:8081`) | Collab & realtime WebSocket (`ws: true`) |

## Environment Variables

Template in [`.env.example`](.env.example); copy to `.env` / `.env.local` and edit:

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Browser-side REST API base path | `/api/v1` |
| `VITE_COLLAB_WS_URL` | WebSocket path for Yjs collab and space realtime events | `/ws` |
| `VITE_DEV_API_PROXY_TARGET` | `/api` proxy target for `npm run dev` only | `http://localhost:8080` |
| `VITE_DEV_COLLAB_WS_PROXY_TARGET` | `/ws` proxy target for `npm run dev` only | `http://localhost:8081` |

## Core Design

### HTTP Layer

`src/api/http.ts` wraps an axios instance (15s timeout): it intercepts business error codes and shows toasts, clears the session on 401 and redirects to login with a `redirect` param; the API base URL can be overridden by the sessionStorage key `notask-flow-android-api-base-url` (injected by the Android collab kernel). The admin console uses a separate session via `adminHttp.ts`.

### Routes & Permissions

Routes live in `src/router/index.ts` with lazy-loaded views; access control has two layers:

- **Global guard** (`router.beforeEach`) driven by route `meta`: `guestOnly` (login/register), `authOnly` (`/app/*`, redirects to `/login` with a redirect param), `adminOnly` (`/admin/*`, separate admin session), `teamOnly` (requires the current space to be a team space), `androidStandalone` (Android collab page, skips all guards).
- **Fine-grained in UI**: the `composables/usePermission.ts` composable checks button/entry-level permissions against the current space's permission list.

Main routes:

| Route | Purpose |
|-------|---------|
| `/login` `/register` `/forgot-password` `/reset-password` | Auth pages (AuthLayout) |
| `/app/notes` `/app/notes/:noteId` `/app/tasks` `/app/todos` `/app/files` `/app/stats` `/app/notifications` `/app/settings` | Personal space (auth required) |
| `/app/projects` `/app/projects/:projectId` `/app/spaces/:spaceId/stats` `/app/space/:spaceId/settings` | Team space (`teamOnly`) |
| `/admin/*` | Admin console: dashboard / users / sessions / settings / logs / monitor / storage / system-notifications |
| `/public/notes/:shareCode` | Publicly shared note (no login) |
| `/invite/:teamCode` | Team invitation landing page |
| `/android/collab/notes/:noteId` | Android collab page (standalone entry, skips guards) |

### State Management

Pinia stores live in `src/stores/`: `user`, `space`, `admin` and `ui` (persisted to localStorage via `pinia-plugin-persistedstate` and synced across tabs), plus session-level stores such as `note`, `task`, `todo`, `project` and `notification`.

### UI Customization & Theming

- **Theming**: Tailwind colors all map to CSS variables (`tailwind.config.ts`); five themes are defined in `src/styles/themes/` (`personal-warm` default, `personal-dark`, `personal-forest`, `personal-ocean`, `team`), switched at runtime via the `data-theme` attribute by `composables/useTheme.ts`; team spaces are forced to the `team` theme; dark mode can follow the system.
- **Element Plus**: imported globally and resolved on demand via `unplugin-vue-components` + `ElementPlusResolver`; no SCSS-variable-level customization.
- **Auto-import**: `unplugin-auto-import` auto-imports vue/router/pinia APIs; components are auto-registered on demand.
- **Fonts**: Newsreader for headings, Plus Jakarta Sans for body text, loaded from Google Fonts.

### Internationalization

`src/i18n/` is a lightweight in-house i18n (provide/inject + localStorage persistence) with dictionaries in `locales/zh-CN.ts` and `locales/en-US.ts`; vue-i18n is not used.

### Build Chunking

The `manualChunks` in `vite.config.ts` splits third-party dependencies into 6 vendor chunks (`vendor-app` / `vendor-editor` / `vendor-collaboration` / `vendor-office` / `vendor-charts` / `vendor-visual`) to keep bundles small.

## Android Collab Kernel

`vite.android-collab.config.ts` builds `src/android-collab-kernel/main.ts` in Library (IIFE) mode as the global `NotaskAndroidCollabKernel`, **writing directly to** `../android/app/src/main/assets/notask_collab/editor.js`. The kernel is based on TipTap + Yjs and communicates with the Android native layer through NativeBridge callbacks (onAwarenessChanged / onCollabStatus / onContentChanged, etc.). After changing kernel code, run `npm run build:android-collab` to regenerate and sync into the Android project (see [android/README_EN.md](../android/README_EN.md)).

## Troubleshooting

| Issue | Check |
|-------|-------|
| API requests return 404 | Backend running? `VITE_DEV_API_PROXY_TARGET` points to the right port? |
| CORS errors | In dev, go through the Vite proxy instead of direct calls; in prod, check backend `notask-flow.security.allowed-origins` |
| WebSocket won't connect | `VITE_DEV_COLLAB_WS_PROXY_TARGET` points to collab-ws (8081)? |
| Fonts not loading | Google Fonts blocked; remove or self-host the fonts for offline environments |
| Logged in but bounced back | Multiple tabs sharing one browser profile can overwrite the session |
| Type check fails | `npm run build` runs `vue-tsc` first — fix type errors before building |

---

Last Updated: 2026-09-04
