# Notask Flow Android 原生移动端设计方案

> 版本：V2.0 优化版  
> 技术方向：Kotlin + Jetpack Compose + Material 3 + Clean Architecture  
> 目标：基于当前 `backend/`、`frontend/`、`android/` 与 `android-html-ui/`，设计一套可落地、可分阶段交付的 Android 原生客户端方案。

---

## 1. 背景与定位

Notask Flow 是个人知识管理与团队任务协作平台，后端为 Java Spring Boot，前端为 Vue 3。Android 端应成为 Notask Flow 的“随身工作台”，重点覆盖移动端高频场景，而不是在 V1 复制 Web 端的全部复杂功能。

### 1.1 Android 端核心目标

- 个人空间：快速记录笔记、查看最近内容、维护待办、处理个人任务。
- 团队空间：查看项目、推进任务、接收通知、浏览团队文件。
- 移动增强：通知提醒、离线草稿、文件预览、快速完成任务。
- 体验一致：保留 Web 端“个人空间温暖 / 团队空间专业”的空间感知设计。

### 1.2 V1 不追求的能力

- 不做实时协作编辑，移动端先使用异步保存与刷新。
- 不做完整管理员后台，复杂空间权限与系统配置仍以 Web 为主。
- 不在首版实现完整离线优先，先保证在线主流程稳定。
- 不在首版实现完整 WYSIWYG 富文本编辑，先使用 Markdown 编辑与预览。

---

## 2. 核心优化决策

本方案在保留原有 5 模块架构和技术栈的基础上，进一步收紧 V1 范围，避免把 Web 端复杂能力一次性搬到移动端。Android 首版以“可登录、可切换空间、可处理个人笔记/待办、可推进团队任务”为主，后续再补齐推送、完整离线和复杂文件能力。

| 决策点 | 风险或背景 | 优化方案 |
| --- | --- | --- |
| Token 刷新 | Swagger 已提供 `/api/v1/auth/refresh`，但仍需处理刷新失败 | 使用 OkHttp `Authenticator` 或 Repository 统一刷新；失败后清 token 并跳转登录 |
| 离线能力 | 全量离线优先会显著放大首版复杂度 | V1 先做在线主流程 + 笔记草稿/待办轻量离线 |
| 推送通知 | 后端已提供 `/api/v1/user/device-tokens` 注册/解绑契约，仍需 Firebase 配置 | 先完成应用内通知列表与 Android API 适配；配置 `google-services.json` 后接 FCM 获取 token |
| 文件预览 | Office、HTML、安全策略复杂 | V1 优先图片、PDF、文本；Office 依赖后端 HTML 预览 |
| 领域层纯度 | `domain` 若依赖 Android 会降低可测性 | `domain` 保持纯 Kotlin，不依赖 Retrofit、Room、Compose |
| 依赖暴露 | `api` 滥用会导致上层依赖泄漏 | 默认使用 `implementation`，仅暴露必要类型 |
| 国际化 | 当前工程尚无完整 i18n 基础 | V1 统一中文文案，后续再系统化 i18n |
| UI 落地 | 功能清单无法直接指导开发 | 页面级描述必须包含组件、状态、交互、技术实现 |

---

## 3. 技术栈

### 3.1 基础技术栈

| 关注点 | 选型 | 说明 |
| --- | --- | --- |
| 开发语言 | Kotlin 2.1+ | 当前工程使用 Kotlin 2.3.21，实际以可构建版本为准 |
| UI 框架 | Jetpack Compose + Material 3 | 全量原生声明式 UI |
| 架构 | Clean Architecture + MVVM/MVI 简化版 | `UiState` + `UiEvent` + `UiEffect` |
| 依赖注入 | Hilt | Application、ViewModel、Repository、Worker 注入 |
| 网络 | Retrofit 2 + OkHttp 4 + Moshi | 对接后端 REST API 和 `ApiResponse<T>` |
| 本地存储 | Room + DataStore Preferences | Room 做缓存/草稿，DataStore 存 Token 与偏好 |
| 分页 | Paging 3 Compose | 对接后端 `pageNum` / `pageSize` |
| 图片 | Coil 3 Compose | 头像、封面、文件图片预览 |
| 后台任务 | WorkManager | 上传、同步、定时任务 |
| 图表 | Vico Compose | 统计页图表 |
| 通知 | Firebase Cloud Messaging | 后端接口补齐后接入 |
| 测试 | JUnit 5 + MockK + Turbine + Compose UI Test | 单元、状态流、UI 关键路径 |

### 3.2 UI 技术栈

| UI 能力 | 技术栈 | 用途 |
| --- | --- | --- |
| 页面与组件 | Jetpack Compose | 全部页面、列表、表单、弹层 |
| 设计系统 | Material 3 | 颜色、排版、形状、按钮、卡片、导航 |
| 动态主题 | `MaterialTheme` + `animateColorAsState` | 空间切换、个人主题、暗色模式 |
| 导航 | Navigation Compose | Auth/Main/Detail/Public 多 NavGraph |
| 状态管理 | ViewModel + StateFlow + Lifecycle Compose | 页面状态与一次性事件 |
| 分页列表 | Paging 3 Compose | 笔记、任务、文件、通知 |
| 下拉刷新 | Material 3 PullToRefresh | 列表刷新 |
| 图片加载 | Coil `AsyncImage` | 封面、头像、图片文件 |
| PDF 预览 | Android `PdfRenderer` | PDF 分页渲染 |
| HTML 预览 | Android WebView | 后端 HTML 预览和公开笔记 |
| 文件选择 | Activity Result API | 上传附件、选择头像 |
| 文件下载 | DownloadManager / OkHttp Stream | 大文件下载与小文件流式保存 |
| 动画 | Compose Animation / SharedTransitionLayout | 空间切换、列表项、详情过渡 |
| 自适应 | WindowSizeClass | 手机、平板、横屏、折叠屏 |
| 无障碍 | Compose Semantics | TalkBack、触摸目标、内容描述 |

---

## 4. 模块结构

当前 Android 工程已采用 5 模块结构，继续保留：

```text
android/
├── app/       # 应用入口、MainActivity、Application、根 NavHost
├── core/      # UI 设计系统、网络基础、数据库、DataStore、通用工具
├── domain/    # 纯领域模型、Repository 契约、UseCase
├── data/      # API、DTO、Mapper、Repository 实现、同步逻辑
└── feature/   # Compose 页面、ViewModel、导航图，按功能分包
```

### 4.1 依赖关系

