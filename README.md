# Notask Flow Android

Notask Flow Android 原生客户端，基于 Kotlin、Jetpack Compose、Hilt、Retrofit、Room、DataStore 和 Clean Architecture 构建。当前分支已经包含登录注册、空间切换、首页、笔记与协作文档、任务、待办、文件、项目、成员、搜索、统计、通知和设置等移动端功能骨架与主要页面。

## 技术栈

| 类型 | 技术 |
|------|------|
| 语言 | Kotlin 2.3.x、Java 21 toolchain |
| UI | Jetpack Compose、Material 3 |
| 架构 | Clean Architecture + MVVM |
| 依赖注入 | Hilt |
| 网络 | Retrofit、OkHttp、Moshi |
| 本地存储 | Room、DataStore |
| 图片加载 | Coil |
| 图表 | Vico |
| 协作文档 | Android WebView + `app/src/main/assets/notask_collab/editor.js` |
| 构建 | Gradle Kotlin DSL、Version Catalog |

## 模块结构

```text
android/
├── app/        # Application、Activity、导航壳、Manifest、网络安全配置、协作编辑器 assets
├── core/       # 通用模型、网络基础、Room、DataStore、主题和通用 UI 组件
├── data/       # Retrofit API、DTO、Repository 实现、DI、URL 归一化
├── domain/     # 领域模型、Repository 契约、UseCase、策略测试
├── feature/    # Compose 页面和 ViewModel，按业务包组织
└── gradle/     # Gradle Wrapper 与 libs.versions.toml
```

## 本地配置

首次构建前建议准备两个本地配置文件：

```powershell
Copy-Item gradle.properties.example gradle.properties
Copy-Item local.properties.example local.properties
```

Android Studio 通常会自动生成 `local.properties`。如果你用命令行构建，请按本机路径修改 `sdk.dir`。

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `sdk.dir` | 示例为 `D:\Development\Android_SDK` | 本机 Android SDK 路径，只写在 `local.properties` |
| `org.gradle.java.home` | 默认注释 | 可选 JDK 21 路径，优先使用系统 `JAVA_HOME` |
| `notask.debugApiBaseUrl` | `http://10.0.2.2:8080/` | Debug 包 REST API 地址 |
| `notask.debugCollabWsUrl` | `ws://10.0.2.2:3000/ws` | Debug 包协作文档 WebSocket 地址 |
| `notask.releaseApiBaseUrl` | `https://api.example.com/` | Release 包 REST API 地址 |
| `notask.releaseCollabWsUrl` | `wss://api.example.com/ws` | Release 包协作文档 WebSocket 地址 |

### 真机联调

真机上的 `127.0.0.1` 指手机自己，不是电脑。真机访问本机后端时，需要把地址改成电脑局域网 IP：

```properties
notask.debugApiBaseUrl=http://192.168.1.20:8080/
notask.debugCollabWsUrl=ws://192.168.1.20:3000/ws
```

协作地址有两种常用写法：

| 场景 | 示例 |
|------|------|
| 通过 Web/Vite/nginx 代理 `/ws` | `ws://192.168.1.20:3000/ws` |
| 直接连接 `collab-ws` | `ws://192.168.1.20:8081/ws` |

你当前 Web 端如果是 `localhost:3000` 代理 `/ws` 到协作服务，Android 真机也应使用电脑局域网 IP 的 `3000/ws`，否则 Web 和 Android 可能连到不同的协作入口。

## 构建

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

Debug APK 输出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

运行领域层测试：

```powershell
.\gradlew.bat :domain:testDebugUnitTest
```

## 协作文档编辑器

Android 协作文档不是原生 Compose 富文本控件，而是 WebView 加载本地 assets：

```text
app/src/main/assets/notask_collab/editor.html
app/src/main/assets/notask_collab/editor.js
```

这些文件来自前端的 Android 协作内核构建。前端协作编辑器逻辑变更后，需要在 `frontend/` 执行：

```powershell
npm run build:android-collab
```

然后再重新构建 Android APK。

协作链路为：

```text
Android NoteEditRoute
  -> 后端 REST API 申请 collab ticket
  -> WebView 注入 baseUrl / collabWsUrl / noteId / spaceId / user
  -> editor.js 连接 collab-ws
  -> Yjs 增量同步正文和在线用户状态
```

## 网络安全配置

- `app/src/main/res/xml/network_security_config.xml`：主配置禁止明文 HTTP。
- `app/src/debug/res/xml/network_security_config.xml`：Debug 包允许明文 HTTP，方便连接本地 `8080`、`3000` 或 `8081`。
- `app/src/main/res/values/network_security.xml` 和 `app/src/debug/res/values/network_security.xml` 控制 Manifest 中的 `android:usesCleartextTraffic`。

Release 包必须使用 HTTPS/WSS 地址。

## 敏感信息规则

- 不要把真实 `gradle.properties`、`local.properties`、签名文件、`keystore.properties`、`secrets.properties` 或 `google-services.json` 提交到仓库。
- `notask.*ApiBaseUrl` 和 `notask.*CollabWsUrl` 是公开服务地址，不是密钥，但 release 发版前必须确认指向正确环境。
- Android APK 中不应放 JWT 签名密钥、管理员密码、SMTP 密码、MinIO 密钥、后端 `COLLAB_INTERNAL_TOKEN` 等服务端秘密。
- 当前站内通知不依赖 Firebase/FCM。后续如果启用 FCM，只在本地或 CI Secret 中提供 `google-services.json`。

## 排障速查

| 现象 | 优先检查 |
|------|----------|
| 登录提示无法连接后端 | `notask.debugApiBaseUrl` 是否指向手机可访问的后端地址，后端是否监听 `0.0.0.0:8080` |
| 模拟器可用但真机不可用 | 真机不能用 `10.0.2.2`，需要电脑局域网 IP |
| 协作文档能加载正文但不同步 | `notask.debugCollabWsUrl` 是否和 Web 端使用同一个 `/ws` 入口 |
| Android 输入后被 Web 覆盖 | Android 与 Web 可能连接不同协作服务，或 `editor.js` 不是最新前端构建产物 |
| 图片、附件预览失败 | `notask.debugApiBaseUrl` 是否是手机可访问地址，后端文件接口是否返回可访问 URL |
| Release 包连错环境 | 检查 `notask.releaseApiBaseUrl` 和 `notask.releaseCollabWsUrl` |

## 开发约定

- 新 API 先在 `data/*/api` 定义 Retrofit 接口，再通过 Repository 暴露给 `domain` 和 `feature`。
- Compose 页面只负责渲染和事件分发，业务状态放在对应 ViewModel。
- 时间格式与后端统一为 `yyyy-MM-dd:HH:mm:ss`，公共适配见 `core/common/DateTimeFormats.kt` 和 `core/network/LocalDateTimeJsonAdapter.kt`。
- 涉及协作文档时，优先保持 Web 和 Android 的 Yjs/Tiptap schema 兼容。
