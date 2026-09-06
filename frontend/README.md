# Notask Flow Frontend

> Vue 3 Web 前端应用

中文 | [English](README_EN.md)

---

## 简介

Web 端同时服务两类用户:普通用户的个人/团队空间界面(`/app/*`),以及平台管理员的管理后台(`/admin/*`,独立会话体系)。文档协同基于 TipTap + Yjs;它还负责为 Android 构建 WebView 协作编辑内核(见下文「Android 协作内核」),并内置中英双语(zh-CN / en-US)与 5 套主题。

## 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | Vue 3.4(Composition API + `<script setup>`) |
| 语言 | TypeScript 5.5 |
| 构建 | Vite 5.4 |
| UI | Element Plus 2.8(按需自动导入)、Tailwind CSS 3.4 |
| 状态管理 | Pinia 3(+ persistedstate 持久化插件) |
| 富文本 | TipTap 3.22 |
| 协作 | Yjs 13.6、y-protocols、y-indexeddb |
| 图表 | ECharts 5.5 |
| 办公预览 | @vue-office(docx / excel / pdf) |

## 目录结构

```
frontend/
├── src/
│   ├── api/               # HTTP 层:http.ts(axios 实例)、adminHttp.ts、modules/(15 个业务模块)
│   ├── collab/            # 协作提供者:NoteCollabProvider、SpaceEventProvider
│   ├── android-collab-kernel/  # Android 协作内核入口(library 模式构建)
│   ├── components/        # 组件:common / files / notes / personal / projects / shared / team
│   ├── composables/       # usePermission、useSpaceRealtimeEvents、useTheme
│   ├── i18n/              # 自研轻量 i18n + locales(zh-CN / en-US)
│   ├── layouts/           # AppLayout、AdminLayout、AuthLayout
│   ├── router/            # 路由与全局守卫
│   ├── stores/            # 9 个 Pinia store
│   ├── styles/            # main.css + themes/(5 套主题 CSS)
│   ├── utils/             # avatar、collabWs、date、device、markdown、sanitize 等
│   ├── views/             # 页面:admin(10) / auth / files / notes / projects / spaces / stats / ...
│   ├── App.vue / main.ts
│   └── auto-imports.d.ts / components.d.ts   # 自动导入生成的类型声明
├── public/                # 静态资源
├── vite.config.ts         # 开发代理 + 自动导入 + 分包
├── vite.android-collab.config.ts   # Android 协作内核构建配置
└── tailwind.config.ts     # 颜色全部映射为 CSS 变量
```

## 环境依赖

- Node.js ≥ 18(推荐 20+)
- 包管理器:**统一使用 npm**(仓库提交 `package-lock.json`,不要混用 pnpm/yarn)
- 前置服务:本地开发需要 [backend](../backend/README.md)(:8080)与 collab-ws(:8081)可用

## 快速启动

```bash
cp .env.example .env   # 或 .env.local,按需修改
npm install
npm run dev
```

访问 `http://localhost:3000`。

## 命令说明

| 命令 | 作用 |
|------|------|
| `npm run dev` | 启动开发服务器(端口 3000,启用 `/api`、`/ws` 代理) |
| `npm run build` | 先 `vue-tsc` 类型检查,再产出生产构建到 `dist/` |
| `npm run build:android-collab` | 构建 Android 协作编辑内核,输出到 Android 工程资产目录 |
| `npm run preview` | 本地预览 `dist/` 构建产物 |
| `npm run lint` | ESLint 检查(注意:eslint 当前未加入 devDependencies) |

## API 代理配置

开发环境跨域由 Vite 代理解决(`vite.config.ts`),生产环境由 Nginx 代理(见 [`deploy/prod/nginx.conf`](../deploy/prod/nginx.conf)),前后端均无需改动业务代码:

| 代理路径 | 目标 | 说明 |
|----------|------|------|
| `/api` | `VITE_DEV_API_PROXY_TARGET`(默认 `http://localhost:8080`) | 后端 REST API |
| `/ws` | `VITE_DEV_COLLAB_WS_PROXY_TARGET`(默认 `http://localhost:8081`) | 协同与实时事件 WebSocket(`ws: true`) |

## 环境变量

模板见 [`.env.example`](.env.example),复制为 `.env` / `.env.local` 后修改:

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_API_BASE_URL` | 浏览器端 REST API 基础路径 | `/api/v1` |
| `VITE_COLLAB_WS_URL` | Yjs 协作与空间实时事件的 WebSocket 路径 | `/ws` |
| `VITE_DEV_API_PROXY_TARGET` | 仅 `npm run dev` 的 `/api` 代理目标 | `http://localhost:8080` |
| `VITE_DEV_COLLAB_WS_PROXY_TARGET` | 仅 `npm run dev` 的 `/ws` 代理目标 | `http://localhost:8081` |
| `VITE_APP_VERSION` | 登录时上报的客户端版本号(留空回退为 `web`) | 与 `package.json` 一致 |

