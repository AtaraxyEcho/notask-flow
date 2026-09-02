# Notask Flow 仓库合并方案

> **版本**：v2.0（根据审查反馈修正）  
> **创建日期**：2026-09-02  
> **最后更新**：2026-09-02

---

## 一、项目概述

### 1.1 当前状态

| 项目 | 路径 | 技术栈 | 仓库 |
|------|------|--------|------|
| Backend | `backend/` | Java 21 + Spring Boot 3.2.12 | 独立 Git 仓库 |
| Frontend | `frontend/` | Vue 3 + TypeScript + Vite | 独立 Git 仓库 |
| Android | `android/` | Kotlin + Jetpack Compose | 独立 Git 仓库 |

### 1.2 目标状态

```
notask-flow/                    # 统一仓库
├── backend/                    # subtree: 后端代码
├── frontend/                   # subtree: 前端代码
├── android/                    # subtree: Android 代码
├── deploy/                     # Docker 部署配置
│   ├── dev/                    # 开发环境
│   ├── prod/                   # 生产环境
│   └── README.md               # 部署文档
├── docs/                       # 文档（可选）
├── README.md                   # 中文主文档
├── README_EN.md                # 英文主文档
├── LICENSE                     # AGPL v3
└── .gitignore                  # 全局忽略规则
```

### 1.3 新仓库地址

```
git@github.com:AtaraxyEcho/notask-flow.git
```

---

## 二、Git Subtree 合并

### 2.1 选择 Subtree 的原因

| 方式 | 优点 | 缺点 |
|------|------|------|
| `git merge --allow-unrelated-histories` | 简单直接 | 历史混在一起，难以追溯 |
| **`git subtree`** | **保留独立历史，可单独拉取/推送** | **需要额外操作** |
| `git submodule` | 历史完全独立 | 需要 `git submodule update`，协作复杂 |

### 2.2 执行步骤

```bash
# ===========================================
# 阶段 1：初始化新仓库（草稿环境）
# ===========================================

# 克隆新仓库到临时草稿目录
git clone git@github.com:AtaraxyEcho/notask-flow.git /tmp/notask-flow-draft
cd /tmp/notask-flow-draft

# ===========================================
# 阶段 2：检查子仓库分支名
# ===========================================

# 检查各子仓库的默认分支名（重要！）
cd /path/to/current/android
git branch -r | grep HEAD  # 输出: origin/HEAD -> origin/main
# 确认 Android 默认分支为 main

cd /path/to/current/backend
git branch -r | grep HEAD  # 确认 backend 默认分支

cd /path/to/current/frontend
git branch -r | grep HEAD  # 确认 frontend 默认分支

# ===========================================
# 阶段 3：添加子仓库（不使用 --squash）
# ===========================================

cd /tmp/notask-flow-draft

# 添加 Android 子仓库（保留完整历史）
git remote add android-remote /path/to/current/android
git fetch android-remote
git subtree add --prefix=android android-remote main

# 添加 Backend 子仓库
git remote add backend-remote /path/to/current/backend
git fetch backend-remote
git subtree add --prefix=backend backend-remote main

# 添加 Frontend 子仓库
git remote add frontend-remote /path/to/current/frontend
git fetch frontend-remote
git subtree add --prefix=frontend frontend-remote main

# ===========================================
# 阶段 4：清理远程引用
# ===========================================

git remote remove android-remote
git remote remove backend-remote
git remote remove frontend-remote
```

### 2.3 重要说明

> ⚠️ **关于 `--squash` 参数**
>
> 本方案**不使用** `--squash` 参数，原因如下：
>
> 1. **保留完整历史**：每个文件的原始作者和提交记录都会保留
> 2. **支持后续同步**：`git subtree pull/push` 可以正常工作
> 3. **避免冲突**：不会因为历史压缩导致 Git 无法计算差异
>
> **后续维护策略**：
> - 合并完成后，**归档（Archive）旧的三个 Git 仓库**
> - 所有新开发统一在新仓库进行
> - 不再通过 `subtree push` 回推代码到原子仓库

