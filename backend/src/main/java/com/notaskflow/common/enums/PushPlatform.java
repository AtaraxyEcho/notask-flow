package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 移动推送平台枚举。
 *
 * @author LIN
 */
@Getter
public enum PushPlatform {

    ANDROID("ANDROID", "Android"),
    IOS("IOS", "iOS");

    @EnumValue
    private final String value;

    private final String description;

    PushPlatform(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