```text
app -> feature
app -> core

feature -> domain
feature -> core

data -> domain
data -> core

domain -> Kotlin 标准库 / 协程基础类型
```

原则：

- `domain` 尽量不依赖 Android，不依赖 Retrofit、Room、Compose。
- `feature` 不直接依赖 DTO，不直接调用 Retrofit。
- `data` 负责 DTO、Room Entity、Mapper、Repository 实现。
- `core` 可以放跨层基础设施，但业务模型逐步迁入 `domain.model`。

### 4.2 包结构建议

```text
core/
  ui/theme/
  ui/components/
  network/
  database/
  datastore/
  common/
  testing/

domain/
  model/
  repository/
  usecase/
  policy/

data/
  auth/
  space/
  note/
  task/
  todo/
  project/
  file/
  notification/
  stats/
  sync/
  di/

feature/
  auth/
  home/
  note/
  task/
  todo/
  project/
  file/
  notification/
  stats/
  settings/
  navigation/
```

---

## 5. 架构设计

### 5.1 单向数据流

每个 Screen 对应一个 ViewModel：

```kotlin
data class XxxUiState(
    val isLoading: Boolean = false,
    val items: List<Xxx> = emptyList(),
    val errorMessage: String? = null
)

sealed interface XxxUiEvent {
    data object Refresh : XxxUiEvent
    data class SelectItem(val id: Long) : XxxUiEvent
}

sealed interface XxxUiEffect {
    data class ShowSnackbar(val message: String) : XxxUiEffect
    data class Navigate(val route: String) : XxxUiEffect
}
```

规则：

- UI 只渲染 `UiState`，通过事件回调通知 ViewModel。
- ViewModel 不持有 Android `Context`，需要资源时由 UI 层处理。
- 一次性事件使用 `SharedFlow` 或 Channel，不塞进长期 `UiState`。

### 5.2 三层模型

```text
后端 JSON
  -> DTO（data 层，Moshi 注解）
  -> Domain Model（domain 层，业务语义稳定）
  -> UiState / UiModel（feature 层，页面展示友好）
```

要求：

- DTO 精确匹配后端字段。
- Domain Model 不暴露网络或数据库细节。
- UI Model 可以组合多个领域对象，例如任务详情页组合任务、成员、评论、附件。

### 5.3 API 响应处理

后端统一响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

Android 端不能只依赖 HTTP 状态码，必须解析业务 `code`：

```kotlin
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class BusinessError(val code: Int, val message: String) : ApiResult<Nothing>
    data class NetworkError(val throwable: Throwable) : ApiResult<Nothing>
    data object Unauthorized : ApiResult<Nothing>
}
```

### 5.4 认证流程

```text
登录
  -> POST /api/v1/auth/login
  -> 保存 tokenValue 到 DataStore
  -> OkHttp AuthInterceptor 注入 Authorization: Bearer <token>
  -> 业务请求
  -> 401 或业务未登录码
  -> POST /api/v1/auth/refresh
  -> 刷新成功：保存新 token 并重试原请求
  -> 刷新失败：清理本地登录态并跳转登录页
```

注意：

- Swagger 已提供 `/api/v1/auth/refresh`，Android 端可以实现静默刷新。
- 刷新请求必须避免递归触发自身认证器。
- 多个请求同时 401 时要做刷新互斥，避免并发刷新覆盖 Token。
- Debug 才开启请求日志，Release 禁止输出 Token、密码、邮箱验证码等敏感信息。

### 5.5 空间上下文

Notask Flow 的核心是 Space。Android 端应维护全局空间状态：

```kotlin
data class AppUiState(
    val isLoggedIn: Boolean,
    val currentUser: UserProfile?,
    val currentSpace: Space?,
    val spaces: List<Space>,
    val themeConfig: ThemeConfig,
    val unreadCount: Int
)
```

空间切换流程：

```text
点击 SpaceSwitcher
  -> 选择空间
  -> 更新 currentSpace
  -> 更新 ThemeConfig.spaceType
  -> 清理详情页返回栈
  -> 刷新当前空间首页数据
```

### 5.6 权限与功能可见性

后端使用基于空间的 RBAC。Android 端不做最终权限判定，但必须根据后端返回的空间、角色、权限信息控制入口可见性，减少无效操作。

原则：

- 后端是最终权限来源，Android 端只做体验层预校验。
- 没有权限的操作不展示，或展示为禁用态并说明原因。
- 所有写操作仍需处理后端拒绝，统一显示业务错误。
- 切换空间后必须刷新权限缓存和当前页面操作项。

建议在 domain 层提供策略对象：

```kotlin
class SpacePermissionPolicy {
    fun canCreateTask(space: Space, permissions: Set<String>): Boolean
    fun canManageProject(space: Space, permissions: Set<String>): Boolean
    fun canUploadFile(space: Space, permissions: Set<String>): Boolean
}
```

权限策略的目标是让 UI 只关心“能否展示某按钮”，而不是在多个页面散落权限字符串判断。

---

## 6. UI 设计方案

### 6.1 设计原则

Android 端采用“空间感知设计”：

- 个人空间像数字日记本，强调温暖、私密、轻量。
- 团队空间像项目工作台，强调清晰、紧凑、协作效率。
- 同一个功能在不同空间可以有不同信息密度和视觉风格。
- 不照搬 Web 三栏布局，手机端优先保证单手可用和信息扫描效率。

### 6.2 个人空间与团队空间对照

| 设计维度 | 个人空间 | 团队空间 |
| --- | --- | --- |
| 主色 | 朝阳/森林/海洋/暗夜主题预设 | 固定品牌蓝 |
| 背景 | 柔和暖白或主题浅色 | 冷调浅色，可带轻微玻璃感 |
| 卡片 | 20-24dp 圆角，轻阴影 | 8-12dp 圆角，边框优先 |
| 内容密度 | 留白更足 | 信息更紧凑 |
| 首页重点 | 问候、最近笔记、今日待办 | 项目进度、任务状态、团队动态 |
| 底部导航 | 首页、笔记、待办、文件、统计 | 项目、任务、文档、文件、更多 |
| 标题风格 | 更柔和、更大 | 更清晰、更克制 |

### 6.3 全局导航

推荐导航结构：

```text
splash
auth
  login
  register
  forgotPassword
main
  personal
    home
    notes
    todos
    files
    stats
  team
    projects
    tasks
    documents
    files
    more
detail
  noteDetail/{noteId}
  noteEdit/{noteId?}
  taskDetail/{taskId}
  projectDetail/{projectId}
  filePreview/{fileId}
modal
  globalSearch
  notifications
  settings
public
  invite/{code}
  publicNote/{shareCode}
```