### 2.4 后续同步命令（如需要）

```bash
# 拉取子仓库更新（仅在归档前使用）
# git subtree pull --prefix=android android-remote main

# 推送修改到子仓库（仅在归档前使用）
# git subtree push --prefix=android android-remote feature-branch
```

---

## 三、Deploy 目录重组

### 3.1 目标结构

```
deploy/
├── README.md                       # 部署文档（新增）
├── dev/
│   ├── docker-compose.yml          # 开发环境（仅基础设施）
│   └── .env.example                # 开发环境变量模板
│
└── prod/
    ├── docker-compose.yml          # 生产环境（全栈部署）
    ├── .env.example                # 生产环境变量模板
    └── docker/
        ├── backend/
        │   └── Dockerfile
        ├── frontend/
        │   ├── Dockerfile
        │   └── nginx.conf
        └── collab-ws/
            └── Dockerfile          # 可选：可直接引用 backend/docker/collab-ws/
```

### 3.2 环境对比

| 配置项 | dev | prod |
|--------|-----|------|
| 后端运行位置 | 宿主机（IDE/Maven） | Docker 容器 |
| 前端运行位置 | 宿主机（npm run dev） | Docker 容器（nginx） |
| 基础设施 | Docker 容器 | Docker 容器 |
| 服务寻址 | `localhost` / `host.docker.internal` | Docker 服务名 |
| 数据持久化 | 可选 | 必须配置 volumes |
| 启动方式 | `docker compose up -d` | `docker compose --profile app up -d` |

### 3.3 Docker 路径修正

> ⚠️ **重要**：`dockerfile` 的路径是**相对于 `context`** 计算的。

#### 3.3.1 后端服务 Build Context

**原配置** (`backend/docker-compose.yml:141-143`)：
```yaml
app:
  build:
    context: .
    dockerfile: docker/backend/Dockerfile
```

**修正后** (`deploy/prod/docker-compose.yml`)：
```yaml
app:
  build:
    context: ../../backend
    dockerfile: ../deploy/prod/Dockerfile/backend/Dockerfile
```

**路径验证**：
- context: `../../backend` → 指向 `notask-flow/backend/`
- dockerfile: `../deploy/prod/Dockerfile/backend/Dockerfile`（相对于 context）
- 实际路径: `notask-flow/deploy/prod/Dockerfile/backend/Dockerfile` ✓

#### 3.3.2 前端服务 Build Context

**原配置** (`backend/docker-compose.yml:247-249`)：
```yaml
frontend:
  build:
    context: ..
    dockerfile: backend/docker/frontend/Dockerfile
```

**修正后**：
```yaml
frontend:
  build:
    context: ../../
    dockerfile: deploy/prod/Dockerfile/frontend/Dockerfile
```

**路径验证**：
- context: `../../` → 指向 `notask-flow/`
- dockerfile: `deploy/prod/Dockerfile/frontend/Dockerfile`（相对于 context）
- 实际路径: `notask-flow/deploy/prod/Dockerfile/frontend/Dockerfile` ✓

#### 3.3.3 Collab-WS Build Context

**原配置** (`backend/docker-compose.yml:213-214`)：
```yaml
collab-ws:
  build:
    context: ./docker/collab-ws
    dockerfile: Dockerfile
```

**修正后**：
```yaml
collab-ws:
  build:
    context: ../../backend/docker/collab-ws
    dockerfile: Dockerfile
```

> ✅ **关于 Collab-WS 源码**
>
> **不复制源码**，直接引用 `backend/docker/collab-ws/` 目录作为 build context。
>
> Dockerfile 保持原样，无需修改。这样可以避免双重维护问题，确保源码只有一个真实来源。

**路径验证**：
- context: `../../backend/docker/collab-ws` → 指向 `notask-flow/backend/docker/collab-ws/`
- dockerfile: `Dockerfile`（相对于 context，即 `backend/docker/collab-ws/Dockerfile`）✓

