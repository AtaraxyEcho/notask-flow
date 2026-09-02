

# CLAUDE.md

本文件为 Claude Code（及同类 AI 编程助手）提供在 `Notask Flow Android` 仓库中工作的完整指导。

## 项目概述

Notask Flow 是一款原生 Android 应用，提供个人知识管理与团队任务协作功能。  
后端为 Java Spring Boot REST API（见仓库 `swagger-api.json`），Web 前端位于 `E:\Codex\Notask-Flow\frontend`。

## 构建与运行

```bash
# Debug 构建
./gradlew :app:assembleDebug

# Release 构建
./gradlew :app:assembleRelease

# 执行所有测试
./gradlew test

# 执行单模块测试
./gradlew :domain:test
./gradlew :data:test

# 代码质量检查（如已配置 ktlint/detekt）
./gradlew ktlintCheck
```

**Base URL 重要说明**：
- Debug 构建在**模拟器**上默认使用 `http://10.0.2.2:8080/`（模拟器访问宿主机 localhost 的专用地址）
- 后端物理地址为 `localhost:8080`，但模拟器自身 `localhost` 指向模拟器内部，因此必须用 `10.0.2.2`
- 真机调试时请使用电脑的局域网 IP（如 `http://192.168.1.100:8080/`）或执行 `adb reverse tcp:8080 tcp:8080` 后使用 `http://localhost:8080/`
- 建议通过 `buildConfigField` 按构建类型区分，避免硬编码

## 模块架构（5 模块）

```
app ──→ feature ──→ core
              │        ↑
              ├──→ data ──→ domain ──→ core（仅 common/testing）
              └──→ domain
```

| 模块      | 职责         | 关键内容                                                     |
| --------- | ------------ | ------------------------------------------------------------ |
| `app`     | 应用入口     | Hilt Application、MainActivity、根 NavHost、`@HiltAndroidApp` |
| `core`    | 基础设施     | 设计系统/主题、Retrofit 配置、OkHttp 拦截器、Room 数据库/实体/DAO、DataStore、公共扩展函数 |
| `domain`  | 纯业务领域   | 数据类、仓库接口、UseCase。**无 Android 依赖**               |
| `data`    | 数据层       | 仓库实现、Retrofit API 接口、DTO/Moshi 适配器、Room 实体/DAO、映射器、离线同步引擎、Hilt DI 模块 |
| `feature` | 展示层（UI） | 所有页面（Screen + ViewModel）、Composable、导航图、MVI 状态 |

## 技术栈

| 关注点   | 库/框架                                     |
| -------- | ------------------------------------------- |
| 语言     | Kotlin 2.3.21                               |
| UI       | Jetpack Compose + Material 3（BOM 2025.01） |
| DI       | Hilt 2.59.2                                 |
| 网络     | Retrofit 2.11 + OkHttp 4.12 + Moshi 1.15.1  |
| 数据库   | Room 2.7.0                                  |
| 配置存储 | DataStore 1.1.1                             |
| 导航     | Navigation Compose 2.8.0（类型安全路由）    |
| 分页     | Paging 3.3.0 + Paging Compose               |
| 图片加载 | Coil 3.0.0                                  |
| 图表     | Vico 2.0.0                                  |
| 后台任务 | WorkManager 2.9.1                           |
| 推送     | Firebase Cloud Messaging 24.0.0             |
| 测试     | JUnit 5、MockK、Turbine、Compose UI Test    |

版本目录 `gradle/libs.versions.toml`。AGP 9.2.0，compileSdk/targetSdk 35，minSdk 26。

## 核心架构模式

### MVI（简化版）

每个页面遵循：

- **UiState**：不可变数据类，持有所有 UI 状态
- **UiEvent**：密封接口，表示用户操作
- **UiEffect**：密封接口，表示一次性副作用（导航、提示等）

ViewModel 暴露 `StateFlow<UiState>`，接收 `UiEvent`，通过 `Channel` 发送 `UiEffect`。

### 三层数据映射

```
DTO（Moshi 注解，严格匹配 JSON） → 映射器扩展函数 → 领域模型 → UiState
```

所有 API 响应包装为 `ApiResponse<T>`：
```kotlin
{ code: Int, message: String?, data: T? }
```
成功时 `code = 200`。分页响应使用 `PageResponse<T>`：
```kotlin
{ total: Long, pageNum: Long, pageSize: Long, list: List<T> }
```

### JWT 认证流程

1. 登录 → `POST /api/v1/auth/login` → 返回 `{ userId, tokenName, tokenValue, expireTime }`
2. `AuthInterceptor` 从 `TokenManager`（DataStore）读取 token，注入 `Authorization: Bearer {token}`
3. 收到 401 时 `AuthRefreshInterceptor` 静默调用 `POST /api/v1/auth/refresh` 并重试原请求
4. 若刷新也失败 → 清空本地数据 → 导航至登录页
5. WorkManager 在 token 剩余寿命 30% 时主动刷新

