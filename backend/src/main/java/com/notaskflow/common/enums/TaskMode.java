package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 任务创建模式。
 *
 * @author LIN
 */
@Getter
public enum TaskMode {

    ASSIGNED("ASSIGNED", "指派模式"),
    OPEN("OPEN", "开放认领模式");

    @EnumValue
    private final String value;

    private final String description;

    TaskMode(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