技术实现：

- Navigation Compose 定义多 NavGraph。
- BottomNav 项切换使用 `saveState=true`、`restoreState=true`。
- 详情页正常入栈，空间切换时清理详情页。
- FCM deep link 后续映射到业务详情页。

### 6.4 全局组件

| 组件 | 设计 | 技术实现 |
| --- | --- | --- |
| `NotaskScaffold` | 统一顶栏、底栏、Snackbar、Insets | Material 3 `Scaffold` |
| `SpaceSwitcher` | 顶栏胶囊按钮，底部弹层选择空间 | `ModalBottomSheet` + `LazyColumn` |
| `SpaceAwareTopAppBar` | 根据空间切换颜色、内容密度 | 自定义 `Row` 或 M3 TopAppBar |
| `SpaceAwareBottomNavBar` | 个人/团队不同导航项 | `NavigationBar` |
| `NotaskCard` | 根据空间切换圆角、阴影、边框 | `Card` + `BorderStroke` |
| `SearchEntry` | 全局搜索入口 | `Surface` + Icon + Text |
| `AvatarGroup` | 成员头像堆叠 | `Box` 叠放头像 |
| `StatusChip` | 任务/项目状态 | `AssistChip` |
| `PriorityChip` | 任务优先级 | 固定颜色 token |
| `EmptyState` | 空页面引导 | 图标/插画 + 文案 + 主按钮 |
| `ErrorState` | 错误重试 | 错误图标 + 重试按钮 |
| `LoadingSkeleton` | 骨架加载 | 自定义 shimmer modifier |

### 6.5 页面状态规范

所有列表和详情页必须统一处理以下状态，避免每个页面各写一套交互：

| 状态 | 展示 | 操作 |
| --- | --- | --- |
| 首次加载 | 骨架屏或居中进度 | 不展示空状态 |
| 下拉刷新 | 顶部刷新指示器 | 保留旧数据 |
| 空数据 | 空状态插画/图标 + 主操作 | 提供新建或返回入口 |
| 网络错误 | 错误说明 + 重试按钮 | 不清空已有缓存 |
| 业务错误 | Snackbar 或字段错误 | 展示后端 message |
| 未登录 | 清理登录态并跳登录 | 保留 redirect |
| 无权限 | 禁用操作或空状态说明 | 提供返回/切换空间 |

建议在 `core.ui.components` 中提供：

```kotlin
@Composable
fun NotaskStateContainer(
    isLoading: Boolean,
    isRefreshing: Boolean,
    errorMessage: String?,
    isEmpty: Boolean,
    onRetry: () -> Unit,
    content: @Composable () -> Unit
)
```

### 6.6 页面设计

#### 6.6.1 登录与注册

功能：

- 登录：邮箱/用户名 + 密码。
- 注册：昵称、用户名、邮箱验证码、密码、可选邀请码。
- 忘记密码：邮箱验证码 + 新密码。

UI：

- 顶部品牌区，底部表单区。
- 输入框显示字段级错误。
- 登录/注册按钮请求中显示 loading。
- 验证码按钮显示倒计时。

技术栈：

- `OutlinedTextField`
- `PasswordVisualTransformation`
- `SnackbarHost`
- `ViewModel + StateFlow`
- Hilt 注入 `LoginUseCase`
- DataStore 保存 Token

#### 6.6.2 个人首页

功能：

- 问候语。
- 最近笔记。
- 今日待办。
- 快捷入口：写笔记、新建待办。
- 本周观察或个人统计摘要。

UI：

- 手机单列 `LazyColumn`。
- 顶部可使用本地插图或轻量封面图。
- 卡片圆角 20-24dp，视觉温暖。
- 避免中英文混排，V1 文案统一中文。

技术栈：

- `LazyColumn`
- `ElevatedCard`
- `Coil AsyncImage` 或本地 drawable
- `PullToRefreshBox`
- `AnimatedContent`

#### 6.6.3 笔记

列表页：

- 搜索入口。
- 笔记本筛选。
- 标签筛选。
- 最近更新列表。
- FAB 新建笔记。
- 加载、空、错误、刷新状态。

编辑页：

- 标题输入。
- Markdown 编辑。
- 编辑/预览切换。
- 自动保存。
- 标签和笔记本选择。
- 版本历史入口。

技术栈：

- Paging 3 Compose
- `LazyColumn`
- `ModalNavigationDrawer`
- `FilterChip`
- `FloatingActionButton`
- `TextField`
- Markdown 渲染库或 Compose Markdown
- Room 草稿缓存
- `snapshotFlow` + `debounce` 自动保存

V1 策略：

- 不做 WYSIWYG。
- 不做实时协作。
- 支持离线草稿，网络恢复后提示用户同步。

#### 6.6.4 待办

功能：

- 全部/未完成/已完成筛选。
- 今日、逾期、本周筛选。
- 新增、编辑、完成、删除。
- 显示来源：手动创建 / 任务生成。

UI：

- 列表项包含圆形勾选、标题、截止时间、来源 Chip。
- 左滑完成，右滑删除。
- 新建使用底部弹层，减少跳转成本。

技术栈：

- `LazyColumn`
- `SwipeToDismissBox`
- `SegmentedButton`
- `DatePicker`
- Room 轻量缓存
- WorkManager 后续补充同步

#### 6.6.5 任务

个人空间：

- 以“我的任务”列表为主。
- 支持状态筛选、优先级筛选、截止时间排序。

团队空间：

- 默认看板视图。
- 手机用横向列切换。
- 平板同时展示多列。

任务详情：

- 标题、状态、优先级、截止时间、项目。
- 成员完成情况。
- 评论时间线。
- 附件列表。
- 合法状态操作按钮。

技术栈：

- `HorizontalPager` 或横向 `LazyRow`
- Paging 3
- `DatePickerDialog`
- `DropdownMenu`
- `AssistChip`
- `AvatarGroup`
- domain 层 `TaskActionPolicy`

状态策略：

```text
UI 不手写状态转换规则
  -> 调用 TaskActionPolicy.availableActions(task, currentUser)
  -> 渲染返回的按钮
  -> 提交动作给 UseCase
  -> 后端校验最终合法性
```

#### 6.6.6 项目

功能：

- 团队空间项目列表。
- 项目详情。
- 项目任务、文档、成员。
- 项目完成率与风险状态。

UI：

- 项目列表使用卡片网格。
- 卡片显示封面色块、名称、描述、完成率、成员头像。
- 项目详情使用顶部封面 + Tab。

