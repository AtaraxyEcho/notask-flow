package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 用户账号状态。
 *
 * @author LIN
 */
@Getter
public enum UserStatus {

    NORMAL("NORMAL", "正常"),
    DISABLED("DISABLED", "禁用");

    @EnumValue
    private final String value;

    private final String description;

    UserStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
