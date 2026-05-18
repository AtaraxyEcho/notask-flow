# Notask Flow Frontend

Notask Flow Web 前端，基于 Vue 3、TypeScript 和 Vite 构建，提供个人空间与团队空间下的笔记、任务、待办、文件、项目、统计、通知和协作文档体验。

## 技术栈

| 类型 | 技术 |
|------|------|
| 框架 | Vue 3 |
| 语言 | TypeScript |
| 构建 | Vite 5 |
| 路由 | Vue Router 4 |
| 状态管理 | Pinia、pinia-plugin-persistedstate |
| UI | Element Plus、Tailwind CSS |
| 请求 | Axios |
| 富文本/协同 | TipTap、Yjs、y-protocols、y-prosemirror |
| 图表 | ECharts、vue-echarts |
| 文件预览 | `@vue-office/docx`、`@vue-office/excel`、`@vue-office/pdf` |
| 安全处理 | DOMPurify |

## 项目结构

```text
frontend/
├── src/
│   ├── api/            # Axios 封装与业务 API 模块
│   ├── collab/         # Yjs 协同编辑和空间实时事件 WebSocket Provider
│   ├── components/     # 通用、个人空间、团队空间、笔记、文件、项目组件
│   ├── composables/    # 主题、权限、空间实时事件等组合逻辑
│   ├── i18n/           # 中英文语言包
│   ├── layouts/        # 登录布局与应用主布局
│   ├── router/         # 路由、鉴权守卫、团队空间守卫
│   ├── stores/         # Pinia 用户、空间、笔记、任务、待办、项目、通知状态
│   ├── styles/         # 全局样式和多主题样式
│   ├── utils/          # 日期、Markdown、跳转、安全清洗等工具
│   └── views/          # 页面级视图
├── public/
├── vite.config.ts
├── package.json
└── README.md
```

## 核心实现

- `src/api/http.ts` 统一配置 Axios：自动附加 `Authorization: Bearer <token>`，解析后端 `ApiResponse`，对 `401` 清理会话并触发登录跳转。
- `src/router/index.ts` 使用懒加载路由，并在守卫中完成登录校验、用户资料加载、空间初始化、团队空间限制和未读通知刷新。
- `src/stores/` 按业务拆分状态，用户会话通过 `pinia-plugin-persistedstate` 持久化，并监听浏览器 `storage` 事件同步多标签页登录态。
- `src/collab/NoteCollabProvider.ts` 使用 Yjs sync/awareness 协议连接协作文档 WebSocket，实现文档增量同步、光标/在线状态和断线重连。
- `src/collab/SpaceEventProvider.ts` 连接空间实时事件 WebSocket，用于任务、通知、成员状态等空间内变更广播。
- `vite.config.ts` 将依赖拆分为 `vendor-app`、`vendor-editor`、`vendor-collaboration`、`vendor-office`、`vendor-charts`、`vendor-visual`，降低首屏和缓存压力。

## 功能页面

| 页面 | 路由 | 说明 |
|------|------|------|
| 登录/注册/找回密码 | `/login`、`/register`、`/forgot-password`、`/reset-password` | 认证入口，支持邮箱验证码和密码重置 |
| 笔记 | `/app/notes`、`/app/notes/:noteId` | 笔记本、标签、富文本编辑、协同编辑、分享、历史版本 |
| 任务 | `/app/tasks` | 个人和团队任务列表、看板、状态流转、成员任务操作 |
| 待办 | `/app/todos` | 个人待办、任务关联待办、完成状态管理 |
| 文件 | `/app/files`、`/app/files/preview/:fileId` | 文件夹、上传、分片上传、预览、回收站、引用关系 |
| 项目 | `/app/projects`、`/app/projects/:projectId` | 团队项目、项目成员、项目任务与文档 |
| 统计 | `/app/stats`、`/app/spaces/:spaceId/stats` | 个人和团队统计图表 |
| 通知 | `/app/notifications` | 站内通知列表与未读状态 |
| 设置 | `/app/settings`、`/app/space/:spaceId/settings` | 个人设置和团队空间设置 |
| 公开访问 | `/public/notes/:shareCode`、`/invite/:teamCode` | 笔记分享页和团队邀请页 |

