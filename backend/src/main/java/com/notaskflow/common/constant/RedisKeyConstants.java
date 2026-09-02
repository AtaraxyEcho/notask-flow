package com.notaskflow.common.constant;

/**
 * Redis 键名常量，统一管理缓存、锁、限流和临时状态键。
 *
 * @author LIN
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() {
    }

    public static final String REGISTER_EMAIL_CODE_PREFIX = "notask:auth:register-email-code:";

    public static final String PASSWORD_RESET_CODE_PREFIX = "notask:auth:password-reset-code:";

    public static final String PASSWORD_RESET_TOKEN_PREFIX = "notask:auth:password-reset-token:";

    public static final String PASSWORD_RESET_EMAIL_TOKEN_PREFIX = "notask:auth:password-reset-email-token:";

    public static final String USER_EMAIL_CHANGE_CODE_PREFIX = "notask:user:email-change-code:";

    public static final String COLLAB_TICKET_PREFIX = "notask:collab:ticket:";

    public static final String SPACE_EVENT_TICKET_PREFIX = "notask:space-event:ticket:";

    public static final String NOTE_VIEW_COUNT_PREFIX = "notask:note:view-count:";

    public static final String NOTE_HISTORY_LOCK_PREFIX = "notask:lock:note-history:";

    public static final String RATE_LIMIT_PREFIX = "notask:rate-limit:";

    public static final String MQ_CONSUMED_EVENT_PREFIX = "notask:mq:consumed:";

    public static final String SPACE_MEMBER_ONLINE_CLIENTS_PREFIX = "notask:space:member:online-clients:";

    public static final String SPACE_FILE_STATS_PREFIX = "notask:space:file-stats:";

    public static final String SPACE_FILE_UPLOAD_CONFIG_PREFIX = "notask:space:file-upload-config:";

    public static final String FILE_UPLOAD_SESSION_PREFIX = "notask:file:upload-session:";

    public static final String SYSTEM_SETTING_PREFIX = "notask:system:setting:";

    public static final String SPACE_PERMISSION_PREFIX = "notask:space:permission:";

    public static final String LOGIN_SESSION_PREFIX = "notask:session:";

    public static final String TOKEN_SESSION_PREFIX = "notask:token-session:";

    public static final String USER_SESSIONS_PREFIX = "notask:user:sessions:";

    public static final String USER_CLIENT_SESSION_PREFIX = "notask:user:client-session:";

    public static final String FILE_TRASH_CLEANUP_LOCK = "notask:lock:file-trash-cleanup";

    public static final String EVENT_FAIL_RETRY_LOCK = "notask:lock:event-fail-retry";

    public static final String LEGACY_SPACE_MEMBER_ONLINE_PREFIX = "notask:space:member:online:";

    /**
     * 构造协作 Ticket 键。
     *
     * @param ticket Ticket 值
     * @return Redis 键
     */
    public static String collabTicket(String ticket) {
        return COLLAB_TICKET_PREFIX + ticket;
    }

    /**
     * 构造空间实时事件 Ticket 键。
     *
     * @param ticket Ticket 值
     * @return Redis 键
     */
    public static String spaceEventTicket(String ticket) {
        return SPACE_EVENT_TICKET_PREFIX + ticket;
    }

    /**
     * 构造笔记浏览计数键。
     *
     * @param noteId 笔记标识
     * @return Redis 键
     */
    public static String noteViewCount(Long noteId) {
        return NOTE_VIEW_COUNT_PREFIX + noteId;
    }

    /**
     * 构造笔记浏览计数键匹配表达式。
     *
     * @return Redis 键匹配表达式
     */
    public static String noteViewCountPattern() {
        return NOTE_VIEW_COUNT_PREFIX + "*";
    }

    /**
     * 构造笔记历史版本锁键。
     *
     * @param noteId 笔记标识
     * @return Redis 键
     */
    public static String noteHistoryLock(Long noteId) {
        return NOTE_HISTORY_LOCK_PREFIX + noteId;
    }

    /**
     * 构造限流键。
     *
     * @param scene 限流场景
     * @param identity 限流身份
     * @return Redis 键
     */
    public static String rateLimit(String scene, String identity) {
        return RATE_LIMIT_PREFIX + scene + ":" + identity;
    }

    /**
     * 构造系统设置缓存键。
     *
     * @param settingKey 设置键
     * @return Redis 键
     */
    public static String systemSetting(String settingKey) {
        return SYSTEM_SETTING_PREFIX + settingKey;
    }

    /**
     * 构造登录会话键。
     *
     * @param sessionId 会话标识
     * @return Redis 键
     */
    public static String loginSession(String sessionId) {
        return LOGIN_SESSION_PREFIX + sessionId;
    }

    /**
     * 构造登录会话匹配表达式。
     *
     * @return Redis 键匹配表达式
     */
    public static String loginSessionPattern() {
        return LOGIN_SESSION_PREFIX + "*";
    }

    /**
     * 构造令牌会话映射键。
     *
     * @param tokenValue 令牌值
     * @return Redis 键
     */
    public static String tokenSession(String tokenValue) {
        return TOKEN_SESSION_PREFIX + tokenValue;
    }

    /**
     * 构造令牌会话映射匹配表达式。
     *
     * @return Redis 键匹配表达式
     */
    public static String tokenSessionPattern() {
        return TOKEN_SESSION_PREFIX + "*";
    }

    /**
     * 构造用户会话集合键。
     *
     * @param userId 用户标识
     * @return Redis 键
     */
    public static String userSessions(Long userId) {
        return USER_SESSIONS_PREFIX + userId;
    }

    /**
     * 构造用户客户端会话键。
     *
     * @param userId 用户标识
     * @param clientType 客户端类型
     * @param deviceId 设备标识
     * @return Redis 键
     */
    public static String userClientSession(Long userId, String clientType, String deviceId) {
        return USER_CLIENT_SESSION_PREFIX + userId + ":" + clientType + ":" + deviceId;
    }

    /**
     * 构造空间权限缓存键。
     *
     * @param spaceId 空间标识
     * @param userId 用户标识
     * @return Redis 键
     */
    public static String spacePermission(Long spaceId, Long userId) {
        return SPACE_PERMISSION_PREFIX + spaceId + ":" + userId;
    }

    /**
     * 构造 MQ 已消费事件键。
     *
     * @param eventId 事件标识
     * @return Redis 键
     */
    public static String mqConsumedEvent(String eventId) {
        return MQ_CONSUMED_EVENT_PREFIX + eventId;
    }

    /**
     * 构造空间文件统计缓存键。
     *
     * @param spaceId 空间标识
     * @return Redis 键
     */
    public static String spaceFileStats(Long spaceId) {
        return SPACE_FILE_STATS_PREFIX + spaceId;
    }

    /**
     * 构造空间文件上传配置缓存键。
     *
     * @param spaceId 空间标识
     * @return Redis 键
     */
    public static String spaceFileUploadConfig(Long spaceId) {
        return SPACE_FILE_UPLOAD_CONFIG_PREFIX + spaceId;
    }

    /**
     * 构造文件直传会话键。
     *
     * @param token 上传会话令牌
     * @return Redis 键
     */
    public static String fileUploadSession(String token) {
        return FILE_UPLOAD_SESSION_PREFIX + token;
    }
}
