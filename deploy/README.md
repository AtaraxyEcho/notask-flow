# Notask Flow 部署指南

> Deployment Guide

中文 | [English](README_EN.md)

---

## 目录结构

```
deploy/
├── collab-ws/           # Yjs 协同 WebSocket 服务源码(独立 Node.js 服务)
│   ├── Dockerfile
│   ├── package.json
│   └── src/server.js
├── dev/
│   ├── docker-compose.yml   # 仅基础设施(7 个服务)
│   ├── .env.example
│   ├── Dockerfile.backend   # 供本地验证镜像构建
│   ├── Dockerfile.frontend
│   └── nginx.conf
└── prod/
    ├── docker-compose.yml   # 基础设施(6 个)+ profile "app" 的 3 个应用服务
    ├── .env.example
    ├── Dockerfile.backend
    ├── Dockerfile.frontend
    └── nginx.conf
```

## 环境说明

| 环境 | 目录 | 用途 | 启动命令 |
|------|------|------|----------|
| dev | `deploy/dev/` | 本地开发(后端/前端跑在宿主机,这里只起基础设施与 collab-ws) | `docker compose up -d` |
| prod | `deploy/prod/` | 生产部署(全部容器化) | `docker compose --profile app up -d` |

> prod 的 profile 说明:`docker compose up -d` 只启动 6 个基础设施服务;加 `--profile app` 才会额外构建并启动 `app`(后端)、`frontend` 与 `collab-ws`。`dev` 与 `prod` 共用同一套 Dockerfile 与 nginx 配置。

## 服务组成

| 服务 | 镜像/构建 | 端口(默认) | 说明 |
|------|-----------|--------------|------|
| app | 源码构建 `Dockerfile.backend`(Maven 21 构建 → JRE 21 运行,非 root) | 8080 | 仅 prod,profile `app` |
| frontend | 源码构建 `Dockerfile.frontend`(Node 22 构建 → Nginx 1.27) | 3000 → 容器 80 | 仅 prod,profile `app` |
| collab-ws | 源码构建 `deploy/collab-ws`(Node 20) | 8081 | dev/prod 都有 |
| mysql | `mysql:8.4` | 3306 | 首次启动自动导入建表/种子 SQL |
| redis | `redis:7.2` | 6379 | 已开启 AOF 持久化 |
| rabbitmq | `rabbitmq:3.13-management` | 5672 / 15672 | 15672 为管理界面 |
| minio + minio-init | `minio` + `mc` | 9000 / 9001 | 9001 为控制台;minio-init 自动建桶 |
| elasticsearch | `elasticsearch:8.15.5` | 9200 | 单节点,默认堆 512m(dev)/ 1g(prod) |

## 快速开始

### 开发环境

```bash
cd deploy/dev
cp .env.example .env
docker compose up -d
```

MySQL 首次启动时自动把 [`backend/src/main/resources/db/`](../backend/src/main/resources/db/) 下的 `schema.sql`、`data.sql` 挂载为初始化脚本导入。注意:**仅数据卷为空时执行**;之后修改 SQL 需手动重放,或 `docker compose down -v` 删卷重建(会清空数据)。

### 生产环境

```bash
cd deploy/prod
cp .env.example .env
# ⚠️ 必须修改 .env 中所有密码和密钥
docker compose --profile app up -d
```

## 必须修改的配置

生产环境部署前,**必须修改** `deploy/prod/.env` 中的以下配置(模板中已标注"必须修改"):

| 配置项 | 说明 | 安全建议 |
|--------|------|----------|
| `ADMIN_PASSWORD` | 管理员密码 | 使用 16+ 位强密码 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | 使用强密码 |
| `MYSQL_PASSWORD` | 应用数据库密码 | 使用强密码 |
| `REDIS_PASSWORD` | Redis 密码 | 使用强密码 |
| `RABBITMQ_PASSWORD` | RabbitMQ 密码 | 使用强密码 |
| `MINIO_SECRET_KEY` | MinIO 访问密钥 | 使用强密钥 |
| `SA_TOKEN_JWT_SECRET` | JWT 签名密钥 | 使用 32+ 位随机字符串 |
| `COLLAB_INTERNAL_TOKEN` | 后端 ↔ collab-ws 内部通信令牌 | 使用 32+ 位随机字符串 |
| `SECURITY_ALLOWED_ORIGINS` | CORS 白名单 | 改为实际前端域名 |

完整变量清单与注释见各 `.env.example`,README 不罗列具体值。

## Nginx 代理规则

`nginx.conf` 打包进前端镜像(prod 的 frontend 容器),三条规则:

| 路径 | 上游 | 说明 |
|------|------|------|
| `/api/` | `app:8080` | 后端 REST API |
| `/ws` | `collab-ws:8081` | WebSocket(Upgrade 头 + 24h 读超时) |
| `/` | 静态文件 | SPA 回退到 `index.html` |

## collab-ws 服务

独立的 Yjs 协同 WebSocket 服务(Node.js):房间按 `space:{spaceId}:note:{noteId}` 组织,连接后 15 秒内必须携带 Ticket 完成 auth(经后端内部接口校验);另提供 `GET /health` 健康检查与 `POST /internal/broadcast` 事件广播(供后端推送空间事件)。本地独立运行:

```bash
cd deploy/collab-ws
npm ci && npm start          # 默认监听 8081,WS 路径 /ws
```

可通过环境变量覆盖:`PORT`、`WS_PATH`、`API_BASE_URL`(指向后端)、`INTERNAL_TOKEN`(与后端 `COLLAB_INTERNAL_TOKEN` 一致)。

## 服务端口

| 服务 | 端口 | 管理界面 |
|------|------|----------|
| 后端 API | 8080 | /swagger-ui/index.html |
| 前端 | 3000 | — |
| 协作 WebSocket | 8081 | /health |
| MySQL | 3306 | — |
| Redis | 6379 | — |
| RabbitMQ | 5672 | 15672(管理界面) |
| MinIO | 9000 | 9001(控制台) |
| Elasticsearch | 9200 | — |

所有端口均可在 `.env` 中通过 `*_HOST_PORT` 变量调整(含 `RABBITMQ_MANAGEMENT_HOST_PORT`)。

---

Last Updated: 2026-09-04
