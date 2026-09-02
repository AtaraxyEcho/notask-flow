# Notask Flow Backend

> Spring Boot 后端 API 服务

[English](#english) | [中文](#中文)

---

## 中文

### 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Java 21 |
| 框架 | Spring Boot 3.2.12 |
| ORM | MyBatis Plus 3.5.7 |
| 认证 | Sa-Token 1.39.0（JWT + Redis） |
| 数据库 | MySQL 8.4 |
| 缓存 | Redis 7.2 |
| 消息队列 | RabbitMQ 3.13 |
| 搜索 | Elasticsearch 8.15.5 |
| 存储 | MinIO |

### 项目结构

```
backend/
├── src/main/java/com/notaskflow/
│   ├── config/            # Spring 配置
│   ├── controller/        # REST 控制器
│   ├── service/           # 业务逻辑层
│   ├── domain/            # 领域模型
│   ├── mapper/            # MyBatis 映射器
│   ├── security/          # 安全认证
│   ├── exception/         # 异常处理
│   ├── event/             # 事件定义
│   ├── listener/          # 事件监听器
│   ├── mq/                # 消息队列
│   └── job/               # 定时任务
├── src/main/resources/
│   ├── application.yml    # 主配置
│   └── db/                # 数据库脚本
└── pom.xml
```

### 本地开发

```bash
# 启动基础设施
cd ../deploy/dev
docker compose up -d

# 启动后端
mvn spring-boot:run
```

### API 文档

启动后访问：http://localhost:8080/swagger-ui/index.html

### 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `SPRING_PROFILES_ACTIVE` | 配置 profile | `dev` |
| `MYSQL_USERNAME` | 数据库用户 | `notask` |
| `MYSQL_PASSWORD` | 数据库密码 | `notask` |
| `REDIS_PASSWORD` | Redis 密码 | `notask` |
| `SA_TOKEN_JWT_SECRET` | JWT 密钥 | — |
| `ADMIN_USERNAME` | 管理员账号 | `Administrator` |
| `ADMIN_PASSWORD` | 管理员密码 | `change-me` |

### 排障速查

| 现象 | 检查项 |
|------|--------|
| MySQL 连接失败 | 数据库是否启动，凭据是否正确 |
| Redis 认证失败 | 密码是否匹配 |
| 登录后被踢回 | JWT 密钥是否变化 |

---

## English

### Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2.12 |
| ORM | MyBatis Plus 3.5.7 |
| Auth | Sa-Token 1.39.0 (JWT + Redis) |
| Database | MySQL 8.4 |
| Cache | Redis 7.2 |
| Message Queue | RabbitMQ 3.13 |
| Search | Elasticsearch 8.15.5 |
| Storage | MinIO |

### Project Structure

```
backend/
├── src/main/java/com/notaskflow/
│   ├── config/            # Spring configuration
│   ├── controller/        # REST controllers
│   ├── service/           # Business logic
│   ├── domain/            # Domain models
│   ├── mapper/            # MyBatis mappers
│   ├── security/          # Security & auth
│   ├── exception/         # Exception handling
│   ├── event/             # Event definitions
│   ├── listener/          # Event listeners
│   ├── mq/                # Message queue
│   └── job/               # Scheduled jobs
├── src/main/resources/
│   ├── application.yml    # Main config
│   └── db/                # Database scripts
└── pom.xml
```

### Local Development

```bash
# Start infrastructure
cd ../deploy/dev
docker compose up -d

# Start backend
mvn spring-boot:run
```

### API Documentation

Access after startup: http://localhost:8080/swagger-ui/index.html

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Config profile | `dev` |
| `MYSQL_USERNAME` | Database user | `notask` |
| `MYSQL_PASSWORD` | Database password | `notask` |
| `REDIS_PASSWORD` | Redis password | `notask` |
| `SA_TOKEN_JWT_SECRET` | JWT secret | — |
| `ADMIN_USERNAME` | Admin account | `Administrator` |
| `ADMIN_PASSWORD` | Admin password | `change-me` |

### Troubleshooting

| Issue | Check |
|-------|-------|
| MySQL connection failed | Database running, credentials correct |
| Redis auth failed | Password matches |
| Redirected to login after auth | JWT secret changed |