技术栈：

- `LazyVerticalGrid`
- `TabRow`
- `LinearProgressIndicator`
- `AvatarGroup`
- Vico 图表
- `DropdownMenu`

#### 6.6.7 文件

功能：

- 文件夹路径浏览。
- 文件上传。
- 文件下载。
- 图片/PDF/文本/HTML 预览。
- 文件重命名、移动、删除。

UI：

- 手机端用面包屑 + 文件列表，不使用复杂树状多栏。
- 每行展示类型图标、文件名、大小、上传者、更新时间。
- 更多菜单承载低频操作。

技术栈：

- Activity Result `OpenDocument`
- OkHttp 上传进度
- WorkManager 大文件上传
- Coil 图片预览
- `PdfRenderer`
- WebView
- DownloadManager

安全要求：

- WebView 设置 `allowFileAccess=false`。
- 不加载未知来源脚本。
- 预览失败时转为外部应用打开。

#### 6.6.8 通知

功能：

- 全部/未读/任务/笔记/系统筛选。
- 标记已读。
- 点击跳转关联资源。
- 未读角标。

UI：

- 通知入口在 TopAppBar，不占底部导航。
- 未读通知使用轻量背景和圆点标识。

技术栈：

- `Badge`
- `LazyColumn`
- `TabRow`
- Navigation deep link
- `POST /api/v1/user/device-tokens`
- `DELETE /api/v1/user/device-tokens`
- FCM SDK 后续接入

#### 6.6.9 统计

功能：

- 个人统计：笔记数量、待办完成、任务趋势。
- 团队统计：任务完成趋势、成员负载、项目进度。

UI：

- 手机单列卡片。
- 平板两列网格。
- 图表不宜过密，优先可读性。

技术栈：

- Vico Compose
- `LazyVerticalGrid`
- `PullToRefreshBox`

#### 6.6.10 设置

功能：

- 个人资料。
- 账号安全。
- 主题与偏好。
- 通知设置。
- 我的团队。
- 关于与退出登录。

UI：

- `LazyColumn` 分组列表。
- 危险操作使用二次确认对话框。
- 主题设置只影响个人空间，团队空间固定品牌主题。

技术栈：

- `Switch`
- `SegmentedButton`
- DataStore Preferences
- `AlertDialog`

---

## 7. 主题设计

### 7.1 ThemeConfig

```kotlin
enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class PersonalPreset { SUNRISE, FOREST, OCEAN, MIDNIGHT }

data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val personalPreset: PersonalPreset = PersonalPreset.SUNRISE,
    val spaceType: SpaceType = SpaceType.PERSONAL
)
```

### 7.2 主题规则

- 个人空间支持朝阳、森林、海洋、暗夜预设。
- 团队空间固定品牌蓝，不跟随个人预设。
- Android 12+ 可选 Dynamic Color，但团队空间不受 Dynamic Color 影响。
- 空间切换时使用 `animateColorAsState` 平滑过渡。

### 7.3 设计 token

| Token | 个人空间 | 团队空间 |
| --- | --- | --- |
| `primary` | 当前个人主题主色 | 品牌蓝 |
| `surface` | 暖白/主题浅色 | 冷调浅色 |
| `cardShape` | 20-24dp | 8-12dp |
| `contentDensity` | 舒展 | 紧凑 |
| `bodyLineHeight` | 较松 | 标准 |

### 7.4 组件 token

| 组件 | 个人空间 | 团队空间 |
| --- | --- | --- |
| TopAppBar 高度 | 64dp | 56dp |
| BottomBar 高度 | 80dp，含系统导航 padding | 80dp，含系统导航 padding |
| 主卡片圆角 | 24dp | 12dp |
| 列表项圆角 | 16dp | 8dp |
| 弹窗圆角 | 28dp | 16dp |
| FAB | CircleShape | 16dp 圆角 |
| 卡片阴影 | 1-3dp | 0-1dp，优先边框 |
| 页面横向 padding | 16dp | 16dp |
| 平板内容最大宽 | 840dp | 1180dp |

### 7.5 字体与文案

- V1 文案统一中文，避免中英文混排。
- 业务名词统一：空间、项目、任务、待办、笔记、文件、通知。
- 个人空间文案可以更柔和，但不能使用过度拟人或装饰性表达影响效率。
- 团队空间文案保持直接，例如“创建任务”“标记完成”“邀请成员”。
- 代码、文件名、接口名、状态枚举使用 monospace 或原始英文枚举，不翻译枚举值本身。

---

## 8. 数据与离线策略

### 8.1 V1 数据策略

V1 不做全量离线优先，按优先级逐步实现：

| 阶段 | 范围 | 说明 |
| --- | --- | --- |
| P0 | 在线 Repository | 认证、空间、笔记、待办、任务、项目主流程 |
| P1 | Room 缓存 | 空间、笔记列表、任务列表、待办列表、通知 |
| P2 | 离线写入 | 笔记草稿、待办完成/新增、任务状态更新 |
| P3 | 完整同步队列 | 失败重试、冲突处理、后台同步 |

### 8.2 Room 表建议

| 表 | 用途 |
| --- | --- |
| `spaces` | 当前用户空间缓存 |
| `notes` | 笔记列表与草稿 |
| `notebooks` | 笔记本树 |
| `todos` | 待办缓存 |
| `tasks` | 任务列表缓存 |
| `projects` | 项目缓存 |
| `notifications` | 通知缓存 |
| `offline_queue` | 后续离线写队列 |

### 8.3 冲突策略

V1：

- 服务端为准。
- 本地草稿与服务端冲突时，提示用户选择保留本地或覆盖。

后续：

- 对笔记引入版本历史对比。
- 对任务状态更新使用后端乐观锁和错误提示。

---

## 9. 网络层设计

### 9.1 Retrofit 配置

要求：

- `baseUrl` 从构建配置或注入配置读取，不在 `data` 模块硬编码。
- Android Emulator 访问宿主机后端时使用 `http://10.0.2.2:8080/`；`127.0.0.1` 只代表模拟器自身。
- 真机调试时使用电脑局域网 IP，例如 `http://192.168.x.x:8080/`，并确保防火墙放行。
- Moshi 注册 `LocalDateTimeJsonAdapter`。
- Debug 使用 BASIC/BODY 日志时必须脱敏 Token。
- Release 禁用请求体日志。

### 9.2 拦截器

