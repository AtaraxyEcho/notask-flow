package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 任务优先级。
 *
 * @author LIN
 */
@Getter
public enum TaskPriority {

    LOW("LOW", "低"),
    MEDIUM("MEDIUM", "中"),
    HIGH("HIGH", "高");

    @EnumValue
    private final String value;

    private final String description;

    TaskPriority(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
