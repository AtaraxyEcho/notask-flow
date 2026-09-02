# Notask Flow

> 个人知识管理与团队任务协作平台

[English](README_EN.md) | 中文

---

## 核心特性

- **个人空间**：笔记编辑、任务管理、待办追踪、文件管理、专注统计
- **团队空间**：项目管理、任务看板、协作文档、成员管理、团队报表
- **实时协作**：基于 Yjs 的多人文档协同编辑
- **多端支持**：Web 前端 + Android 原生客户端

## 技术栈

| 组件 | 技术 |
|------|------|
| 后端 | Java 21, Spring Boot 3.2, MyBatis-Plus, Sa-Token |
| 前端 | Vue 3, TypeScript, Vite, Element Plus, TipTap |
| Android | Kotlin, Jetpack Compose, Hilt, Retrofit, Room |
| 数据库 | MySQL 8.4, Redis 7.2 |
| 消息队列 | RabbitMQ 3.13 |
| 搜索 | Elasticsearch 8.15.5 |
| 存储 | MinIO |

## 项目结构

```
notask-flow/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── android/          # Kotlin Android 客户端
├── deploy/           # Docker 部署配置
│   ├── dev/          # 开发环境
│   └── prod/         # 生产环境
└── docs/             # 文档
```

## 快速开始

### Docker 一键部署（推荐）

```bash
git clone git@github.com:AtaraxyEcho/notask-flow.git
cd notask-flow
cp deploy/prod/.env.example deploy/prod/.env
# 编辑 .env 文件，修改所有密码和密钥
cd deploy/prod
docker compose --profile app up -d
```

### 本地开发

```bash
# 启动基础设施
cd deploy/dev && docker compose up -d

# 启动后端
cd backend && mvn spring-boot:run

# 启动前端
cd frontend && npm install && npm run dev
```

## 子项目文档

| 项目 | 文档 | 说明 |
|------|------|------|
| 后端 | [backend/README.md](backend/README.md) | API 服务、数据库、中间件 |
| 前端 | [frontend/README.md](frontend/README.md) | Web 界面、组件、状态管理 |
| Android | [android/README.md](android/README.md) | 移动客户端、模块架构 |
| 部署 | [deploy/README.md](deploy/README.md) | Docker 配置、环境变量 |

## 许可证

本项目使用 [GNU Affero General Public License v3.0](LICENSE) 许可证。