#### 3.3.4 MySQL 初始化脚本

**原配置** (`backend/docker-compose.yml:22-23`)：
```yaml
mysql:
  volumes:
    - ./src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro
    - ./src/main/resources/db/data.sql:/docker-entrypoint-initdb.d/02-data.sql:ro
```

**修正后**：
```yaml
mysql:
  volumes:
    - ../../backend/src/main/resources/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro
    - ../../backend/src/main/resources/db/data.sql:/docker-entrypoint-initdb.d/02-data.sql:ro
```

**路径验证**：
- `../../backend/src/main/resources/db/schema.sql` → 指向 `notask-flow/backend/src/main/resources/db/schema.sql` ✓

### 3.4 Collab-WS 说明

Collab-WS 的 Dockerfile 和源码保留在 `backend/docker/collab-ws/` 目录中，docker-compose.yml 直接引用该目录作为 build context，无需复制。

### 3.5 环境变量文件

#### Dev 环境 (`.env.example`)

```bash
# ===========================================
# 开发环境配置
# ===========================================

# Spring 配置
SPRING_PROFILES_ACTIVE=dev

# 基础设施端口
MYSQL_HOST_PORT=3306
REDIS_HOST_PORT=6379
RABBITMQ_HOST_PORT=5672
MINIO_API_HOST_PORT=9000
MINIO_CONSOLE_HOST_PORT=9001
ELASTICSEARCH_HOST_PORT=9200

# 数据库配置
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=notask_flow
MYSQL_USERNAME=notask
MYSQL_PASSWORD=notask

# Redis
REDIS_PASSWORD=notask

# RabbitMQ
RABBITMQ_USERNAME=notask
RABBITMQ_PASSWORD=notask

# MinIO
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET=notask-flow

# Elasticsearch
ES_JAVA_OPTS=-Xms512m -Xmx512m
ELASTICSEARCH_ENABLED=true

# JWT
SA_TOKEN_JWT_SECRET=dev-jwt-secret-change-me-in-production

# 管理员账号（开发环境可使用默认值）
ADMIN_USERNAME=Administrator
ADMIN_PASSWORD=change-me

# 协作服务
COLLAB_INTERNAL_TOKEN=dev-collab-internal-token
COLLAB_API_BASE_URL=http://host.docker.internal:8080
COLLAB_REALTIME_BROADCAST_URL=http://localhost:8081/internal/broadcast
COLLAB_WS_HOST_PORT=8081

# 时区
TZ=Asia/Hong_Kong
```

#### Prod 环境 (`.env.example`)

```bash
# ===========================================
# 生产环境配置
# ===========================================

# ⚠️ 警告：部署前必须修改所有默认值！

# Spring 配置
SPRING_PROFILES_ACTIVE=docker

# 管理员账号（必须修改）
ADMIN_USERNAME=Administrator
ADMIN_PASSWORD=change-me-admin-password

# 基础设施端口
APP_HOST_PORT=8080
FRONTEND_HOST_PORT=3000
COLLAB_WS_HOST_PORT=8081
MYSQL_HOST_PORT=3306
REDIS_HOST_PORT=6379
RABBITMQ_HOST_PORT=5672
RABBITMQ_MANAGEMENT_HOST_PORT=15672
MINIO_API_HOST_PORT=9000
MINIO_CONSOLE_HOST_PORT=9001
ELASTICSEARCH_HOST_PORT=9200

# 数据库（必须修改密码）
MYSQL_ROOT_PASSWORD=change-me-root-password
MYSQL_DATABASE=notask_flow
MYSQL_USERNAME=notask
MYSQL_PASSWORD=change-me-db-password

# Redis（必须修改密码）
REDIS_PASSWORD=change-me-redis-password

# RabbitMQ（必须修改密码）
RABBITMQ_USERNAME=notask
RABBITMQ_PASSWORD=change-me-rabbitmq-password

# MinIO（必须修改密钥）
MINIO_ACCESS_KEY=change-me-minio-access-key
MINIO_SECRET_KEY=change-me-minio-secret-key
MINIO_BUCKET=notask-flow

# Elasticsearch
ES_JAVA_OPTS=-Xms1g -Xmx1g
ELASTICSEARCH_ENABLED=true

# JWT（必须修改为强随机字符串）
SA_TOKEN_JWT_SECRET=change-me-to-a-long-random-jwt-secret

# 协作服务（必须修改为强随机字符串）
COLLAB_INTERNAL_TOKEN=change-me-to-a-long-random-internal-token
COLLAB_API_BASE_URL=http://app:8080
COLLAB_REALTIME_BROADCAST_URL=http://collab-ws:8081/internal/broadcast

# CORS（按实际域名配置）
SECURITY_ALLOWED_ORIGINS=https://your-domain.com

# 邮件（可选）
NOTASK_MAIL_FROM=no-reply@your-domain.com
NOTASK_APP_BASE_URL=https://your-domain.com
MAIL_HOST=smtp.your-mail-provider.com
MAIL_PORT=465
MAIL_USERNAME=your-email@your-domain.com
MAIL_PASSWORD=change-me-mail-password

# 时区
TZ=Asia/Hong_Kong
```

