# AGENTS.md

本文件为 Codex (Codex.ai/code) 在此仓库中编写代码时提供指导。

## 代码风格

**【强制】** 所有类必须在文件头部通过 `import` 导入，业务代码中禁止使用完全限定类名（FQCN）。违反此规则视为硬编码，降低可读性和可维护性，如有特殊情况（例如同一文件中需要同时使用两个不同包下的同名类），请先向我说明，经确认后再执行。

### 1. 导入语句规范

- **禁止使用通配符导入**（如 `import java.util.*`），必须列出具体使用的类。
- **静态导入**（`import static`）仅允许用于常量或枚举，且不得滥用（同一类中静态导入的方法不能超过3个）。

### 2. 注解使用

- `@Override`：必须显式标注，任何重写父类方法或实现接口方法均需添加。
- `@Transactional`：只用于Service层，禁止在Controller层使用。
- `@Autowired` / `@Resource`：优先使用构造器注入（`@RequiredArgsConstructor`），避免字段注入。
- 实体类除`@Data`注解外还需加入`@AllArgsConstructor`、`@NoArgsConstructor`

### 3. 异常处理

- **禁止** 捕获异常后不做任何处理（空catch块）。
- **禁止** 在finally块中使用`return`（会吞掉异常）。
- **禁止** 直接打印`e.printStackTrace()`，必须使用日志记录（如`log.error("xxx", e)`）。

### 4. 控制语句

- **switch**：每个case都要以`break`或`return`结束，default分支必须存在。
- **if/else**：超过3层嵌套必须重构（提取方法或使用早期返回）。
- **禁止** 使用`goto`（Java保留字但无用）。

### 5. 集合与泛型

- 创建集合时必须指定泛型类型（如 `List<String> list = new ArrayList<>();`），禁止原生类型。
- 遍历集合时优先使用增强for或`forEach`，避免手动使用下标。

### 6. 资源管理

- 所有实现了`AutoCloseable`的资源（流、连接等）必须使用`try-with-resources`，禁止手动`close`。

### 7. 注释规范

- 类、接口、枚举必须包含Javadoc，说明功能、作者。
- 公共方法必须包含Javadoc，描述参数、返回值、异常。
- **禁止** 使用尾行注释（`// comment` 应放在代码上方独立一行）。
- 注释描述要求客观，使用中文

### 8. 数据库操作（MyBatis-Plus）

- 查询条件必须使用`LambdaQueryWrapper` / `LambdaUpdateWrapper`，避免字符串硬编码字段名。
- **禁止** 在业务代码中直接写SQL字符串（除非使用注解`@Select`等且SQL简单）。
- 业务代码中禁止使用SQL拼接

---

## Android (Kotlin) 代码规范

本规范适用于 **Notask Flow Android 移动端** 的 Kotlin 代码编写。

### 1. 语言特性

- **强制** 使用 Kotlin 2.1+，禁止使用 Java 混编（除非调用第三方 Java 库）。
- **禁止** 使用 `!!` 强制解包，必须使用安全调用 `?.` 或 `let`、`?:` 等操作符处理空值。
- 数据类必须使用 `data class`，并确保属性使用 `val`/`var` 适当声明。
- 使用 `sealed class` 定义密封类（如 `UiState`、`UiEvent`、`UiEffect`），禁止使用枚举代替状态。

### 2. 协程

- 所有长时间运行的操作（网络、数据库、文件）必须在协程中执行。禁止使用 `Thread` 或 `AsyncTask`。
- ViewModel 中使用 `viewModelScope` 启动协程；Activity/Fragment 中使用 `lifecycleScope`。
- **禁止** 使用 `GlobalScope`，除非有明确理由并经过评审。
- 异常捕获必须在协程内部使用 `try/catch`，禁止依赖 `CoroutineExceptionHandler` 作为唯一处理方式。

### 3. 依赖注入

- 使用 **Hilt** 作为依赖注入框架，禁止手动 `new` 对象进行依赖传递。
- 每个 `Activity`、`Fragment`、`ViewModel`、`Service` 必须使用 `@AndroidEntryPoint`。
- 提供依赖的 Module 必须使用 `@Module` 和 `@InstallIn`，并明确作用域（如 `SingletonComponent::class`）。

### 4. 架构（MVVM / MVI）

