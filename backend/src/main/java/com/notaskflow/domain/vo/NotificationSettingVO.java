package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通知偏好视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSettingVO {

    private String themeMode;

    private String personalThemePreset;

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

    private String quietStartTime;

    private String quietEndTime;
}