> ✅ **环境变量名验证**
>
> 经检查后端 `application.yml:84-87`，确认环境变量名为：
> - `ADMIN_USERNAME`（映射到 `notask-flow.admin.username`）
> - `ADMIN_PASSWORD`（映射到 `notask-flow.admin.password`）
>
> 本方案中的变量名与后端配置一致，无需修改。

---

## 四、README 更新计划

### 4.1 文件清单

| 文件 | 位置 | 语言 | 内容定位 |
|------|------|------|----------|
| `README.md` | 根目录 | 中文 | 项目全景、快速开始、贡献指南 |
| `README_EN.md` | 根目录 | 英文 | 对应中文版 |
| `backend/README.md` | 后端 | 中英双语 | 后端技术栈、启动、配置、API |
| `frontend/README.md` | 前端 | 中英双语 | 前端技术栈、开发、构建、部署 |
| `android/README.md` | Android | 中英双语 | Android 技术栈、模块、构建、调试 |
| `deploy/README.md` | 部署 | 中英双语 | 部署指南、环境差异、安全清单 |

### 4.2 根目录 README 内容框架

```markdown
# Notask Flow

> 个人知识管理与团队任务协作平台

[English](README_EN.md) | 中文

## 核心特性

- **个人空间**：笔记编辑、任务管理、待办追踪、文件管理、专注统计
- **团队空间**：项目管理、任务看板、协作文档、成员管理、团队报表
- **实时协作**：基于 Yjs 的多人文档协同编辑
- **多端支持**：Web 前端 + Android 原生客户端

## 快速开始

### Docker 一键部署（推荐）

```bash
# 克隆仓库
git clone git@github.com:AtaraxyEcho/notask-flow.git
cd notask-flow

# 配置环境变量
cp deploy/prod/.env.example deploy/prod/.env
# 编辑 .env 文件，修改所有密码和密钥

# 启动服务
cd deploy/prod
docker compose --profile app up -d
```

### 本地开发

```bash
# 启动基础设施
cd deploy/dev
docker compose up -d

# 启动后端
cd backend
mvn spring-boot:run

# 启动前端
cd frontend
npm install
npm run dev
```

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

## 文档

- [后端文档](backend/README.md)
- [前端文档](frontend/README.md)
- [Android 文档](android/README.md)
- [部署指南](deploy/README.md)

## 许可证

本项目使用 [GNU Affero General Public License v3.0](LICENSE) 许可证。
```

### 4.3 Deploy README 内容框架

