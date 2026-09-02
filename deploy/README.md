# Notask Flow 部署指南

> Deployment Guide

[English](#english) | [中文](#中文)

---

## 中文

### 目录结构

```
deploy/
├── collab-ws/           # 协作服务源码（共享）
│   ├── Dockerfile
│   ├── package.json
│   └── src/
├── dev/
│   ├── docker-compose.yml
│   ├── .env.example
│   ├── Dockerfile.backend
│   ├── Dockerfile.frontend
│   └── nginx.conf
└── prod/
    ├── docker-compose.yml
    ├── .env.example
    ├── Dockerfile.backend
    ├── Dockerfile.frontend
    └── nginx.conf
```

### 环境说明

| 环境 | 目录 | 用途 | 启动命令 |
|------|------|------|----------|
| dev | `deploy/dev/` | 本地开发 | `docker compose up -d` |
| prod | `deploy/prod/` | 生产部署 | `docker compose --profile app up -d` |

### 快速开始

#### 开发环境

```bash
cd deploy/dev
cp .env.example .env
docker compose up -d
```

#### 生产环境

```bash
cd deploy/prod
cp .env.example .env
# ⚠️ 必须修改 .env 中所有密码和密钥
docker compose --profile app up -d
```

### 必须修改的配置

生产环境部署前，**必须修改**以下配置：

| 配置项 | 说明 | 安全建议 |
|--------|------|----------|
| `ADMIN_PASSWORD` | 管理员密码 | 使用 16+ 位强密码 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | 使用强密码 |
| `MYSQL_PASSWORD` | 应用数据库密码 | 使用强密码 |
| `REDIS_PASSWORD` | Redis 密码 | 使用强密码 |
| `SA_TOKEN_JWT_SECRET` | JWT 签名密钥 | 使用 32+ 位随机字符串 |
| `COLLAB_INTERNAL_TOKEN` | 内部服务通信密钥 | 使用 32+ 位随机字符串 |

### 服务端口

| 服务 | 端口 | 管理界面 |
|------|------|----------|
| 后端 API | 8080 | /swagger-ui/index.html |
| 前端 | 3000 | — |
| 协作 WebSocket | 8081 | — |
| MySQL | 3306 | — |
| Redis | 6379 | — |
| RabbitMQ | 5672 | 15672（管理界面） |
| MinIO | 9000 | 9001（控制台） |
| Elasticsearch | 9200 | — |

---

## English

### Directory Structure

```
deploy/
├── collab-ws/           # Collaboration service source (shared)
│   ├── Dockerfile
│   ├── package.json
│   └── src/
├── dev/
│   ├── docker-compose.yml
│   ├── .env.example
│   ├── Dockerfile.backend
│   ├── Dockerfile.frontend
│   └── nginx.conf
└── prod/
    ├── docker-compose.yml
    ├── .env.example
    ├── Dockerfile.backend
    ├── Dockerfile.frontend
    └── nginx.conf
```

### Environment Overview

| Environment | Directory | Purpose | Start Command |
|-------------|-----------|---------|---------------|
| dev | `deploy/dev/` | Local development | `docker compose up -d` |
| prod | `deploy/prod/` | Production deployment | `docker compose --profile app up -d` |

### Quick Start

#### Development Environment

```bash
cd deploy/dev
cp .env.example .env
docker compose up -d
```

#### Production Environment

```bash
cd deploy/prod
cp .env.example .env
# ⚠️ Must modify all passwords and secrets in .env
docker compose --profile app up -d
```

### Required Configuration Changes

Before deploying to production, **you must modify** the following configurations:

| Configuration | Description | Security Recommendation |
|---------------|-------------|------------------------|
| `ADMIN_PASSWORD` | Admin password | Use 16+ character strong password |
| `MYSQL_ROOT_PASSWORD` | MySQL root password | Use strong password |
| `MYSQL_PASSWORD` | Application database password | Use strong password |
| `REDIS_PASSWORD` | Redis password | Use strong password |
| `SA_TOKEN_JWT_SECRET` | JWT signing secret | Use 32+ character random string |
| `COLLAB_INTERNAL_TOKEN` | Internal service communication token | Use 32+ character random string |

### Service Ports

| Service | Port | Management Interface |
|---------|------|---------------------|
| Backend API | 8080 | /swagger-ui/index.html |
| Frontend | 3000 | — |
| Collaboration WebSocket | 8081 | — |
| MySQL | 3306 | — |
| Redis | 6379 | — |
| RabbitMQ | 5672 | 15672 (Management UI) |
| MinIO | 9000 | 9001 (Console) |
| Elasticsearch | 9200 | — |
