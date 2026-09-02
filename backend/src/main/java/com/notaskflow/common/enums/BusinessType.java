package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 业务对象类型。
 *
 * @author LIN
 */
@Getter
public enum BusinessType {

    NOTE("NOTE", "笔记"),
    TASK("TASK", "任务"),
    TODO("TODO", "待办"),
    SPACE_JOIN_REQUEST("SPACE_JOIN_REQUEST", "团队加入申请"),
    SYSTEM("SYSTEM", "系统");

    @EnumValue
    private final String value;

    private final String description;

    BusinessType(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