| 拦截器 | 作用 |
| --- | --- |
| `AuthInterceptor` | 注入 Bearer Token |
| `ErrorMappingInterceptor` 或 Repository 包装 | 统一处理业务 code |
| `RequestLoggingInterceptor` | Debug 日志 |

### 9.3 分页

后端分页参数：

```text
pageNum: 默认 1
pageSize: 默认 10，最大 100
```

Android：

- PagingSource 从 1 开始。
- `nextKey = pageNum + 1`。
- 当 `list.isEmpty()` 或已达到 `total` 时结束。

### 9.4 后端 API 到 Android 模块映射

以下接口来自 `swagger-api.json`，Android 端应按业务域拆分 API、Repository 和页面。

#### 9.4.1 认证与用户

| 能力 | 后端接口 | Android 归属 | 首版优先级 |
| --- | --- | --- | --- |
| 登录 | `POST /api/v1/auth/login` | `data.auth.AuthApi`、`AuthRepository`、`feature.auth.LoginRoute` | P0 |
| 登出 | `POST /api/v1/auth/logout` | `AuthRepository.logout()` | P0 |
| 刷新令牌 | `POST /api/v1/auth/refresh` | `TokenRefreshAuthenticator` 或 `AuthRepository.refresh()` | P0 |
| 注册 | `POST /api/v1/auth/register` | `feature.auth.RegisterRoute` | P1 |
| 注册验证码 | `POST /api/v1/auth/register/send-email-code` | `RegisterViewModel` | P1 |
| 忘记密码 | `POST /api/v1/auth/forgot-password` | `feature.auth.ForgotPasswordRoute` | P1 |
| 校验重置码 | `POST /api/v1/auth/verify-reset-code` | `ForgotPasswordViewModel` | P1 |
| 重置密码 | `POST /api/v1/auth/reset-password` | `ForgotPasswordViewModel` | P1 |
| 认证配置 | `GET /api/v1/auth/settings` | `AuthSettingsRepository` | P1 |
| 当前用户资料 | `GET /api/v1/user/profile` | `data.user.UserApi`、`feature.settings` | P0 |
| 更新资料 | `PUT /api/v1/user/profile` | `UserRepository.updateProfile()` | P1 |
| 修改密码 | `PUT /api/v1/user/password` | `feature.settings` | P1 |
| 上传头像 | `POST /api/v1/user/avatar` | `UserRepository.uploadAvatar()` | P2 |
| 搜索用户 | `GET /api/v1/user/search` | 成员选择器、任务指派 | P1 |
| 通知偏好 | `GET/PUT /api/v1/user/notification-settings` | 设置页 | P2 |

#### 9.4.2 空间与权限

| 能力 | 后端接口 | Android 归属 | 首版优先级 |
| --- | --- | --- | --- |
| 空间列表 | `GET /api/v1/spaces` | `SpaceRepository.getSpaces()`、`SpaceSwitcher` | P0 |
| 创建团队空间 | `POST /api/v1/spaces` | `feature.home` 或设置页 | P2 |
| 空间详情 | `GET /api/v1/spaces/{spaceId}` | `SpaceRepository.getSpace()` | P0 |
| 更新空间 | `PUT /api/v1/spaces/{spaceId}` | 团队设置 | P2 |
| 删除团队空间 | `DELETE /api/v1/spaces/{spaceId}` | 团队设置 | P3 |
| 当前空间权限 | `GET /api/v1/spaces/{spaceId}/permissions` | `SpacePermissionRepository`、权限策略 | P0 |
| 空间成员 | `GET /api/v1/spaces/{spaceId}/members` | 成员选择器、团队成员页 | P1 |
| 添加成员 | `POST /api/v1/spaces/{spaceId}/members` | 团队成员页 | P2 |
| 更新成员角色 | `PUT /api/v1/spaces/{spaceId}/members/{userId}` | 团队成员页 | P2 |
| 移除成员 | `DELETE /api/v1/spaces/{spaceId}/members/{userId}` | 团队成员页 | P2 |
| 退出团队 | `DELETE /api/v1/spaces/{spaceId}/members/me` | 设置页 | P2 |
| 创建邀请码 | `POST /api/v1/spaces/{spaceId}/invites` | 团队邀请 | P2 |
| 预览邀请码 | `GET /api/v1/spaces/invites/{code}` | 邀请落地页 | P2 |
| 使用邀请码加入 | `POST /api/v1/spaces/invites/{code}/join` | 邀请落地页 | P2 |
| 加入申请 | `POST /api/v1/team-applications` | 团队申请 | P3 |
| 待审批申请 | `GET /api/v1/team-applications/pending` | 团队管理 | P3 |
| 我的申请 | `GET /api/v1/team-applications/mine` | 设置页 | P3 |
| 审批/拒绝 | `POST /api/v1/team-applications/{requestId}/approve/reject` | 团队管理 | P3 |

#### 9.4.3 笔记、笔记本与标签

| 能力 | 后端接口 | Android 归属 | 首版优先级 |
| --- | --- | --- | --- |
| 笔记分页 | `GET /api/v1/spaces/{spaceId}/notes` | `NoteRepository.getNotes()`、`NoteListRoute` | P0 |
| 创建笔记 | `POST /api/v1/spaces/{spaceId}/notes` | `NoteEditRoute` | P0 |
| 笔记详情 | `GET /api/v1/spaces/{spaceId}/notes/{id}` | `NoteDetailRoute`、`NoteEditRoute` | P0 |
| 更新笔记 | `PUT /api/v1/spaces/{spaceId}/notes/{id}` | `NoteEditViewModel` | P0 |
| 删除笔记 | `DELETE /api/v1/spaces/{spaceId}/notes/{id}` | `NoteDetailRoute` | P1 |
| 搜索笔记 | `GET /api/v1/spaces/{spaceId}/notes/search` | 全局搜索、笔记搜索 | P1 |
| 分享笔记 | `POST /api/v1/spaces/{spaceId}/notes/{id}/share` | 分享弹层 | P2 |
| 公开笔记 | `GET /api/v1/public/notes/{shareCode}` | `PublicNoteRoute` | P2 |
| 笔记历史 | `GET /api/v1/spaces/{spaceId}/notes/{id}/history` | 版本历史 | P2 |
| 指定版本 | `GET /api/v1/spaces/{spaceId}/notes/{id}/history/{version}` | 版本预览 | P2 |
| 恢复版本 | `POST /api/v1/spaces/{spaceId}/notes/{id}/history/{version}/restore` | 版本历史 | P2 |
| 协作正文保存 | `PUT /api/v1/spaces/{spaceId}/notes/{id}/collab-content` | 非 V1，后续协作 | P3 |
| 协作 Ticket | `POST /api/v1/spaces/{spaceId}/notes/{id}/collab-ticket` | 非 V1，后续协作 | P3 |
| 协作检查点 | `POST /api/v1/spaces/{spaceId}/notes/{id}/checkpoints` | 非 V1，后续协作 | P3 |
| 笔记本树 | `GET /api/v1/spaces/{spaceId}/notebooks` | `NotebookRepository`、笔记筛选 | P0 |
| 创建笔记本 | `POST /api/v1/spaces/{spaceId}/notebooks` | 笔记本弹层 | P1 |
| 笔记本详情/更新/删除 | `GET/PUT/DELETE /api/v1/spaces/{spaceId}/notebooks/{id}` | 笔记本管理 | P2 |
| 标签列表 | `GET /api/v1/spaces/{spaceId}/tags` | 标签筛选 | P1 |
| 创建标签 | `POST /api/v1/spaces/{spaceId}/tags` | 标签管理 | P2 |
| 更新/删除标签 | `PUT/DELETE /api/v1/spaces/{spaceId}/tags/{id}` | 标签管理 | P2 |
| 绑定笔记标签 | `POST /api/v1/spaces/{spaceId}/notes/{id}/tags` | 笔记编辑 | P1 |
| 移除笔记标签 | `DELETE /api/v1/spaces/{spaceId}/notes/{id}/tags/{tagId}` | 笔记编辑 | P1 |

