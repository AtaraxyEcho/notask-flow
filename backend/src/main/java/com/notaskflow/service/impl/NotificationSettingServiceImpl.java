package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.domain.dto.request.NotificationSettingUpdateRequest;
import com.notaskflow.domain.entity.NotificationSetting;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.vo.NotificationSettingVO;
import com.notaskflow.exception.BusinessException;
import com.notaskflow.exception.ErrorCode;
import com.notaskflow.mapper.NotificationSettingMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.service.MailNotificationService;
import com.notaskflow.service.NotificationSettingService;
import com.notaskflow.utils.LoginUserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 通知偏好服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSettingServiceImpl implements NotificationSettingService {

    private final NotificationSettingMapper notificationSettingMapper;

    private final UserMapper userMapper;

    private final MailNotificationService mailNotificationService;

    /**
     * 获取当前用户通知偏好。
     *
     * @return 通知偏好
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationSettingVO getCurrentSetting() {
        return toVO(findOrCreate(LoginUserUtil.currentUserId()));
    }

    /**
     * 更新当前用户通知偏好。
     *
     * @param request 通知偏好更新请求
     * @return 通知偏好
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationSettingVO updateCurrentSetting(NotificationSettingUpdateRequest request) {
        Long currentUserId = LoginUserUtil.currentUserId();
        NotificationSetting setting = findOrCreate(currentUserId);
        boolean emailWasDisabled = !Boolean.TRUE.equals(setting.getEmailEnabled());

        setting.setThemeMode(valueOrDefault(request.getThemeMode(), setting.getThemeMode()));
        setting.setPersonalThemePreset(valueOrDefault(request.getPersonalThemePreset(), setting.getPersonalThemePreset()));
        setting.setSidebarMode(valueOrDefault(request.getSidebarMode(), setting.getSidebarMode()));
        setting.setTaskNoticeEnabled(valueOrDefault(request.getTaskNoticeEnabled(), setting.getTaskNoticeEnabled()));
        setting.setNoteNoticeEnabled(valueOrDefault(request.getNoteNoticeEnabled(), setting.getNoteNoticeEnabled()));
        setting.setMentionNoticeEnabled(valueOrDefault(request.getMentionNoticeEnabled(), setting.getMentionNoticeEnabled()));
        setting.setSystemNoticeEnabled(valueOrDefault(request.getSystemNoticeEnabled(), setting.getSystemNoticeEnabled()));
        setting.setEmailEnabled(valueOrDefault(request.getEmailEnabled(), setting.getEmailEnabled()));
        setting.setTaskEmailEnabled(valueOrDefault(request.getTaskEmailEnabled(), setting.getTaskEmailEnabled()));
        setting.setTodoEmailEnabled(valueOrDefault(request.getTodoEmailEnabled(), setting.getTodoEmailEnabled()));
        setting.setMentionEmailEnabled(valueOrDefault(request.getMentionEmailEnabled(), setting.getMentionEmailEnabled()));
        setting.setQuietEnabled(valueOrDefault(request.getQuietEnabled(), setting.getQuietEnabled()));
        setting.setQuietStartTime(valueOrDefault(request.getQuietStartTime(), setting.getQuietStartTime()));
        setting.setQuietEndTime(valueOrDefault(request.getQuietEndTime(), setting.getQuietEndTime()));
        notificationSettingMapper.updateById(setting);

        if (emailWasDisabled && Boolean.TRUE.equals(setting.getEmailEnabled())) {
            sendEnableMailOrThrow(currentUserId);
        }

        log.info("通知偏好已更新，userId={}", currentUserId);
        return toVO(setting);
    }

    /**
     * 查询或创建用户通知偏好。
     *
     * @param userId 用户标识
     * @return 通知偏好实体
     */
    private NotificationSetting findOrCreate(Long userId) {
        NotificationSetting setting = notificationSettingMapper.selectOne(Wrappers.<NotificationSetting>lambdaQuery()
                .eq(NotificationSetting::getUserId, userId));
        if (setting != null) {
            return setting;
        }
        NotificationSetting created = defaultSetting(userId);
        notificationSettingMapper.insert(created);
        return created;
    }

    /**
     * 发送启用邮件通知确认邮件。
     *
     * @param userId 用户标识
     */
    private void sendEnableMailOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || !StringUtils.hasText(user.getEmail())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前账户未配置有效邮箱");
        }
        String displayName = StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
        boolean mailSent = mailNotificationService.sendNotificationEnabledMail(user.getEmail(), displayName);
        if (!mailSent) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "邮件服务不可用，请检查 SMTP 配置");
        }
    }

    /**
     * 创建默认通知偏好。
     *
     * @param userId 用户标识
     * @return 默认通知偏好
     */
    private NotificationSetting defaultSetting(Long userId) {
        NotificationSetting setting = new NotificationSetting();
        setting.setUserId(userId);
        setting.setThemeMode("light");
        setting.setPersonalThemePreset("sunrise");
        setting.setSidebarMode("expanded");
        setting.setTaskNoticeEnabled(true);
        setting.setNoteNoticeEnabled(true);
        setting.setMentionNoticeEnabled(true);
        setting.setSystemNoticeEnabled(true);
        setting.setEmailEnabled(false);
        setting.setTaskEmailEnabled(true);
        setting.setTodoEmailEnabled(true);
        setting.setMentionEmailEnabled(true);
        setting.setQuietEnabled(false);
        setting.setQuietStartTime("22:00");
        setting.setQuietEndTime("08:00");
        return setting;
    }

    /**
     * 返回请求值或当前值。
     *
     * @param requestValue 请求值
     * @param currentValue 当前值
     * @param <T> 值类型
     * @return 合并后的值
     */
    private <T> T valueOrDefault(T requestValue, T currentValue) {
        return requestValue == null ? currentValue : requestValue;
    }

    /**
     * 转换通知偏好视图对象。
     *
     * @param setting 通知偏好实体
     * @return 通知偏好视图对象
     */
    private NotificationSettingVO toVO(NotificationSetting setting) {
        return new NotificationSettingVO(
                setting.getThemeMode(),
                setting.getPersonalThemePreset(),
                setting.getSidebarMode(),
                setting.getTaskNoticeEnabled(),
                setting.getNoteNoticeEnabled(),
                setting.getMentionNoticeEnabled(),
                setting.getSystemNoticeEnabled(),
                setting.getEmailEnabled(),
                setting.getTaskEmailEnabled(),
                setting.getTodoEmailEnabled(),
                setting.getMentionEmailEnabled(),
                setting.getQuietEnabled(),
                setting.getQuietStartTime(),
                setting.getQuietEndTime()
        );
    }
}
