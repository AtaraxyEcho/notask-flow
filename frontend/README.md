# Notask Flow Frontend

> Vue 3 Web 前端应用

[English](#english) | [中文](#中文)

---

## 中文

### 技术栈

| 组件 | 技术 |
|------|------|
| 框架 | Vue 3 |
| 语言 | TypeScript |
| 构建 | Vite 5 |
| UI | Element Plus, Tailwind CSS |
| 状态管理 | Pinia |
| 富文本 | TipTap |
| 协作 | Yjs |

### 项目结构

```
frontend/
├── src/
│   ├── api/            # API 请求
│   ├── components/     # 组件
│   ├── views/          # 页面
│   ├── stores/         # 状态管理
│   ├── router/         # 路由
│   ├── composables/    # 组合函数
│   └── styles/         # 样式
├── public/             # 静态资源
└── package.json
```

### 本地开发

```bash
npm install
npm run dev
```

访问：http://localhost:3000

### 构建

```bash
npm run build
```

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `VITE_API_BASE_URL` | API 基础路径 | `/api/v1` |
| `VITE_COLLAB_WS_URL` | WebSocket 路径 | `/ws` |

### 页面路由

| 路由 | 说明 |
|------|------|
| `/login` | 登录 |
| `/register` | 注册 |
| `/app/notes` | 笔记 |
| `/app/tasks` | 任务 |
| `/app/todos` | 待办 |
| `/app/files` | 文件 |
| `/app/projects` | 项目 |

### 排障速查

| 现象 | 检查项 |
|------|--------|
| API 请求 404 | `VITE_API_BASE_URL` 是否配置 |
| CORS 错误 | 后端 CORS 配置 |
| 字体不显示 | Google Fonts 访问 |

---

## English

### Tech Stack

| Component | Technology |
|-----------|------------|
| Framework | Vue 3 |
| Language | TypeScript |
| Build | Vite 5 |
| UI | Element Plus, Tailwind CSS |
| State | Pinia |
| Rich Text | TipTap |
| Collaboration | Yjs |

### Project Structure

```
frontend/
├── src/
│   ├── api/            # API requests
│   ├── components/     # Components
│   ├── views/          # Pages
│   ├── stores/         # State management
│   ├── router/         # Routing
│   ├── composables/    # Composables
│   └── styles/         # Styles
├── public/             # Static assets
└── package.json
```

### Local Development

```bash
npm install
npm run dev
```

Access: http://localhost:3000

### Build

```bash
npm run build
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | API base path | `/api/v1` |
| `VITE_COLLAB_WS_URL` | WebSocket path | `/ws` |

### Page Routes

| Route | Description |
|-------|-------------|
| `/login` | Login |
| `/register` | Register |
| `/app/notes` | Notes |
| `/app/tasks` | Tasks |
| `/app/todos` | Todos |
| `/app/files` | Files |
| `/app/projects` | Projects |

### Troubleshooting

| Issue | Check |
|-------|-------|
| API 404 | `VITE_API_BASE_URL` configured |
| CORS error | Backend CORS config |
| Fonts not loading | Google Fonts access |