```markdown
# Notask Flow 部署指南

[English](README_EN.md) | 中文

## 环境说明

| 环境 | 目录 | 用途 | 启动命令 |
|------|------|------|----------|
| dev | `deploy/dev/` | 本地开发 | `docker compose up -d` |
| prod | `deploy/prod/` | 生产部署 | `docker compose --profile app up -d` |

## 快速开始

### 开发环境

```bash
cd deploy/dev
cp .env.example .env
docker compose up -d
```

### 生产环境

```bash
cd deploy/prod
cp .env.example .env
# ⚠️ 必须修改 .env 中所有密码和密钥
docker compose --profile app up -d
```

## 必须修改的配置

生产环境部署前，**必须修改**以下配置：

| 配置项 | 说明 | 安全建议 |
|--------|------|----------|
| `ADMIN_PASSWORD` | 管理员密码 | 使用 16+ 位强密码 |
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | 使用强密码 |
| `MYSQL_PASSWORD` | 应用数据库密码 | 使用强密码 |
| `REDIS_PASSWORD` | Redis 密码 | 使用强密码 |
| `SA_TOKEN_JWT_SECRET` | JWT 签名密钥 | 使用 32+ 位随机字符串 |
| `COLLAB_INTERNAL_TOKEN` | 内部服务通信密钥 | 使用 32+ 位随机字符串 |

## 服务端口

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

## 常见问题

请参考各子项目 README 中的排障速查部分。
```

### 4.4 子目录 README 精简原则

每个子目录 README 应包含：
1. **技术栈表格**
2. **本地开发命令**
3. **配置说明**（环境变量）
4. **构建/部署命令**
5. **英文摘要**（2-3 段）

删除内容：
- Docker 相关配置（已移至 `deploy/README.md`）
- 重复的项目结构说明
- 过长的 UI 展示（保留 2-3 张关键截图）

---

## 五、.gitignore 完善

### 5.1 完整忽略规则

```gitignore
# ==================== 系统文件 ====================
.DS_Store
Thumbs.db
Desktop.ini
*.swp
*.swo
*~

# ==================== IDE/编辑器 ====================
.idea/
.vscode/
*.iml
*.iws
*.ipr
.project
.classpath
.settings/
.factorypath
*.code-workspace

# ==================== 根目录 ====================
.claude/
docs/
issue.md

# ==================== Backend (Java/Spring) ====================
backend/target/
backend/.env
backend/.env.*
backend/logs/
backend/*.log
backend/.mvn/wrapper/maven-wrapper.jar
backend/out/

# ==================== Frontend (Node/Vue) ====================
frontend/node_modules/
frontend/dist/
frontend/.env.local
frontend/.env.development.local
frontend/.env.test.local
frontend/.env.production.local
frontend/npm-debug.log*
frontend/yarn-debug.log*
frontend/yarn-error.log*
frontend/pnpm-debug.log*
frontend/.vite/

# ==================== Android (Kotlin/Gradle) ====================
android/.gradle/
android/build/
android/app/build/
android/core/build/
android/data/build/
android/domain/build/
android/feature/build/
android/local.properties
android/keystore.properties
android/secrets.properties
android/google-services.json
android/*.jks
android/*.keystore
android/captures/
android/.externalNativeBuild/
android/.cxx/

# ==================== Deploy ====================
deploy/dev/.env
deploy/prod/.env

# ==================== 日志 ====================
logs/
*.log
```

### 5.2 子目录 .gitignore 合并来源

| 规则来源 | 说明 |
|----------|------|
| `android/.gitignore` | Gradle、Android Studio 配置 |
| `backend/.gitignore` | Maven、Spring Boot 日志 |
| `frontend/.gitignore` | Node_modules、Vite 缓存 |
| 根目录 `.gitignore` | IDE、系统文件 |

---

## 六、LICENSE 处理

### 6.1 当前状态

| 文件 | 位置 | 内容 |
|------|------|------|
| `android/LICENSE` | android/ | GNU AGPL v3 |
| `backend/LICENSE` | backend/ | GNU AGPL v3 |
| `frontend/LICENSE` | frontend/ | GNU AGPL v3 |

三个 LICENSE 文件内容完全相同。

### 6.2 处理方案

1. **保留**：根目录 `LICENSE`（从任一子目录复制）
2. **删除**：
   - `android/LICENSE`
   - `backend/LICENSE`
   - `frontend/LICENSE`
