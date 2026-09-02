package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用户通知偏好实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_notification_setting")
public class NotificationSetting extends BaseEntity {

    private Long userId;

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