#### 9.4.4 待办、任务与评论

| 能力 | 后端接口 | Android 归属 | 首版优先级 |
| --- | --- | --- | --- |
| 待办分页 | `GET /api/v1/spaces/{spaceId}/todos` | `TodoRepository.getTodos()`、`TodoListRoute` | P0 |
| 创建待办 | `POST /api/v1/spaces/{spaceId}/todos` | 新建待办弹层 | P0 |
| 待办详情 | `GET /api/v1/spaces/{spaceId}/todos/{id}` | 待办编辑 | P1 |
| 更新待办 | `PUT /api/v1/spaces/{spaceId}/todos/{id}` | 待办编辑 | P0 |
| 删除待办 | `DELETE /api/v1/spaces/{spaceId}/todos/{id}` | 待办列表 | P0 |
| 完成待办 | `PUT /api/v1/spaces/{spaceId}/todos/{id}/complete` | 滑动/勾选完成 | P0 |
| 取消完成 | `PUT /api/v1/spaces/{spaceId}/todos/{id}/uncomplete` | 待办列表 | P1 |
| 任务分页 | `GET /api/v1/spaces/{spaceId}/tasks` | `TaskRepository.getTasks()`、任务列表/看板 | P1 |
| 创建任务 | `POST /api/v1/spaces/{spaceId}/tasks` | 新建任务 | P1 |
| 任务详情 | `GET /api/v1/spaces/{spaceId}/tasks/{id}` | 任务详情 | P1 |
| 更新任务 | `PUT /api/v1/spaces/{spaceId}/tasks/{id}` | 任务编辑 | P1 |
| 删除任务 | `DELETE /api/v1/spaces/{spaceId}/tasks/{id}` | 任务详情 | P2 |
| 修改任务状态 | `PATCH /api/v1/spaces/{spaceId}/tasks/{id}/status` | `TaskActionPolicy` 动作 | P1 |
| 开始职责 | `POST /api/v1/spaces/{spaceId}/tasks/{taskId}/members/{memberId}/start` | 成员任务动作 | P1 |
| 完成职责 | `POST /api/v1/spaces/{spaceId}/tasks/{taskId}/members/{memberId}/complete` | 成员任务动作 | P1 |
| 认领开放任务 | `POST /api/v1/spaces/{spaceId}/tasks/{taskId}/claim` | 任务详情 | P1 |
| 指派任务职责 | `POST /api/v1/spaces/{spaceId}/tasks/{taskId}/assign` | 任务编辑/详情 | P1 |
| 移除任务成员 | `DELETE /api/v1/spaces/{spaceId}/tasks/{taskId}/members/{memberId}` | 任务编辑 | P2 |
| 任务评论列表 | `GET /api/v1/spaces/{spaceId}/tasks/{id}/comments` | 任务详情评论区 | P1 |
| 添加任务评论 | `POST /api/v1/spaces/{spaceId}/tasks/{id}/comments` | 评论输入栏 | P1 |
| 删除评论 | `DELETE /api/v1/comments/{id}` | 评论菜单 | P2 |
| 任务附件 | `GET /api/v1/spaces/{spaceId}/tasks/{id}/attachments` | 任务详情附件区 | P2 |

#### 9.4.5 项目、文件、通知与统计