3. **引用**：在根目录 README 中声明许可证

---

## 七、执行步骤清单

### 阶段 1：草稿环境准备

| 步骤 | 操作 | 说明 |
|------|------|------|
| 1.1 | 创建草稿目录 | `/tmp/notask-flow-draft` |
| 1.2 | 克隆新仓库到草稿目录 | 避免污染正式仓库 |
| 1.3 | 检查各子仓库默认分支名 | 确认 `main` 或 `master` |

### 阶段 2：Subtree 合并

| 步骤 | 命令 | 说明 |
|------|------|------|
| 2.1 | `git remote add android-remote /path/to/android` | 添加 Android 远程 |
| 2.2 | `git fetch android-remote` | 拉取 Android 历史 |
| 2.3 | `git subtree add --prefix=android android-remote main` | 合并 Android（不压缩） |
| 2.4 | 重复 2.1-2.3 合并 backend 和 frontend | 合并其他子仓库 |
| 2.5 | `git remote remove *-remote` | 清理远程引用 |

### 阶段 3：Deploy 目录创建

| 步骤 | 操作 | 说明 |
|------|------|------|
| 3.1 | `mkdir -p deploy/{dev,prod}/docker/{backend,frontend}` | 创建目录结构 |
| 3.2 | 复制 Dockerfile 文件 | 从 backend/docker/ 复制 |
| 3.3 | 复制 nginx.conf | 放入 frontend/ 目录 |
| 3.4 | 创建 docker-compose.yml | 从 backend/ 复制并修正路径 |
| 3.5 | 创建 .env.example | 分别创建 dev 和 prod 版本 |
| 3.6 | 创建 deploy/README.md | 部署文档 |

### 阶段 4：文件清理

| 步骤 | 操作 | 说明 |
|------|------|------|
| 4.1 | 删除 `android/LICENSE` | 仅保留根目录 |
| 4.2 | 删除 `backend/LICENSE` | 仅保留根目录 |
| 4.3 | 删除 `frontend/LICENSE` | 仅保留根目录 |
| 4.4 | 删除 `backend/docker/backend/` | 已移至 deploy/ |
| 4.5 | 删除 `backend/docker/frontend/` | 已移至 deploy/ |
| 4.6 | 删除 `backend/docker-compose*.yml` | 已移至 deploy/ |
| 4.7 | 删除 `backend/.env.example` | 已移至 deploy/ |
| ⚠️ | **保留** `backend/docker/collab-ws/` | 源码真实来源，不可删除 |

### 阶段 5：README 更新

| 步骤 | 操作 | 说明 |
|------|------|------|
| 5.1 | 创建根目录 `README.md` | 中文主文档 |
| 5.2 | 创建根目录 `README_EN.md` | 英文主文档 |
| 5.3 | 更新 `backend/README.md` | 精简 + 英文摘要 |
| 5.4 | 更新 `frontend/README.md` | 精简 + 英文摘要 |
| 5.5 | 更新 `android/README.md` | 精简 + 英文摘要 |

### 阶段 6：.gitignore 更新

| 步骤 | 操作 | 说明 |
|------|------|------|
| 6.1 | 替换根目录 `.gitignore` | 合并所有忽略规则 |

### 阶段 7：验证与测试

| 步骤 | 操作 | 说明 |
|------|------|------|
| 7.1 | 测试 dev docker-compose | `docker compose config` 验证配置 |
| 7.2 | 测试 prod docker-compose | `docker compose config` 验证配置 |
| 7.3 | 检查所有文件引用路径 | 确保无断链 |

### 阶段 8：提交与推送

| 步骤 | 命令 | 说明 |
|------|------|------|
| 8.1 | `git add -A` | 暂存所有变更 |
| 8.2 | `git commit -m "chore: merge repositories and restructure deploy"` | 提交 |
| 8.3 | `git push origin main` | 推送到远程 |

### 阶段 9：旧仓库归档