### 离线优先同步

- **读路径**：`Room DAO（Flow<List<T>>）` → 领域模型 → UI。后台网络拉取更新 Room，Room 自动发射新数据。
- **写路径**：网络调用成功 → 更新 Room 缓存。若 `IOException` → 写入 `OfflineQueueEntity` → 乐观 UI 显示“待同步”标记。
- **SyncManager**：监听网络恢复，FIFO 处理队列，指数退避（最多重试 3 次）。WorkManager 每 15 分钟同步兜底。
- **冲突解决**：v1 服务器时间戳优先。

### 依赖注入（Hilt）

模块位于 `data/di/`：

- `NetworkModule`：Retrofit + OkHttpClient
- `DatabaseModule`：Room 数据库 + DAO
- `DataStoreModule`：TokenManager、UserPreferences
- Repository 绑定：`@Binds` 接口 → 实现类

## 主题系统（运行时切换）

```kotlin
data class ThemeConfig(
    val mode: ThemeMode,           // LIGHT, DARK, SYSTEM
    val personalPreset: PersonalPreset, // SUNRISE, FOREST, OCEAN, MIDNIGHT
    val spaceType: SpaceType       // PERSONAL, TEAM
)
```

**5 套配色方案**（精确十六进制值见 `android-ui-ux-design.md`）：

- Sunrise（暖陶土色 `#9F4122`，表面 `#FFF8F6`）
- Forest（绿色 `#396B3E`，表面 `#FAFDF8`）
- Ocean（蓝色 `#005D90`，表面 `#FBF8FF`）
- Midnight（深色，主色 `#FFB59E`，表面 `#141210`）
- Team（固定品牌蓝 `#005D90`，表面 `#FBF8FF`）

**空间差异化设计令牌**：

- 个人空间：卡片圆角 24dp，投影阴影，行高 1.8，标题 Newsreader 衬线字体，轮廓图标
- 团队空间：卡片圆角 12dp，1dp 边框，行高 1.4，系统无衬线字体，填充图标。背景双径向渐变，玻璃拟态。

切换时关键颜色使用 `animateColorAsState`（400ms 缓动），形状/字体立即切换。

## 导航结构

```
NavHost（startDestination = "splash"）
├── splash → SplashScreen
├── auth（嵌套图）：login, register, forgotPassword, resetPassword
├── home（需鉴权）：
│   ├── notes, notes/{noteId}
│   ├── tasks, tasks/{taskId}
│   ├── todos
│   ├── files, files/preview/{fileId}
│   ├── projects, projects/{projectId}
│   ├── stats
│   ├── notifications
│   ├── settings
│   └── team_members, team_members/pending
├── publicNote/{shareCode}（无需鉴权）
└── invite/{teamCode}（无需鉴权）
```

底部导航：个人空间 = 首页/笔记/任务/待办/文件；团队空间 = 首页(项目)/文档/任务/文件/成员。切换空间清空返回栈，各标签保存状态。

## API 约定

所有端点前缀 `/api/v1/`。请求/响应 JSON 小驼峰。路径 ID 类型 `Long`。分页参数 `pageNum`（1-based）和 `pageSize`。

主要端点分组（详见 `swagger-api.json`）：

- 认证：login, register, refresh, logout, forgot/reset-password
- 用户：profile, password, avatar, notification-settings, search
- 空间：list, create, get, update, delete, 成员, 邀请, 权限, join
- 笔记：CRUD, search, 历史版本, 恢复, 分享, 协作, 标签
- 任务：CRUD, 状态 PATCH, 指派, 认领, 成员, 评论, 附件
- 待办：CRUD, 完成/取消完成
- 文件：上传（直接+分块）, CRUD, 文件夹, 树, 统计, 预览, 下载 URL, 回收站, 操作日志, 引用
- 项目：CRUD, 归档, 成员, 笔记, 任务
- 通知：列表, 未读数, 标记已读, 删除, 清除已读
- 统计：个人, 趋势, 空间活动, 负载, 角色完成度
- 标签/笔记本/评论/团队申请/附件等

## UI 实现参考

`android-html-ui/` 包含每个页面的 HTML/CSS 模拟，使用 Tailwind 并遵循设计令牌。**UI 实现必须与 HTML 参考完全一致**。

关键页面覆盖：
- `login_screen`
- `home_screen_personal_space_english_nav`
- `note_dashboard_notebook_hierarchy`
- `note_editor_card_image_style`
- `task_kanban_with_optimized_switcher`
- `task_list_with_hidden_scrollbars`
- `task_detail_personal_space`
- `todo_list_personal_space_corrected_nav`
- `files_personal_space`
- `team_space_*`

设计令牌详细记录在 `android-html-ui/notask_flow_android_design_system/DESIGN.md`。