| 能力 | 后端接口 | Android 归属 | 首版优先级 |
| --- | --- | --- | --- |
| 项目分页 | `GET /api/v1/spaces/{spaceId}/projects` | `ProjectRepository`、项目列表 | P1 |
| 创建项目 | `POST /api/v1/spaces/{spaceId}/projects` | 新建项目 | P2 |
| 项目详情 | `GET /api/v1/spaces/{spaceId}/projects/{projectId}` | 项目详情 | P1 |
| 更新/删除项目 | `PUT/DELETE /api/v1/spaces/{spaceId}/projects/{projectId}` | 项目设置 | P2 |
| 项目归档 | `PUT /api/v1/spaces/{spaceId}/projects/{projectId}/archive` | 项目菜单 | P2 |
| 项目成员 | `GET/POST /api/v1/spaces/{spaceId}/projects/{projectId}/members` | 项目成员 Tab | P2 |
| 项目成员角色/移除 | `PUT/DELETE /api/v1/spaces/{spaceId}/projects/{projectId}/members/{userId}` | 项目成员 Tab | P2 |
| 项目关联任务 | `GET /api/v1/spaces/{spaceId}/projects/{projectId}/tasks` | 项目详情任务 Tab | P1 |
| 项目关联文档 | `GET /api/v1/spaces/{spaceId}/projects/{projectId}/notes` | 项目详情文档 Tab | P1 |
| 项目选项 | `GET /api/v1/spaces/{spaceId}/projects/options` | 任务创建项目选择器 | P1 |
| 文件分页 | `GET /api/v1/spaces/{spaceId}/files` | 文件列表 | P1 |
| 文件详情 | `GET /api/v1/spaces/{spaceId}/files/{fileId}` | 文件详情/预览 | P1 |
| 文件上传 | `POST /api/v1/spaces/{spaceId}/files/upload` | 小文件上传 | P1 |
| 预签名上传 | `POST /api/v1/spaces/{spaceId}/files/upload-url`、`POST /api/v1/spaces/{spaceId}/files/complete` | 大文件/直传 | P2 |
| 分片上传 | `POST /api/v1/spaces/{spaceId}/files/chunk-upload/init`、`POST /api/v1/spaces/{spaceId}/files/chunk-upload/{uploadToken}/chunks`、`POST /api/v1/spaces/{spaceId}/files/chunk-upload/complete` | 大文件上传 Worker | P2 |
| 文件夹树 | `GET /api/v1/spaces/{spaceId}/files/tree` | 文件面包屑/移动弹层 | P1 |
| 创建/更新/删除文件夹 | `POST /api/v1/spaces/{spaceId}/files/folders`、`PUT/DELETE /api/v1/spaces/{spaceId}/files/folders/{folderId}` | 文件夹管理 | P2 |
| 文件预览 | `GET /api/v1/spaces/{spaceId}/files/{fileId}/preview-url`、`GET /api/v1/spaces/{spaceId}/files/{fileId}/preview-text`、`GET /api/v1/spaces/{spaceId}/files/{fileId}/preview-html` | 文件预览页 | P1 |
| 下载地址 | `GET /api/v1/spaces/{spaceId}/files/{fileId}/download-url` | 下载动作 | P1 |
| 回收站恢复/物理删除 | `POST /api/v1/spaces/{spaceId}/files/{fileId}/restore`、`DELETE /api/v1/spaces/{spaceId}/files/{fileId}/physical` | 文件回收站 | P3 |
| 附件上传 | `POST /api/v1/spaces/{spaceId}/attachments` | 笔记/任务附件 | P2 |
| 附件绑定/解绑 | `POST /api/v1/spaces/{spaceId}/attachments/bind`、`DELETE /api/v1/spaces/{spaceId}/attachments/{id}/unbind` | 附件关联 | P2 |
| 附件下载 | `GET /api/v1/spaces/{spaceId}/attachments/{id}/download` | 附件下载 | P2 |
| 通知分页 | `GET /api/v1/notifications` | 通知列表 | P1 |
| 未读数量 | `GET /api/v1/notifications/unread-count` | TopAppBar Badge | P1 |
| 标记已读 | `PUT /api/v1/notifications/{id}/read` | 通知列表 | P1 |
| 批量已读 | `PUT /api/v1/notifications/read-all`、`PUT /api/v1/notifications/read` | 通知列表 | P1 |
| 删除通知 | `DELETE /api/v1/notifications/{id}`、`DELETE /api/v1/notifications/read` | 通知列表 | P2 |
| 个人统计 | `GET /api/v1/stats/personal` | 个人统计页/首页摘要 | P1 |
| 个人笔记趋势 | `GET /api/v1/stats/personal/note-trend` | 个人统计页 | P1 |
| 团队趋势 | `GET /api/v1/spaces/{spaceId}/stats/trend` | 团队统计 | P2 |
| 角色完成 | `GET /api/v1/spaces/{spaceId}/stats/role-completion` | 团队统计 | P2 |
| 成员负载 | `GET /api/v1/spaces/{spaceId}/stats/load` | 团队统计 | P2 |
| 近期动态 | `GET /api/v1/spaces/{spaceId}/stats/activities` | 团队首页/统计 | P2 |

命名规则：

- API 接口：`AuthApi`、`NoteApi`、`TaskApi`。
- DTO：`LoginResponseDto`、`NoteDto`、`TaskDto`。
- Mapper：`NoteDto.toDomain()`、`NoteEntity.toDomain()`。
- Repository 实现：`NoteRepositoryImpl`。
- UseCase：按用户动作命名，例如 `CreateNoteUseCase`、`CompleteTodoUseCase`。

### 9.5 错误码处理

Android 端错误显示分层：

| 错误类型 | 处理 |
| --- | --- |
| 未登录/Token 失效 | 清理登录态，跳转登录页 |
| 无权限 | 保留页面，显示无权限说明或禁用按钮 |
| 参数校验错误 | 表单字段下展示错误 |
| 业务状态错误 | Snackbar 展示后端 message |
| 网络不可用 | 保留缓存，提示离线或重试 |
| 服务端异常 | 统一错误页或 Snackbar，不暴露堆栈 |

禁止在 UI 层直接拼接异常字符串；错误文案由 Repository/UseCase 转换为稳定的 UI message。

---

## 10. 安全与权限

### 10.1 Token 安全

- Token 存入 DataStore，禁止写入日志、崩溃上报或普通文本文件。
- Debug 网络日志需要脱敏 `Authorization`。
- 登出时清理 Token、当前用户、当前空间和敏感缓存。
- 账号切换时清理上一个用户的 Room 私有数据或按 userId 分区。

### 10.2 WebView 安全

用于 HTML/Office 预览的 WebView 必须遵守：

- `settings.allowFileAccess = false`
- `settings.allowContentAccess = false`，除非明确需要本地内容 URI
- 禁止任意 JavaScript bridge
- 只加载后端可信预览 URL 或本地安全 HTML
- 离开页面时停止加载并释放 WebView

### 10.3 文件安全

- 文件选择使用 SAF 或系统 Photo Picker，不直接访问裸文件路径。
- 下载文件优先使用系统 DownloadManager 或应用私有目录。
- 分享文件使用 `FileProvider` 或系统分享 Intent，不暴露真实路径。
- 上传前校验大小、MIME 类型和后端允许的配置。

### 10.4 Android 权限

| 权限 | 使用场景 | 策略 |
| --- | --- | --- |
| `POST_NOTIFICATIONS` | Android 13+ 推送通知 | 首次需要通知时再请求 |
| 图片/媒体读取 | 头像、附件上传 | 优先 Photo Picker，减少权限请求 |
| 网络状态 | 同步队列、离线提示 | 使用 ConnectivityManager |
| 相机 | 后续头像拍照/附件拍照 | 非 V1 必需 |

---

## 11. 性能与体验约束

### 11.1 Compose 性能

- 列表必须使用 `LazyColumn` / `LazyVerticalGrid`，避免大列表直接 `Column`。
- 列表项使用稳定 key，例如 `key = item.id`。
- UI 状态类尽量使用不可变集合或稳定数据结构。
- 避免在 Composable 中直接执行格式化复杂计算，放到 ViewModel 或 mapper。
- 图片列表必须限制尺寸，Coil 设置合理 placeholder/error。

### 11.2 网络与缓存