- **强制** 遵循 **Clean Architecture** + **MVVM（MVI 简化版）**：
  - **Data Layer**：Repository 实现 + 数据源（API、本地数据库、缓存）。
  - **Domain Layer**：UseCase（可选，但复杂业务必须抽取）。
  - **Presentation Layer**：ViewModel + Compose UI。
- 每个 Screen（Composable）必须对应一个 ViewModel，ViewState 定义为 `UiState` 密封类。
- 业务逻辑放在 ViewModel 或 UseCase 中，UI 层只负责渲染和发送事件。

### 5. 命名与代码风格

- 类名：UpperCamelCase（如 `NoteRepositoryImpl`）。
- 函数/属性：lowerCamelCase（如 `loadNotes`、`isLoading`）。
- **常量**：`CONSTANT_CASE`（如 `BASE_URL`、`MAX_RETRY`）。
- 布局文件（如使用 XML）使用小写+下划线，但 Compose 中无需布局文件。
- 测试类名：以 `Test` 结尾，测试方法名使用描述性英文或反引号中文（如 `` `should return error when network fails` ``）。

### 6. Jetpack Compose

- **强制** 使用 Compose 构建 UI，禁止使用 XML 布局（除非保留旧代码）。
- Composable 函数命名使用 UpperCamelCase，并以 `@Composable` 注解。
- **状态提升**：无状态的 Composable 应通过参数接收状态和回调。
- **禁止** 在 Composable 内部直接调用 `viewModel()` 获取 ViewModel（应通过参数传递）。
- 副作用（`LaunchedEffect`、`DisposableEffect`、`SideEffect`）必须小心使用，避免无限循环。
- **重组优化**：使用 `derivedStateOf`、`remember`、`key` 避免不必要的重组。

### 7. 数据库（Room）

- 实体类使用 `@Entity`，表名和字段名使用 `snake_case`。
- **强制** 所有数据库操作（`@Query`、`@Insert`、`@Update`、`@Delete`）必须定义在 `@Dao` 接口中。
- 复杂查询必须使用 `@Query` 并编写 SQL，**禁止** 在业务代码中拼接 SQL。
- 使用 `Flow` 或 `LiveData` 作为返回值，避免阻塞主线程。

### 8. 网络与序列化

- 使用 **Retrofit + Moshi** 进行网络请求，禁止使用原生 `HttpURLConnection`。
- API 接口定义在 `data` 模块的 `api` 包下，返回类型为 `ApiResponse<T>` 或 `PageResponse<T>`。
- 所有 DTO 必须使用 `@JsonClass(generateAdapter = true)` 注解。
- **禁止** 在 DTO 中包含业务逻辑，仅用于数据传递。

### 9. 资源与主题

- 颜色、尺寸、字符串资源必须定义在 `res/values/` 下，**禁止** 硬编码在代码中。
- 支持 **亮色/暗色主题**，使用 Material 3 主题并适配系统设置。

### 10. 测试

- 单元测试必须覆盖 ViewModel、UseCase、Repository。
- 使用 **JUnit5 + MockK + Turbine** 进行异步测试。
- Compose UI 测试使用 `ComposeTestRule`，模拟用户交互。
- **强制** 测试命名清晰，遵循 `should_expectedBehavior_when_condition` 格式。

### 11. Android 特有规范

- **权限请求**：使用 `Accompanist` 或 `rememberLauncherForActivityResult`，**禁止** 同步请求权限。
- **文件访问**：使用 `MediaStore` 或 `SAF`，**禁止** 直接访问文件路径（Android 10+ 限制）。
- **WebView**：必须配置安全设置（`setAllowFileAccess(false)` 等），并清理缓存。
- **后台任务**：使用 `WorkManager`，**禁止** 使用 `Service` 或 `IntentService`（除非前台服务）。
- **推送通知**：使用 FCM，通知负载必须包含 `type` 和 `businessId` 用于深链接。

### 12. 构建配置

- 使用 **版本目录**（`libs.versions.toml`）管理依赖。
- 模块按功能拆分（如 `:feature-note`、`:core-network`），**禁止** 将所有代码放在 `:app` 模块。
- debug 和 release 构建类型必须配置 ProGuard/R8 规则，避免未混淆的敏感信息。

---

## 上下文管理规则

当 Codex 判断当前对话的上下文使用量即将达到限制（例如超过 80%），或收到用户的“继续”请求但可能因上下文过长而出错时：