## 本地开发

### 前置要求

- Node.js 20+
- npm 10+
- 后端服务 `http://localhost:8080`
- 协同 WebSocket 服务 `http://localhost:8081`

### 安装与启动

```bash
npm install
```

首次本地运行建议创建 `frontend/.env.local`：

```env
VITE_API_BASE_URL=/api/v1
VITE_COLLAB_WS_URL=/ws
```

然后启动开发服务：

```bash
npm run dev
```

默认访问地址为 `http://localhost:3000`。

开发代理在 `vite.config.ts` 中配置：

| 前端路径 | 代理目标 | 说明 |
|----------|----------|------|
| `/api` | `http://localhost:8080` | 后端 REST API |
| `/ws` | `http://localhost:8081` | 协作文档与空间实时事件 |

### 构建与检查

```bash
npm run build
npm run lint
```

`npm run build` 会先执行 `vue-tsc -b` 类型检查，再执行 Vite 构建。

## 开发约定

- `index.html` 必须位于 `frontend/` 根目录，这是 Vite 的 HTML 入口；`src/main.ts` 负责挂载 Vue 应用。
- 业务请求统一写在 `src/api/modules/`，不要在页面组件中直接拼接散落的请求地址。
- 页面组件放在 `src/views/`，可复用组件放在 `src/components/`，跨页面状态放在 `src/stores/`。
- 图标当前主要使用 `Material Symbols Outlined`，写法为 `<span class="material-symbols-outlined">delete</span>`。
- `Newsreader` 用于笔记阅读/编辑气质较强的区域，`Plus Jakarta Sans` 用于部分现代 UI 文本。
- 外部字体依赖 Google Fonts；如果部署环境访问不稳定，应考虑把字体改为本地自托管。

## 环境变量

本地开发和生产构建都通过 Vite 环境变量配置后端地址：

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `VITE_API_BASE_URL` | 建议 `/api/v1` | REST API 基础路径。未配置时 Axios 会按当前页面路径请求，容易绕过 Vite 代理 |
| `VITE_COLLAB_WS_URL` | 建议 `/ws` | WebSocket 基础路径。未配置时协同组件会自行按当前 origin 推导 |

Docker 构建时，这两个变量由 `backend/docker-compose.yml` 的 `frontend` 服务通过 build args 注入。

## 与后端的协作关系

- REST API 统一走 `/api/v1`，响应结构由 `ApiResponse` 包装，前端只消费 `data`。
- 登录态来自 Sa-Token JWT，前端只保存 token，不直接处理权限规则。
- 当前空间由 `spaceStore` 管理，团队页面会根据空间类型和权限决定是否可进入。
- 协作文档和空间实时事件先向后端申请一次性 ticket，再交给 `collab-ws` 校验，避免在 WebSocket URL 中长期暴露 JWT。

## 联调检查清单

| 现象 | 优先检查 |
|------|----------|
| 登录接口请求到 `http://localhost:3000/auth/login` | 是否配置了 `VITE_API_BASE_URL=/api/v1` |
| 浏览器提示 CORS | 后端 `notask-flow.security.allowed-origins` 是否包含前端地址 |
| 协同编辑连接失败 | `collab-ws` 是否启动，`VITE_COLLAB_WS_URL` 是否为 `/ws`，后端 `COLLAB_INTERNAL_TOKEN` 是否和 `collab-ws` 一致 |
| 图标显示成英文单词 | Google Material Symbols 字体是否加载成功 |
| 页面刷新后 404 | nginx 或部署服务器是否配置了 history fallback 到 `index.html` |

## 部署说明

全栈 Docker 部署时，前端由 `backend/docker/frontend/Dockerfile` 构建，并由 nginx 托管静态文件。nginx 同时代理：

- `/api/` 到后端 Spring Boot 服务。
- `/ws` 到 `collab-ws` WebSocket 服务。

单独部署前端时，需要在静态服务器中保留同样的 API 和 WebSocket 反向代理规则，或把 `VITE_API_BASE_URL`、`VITE_COLLAB_WS_URL` 配置为可访问的完整地址。
