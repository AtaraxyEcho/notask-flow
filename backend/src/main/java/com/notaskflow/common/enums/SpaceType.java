package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 空间类型。
 *
 * @author LIN
 */
@Getter
public enum SpaceType {

    PERSONAL("PERSONAL", "个人空间"),
    TEAM("TEAM", "团队空间");

    @EnumValue
    private final String value;

    private final String description;

    SpaceType(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
