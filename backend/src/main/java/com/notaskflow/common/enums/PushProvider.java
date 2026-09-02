package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 移动推送服务提供商枚举。
 *
 * @author LIN
 */
@Getter
public enum PushProvider {

    FCM("FCM", "Firebase Cloud Messaging"),
    APNS("APNS", "Apple Push Notification service");

    @EnumValue
    private final String value;

    private final String description;

    PushProvider(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
