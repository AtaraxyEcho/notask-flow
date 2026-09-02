下面这版是我整理后的“最终可实施设计方案 + 执行流程”。核心原则是：**管理平台和普通用户体系彻底隔离，V1 只支持一个 Administrator，不做管理员角色体系；公告复用通知系统，不单独做公告模块。**

**一、整体架构**
```text
普通用户端
/login /app/**
使用 userToken
clientType = WEB / ANDROID / IOS

管理平台
/admin/login /admin/**
使用 adminToken
clientType = ADMIN_WEB

后端
/api/v1/auth/**
/api/v1/spaces/**
/api/v1/admin/**

Redis
统一会话中心，支持多端互斥、踢下线、会话查询

MySQL
用户、系统配置、通知、登录日志、管理操作日志、审计数据
```

**二、账号与登录边界**
V1 只允许一个管理账号：

```text
username = Administrator
```

建议配置：

```yaml
notask-flow:
  admin:
    username: ${ADMIN_USERNAME:Administrator}
    password: ${ADMIN_PASSWORD:change-me}
```

普通登录接口必须拒绝 `Administrator`：

```text
POST /api/v1/auth/login
```

管理登录接口只允许 `Administrator`：

```text
POST /api/v1/admin/auth/login
```

普通用户不能访问 `/api/v1/admin/**`，Administrator 不能访问 `/api/v1/spaces/**` 和普通 `/app/**`。

**三、多端会话模型**
登录请求统一增加：

```json
{
  "username": "test",
  "password": "123456",
  "clientType": "WEB",
  "deviceId": "browser-uuid",
  "deviceName": "Chrome on Windows",
  "appVersion": "2.1.0"
}
```

`clientType`：

```text
WEB
ADMIN_WEB
ANDROID
IOS
```

Redis Key：

```text
notask:session:{sessionId}
notask:user:sessions:{userId}
notask:user:client-session:{userId}:{clientType}:{deviceId}
```

互斥规则：

| 客户端        | 规则                                |
| ------------- | ----------------------------------- |
| WEB           | 同账号只允许一个 Web 会话           |
| ADMIN_WEB     | Administrator 只允许一个管理端会话  |
| ANDROID       | 同账号同设备只保留一个 Android 会话 |
| IOS           | 同账号同设备只保留一个 iOS 会话     |
| WEB + MOBILE  | 可共存                              |
| ANDROID + IOS | 可共存                              |

JWT Payload：

```json
{
  "userId": 1,
  "sessionId": "uuid",
  "clientType": "WEB"
}
```

每次请求校验：

```text
JWT合法
Redis session存在
用户未禁用
clientType 与接口边界匹配
```

**四、Token 有效期**
建议：

| 类型      | Access Token | Refresh Token |
| --------- | -----------: | ------------: |
| WEB       |       4 小时 |       V1 不发 |
| ADMIN_WEB |       2 小时 |          不发 |
| ANDROID   |       1 小时 |         30 天 |
| IOS       |       1 小时 |         30 天 |

V1 可以先只实现 Access Token，但请求/响应结构预留 refreshToken 字段。

**五、管理平台前端路由**
新增独立管理端路由组：

```text
/admin/login
/admin
/admin/dashboard
/admin/users
/admin/sessions
/admin/logs
/admin/settings
/admin/monitor
/admin/storage
/admin/system-notifications
```

重定向规则：

```text
/admin -> /admin/dashboard
未登录访问 /admin/** -> /admin/login?redirect=xxx
普通用户访问 /admin/** -> /admin/login
Administrator 访问 /app/** -> /admin/dashboard
```

前端结构：

```text
src/layouts/AdminLayout.vue
src/views/admin/AdminLoginView.vue
src/views/admin/AdminDashboardView.vue
src/views/admin/AdminUserView.vue
src/views/admin/AdminSessionView.vue
src/views/admin/AdminLogView.vue
src/views/admin/AdminSettingView.vue
src/views/admin/AdminMonitorView.vue
src/views/admin/AdminStorageView.vue
src/views/admin/AdminSystemNotificationView.vue
src/stores/admin.ts
src/api/adminHttp.ts
```

普通用户 `userStore` 和管理端 `adminStore` 必须分开，普通 API 和管理 API 也建议分开 axios 实例。

**六、管理平台功能范围**
首页大盘：

| 模块     | 内容                               |
| -------- | ---------------------------------- |
| 用户统计 | 总用户、今日新增、近 7 日活跃      |
| 内容统计 | 笔记数、任务数、待办数、团队空间数 |
| 存储统计 | 总文件数、总占用、今日新增存储     |
| 趋势图   | 近 30 天用户、内容、存储趋势       |

用户管理：

```text
用户列表
搜索昵称/用户名/邮箱
筛选状态/邮箱验证/注册时间
启用用户
禁用用户并清理全部会话
重置密码
逻辑删除用户
```

会话管理：

```text
查看全部活跃会话
按用户、IP、clientType 搜索
踢出指定 session
踢出某个用户全部 session
```

系统配置：

```text
是否开放注册
注册是否需要邮箱验证
是否启用邮件发送
是否允许公开分享笔记
分享默认过期时间
笔记历史最大版本数
协作 ticket 过期秒数
新团队默认是否需要审核加入
Web Token 有效期
Mobile Token 有效期
移动端 Refresh Token 有效期
附件上传限制
```

日志审计：