---

## 代码约束（强制遵守）

以下规则对所有 Kotlin/Compose 代码必须严格执行：

### 1. 禁止全限定名

- ✅ 正确：使用 `import` 后直接使用短名
- ❌ 错误：`fun process(com.example.Note note)`

### 2. 禁止重要参数硬编码

重要参数包括：网络超时、分页大小、文件分块阈值（50MB）、缓存大小、重试次数等。

- ✅ 正确：定义在 `BuildConfig`、`Constants` 对象或 `gradle.properties`
- ❌ 错误：`connectTimeout(30, TimeUnit.SECONDS)` 中的 `30` 未提取为常量

### 3. 密封类处理必须使用 `when` 覆盖所有分支

```kotlin
when (apiResponse) {
    is ApiResponse.Success -> handleSuccess()
    is ApiResponse.Error -> handleError()
    is ApiResponse.Loading -> showLoading()
} // 编译器检查完备性
```

### 4. Kotlin 代码风格

- **命名**：
  - 类/接口：`UpperCamelCase`
  - 函数/属性：`lowerCamelCase`
  - 常量：`UPPER_SNAKE_CASE`
  - 测试函数：用反引号包围的自然语言
- **函数长度**：单一函数 ≤ 40 行（getter/setter 除外）
- **注释**：公共 API 必须有 KDoc；复杂逻辑注释 “为什么”；禁止废话注释；必须使用中文描述注释
- **Data class**：所有纯数据载体必须用 `data class`
- **作用域函数**：避免嵌套超过 2 层
- **协程**：使用 `viewModelScope` 或 `rememberCoroutineScope`；禁止 `GlobalScope`
- **集合操作**：优先使用 `map`、`filter`、`groupBy` 等标准库函数
- **空安全**：禁止 `!!`，使用 `?.`、`?:`、`requireNotNull()`

### 5. 架构分层约束

- `domain` 层 **禁止** 依赖 Android 框架类（`Context`、`SharedPreferences` 等）
- `data` 层 **禁止** 依赖 `feature` 或 `app`
- `core` 中 Android 无关的工具放 `core/common`，Android 特定基础设施放 `core/utils`
- `UseCase` 必须无状态，仅依赖仓库接口

### 6. 错误处理与日志

- 所有网络/数据库操作须捕获异常并映射为业务错误（如 `Result<T, DomainError>`）
- 禁止吞掉异常（catch 空块），至少打印 `Log.e(TAG, "msg", ex)`
- 每个类定义 `private const val TAG = "ClassName"`

### 7. 资源与性能

- 图片加载统一用 Coil，禁止直接操作 `BitmapFactory`
- 大列表（>50 项）必须使用 `LazyColumn` + `PagingData`
- 不稳定的 Composable 操作（如读取 DataStore）使用 `produceState` 或 `remember { mutableStateOf }` + 协程

---

## 关键约束（v1 范围）

- **不实现**：实时协作（WebSocket/ Yjs）、成员在线状态、富文本编辑器（v1 使用 Markdown）、Elasticsearch 全文搜索、空间加入审批流程、3D/GSAP 动画
- **任务状态跳转**：仅允许 `PENDING/OPEN → IN_PROGRESS → COMPLETED/CANCELLED`，UI 只显示合法下一状态按钮
- **文件上传**：`<50MB` 直接 multipart；`≥50MB` 分块上传（init → chunk → complete）
- **“项目”模块**：仅团队空间可见，个人空间不显示任何项目 UI
- **触摸目标**：所有可交互元素尺寸 ≥48dp（可用 `Modifier.minimumTouchTargetSize()`）

---

## 辅助资源

- 后端 API 完整定义：`swagger-api.json`
- Web 前端源码：`E:\Codex\Notask-Flow\frontend`
- 设计令牌与 HTML 参考：`android-html-ui/`
- 数据库 Schema：通过 Room 实体类定义在 `core` 模块的 `data/room/entity/`

---

本文件是 AI 助手在仓库中进行代码生成、审查和问题回答的最终依据。如有任何冲突，以本文件为准。
## 冲突解决优先级

当本文件与项目内其他文档（如 `README.md`、`DESIGN.md`、`android-ui-ux-design.md`、官方 Android 开发文档等）存在不一致时，**以本文件为准**。本文件是 AI 助手在仓库中进行代码生成、审查和问题回答的最终依据。

具体优先级从高到低：
1. 本文件（`CLAUDE.md`）
2. 项目根目录下的 `swagger-api.json`（API 契约）
3. `android-html-ui/` 目录中的 HTML 参考实现
4. 各模块内的 `README.md` 或注释
5. 通用最佳实践（如 Kotlin 官方指南、Material Design 规范）—— 仅当本文件未明确约定时参考

若需对本文件进行修改，请通过 Pull Request 提交变更，经团队评审后更新。
