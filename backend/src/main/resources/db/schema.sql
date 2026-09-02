CREATE DATABASE IF NOT EXISTS `notask_flow`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `notask_flow`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `nt_event_fail_log`;
DROP TABLE IF EXISTS `nt_admin_operation_log`;
DROP TABLE IF EXISTS `nt_login_log`;
DROP TABLE IF EXISTS `nt_business_attachment`;
DROP TABLE IF EXISTS `nt_attachment`;
DROP TABLE IF EXISTS `nt_user_device_token`;
DROP TABLE IF EXISTS `nt_notification_setting`;
DROP TABLE IF EXISTS `nt_system_setting`;
DROP TABLE IF EXISTS `nt_notification`;
DROP TABLE IF EXISTS `nt_todo`;
DROP TABLE IF EXISTS `nt_comment_mention`;
DROP TABLE IF EXISTS `nt_task_comment`;
DROP TABLE IF EXISTS `nt_task_member`;
DROP TABLE IF EXISTS `nt_task`;
DROP TABLE IF EXISTS `nt_project_member`;
DROP TABLE IF EXISTS `nt_project`;
DROP TABLE IF EXISTS `nt_note_tag`;
DROP TABLE IF EXISTS `nt_tag`;
DROP TABLE IF EXISTS `nt_note_history`;
DROP TABLE IF EXISTS `nt_note`;
DROP TABLE IF EXISTS `nt_notebook`;
DROP TABLE IF EXISTS `nt_space_join_request`;
DROP TABLE IF EXISTS `nt_space_member`;
DROP TABLE IF EXISTS `nt_space`;
DROP TABLE IF EXISTS `nt_user`;
DROP TABLE IF EXISTS `nt_role_permission`;
DROP TABLE IF EXISTS `nt_role`;
DROP TABLE IF EXISTS `nt_permission`;

