package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 任务成员分配方式。
 *
 * @author LIN
 */
@Getter
public enum AssignmentType {

    ASSIGNED("ASSIGNED", "系统指派"),
    CLAIMED("CLAIMED", "成员认领");

    @EnumValue
    private final String value;

    private final String description;

    AssignmentType(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
