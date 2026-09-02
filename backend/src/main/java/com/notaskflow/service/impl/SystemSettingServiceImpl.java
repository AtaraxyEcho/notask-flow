package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.constant.RedisKeyConstants;
import com.notaskflow.domain.dto.request.SystemSettingUpdateRequest;
import com.notaskflow.domain.entity.SystemSetting;
import com.notaskflow.domain.vo.AuthSystemSettingVO;
import com.notaskflow.domain.vo.SystemSettingVO;
import com.notaskflow.mapper.SystemSettingMapper;
import com.notaskflow.service.SystemSettingService;
import com.notaskflow.utils.RedisUtil;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统设置服务实现。
 *
 * @author LIN
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final String REGISTRATION_ENABLED_KEY = "auth.registration.enabled";

    private static final String REGISTER_EMAIL_VERIFICATION_REQUIRED_KEY =
            "auth.registration.email-verification-required";

    private static final String SINGLE_DEVICE_LOGIN_ONLY_KEY = "auth.login.single-device-only";

    private static final String MAIL_ENABLED_KEY = "mail.enabled";

    private static final String LOGIN_FAILURE_LIMIT_KEY = "auth.login.failure-limit";

    private static final String LOGIN_FAILURE_WINDOW_MINUTES_KEY = "auth.login.failure-window-minutes";

    private static final String WEB_ACCESS_TOKEN_SECONDS_KEY = "auth.token.web-access-seconds";

    private static final String ADMIN_ACCESS_TOKEN_SECONDS_KEY = "auth.token.admin-access-seconds";

    private static final String MOBILE_ACCESS_TOKEN_SECONDS_KEY = "auth.token.mobile-access-seconds";

    private static final String NOTE_SHARE_ENABLED_KEY = "note.share.enabled";

    private static final String NOTE_SHARE_DEFAULT_EXPIRE_MINUTES_KEY = "note.share.default-expire-minutes";

    private static final String NOTE_HISTORY_MAX_VERSIONS_KEY = "note.history.max-versions";

    private static final String COLLAB_TICKET_EXPIRE_SECONDS_KEY = "collab.ticket-expire-seconds";

    private static final String NEW_TEAM_JOIN_APPROVAL_REQUIRED_KEY = "space.join.default-require-approval";

    private static final Duration SETTING_CACHE_TTL = Duration.ofMinutes(5);

    private final SystemSettingMapper systemSettingMapper;

    private final RedisUtil redisUtil;

    /**
     * 判断是否开放新用户注册。
     *
     * @return 是否开放新用户注册
     */
    @Override
    public boolean isRegistrationEnabled() {
        return getBooleanValue(REGISTRATION_ENABLED_KEY, true);
    }

    /**
     * 判断注册是否必须验证邮箱。
     *
     * @return 注册是否必须验证邮箱
     */
    @Override
    public boolean isRegisterEmailVerificationRequired() {
        return getBooleanValue(REGISTER_EMAIL_VERIFICATION_REQUIRED_KEY, true);
    }

    /**
     * 判断是否仅允许同一账号单设备登录。
     *
     * @return 是否仅允许单设备登录
     */
    @Override
    public boolean isSingleDeviceLoginOnly() {
        return getBooleanValue(SINGLE_DEVICE_LOGIN_ONLY_KEY, true);
    }

    /**
     * 判断是否启用系统邮件发送。
     *
     * @return 是否启用邮件发送
     */
    @Override
    public boolean isMailEnabled() {
        return getBooleanValue(MAIL_ENABLED_KEY, true);
    }

    /**
     * 获取登录失败锁定阈值。
     *
     * @return 登录失败锁定阈值
     */
    @Override
    public int getLoginFailureLimit() {
        return getIntValue(LOGIN_FAILURE_LIMIT_KEY, 10, 1, 100);
    }

    /**
     * 获取登录失败锁定窗口。
     *
     * @return 登录失败锁定窗口
     */
    @Override
    public Duration getLoginFailureWindow() {
        return Duration.ofMinutes(getIntValue(LOGIN_FAILURE_WINDOW_MINUTES_KEY, 5, 1, 1440));
    }

    /**
     * 获取 Web 访问令牌有效秒数。
     *
     * @return Web 访问令牌有效秒数
     */
    @Override
    public int getWebAccessTokenSeconds() {
        return getIntValue(WEB_ACCESS_TOKEN_SECONDS_KEY, 14400, 600, 604800);
    }

    /**
     * 获取管理端访问令牌有效秒数。
     *
     * @return 管理端访问令牌有效秒数
     */
    @Override
    public int getAdminAccessTokenSeconds() {
        return getIntValue(ADMIN_ACCESS_TOKEN_SECONDS_KEY, 7200, 600, 86400);
    }

    /**
     * 获取移动端访问令牌有效秒数。
     *
     * @return 移动端访问令牌有效秒数
     */
    @Override
    public int getMobileAccessTokenSeconds() {
        return getIntValue(MOBILE_ACCESS_TOKEN_SECONDS_KEY, 3600, 600, 604800);
    }

    /**
     * 判断是否允许公开分享笔记。
     *
     * @return 是否允许公开分享笔记
     */
    @Override
    public boolean isNoteShareEnabled() {
        return getBooleanValue(NOTE_SHARE_ENABLED_KEY, true);
    }

    /**
     * 获取分享链接默认过期分钟数。
     *
     * @return 分享链接默认过期分钟数
     */
    @Override
    public int getNoteShareDefaultExpireMinutes() {
        return getIntValue(NOTE_SHARE_DEFAULT_EXPIRE_MINUTES_KEY, 10080, 0, 525600);
    }

    /**
     * 获取笔记历史最大版本数。
     *
     * @return 笔记历史最大版本数
     */
    @Override
    public int getNoteHistoryMaxVersions() {
        return getIntValue(NOTE_HISTORY_MAX_VERSIONS_KEY, 20, 1, 500);
    }

    /**
     * 获取协作 Ticket 过期秒数。
     *
     * @return 协作 Ticket 过期秒数
     */
    @Override
    public int getCollabTicketExpireSeconds() {
        return getIntValue(COLLAB_TICKET_EXPIRE_SECONDS_KEY, 60, 10, 3600);
    }

    /**
     * 判断新团队默认是否需要审核加入。
     *
     * @return 新团队默认是否需要审核加入
     */
    @Override
    public boolean isNewTeamJoinApprovalRequired() {
        return getBooleanValue(NEW_TEAM_JOIN_APPROVAL_REQUIRED_KEY, true);
    }

    /**
     * 获取认证相关系统设置。
     *
     * @return 认证系统设置
     */
    @Override
    public AuthSystemSettingVO getAuthSettings() {
        return new AuthSystemSettingVO(
                isRegistrationEnabled(),
                isRegisterEmailVerificationRequired(),
                isSingleDeviceLoginOnly()
        );
    }

    /**
     * 查询全部系统设置。
     *
     * @return 系统设置列表
     */
    @Override
    public List<SystemSettingVO> listSettings() {
        return systemSettingMapper.selectList(Wrappers.<SystemSetting>lambdaQuery()
                        .orderByAsc(SystemSetting::getSettingKey))
                .stream()
                .map(this::toVO)
                .toList();
    }

    /**
     * 更新系统设置。
     *
     * @param request 系统设置更新请求
     * @return 系统设置列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<SystemSettingVO> updateSettings(SystemSettingUpdateRequest request) {
        for (Map.Entry<String, String> entry : request.getSettings().entrySet()) {
            updateSetting(entry.getKey(), entry.getValue());
        }
        return listSettings();
    }

    private boolean getBooleanValue(String settingKey, boolean defaultValue) {
        String value = getSettingValue(settingKey);
        if (value == null) {
            return defaultValue;
        }
        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalizedValue) {
            case "true", "1", "yes", "on" -> true;
            case "false", "0", "no", "off" -> false;
            default -> defaultValue;
        };
    }

    private int getIntValue(String settingKey, int defaultValue, int minValue, int maxValue) {
        String value = getSettingValue(settingKey);
        if (value == null) {
            return defaultValue;
        }
        try {
            int parsedValue = Integer.parseInt(value.trim());
            return Math.max(minValue, Math.min(maxValue, parsedValue));
        } catch (NumberFormatException exception) {
            log.warn("系统设置整数值格式异常，settingKey={}, settingValue={}",
                    settingKey, value, exception);
            return defaultValue;
        }
    }

    private String getSettingValue(String settingKey) {
        String cacheKey = RedisKeyConstants.systemSetting(settingKey);
        String cachedValue = null;
        try {
            cachedValue = redisUtil.getString(cacheKey);
        } catch (RuntimeException exception) {
            log.warn("读取系统设置缓存失败，settingKey={}", settingKey, exception);
        }
        if (cachedValue != null) {
            return cachedValue;
        }
        SystemSetting setting = systemSettingMapper.selectOne(Wrappers.<SystemSetting>lambdaQuery()
                .eq(SystemSetting::getSettingKey, settingKey));
        if (setting == null || setting.getSettingValue() == null) {
            return null;
        }
        try {
            redisUtil.set(cacheKey, setting.getSettingValue(), SETTING_CACHE_TTL);
        } catch (RuntimeException exception) {
            log.warn("写入系统设置缓存失败，settingKey={}", settingKey, exception);
        }
        return setting.getSettingValue();
    }

    private void updateSetting(String settingKey, String settingValue) {
        SystemSetting setting = systemSettingMapper.selectOne(Wrappers.<SystemSetting>lambdaQuery()
                .eq(SystemSetting::getSettingKey, settingKey));
        if (setting == null) {
            setting = new SystemSetting();
            setting.setSettingKey(settingKey);
            setting.setSettingValue(settingValue);
            setting.setDescription("管理端新增配置");
            systemSettingMapper.insert(setting);
        } else {
            setting.setSettingValue(settingValue);
            systemSettingMapper.updateById(setting);
        }
        redisUtil.delete(RedisKeyConstants.systemSetting(settingKey));
    }

    private SystemSettingVO toVO(SystemSetting setting) {
        return new SystemSettingVO(setting.getSettingKey(), setting.getSettingValue(), setting.getDescription());
    }
}