CREATE TABLE `nt_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限标识',
  `code` varchar(50) NOT NULL COMMENT '权限编码',
  `name` varchar(50) NOT NULL COMMENT '权限名称',
  `description` varchar(200) DEFAULT NULL COMMENT '权限描述',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code_deleted` (`code`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

CREATE TABLE `nt_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色标识',
  `code` varchar(30) NOT NULL COMMENT '角色编码',
  `name` varchar(30) NOT NULL COMMENT '角色名称',
  `description` varchar(200) DEFAULT NULL COMMENT '角色描述',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code_deleted` (`code`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

CREATE TABLE `nt_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色权限关系标识',
  `role_id` bigint NOT NULL COMMENT '角色标识',
  `permission_id` bigint NOT NULL COMMENT '权限标识',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission_deleted` (`role_id`, `permission_id`, `is_deleted`),
  KEY `idx_role_permission_permission` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

CREATE TABLE `nt_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户标识',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `password` varchar(255) NOT NULL COMMENT '密码哈希',
  `email` varchar(100) NOT NULL COMMENT '邮箱',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像存储路径',
  `status` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '账号状态',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username_deleted` (`username`, `is_deleted`),
  UNIQUE KEY `uk_user_email_deleted` (`email`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE `nt_space` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '空间标识',
  `name` varchar(100) NOT NULL COMMENT '空间名称',
  `type` varchar(20) NOT NULL COMMENT '空间类型，PERSONAL 或 TEAM',
  `owner_user_id` bigint NOT NULL COMMENT '空间所有者用户标识',
  `join_approval_required` tinyint(1) NOT NULL DEFAULT 1 COMMENT '加入空间是否需要审核',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_space_owner` (`owner_user_id`),
  KEY `idx_space_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间表';

CREATE TABLE `nt_space_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '空间成员关系标识',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `user_id` bigint NOT NULL COMMENT '用户标识',
  `role_id` bigint NOT NULL COMMENT '角色标识',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_joined` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_space_member_deleted` (`space_id`, `user_id`, `is_deleted`),
  KEY `idx_space_member_user` (`user_id`),
  KEY `idx_space_member_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间成员表';

CREATE TABLE `nt_space_join_request` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '团队加入申请标识',
  `applicant_user_id` bigint NOT NULL COMMENT '申请用户标识',
  `supervisor_user_id` bigint NOT NULL COMMENT '审批上级用户标识',
  `target_space_id` bigint DEFAULT NULL COMMENT '审批通过后的目标空间标识',
  `team_name` varchar(100) DEFAULT NULL COMMENT '申请加入的团队名称描述',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '申请状态',
  `remark` varchar(500) DEFAULT NULL COMMENT '申请说明',
  `reject_reason` varchar(500) DEFAULT NULL COMMENT '拒绝原因',
  `reviewer_user_id` bigint DEFAULT NULL COMMENT '审批用户标识',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审批时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_space_join_applicant_status` (`applicant_user_id`, `status`, `gmt_create`),
  KEY `idx_space_join_supervisor_status` (`supervisor_user_id`, `status`, `gmt_create`),
  KEY `idx_space_join_target_space` (`target_space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='团队加入申请表';

CREATE TABLE `nt_notebook` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '笔记本标识',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父级笔记本标识',
  `path` varchar(500) NOT NULL DEFAULT '' COMMENT '层级路径',
  `name` varchar(100) NOT NULL COMMENT '笔记本名称',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_notebook_space_parent` (`space_id`, `parent_id`),
  KEY `idx_notebook_path` (`path`(100))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记本表';

CREATE TABLE `nt_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目标识',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `name` varchar(100) NOT NULL COMMENT '项目名称',
  `description` varchar(1000) DEFAULT NULL COMMENT '项目描述',
  `cover_color` varchar(20) NOT NULL DEFAULT '#6366f1' COMMENT '封面颜色',
  `cover_image_url` varchar(255) DEFAULT NULL COMMENT '封面图片地址',
  `is_archived` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否归档',
  `owner_user_id` bigint NOT NULL COMMENT '项目负责人用户标识',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_project_space_archived` (`space_id`, `is_archived`, `is_deleted`),
  KEY `idx_project_owner` (`owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目表';

CREATE TABLE `nt_project_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '项目成员关系标识',
  `project_id` bigint NOT NULL COMMENT '项目标识',
  `user_id` bigint NOT NULL COMMENT '用户标识',
  `role` varchar(20) NOT NULL COMMENT '项目成员角色',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_member_deleted` (`project_id`, `user_id`, `is_deleted`),
  KEY `idx_project_member_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='项目成员表';

CREATE TABLE `nt_note` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '笔记标识',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `notebook_id` bigint NOT NULL COMMENT '笔记本标识',
  `project_id` bigint DEFAULT NULL COMMENT '所属项目标识',
  `user_id` bigint NOT NULL COMMENT '创建者用户标识',
  `title` varchar(200) NOT NULL COMMENT '笔记标题',
  `content` longtext COMMENT 'Markdown 内容',
  `content_html` longtext COMMENT 'HTML 内容',
  `is_public` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否公开分享',
  `share_code` varchar(32) DEFAULT NULL COMMENT '分享码',
  `share_expire` datetime DEFAULT NULL COMMENT '分享过期时间',
  `view_count` int NOT NULL DEFAULT 0 COMMENT '阅读数量',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_share_code` (`share_code`),
  KEY `idx_note_space_notebook` (`space_id`, `notebook_id`),
  KEY `idx_note_space_project` (`space_id`, `project_id`),
  KEY `idx_note_public_share` (`is_public`, `share_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记表';

CREATE TABLE `nt_note_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '笔记历史标识',
  `note_id` bigint NOT NULL COMMENT '笔记标识',
  `title` varchar(200) NOT NULL COMMENT '历史标题',
  `content` longtext COMMENT '历史内容',
  `version` int NOT NULL COMMENT '版本号',
  `change_summary` varchar(255) DEFAULT NULL COMMENT '变更摘要',
  `save_type` varchar(20) NOT NULL DEFAULT 'AUTO' COMMENT '保存类型',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_history_version` (`note_id`, `version`),
  KEY `idx_note_history_note` (`note_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记版本表';

CREATE TABLE `nt_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签标识',
  `name` varchar(50) NOT NULL COMMENT '标签名称',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tag_space_name_deleted` (`space_id`, `name`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

CREATE TABLE `nt_note_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '笔记标签关系标识',
  `note_id` bigint NOT NULL COMMENT '笔记标识',
  `tag_id` bigint NOT NULL COMMENT '标签标识',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_note_tag_deleted` (`note_id`, `tag_id`, `is_deleted`),
  KEY `idx_note_tag_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='笔记标签关联表';

CREATE TABLE `nt_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务标识',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `project_id` bigint DEFAULT NULL COMMENT '所属项目标识',
  `title` varchar(200) NOT NULL COMMENT '任务标题',
  `description` text COMMENT '任务描述',
  `creator_id` bigint NOT NULL COMMENT '创建者用户标识',
  `mode` varchar(20) NOT NULL DEFAULT 'ASSIGNED' COMMENT '任务模式，ASSIGNED 或 OPEN',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
  `priority` varchar(10) NOT NULL DEFAULT 'MEDIUM' COMMENT '任务优先级',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_space_status` (`space_id`, `status`),
  KEY `idx_task_space_project_status` (`space_id`, `project_id`, `status`),
  KEY `idx_task_creator` (`creator_id`),
  KEY `idx_task_deadline` (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务主表';

CREATE TABLE `nt_task_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务成员标识',
  `task_id` bigint NOT NULL COMMENT '任务标识',
  `user_id` bigint NOT NULL COMMENT '责任人用户标识',
  `responsibility` varchar(500) NOT NULL COMMENT '职责说明',
  `assignment_type` varchar(20) NOT NULL DEFAULT 'ASSIGNED' COMMENT '分配类型',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '成员状态',
  `is_required` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否必需完成',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `completion_remark` text DEFAULT NULL COMMENT '完成说明',
  `version` int NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_member_user_status` (`user_id`, `status`),
  KEY `idx_task_member_status_required` (`task_id`, `status`, `is_required`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务成员表';

CREATE TABLE `nt_task_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务评论标识',
  `task_id` bigint NOT NULL COMMENT '任务标识',
  `user_id` bigint NOT NULL COMMENT '评论用户标识',
  `parent_comment_id` bigint DEFAULT NULL COMMENT '父级评论标识',
  `content` text NOT NULL COMMENT '评论内容',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_comment_task` (`task_id`),
  KEY `idx_task_comment_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务评论表';

CREATE TABLE `nt_comment_mention` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论提及标识',
  `comment_id` bigint NOT NULL COMMENT '评论标识',
  `user_id` bigint NOT NULL COMMENT '被提及用户标识',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_comment_mention_comment` (`comment_id`),
  KEY `idx_comment_mention_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论提及表';

CREATE TABLE `nt_todo` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '待办标识',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `user_id` bigint NOT NULL COMMENT '用户标识',
  `task_member_id` bigint DEFAULT NULL COMMENT '关联任务成员标识',
  `title` varchar(200) NOT NULL COMMENT '待办标题',
  `is_completed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否完成',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_todo_user_completed_deleted` (`user_id`, `is_completed`, `is_deleted`),
  KEY `idx_todo_task_member` (`task_member_id`),
  KEY `idx_todo_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='待办表';

CREATE TABLE `nt_notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知标识',
  `user_id` bigint NOT NULL COMMENT '接收用户标识',
  `space_id` bigint DEFAULT NULL COMMENT '空间标识',
  `type` varchar(30) NOT NULL COMMENT '通知类型',
  `business_type` varchar(20) NOT NULL DEFAULT 'TASK' COMMENT '业务类型',
  `business_id` bigint DEFAULT NULL COMMENT '业务标识',
  `title` varchar(200) NOT NULL COMMENT '通知标题',
  `content` varchar(500) DEFAULT NULL COMMENT '通知内容',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_notification_user_read_create` (`user_id`, `is_read`, `gmt_create`),
  KEY `idx_notification_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

CREATE TABLE `nt_notification_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知偏好标识',
  `user_id` bigint NOT NULL COMMENT '用户标识',
  `theme_mode` varchar(16) NOT NULL DEFAULT 'light' COMMENT '主题模式',
  `personal_theme_preset` varchar(24) NOT NULL DEFAULT 'sunrise' COMMENT '个人主题预设',
  `sidebar_mode` varchar(16) NOT NULL DEFAULT 'expanded' COMMENT '侧边栏模式',
  `task_notice_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用任务站内通知',
  `note_notice_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用笔记站内通知',
  `mention_notice_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用提及站内通知',
  `system_notice_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用系统站内通知',
  `email_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用邮件通知',
  `task_email_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用任务邮件通知',
  `todo_email_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用待办邮件通知',
  `mention_email_enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用提及邮件通知',
  `quiet_enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否启用免打扰',
  `quiet_start_time` varchar(5) NOT NULL DEFAULT '22:00' COMMENT '免打扰开始时间',
  `quiet_end_time` varchar(5) NOT NULL DEFAULT '08:00' COMMENT '免打扰结束时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_setting_user` (`user_id`),
  KEY `idx_notification_setting_user_deleted` (`user_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知偏好表';

CREATE TABLE `nt_user_device_token` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备推送令牌标识',
  `user_id` bigint NOT NULL COMMENT '用户标识',
  `platform` varchar(20) NOT NULL COMMENT '推送平台',
  `provider` varchar(20) NOT NULL DEFAULT 'FCM' COMMENT '推送服务提供商',
  `device_id` varchar(100) NOT NULL COMMENT '设备标识',
  `device_name` varchar(120) DEFAULT NULL COMMENT '设备名称',
  `device_token` varchar(1024) NOT NULL COMMENT '设备推送令牌',
  `app_version` varchar(40) DEFAULT NULL COMMENT '应用版本',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `last_active_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近活跃时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_device_token_user_platform` (`user_id`, `platform`, `enabled`, `is_deleted`),
  KEY `idx_user_device_token_device` (`user_id`, `platform`, `device_id`, `is_deleted`),
  KEY `idx_user_device_token_token` (`platform`, `device_token`(191), `enabled`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户移动端推送设备令牌表';

CREATE TABLE `nt_system_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '系统设置标识',
  `setting_key` varchar(80) NOT NULL COMMENT '设置键',
  `setting_value` varchar(200) NOT NULL COMMENT '设置值',
  `description` varchar(255) DEFAULT NULL COMMENT '设置说明',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_setting_key_deleted` (`setting_key`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统设置表';

CREATE TABLE `nt_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '附件标识',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `storage_path` varchar(500) NOT NULL COMMENT '对象存储路径',
  `bucket_name` varchar(100) NOT NULL COMMENT '存储桶名称',
  `file_size` bigint NOT NULL COMMENT '文件大小',
  `mime_type` varchar(100) DEFAULT NULL COMMENT '媒体类型',
  `uploader_id` bigint NOT NULL COMMENT '上传用户标识',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_attachment_uploader` (`uploader_id`),
  KEY `idx_attachment_space` (`space_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='附件表';

CREATE TABLE `nt_business_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '业务附件关系标识',
  `attachment_id` bigint NOT NULL COMMENT '附件标识',
  `business_type` varchar(20) NOT NULL COMMENT '业务类型',
  `business_id` bigint NOT NULL COMMENT '业务标识',
  `reference_key` varchar(100) NOT NULL DEFAULT '' COMMENT '引用标识',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_attachment_reference` (`attachment_id`, `business_type`, `business_id`, `reference_key`, `is_deleted`),
  KEY `idx_business_attachment_business` (`business_type`, `business_id`),
  KEY `idx_business_attachment_attachment` (`attachment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='业务附件关联表';

CREATE TABLE `nt_file_folder` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件夹标识',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父文件夹标识，0表示根目录',
  `name` varchar(80) NOT NULL COMMENT '文件夹名称',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序值',
  `created_by` bigint NOT NULL COMMENT '创建用户标识',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_folder_name_deleted` (`space_id`, `parent_id`, `name`, `is_deleted`),
  KEY `idx_file_folder_space_parent` (`space_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件夹表';

CREATE TABLE `nt_file_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件管理条目标识',
  `attachment_id` bigint NOT NULL COMMENT '附件标识',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `folder_id` bigint NOT NULL DEFAULT 0 COMMENT '文件夹标识，0表示根目录',
  `display_name` varchar(255) NOT NULL COMMENT '展示文件名',
  `created_by` bigint NOT NULL COMMENT '创建用户标识',
  `trashed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否在回收站',
  `deleted_at` datetime DEFAULT NULL COMMENT '移入回收站时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除用户标识',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_item_attachment_deleted` (`attachment_id`, `is_deleted`),
  KEY `idx_file_item_space_folder` (`space_id`, `folder_id`, `trashed`),
  KEY `idx_file_item_space_deleted` (`space_id`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件管理条目表';

CREATE TABLE `nt_file_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件操作日志标识',
  `file_id` bigint DEFAULT NULL COMMENT '文件标识',
  `space_id` bigint NOT NULL COMMENT '空间标识',
  `operator_id` bigint NOT NULL COMMENT '操作用户标识',
  `operation_type` varchar(40) NOT NULL COMMENT '操作类型',
  `detail` varchar(500) DEFAULT NULL COMMENT '操作详情',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_file_operation_space_file` (`space_id`, `file_id`),
  KEY `idx_file_operation_operator` (`operator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件操作日志表';

CREATE TABLE `nt_event_fail_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '事件失败日志标识',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型',
  `event_data` json NOT NULL COMMENT '事件数据',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `retry_count` int NOT NULL DEFAULT 0 COMMENT '重试次数',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_fail_status_create` (`status`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MQ发送失败补偿表';

CREATE TABLE `nt_login_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '登录日志标识',
  `user_id` bigint DEFAULT NULL COMMENT '用户标识',
  `account` varchar(100) NOT NULL COMMENT '登录账号',
  `client_type` varchar(30) DEFAULT NULL COMMENT '客户端类型',
  `device_id` varchar(100) DEFAULT NULL COMMENT '设备标识',
  `ip_address` varchar(80) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT 'User-Agent',
  `success` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否成功',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_log_user_create` (`user_id`, `gmt_create`),
  KEY `idx_login_log_account_create` (`account`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

CREATE TABLE `nt_admin_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理操作日志标识',
  `operator` varchar(100) NOT NULL COMMENT '操作人',
  `method` varchar(10) NOT NULL COMMENT '请求方法',
  `path` varchar(255) NOT NULL COMMENT '请求路径',
  `operation_name` varchar(255) DEFAULT NULL COMMENT '操作名称',
  `ip_address` varchar(80) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT 'User-Agent',
  `success` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否成功',
  `error_message` varchar(500) DEFAULT NULL COMMENT '错误信息',
  `is_deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
  `gmt_create` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_admin_operation_operator_create` (`operator`, `gmt_create`),
  KEY `idx_admin_operation_path_create` (`path`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理操作日志表';

SET FOREIGN_KEY_CHECKS = 1;
