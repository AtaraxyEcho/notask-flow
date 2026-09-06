# Notask Flow Deployment Guide

[中文](README.md) | English

---

## Directory Structure

```
deploy/
├── collab-ws/           # Yjs collaboration WebSocket service source (standalone Node.js)
│   ├── Dockerfile
│   ├── package.json
│   └── src/server.js
├── dev/
│   ├── docker-compose.yml   # Infrastructure only (7 services)
│   ├── .env.example
│   ├── Dockerfile.backend   # For verifying image builds locally
│   ├── Dockerfile.frontend
│   └── nginx.conf
└── prod/
    ├── docker-compose.yml   # Infrastructure (6) + 3 app services under profile "app"
    ├── .env.example
    ├── Dockerfile.backend
    ├── Dockerfile.frontend
    └── nginx.conf
```

## Environment Overview

| Environment | Directory | Purpose | Start Command |
|-------------|-----------|---------|---------------|
| dev | `deploy/dev/` | Local development (backend/frontend run on the host; only infrastructure and collab-ws here) | `docker compose up -d` |
| prod | `deploy/prod/` | Production (fully containerized) | `docker compose --profile app up -d` |

> prod profile notes: `docker compose up -d` starts only the 6 infrastructure services; adding `--profile app` also builds and starts `app` (backend), `frontend` and `collab-ws`. `dev` and `prod` share the same Dockerfiles and nginx config.

## Service Composition

| Service | Image/Build | Port (default) | Notes |
|---------|-------------|----------------|-------|
| app | Built from source, `Dockerfile.backend` (Maven 21 build → JRE 21, non-root) | 8080 | prod only, profile `app` |
| frontend | Built from source, `Dockerfile.frontend` (Node 22 build → Nginx 1.27) | 3000 → container 80 | prod only, profile `app` |
| collab-ws | Built from `deploy/collab-ws` (Node 20) | 8081 | present in both dev and prod |
| mysql | `mysql:8.4` | 3306 | schema/seed SQL imported automatically on first start |
| redis | `redis:7.2` | 6379 | AOF persistence enabled |
| rabbitmq | `rabbitmq:3.13-management` | 5672 / 15672 | 15672 is the management UI |
| minio + minio-init | `minio` + `mc` | 9000 / 9001 | 9001 is the console; minio-init creates the bucket |
| elasticsearch | `elasticsearch:8.15.5` | 9200 | single node, default heap 512m (dev) / 1g (prod) |

## Quick Start

### Development Environment

```bash
cd deploy/dev
cp .env.example .env
docker compose up -d
```

On first start, MySQL automatically imports `schema.sql` and `data.sql` from [`backend/src/main/resources/db/`](../backend/src/main/resources/db/), mounted as init scripts. Note: they run **only when the data volume is empty**; later SQL changes must be replayed manually, or recreate volumes with `docker compose down -v` (wipes data).

### Production Environment

```bash
cd deploy/prod
cp .env.example .env
# ⚠️ Must modify all passwords and secrets in .env
docker compose --profile app up -d
```

## Required Configuration Changes

Before deploying to production, **you must modify** the following in `deploy/prod/.env` (marked "must change" in the template):

| Configuration | Description | Security Recommendation |
|---------------|-------------|------------------------|
| `ADMIN_PASSWORD` | Admin password | Use a 16+ character strong password |
| `MYSQL_ROOT_PASSWORD` | MySQL root password | Use a strong password |
| `MYSQL_PASSWORD` | Application database password | Use a strong password |
| `REDIS_PASSWORD` | Redis password | Use a strong password |
| `RABBITMQ_PASSWORD` | RabbitMQ password | Use a strong password |
| `MINIO_SECRET_KEY` | MinIO secret key | Use a strong key |
| `SA_TOKEN_JWT_SECRET` | JWT signing secret | Use a 32+ character random string |
| `COLLAB_INTERNAL_TOKEN` | Backend ↔ collab-ws internal token | Use a 32+ character random string |
| `SECURITY_ALLOWED_ORIGINS` | CORS whitelist | Set to your actual frontend domain |

See each `.env.example` for the full variable list with comments; specific values are not repeated here.

## Nginx Proxy Rules

`nginx.conf` is baked into the frontend image (prod frontend container) with three rules:

| Path | Upstream | Purpose |
|------|----------|---------|
| `/api/` | `app:8080` | Backend REST API |
| `/ws` | `collab-ws:8081` | WebSocket (Upgrade headers + 24h read timeout) |
| `/` | Static files | SPA fallback to `index.html` |

## collab-ws Service

A standalone Yjs collaboration WebSocket service (Node.js): rooms are organized as `space:{spaceId}:note:{noteId}`; connections must complete auth with a ticket within 15 seconds (verified via the backend's internal API). It also exposes `GET /health` and `POST /internal/broadcast` (used by the backend to push space events). Run it standalone:

```bash
cd deploy/collab-ws
npm ci && npm start          # listens on 8081 by default, WS path /ws
```

Environment overrides: `PORT`, `WS_PATH`, `API_BASE_URL` (points to the backend), `INTERNAL_TOKEN` (must match the backend's `COLLAB_INTERNAL_TOKEN`).

## Service Ports

| Service | Port | Management Interface |
|---------|------|---------------------|
| Backend API | 8080 | /swagger-ui/index.html |
| Frontend | 3000 | — |
| Collaboration WebSocket | 8081 | /health |
| MySQL | 3306 | — |
| Redis | 6379 | — |
| RabbitMQ | 5672 | 15672 (Management UI) |
| MinIO | 9000 | 9001 (Console) |
| Elasticsearch | 9200 | — |

All ports are adjustable via `*_HOST_PORT` variables in `.env` (including `RABBITMQ_MANAGEMENT_HOST_PORT`).

---

Last Updated: 2026-09-04
