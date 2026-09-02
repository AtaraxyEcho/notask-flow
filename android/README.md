# Notask Flow Android

> Kotlin Android 原生客户端

[English](#english) | [中文](#中文)

---

## 中文

### 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Kotlin 2.3.x |
| UI | Jetpack Compose, Material 3 |
| 架构 | Clean Architecture + MVVM |
| 依赖注入 | Hilt |
| 网络 | Retrofit, OkHttp, Moshi |
| 本地存储 | Room, DataStore |
| 构建 | Gradle Kotlin DSL |

### 模块结构

```
android/
├── app/          # 应用入口、导航
├── core/         # 通用组件、网络、数据库
├── data/         # API、DTO、Repository
├── domain/       # 领域模型、UseCase
└── feature/      # 页面和 ViewModel
```

### 本地开发

```bash
# 构建 Debug APK
./gradlew.bat :app:assembleDebug

# 运行测试
./gradlew.bat :domain:testDebugUnitTest
```

### 配置

首次构建前配置 `gradle.properties` 和 `local.properties`：

```properties
# gradle.properties
notask.debugApiBaseUrl=http://10.0.2.2:8080/
notask.debugCollabWsUrl=ws://10.0.2.2:3000/ws
```

### 排障速查

| 现象 | 检查项 |
|------|--------|
| 无法连接后端 | API 地址是否正确 |
| 模拟器可用真机不可用 | 不能用 `10.0.2.2`，需用局域网 IP |
| 协作文档不同步 | WebSocket 地址是否正确 |

---

## English

### Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin 2.3.x |
| UI | Jetpack Compose, Material 3 |
| Architecture | Clean Architecture + MVVM |
| DI | Hilt |
| Network | Retrofit, OkHttp, Moshi |
| Storage | Room, DataStore |
| Build | Gradle Kotlin DSL |

### Module Structure

```
android/
├── app/          # App entry, navigation
├── core/         # Common components, network, database
├── data/         # API, DTO, Repository
├── domain/       # Domain models, UseCase
└── feature/      # Pages and ViewModels
```

### Local Development

```bash
# Build Debug APK
./gradlew.bat :app:assembleDebug

# Run tests
./gradlew.bat :domain:testDebugUnitTest
```

### Configuration

Configure `gradle.properties` and `local.properties` before first build:

```properties
# gradle.properties
notask.debugApiBaseUrl=http://10.0.2.2:8080/
notask.debugCollabWsUrl=ws://10.0.2.2:3000/ws
```

### Troubleshooting

| Issue | Check |
|-------|-------|
| Cannot connect backend | API URL correct |
| Works on emulator not device | Use LAN IP, not `10.0.2.2` |
| Collab doc not syncing | WebSocket URL correct |
