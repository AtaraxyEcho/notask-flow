# Notask Flow

> Personal Knowledge Management & Team Task Collaboration Platform

English | [中文](README.md)

---

## Features

- **Personal Space**: Note editing, task management, todo tracking, file management, focus statistics
- **Team Space**: Project management, task kanban, collaborative documents, member management, team reports
- **Real-time Collaboration**: Multi-user document editing based on Yjs
- **Multi-platform Support**: Web frontend + Android native client

## Tech Stack

| Component | Technology |
|-----------|------------|
| Backend | Java 21, Spring Boot 3.2, MyBatis-Plus, Sa-Token |
| Frontend | Vue 3, TypeScript, Vite, Element Plus, TipTap |
| Android | Kotlin, Jetpack Compose, Hilt, Retrofit, Room |
| Database | MySQL 8.4, Redis 7.2 |
| Message Queue | RabbitMQ 3.13 |
| Search | Elasticsearch 8.15.5 |
| Storage | MinIO |

## Project Structure

```
notask-flow/
├── backend/          # Spring Boot backend
├── frontend/         # Vue 3 frontend
├── android/          # Kotlin Android client
├── deploy/           # Docker deployment configs
│   ├── dev/          # Development environment
│   └── prod/         # Production environment
└── docs/             # Documentation
```

## Quick Start

### Docker Deployment (Recommended)

```bash
git clone git@github.com:AtaraxyEcho/notask-flow.git
cd notask-flow
cp deploy/prod/.env.example deploy/prod/.env
# Edit .env file, modify all passwords and secrets
cd deploy/prod
docker compose --profile app up -d
```

### Local Development

```bash
# Start infrastructure
cd deploy/dev && docker compose up -d

# Start backend
cd backend && mvn spring-boot:run

# Start frontend
cd frontend && npm install && npm run dev
```

## Sub-project Documentation

| Project | Documentation | Description |
|---------|---------------|-------------|
| Backend | [backend/README.md](backend/README.md) | API service, database, middleware |
| Frontend | [frontend/README.md](frontend/README.md) | Web UI, components, state management |
| Android | [android/README.md](android/README.md) | Mobile client, module architecture |
| Deployment | [deploy/README.md](deploy/README.md) | Docker configuration, environment variables |

## License

This project is licensed under the [GNU Affero General Public License v3.0](LICENSE).
