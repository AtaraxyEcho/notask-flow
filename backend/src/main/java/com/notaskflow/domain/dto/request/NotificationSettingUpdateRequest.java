package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 通知偏好更新请求。
 *
 * @author LIN
 */
@Data
public class NotificationSettingUpdateRequest {

    @Pattern(regexp = "^(light|dark|system)$", message = "主题模式仅支持 light、dark、system")
    private String themeMode;

    @Pattern(regexp = "^(sunrise|forest|ocean|midnight)$", message = "个人主题预设仅支持 sunrise、forest、ocean、midnight")
    private String personalThemePreset;

    @Pattern(regexp = "^(expanded|auto|collapsed)$", message = "侧边栏模式仅支持 expanded、auto、collapsed")
    private String sidebarMode;

    private Boolean taskNoticeEnabled;

    private Boolean noteNoticeEnabled;

    private Boolean mentionNoticeEnabled;

    private Boolean systemNoticeEnabled;

    private Boolean emailEnabled;

    private Boolean taskEmailEnabled;

    private Boolean todoEmailEnabled;

    private Boolean mentionEmailEnabled;

    private Boolean quietEnabled;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "免打扰开始时间格式应为 HH:mm")
    private String quietStartTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "免打扰结束时间格式应为 HH:mm")
    private String quietEndTime;
}