```text
登录日志
管理操作日志
关键业务异常摘要
CSV 导出
```

性能监控：

```text
Spring Boot Actuator
JVM 内存
系统 CPU
磁盘
Hikari 连接池
HTTP 请求耗时
Redis/RabbitMQ/MinIO 健康状态
```

存储管理：

```text
总存储占用
用户存储 Top 10
团队空间存储 Top 10
孤立文件扫描
孤立文件清理
不提供文件预览和下载
```

系统通知：

```text
发送全员系统通知
复用 nt_notification
type = SYSTEM_ANNOUNCEMENT
支持 WebSocket 实时推送
后续 Android 复用 FCM
```

**七、后端接口设计**
管理认证：

```text
POST /api/v1/admin/auth/login
POST /api/v1/admin/auth/logout
GET  /api/v1/admin/auth/me
```

用户管理：

```text
GET    /api/v1/admin/users
GET    /api/v1/admin/users/{userId}
PATCH  /api/v1/admin/users/{userId}/enable
PATCH  /api/v1/admin/users/{userId}/disable
POST   /api/v1/admin/users/{userId}/reset-password
DELETE /api/v1/admin/users/{userId}
```

会话管理：

```text
GET    /api/v1/admin/sessions
DELETE /api/v1/admin/sessions/{sessionId}
DELETE /api/v1/admin/users/{userId}/sessions
```

系统配置：

```text
GET /api/v1/admin/settings
PUT /api/v1/admin/settings
```

日志：

```text
GET /api/v1/admin/logs/login
GET /api/v1/admin/logs/operations
GET /api/v1/admin/logs/system
GET /api/v1/admin/logs/export
```

监控：

```text
GET /api/v1/admin/monitor/health
GET /api/v1/admin/monitor/jvm
GET /api/v1/admin/monitor/system
GET /api/v1/admin/monitor/http
GET /api/v1/admin/monitor/datasource
```

存储：

```text
GET  /api/v1/admin/storage/summary
GET  /api/v1/admin/storage/top-users
GET  /api/v1/admin/storage/top-spaces
POST /api/v1/admin/storage/orphan-files/scan
GET  /api/v1/admin/storage/orphan-files
POST /api/v1/admin/storage/orphan-files/clean
```

系统通知：

```text
POST /api/v1/admin/system-notifications
GET  /api/v1/admin/system-notifications/history
```

**八、数据库表**
新增或补充：

```text
nt_login_log
nt_admin_operation_log
```

`nt_login_log`：

```text
id
user_id
username
client_type
device_id
ip
user_agent
success
failure_reason
gmt_create
```

`nt_admin_operation_log`：

```text
id
admin_username
operation_type
business_type
business_id
operation_desc
request_ip
user_agent
gmt_create
```

公告不单独建表，直接复用：

```text
nt_notification
```

新增通知类型：

```text
SYSTEM_ANNOUNCEMENT
```

**九、后端执行流程**
1. 新增 `ClientType` 枚举和登录请求字段。
2. 新增 `SessionService`，负责创建、互斥、校验、踢出、刷新活跃时间。
3. 调整登录逻辑，登录成功后创建 Redis session，JWT 携带 `sessionId`。
4. 调整认证拦截，每次请求校验 Redis session。
5. 新增 `AdminAuthController`，只允许 Administrator + ADMIN_WEB。
6. 普通登录接口拒绝 Administrator。
7. 新增 `/api/v1/admin/**` 拦截规则，只允许 ADMIN_WEB。
8. 新增用户管理、会话管理、系统配置、日志、监控、存储、系统通知接口。
9. 所有管理端写操作记录 `nt_admin_operation_log`。
10. 用户禁用、删除、重置密码、踢会话全部进行二次确认和审计。

**十、前端执行流程**
1. 新增 `adminStore`，独立保存 `adminToken`。
2. 新增 `adminHttp.ts`，只请求 `/api/v1/admin/**`。
3. 新增 `AdminLoginView`。
4. 新增 `AdminLayout`。
5. 新增 `/admin/**` 路由组和守卫。
6. 普通端路由守卫拒绝 ADMIN_WEB。
7. 管理端路由守卫拒绝普通 userToken。
8. 实现 dashboard、users、sessions、settings 四个核心页面。
9. 再补 logs、monitor、storage、system-notifications 页面。
10. 管理端所有危险操作统一弹二次确认。

**十一、推荐实施阶段**
第一阶段：多端会话重构

```text
clientType/deviceId
Redis session
JWT sessionId
Web 单端互斥
AdminWeb 单端互斥
会话校验
踢下线
```

第二阶段：管理端基础框架

```text
/admin/login
AdminLayout
adminStore
adminHttp
路由隔离
Administrator 登录/退出/me
```

第三阶段：核心管理功能

```text
用户管理
会话管理
系统配置
管理操作日志
登录日志
```

第四阶段：运维能力

```text
Dashboard
Actuator 监控
存储统计
孤立文件扫描/清理
```

第五阶段：通知与移动端准备

```text
系统通知
WebSocket 推送
Android/iOS clientType
Refresh Token 预留或实现
FCM token 预留
```

**最终建议**
先做第一阶段和第二阶段。它们是管理平台、Android 移动端、多端登录控制的共同基础。等这两部分稳定后，再逐步补用户管理、会话管理、系统配置和监控模块，会更稳，也不容易把普通用户端搞乱。