- 列表页优先显示缓存，再刷新网络。
- 下拉刷新不清空旧数据。
- 搜索输入使用 debounce，避免每个字符请求接口。
- 文件上传显示进度，失败保留重试入口。

### 11.3 启动体验

启动流程：

```text
Splash
  -> 读取 Token
  -> 有 Token：请求用户资料与空间列表
  -> 成功：进入上次空间
  -> 失败：清理登录态进入登录页
  -> 无 Token：进入登录页
```

Splash 不展示超过必要时间；如果初始化超过 800ms，应显示轻量加载状态。

---

## 12. 功能优先级与实施计划

### 12.1 功能优先级

| 优先级 | 功能 | 说明 |
| --- | --- | --- |
| P0 | 登录、登出、Token、用户资料、空间列表 | 所有业务基础 |
| P0 | 个人首页、空间切换、全局主题 | App 体验骨架 |
| P0 | 笔记列表/详情/Markdown 编辑 | 个人核心 |
| P0 | 待办列表/新增/完成/删除 | 移动高频 |
| P1 | 团队项目列表/详情 | 团队入口 |
| P1 | 任务列表/看板/详情/状态流转 | 协作核心 |
| P1 | 通知列表/未读数/跳转 | 协作反馈 |
| P1 | 文件列表/图片/PDF/文本预览 | 实用能力 |
| P2 | FCM SDK 接入、分块上传、完整离线队列 | 移动增强 |
| P2 | 统计图表、成员管理、复杂设置 | 完整度增强 |

### 12.2 迭代计划

| 迭代 | 目标 | 范围 |
| --- | --- | --- |
| M1 | 可登录可切换空间 | 认证、Token、用户资料、空间列表、动态主题、导航壳 |
| M2 | 个人效率闭环 | 个人首页、笔记列表/编辑、待办列表/编辑、基础缓存 |
| M3 | 团队协作闭环 | 项目、任务、评论、通知、团队文件列表 |
| M4 | 移动增强 | FCM SDK、文件上传/预览、离线草稿、平板适配 |
| M5 | 质量与发布 | 单测、UI 测试、ProGuard、性能优化、Release 构建 |

### 12.3 文件级交付清单

M1 优先改造：

| 文件/目录 | 目标 |
| --- | --- |
| `android/app/src/main/java/com/notaskflow/app/MainActivity.kt` | 接入 App 级状态、动态主题、根导航 |
| `android/feature/src/main/java/com/notaskflow/feature/navigation/` | 新增集中路由定义和 NavGraph |
| `android/feature/src/main/java/com/notaskflow/feature/home/HomeViewModel.kt` | 承接空间、Tab、通知角标、首页数据 |
| `android/data/src/main/java/com/notaskflow/data/di/NetworkModule.kt` | 移除硬编码 baseUrl，统一错误处理 |
| `android/core/src/main/java/com/notaskflow/core/ui/components/` | 沉淀 Scaffold、状态组件、Chip、Avatar |
| `android/domain/src/main/java/com/notaskflow/domain/model/` | 迁入核心业务模型 |

M2 优先新增：

| 文件/目录 | 目标 |
| --- | --- |
| `feature/note/NoteListViewModel.kt` | 笔记列表状态、分页、刷新 |
| `feature/note/NoteEditViewModel.kt` | Markdown 编辑、自动保存、草稿 |
| `feature/todo/TodoListViewModel.kt` | 待办列表、筛选、完成动作 |
| `data/note/` | NoteApi、DTO、Mapper、Repository |
| `data/todo/` | TodoApi、DTO、Mapper、Repository |
| `core/database/` | notes、todos、notebooks 缓存表 |

M3 优先新增：

| 文件/目录 | 目标 |
| --- | --- |
| `feature/task/` | 任务列表、看板、详情 |
| `feature/project/` | 项目列表、详情 |
| `feature/notification/` | 通知列表和未读数 |
| `domain/policy/TaskActionPolicy.kt` | 任务合法动作生成 |
| `data/task/` | TaskApi、Repository、评论/成员数据 |
| `data/project/` | ProjectApi、Repository |

---

## 13. 测试与验收

### 13.1 单元测试

- UseCase：覆盖业务分支。
- Repository：MockWebServer 验证成功、业务错误、网络错误。
- ViewModel：Turbine 验证 `UiState` 与 `UiEffect`。
- Policy：任务状态动作生成必须单测覆盖。

### 13.2 UI 测试

每个核心页面至少覆盖：

- 加载态。
- 空状态。
- 错误态。
- 成功态。
- 刷新态。
- 表单校验。

### 13.3 UI 验收标准

- 所有可点击区域不小于 48dp。
- 文本不溢出、不重叠。
- 个人/团队空间切换后，颜色、导航、首页内容同步变化。
- TalkBack 能读出按钮用途，装饰图标不朗读。
- 暗色模式正文对比度满足 WCAG AA。
- 手机、平板、横屏布局可用。

### 13.4 构建验证

```powershell
cd android
.\gradlew.bat :app:assembleDebug
.\gradlew.bat test
```

如 AGP、Kotlin、KSP 版本组合无法解析或构建失败，应优先回退到稳定组合，而不是继续堆叠 workaround。

---

## 14. 当前工程需要优先修正的问题

1. `HomeRoute` 内的空间列表、用户姓名、统计数据仍是硬编码，应改为 ViewModel 驱动。
2. `NotaskFlowTheme` 目前没有接入动态 `ThemeConfig`，空间切换没有真正驱动全局主题。
3. `NetworkModule` 写死 `DEFAULT_BASE_URL`，而 app 模块已有 `BuildConfig.BASE_URL`，需要统一配置来源。
4. `android/README.md` 中的 `core/*`、`feature/*` 表述与当前 Gradle include 不一致，应改成 5 模块 + 包分层。
5. 当前 UI 文案中英文混排明显，V1 应统一中文。
6. `domain` 与 `core.model` 的边界需要收敛，业务模型逐步迁入 `domain.model`。
7. `feature` 模块可以暂时聚合所有页面，等页面数量明显增长后再拆分功能模块，不要过早拆分。

---

## 15. 结论

Android 端的正确路线是：先把认证、空间、主题、导航、笔记、待办、团队任务这些主流程做稳，再逐步扩展离线、推送、文件和统计。UI 上要继承 Web 端双空间设计语言，但交互必须为手机重新设计，优先减少跳转、降低输入成本、强化通知和快速处理能力。

这份方案可作为后续 Android 开发的主文档，具体实现时应以当前后端接口、Gradle 可构建版本和已有 Compose 组件为准持续迭代。