## 核心设计

### HTTP 层

`src/api/http.ts` 封装 axios 实例(超时 15s):统一拦截业务错误码并弹提示,401 时清空会话并携带 redirect 跳转登录页;API 基础地址可被 sessionStorage 的 `notask-flow-android-api-base-url` 覆盖(Android 协作内核注入用)。管理端使用独立的 `adminHttp.ts` 会话。

### 路由与权限

路由定义在 `src/router/index.ts`,所有视图懒加载;访问控制分两层:

- **全局守卫**(`router.beforeEach`)按 `meta` 分层:`guestOnly`(登录/注册)、`authOnly`(`/app/*`,未登录跳 `/login` 并携带 redirect)、`adminOnly`(`/admin/*`,独立的管理端会话)、`teamOnly`(要求当前空间为团队空间)、`androidStandalone`(Android 协作页,跳过全部守卫)。
- **界面内细化**:组合函数 `composables/usePermission.ts` 基于当前空间的权限数组控制按钮/入口级权限。

主要路由:

| 路由 | 说明 |
|------|------|
| `/login` `/register` `/forgot-password` `/reset-password` | 认证页(AuthLayout) |
| `/app/notes` `/app/notes/:noteId` `/app/tasks` `/app/todos` `/app/files` `/app/stats` `/app/notifications` `/app/settings` | 个人空间(需登录) |
| `/app/projects` `/app/projects/:projectId` `/app/spaces/:spaceId/stats` `/app/space/:spaceId/settings` | 团队空间(`teamOnly`) |
| `/admin/*` | 管理后台:dashboard / users / sessions / settings / logs / monitor / storage / system-notifications |
| `/public/notes/:shareCode` | 公开分享笔记(免登录) |
| `/invite/:teamCode` | 团队邀请落地页 |
| `/android/collab/notes/:noteId` | Android 协作页(独立入口,跳过守卫) |

### 状态管理

Pinia store 位于 `src/stores/`:`user`、`space`、`admin`、`ui`(这 4 个经 `pinia-plugin-persistedstate` 持久化到 localStorage,并跨标签页同步),以及 `note`、`task`、`todo`、`project`、`notification` 等会话级 store。

### UI 定制与主题

- **主题**:Tailwind 颜色全部映射到 CSS 变量(`tailwind.config.ts`),5 套主题定义在 `src/styles/themes/`(`personal-warm` 默认、`personal-dark`、`personal-forest`、`personal-ocean`、`team`),运行时通过 `data-theme` 属性切换,`composables/useTheme.ts` 负责切换逻辑;团队空间强制使用 `team` 主题;暗色模式支持跟随系统。
- **Element Plus**:全局引入并经 `unplugin-vue-components` + `ElementPlusResolver` 按需解析,未做 SCSS 变量级定制。
- **自动导入**:`unplugin-auto-import` 自动导入 vue/router/pinia API;组件按需自动注册。
- **字体**:标题 Newsreader、正文 Plus Jakarta Sans,经 Google Fonts 加载。

### 国际化

`src/i18n/` 为自研轻量 i18n(provide/inject + localStorage 持久化),词库在 `locales/zh-CN.ts` 与 `locales/en-US.ts`,未引入 vue-i18n。

### 构建分包

`vite.config.ts` 的 `manualChunks` 将第三方依赖拆为 6 个 vendor chunk(`vendor-app` / `vendor-editor` / `vendor-collaboration` / `vendor-office` / `vendor-charts` / `vendor-visual`),避免单包过大。

## Android 协作内核

`vite.android-collab.config.ts` 以 Library(IIFE)模式将 `src/android-collab-kernel/main.ts` 构建为全局变量 `NotaskAndroidCollabKernel`,**输出直接写入** `../android/app/src/main/assets/notask_collab/editor.js`。内核基于 TipTap + Yjs,通过 NativeBridge 回调(onAwarenessChanged / onCollabStatus / onContentChanged 等)与 Android 原生层通信。修改内核代码后需执行 `npm run build:android-collab` 重新生成并同步到 Android 工程(见 [android/README.md](../android/README.md))。

## 排障速查

| 现象 | 检查项 |
|------|--------|
| API 请求 404 | 后端是否启动,`VITE_DEV_API_PROXY_TARGET` 是否指向正确端口 |
| CORS 错误 | 开发环境应走 Vite 代理而非直连;生产检查后端 `notask-flow.security.allowed-origins` |
| WebSocket 连不上 | `VITE_DEV_COLLAB_WS_PROXY_TARGET` 是否指向 collab-ws(8081) |
| 字体不显示 | Google Fonts 网络访问受限,可离线环境移除或自托管字体 |
| 登录成功但跳回 | 检查是否多标签页共用同一浏览器 profile 导致会话覆盖 |
| 类型检查失败 | `npm run build` 先跑 `vue-tsc`,先修复类型错误再构建 |

---

Last Updated: 2026-09-06
