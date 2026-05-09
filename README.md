# Notask Flow Android

Notask Flow Android 原生客户端骨架，基于 Kotlin、Jetpack Compose、Hilt、Retrofit、Room、DataStore 与 Clean Architecture 多模块结构。

## 模块

- `app`：应用入口、Hilt Application、根导航壳。
- `core/*`：通用能力、领域模型、网络、数据库、DataStore、UI 设计系统与测试工具。
- `data`：API、DTO、Repository 实现与 DI。
- `domain`：Repository 契约与 UseCase。
- `feature/*`：按业务屏幕拆分的 Compose 功能模块。

## 构建

```powershell
cd android
gradle :app:assembleDebug
```
