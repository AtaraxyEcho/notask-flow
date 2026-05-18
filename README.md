# Notask Flow Android

Notask Flow Android 原生客户端骨架，基于 Kotlin、Jetpack Compose、Hilt、Retrofit、Room、DataStore 与 Clean Architecture 多模块结构。

## 模块

- `app`：应用入口、Hilt Application、根导航壳。
- `core`：通用能力、网络基础、数据库、DataStore、UI 设计系统与测试工具。
- `data`：API、DTO、Repository 实现与 DI。
- `domain`：领域模型、Repository 契约与 UseCase，尽量保持纯 Kotlin。
- `feature`：按业务包组织的 Compose 页面、ViewModel 与导航入口。

## 构建

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```