| 步骤 | 操作 | 说明 |
|------|------|------|
| 9.1 | 在 GitHub 上 Archive 旧仓库 | 设置为 Read-only |
| 9.2 | 在旧仓库 README 添加指向新仓库的链接 | 引导迁移 |

---

## 八、验证清单

### 8.1 Subtree 合并验证

- [ ] 三个子仓库代码完整合并
- [ ] 历史记录可追溯（`git log --follow` 可追踪文件变更）
- [ ] 子目录结构正确

### 8.2 Deploy 目录验证

- [ ] `deploy/dev/docker-compose.yml` 配置正确（`docker compose config`）
- [ ] `deploy/prod/docker-compose.yml` 配置正确（`docker compose config`）
- [ ] 所有 Docker 路径引用正确
- [ ] 环境变量模板完整
- [ ] collab-ws Dockerfile 可正确构建

### 8.3 文件清理验证

- [ ] LICENSE 仅根目录一份
- [ ] `backend/docker/backend/` 已删除
- [ ] `backend/docker/frontend/` 已删除
- [ ] `backend/docker/collab-ws/` **保留**（作为唯一源码来源）
- [ ] 无冗余文件

### 8.4 README 验证

- [ ] 根目录 README 中英双语完整
- [ ] deploy/README.md 创建完成
- [ ] 子目录 README 精简且包含英文摘要
- [ ] 所有链接有效（无 CONTRIBUTING.md 死链）

### 8.5 .gitignore 验证

- [ ] 无敏感信息泄露风险
- [ ] 所有构建产物被忽略
- [ ] IDE 配置被忽略

---

## 九、风险与注意事项

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| Subtree 合并冲突 | 中 | 确保子仓库 main 分支是最新的 |
| Docker 路径错误 | 高 | 使用 `docker compose config` 验证 |
| 环境变量遗漏 | 高 | 对比新旧 .env.example |
| collab-ws 源码不一致 | 高 | 保留 `backend/docker/collab-ws/` 作为唯一来源 |
| 分支名不匹配 | 中 | 合并前检查各子仓库默认分支 |
| 敏感信息泄露 | 高 | 检查 .gitignore 覆盖所有敏感文件 |

---

## 十、完成时间估算

| 阶段 | 预计时间 |
|------|----------|
| 草稿环境准备 | 5 分钟 |
| Subtree 合并 | 10 分钟 |
| Deploy 目录重组 | 25 分钟 |
| README 更新 | 30 分钟 |
| .gitignore 完善 | 5 分钟 |
| 验证与测试 | 15 分钟 |
| 提交与推送 | 5 分钟 |
| **总计** | **约 95 分钟** |

---

## 附录：关键配置文件参考

### A. 后端环境变量映射

| 环境变量 | Spring 配置 | 默认值 |
|----------|-------------|--------|
| `ADMIN_USERNAME` | `notask-flow.admin.username` | `Administrator` |
| `ADMIN_PASSWORD` | `notask-flow.admin.password` | `change-me` |
| `COLLAB_INTERNAL_TOKEN` | `notask-flow.collab.internal-token` | — |
| `SA_TOKEN_JWT_SECRET` | — | — |

### B. Docker 路径对照表

| 服务 | 原路径 | 新路径 |
|------|--------|--------|
| Backend Build Context | `backend/` | `backend/`（通过相对路径引用） |
| Backend Dockerfile | `backend/docker/backend/Dockerfile` | `deploy/prod/docker/backend/Dockerfile` |
| Frontend Build Context | 项目根目录 | 项目根目录（通过相对路径引用） |
| Frontend Dockerfile | `backend/docker/frontend/Dockerfile` | `deploy/prod/docker/frontend/Dockerfile` |
| Collab-WS Build Context | `backend/docker/collab-ws/` | `backend/docker/collab-ws/`（不变） |
| Collab-WS Dockerfile | `backend/docker/collab-ws/Dockerfile` | 同左（不变） |
| MySQL Schema | `backend/src/main/resources/db/schema.sql` | 同左（通过相对路径引用） |