1. **主动提醒用户**：告知当前上下文使用情况，并询问是否执行 `/compact` 压缩。
2. **获得确认后**：执行 `/compact` 命令压缩历史，然后继续执行未完成的任务。
3. **压缩后**：确认压缩成功，然后基于压缩后的摘要继续回答用户的问题。

## 项目概述

Notask Flow 是一个个人知识管理与团队任务协作平台。本仓库包含该项目的后端规范与需求文档。

## 源文件

所有源文件都必须编码为 UTF-8。

## 当前状态

这是一个**仅含规范的仓库**。实际的 Java Spring Boot 实现尚未开始。主要文档为 `backend.md`，其中包含完整的 V1.5 需求规格。

## 关键文档

- **backend.md** — 完整的后端需求规格（V1.5）
  - 技术栈：Java 21、Spring Boot 3.2.x、Sa-Token、MyBatis-Plus、MySQL、Redis、RabbitMQ、Elasticsearch、MinIO
  - 基于空间（Space）的 RBAC 权限体系架构
  - 完整的数据库 schema（DDL）
  - 符合 Swagger 3 规范的 RESTful API 规格
  - 事件驱动架构模式
  - 错误处理与自定义异常
  - 遵循阿里巴巴 Java 开发手册
- **android.md**

## 开发指南

在实现后端代码库时，请遵循规格中的以下原则：

### 架构

- **基于空间的 RBAC**：所有资源（笔记、任务、待办、附件）均归属于某个 `Space`。API 路径遵循 `/api/v1/spaces/{spaceId}/...`
- **分层结构**：Controller → Service → Mapper，各层关注点清晰分离
- **领域模型**：DO（数据对象）、DTO（数据传输对象）、VO（视图对象）、Query 对象
- **事件驱动**：使用 `ApplicationEventPublisher` 配合 `@TransactionalEventListener(phase = AFTER_COMMIT)` 进行异步处理

### 关键模式

- **Sa-Token JWT**：令牌样式为 `jwt-mixin`，使用 Redis 会话存储。Redis **必须**启用 AOF 持久化以支持登出黑名单。
- **权限系统**：实现 `StpInterface.getPermissionList()` 方法，该方法根据从 `RequestContext` 获取的当前 `spaceId`，查询 `nt_space_member` → `nt_role_permission` 表。
- **任务状态机**：采用枚举驱动的轻量级状态机，支持状态转换校验。当成员子任务状态变化时，任务的整体状态自动更新。
- **乐观锁**：在 `nt_task_member` 表中使用 `version` 字段实现并发控制。始终使用条件更新（`eq(expectedStatus)`）。
- **待办同步**：当任务成员完成子任务时，**在同一个事务中**更新关联的待办事项，以保证强一致性。
- **版本历史**：每个笔记自动保留最近 50 个版本，过旧版本由定时任务清理。
- **附件绑定**：通过 `reference_key` 字段支持同一附件的多处引用。

### 数据库规范

- 表前缀：`nt_`
- 字符集：`utf8mb4`
- 引擎：`InnoDB`
- 必须字段：`gmt_create`、`gmt_modified`、`is_deleted`（tinyint(1)，默认 0）
- 命名：小写 + 下划线，索引命名：`pk_`、`uk_`、`idx_`
- 逻辑删除 + 唯一约束：删除用户时，需修改其唯一字段（`username`、`email`）以避免违反唯一约束

### 错误处理

- 自定义异常层次：`BusinessException`、`IllegalTaskStateException`、`TaskMemberNotFoundException` 等
- 全局异常处理器，统一返回 `ApiResponse` 格式（HTTP 状态码始终为 200，业务状态码位于 `code` 字段）
- 错误码：1xxx（系统）、2xxx（任务）、3xxx（笔记）、4xxx（文件）

### API 规范

- 基础路径：`/api/v1`
- 认证：`Authorization: Bearer <jwt>`
- 分页参数：`pageNum`（默认 1）、`pageSize`（默认 10，最大 100）
- 响应格式：

  ```json
  {
    "code": 200,
    "message": "success",
    "data": { ... }
  }
  ```

### 测试要求

- 最低 80% 测试覆盖率
- 测试遵循 AAA（Arrange-Act-Assert）模式
- 使用具有描述性的测试名称

## 何时使用特定 Agent

- **planner**：复杂功能、重构、实现规划
- **tdd-guide**：新功能、缺陷修复（先写测试）
- **code-reviewer**：编写/修改代码后
- **security-reviewer**：提交前，当涉及认证/支付/用户数据变更时
- **build-error-resolver**：构建失败时
