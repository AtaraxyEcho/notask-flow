package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.notaskflow.exception.IllegalTaskStateException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;

/**
 * 任务整体状态枚举，定义任务生命周期和允许的状态流转。
 *
 * @author LIN
 */
@Getter
public enum TaskStatus {

    PENDING("PENDING", "待开始"),
    OPEN("OPEN", "开放待认领"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    private final String value;

    private final String description;

    TaskStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 校验任务状态流转是否合法。
     *
     * @param target 目标状态
     */
    public void checkTransition(TaskStatus target) {
        if (target == null) {
            throw new IllegalTaskStateException("目标任务状态不能为空");
        }
        if (this == target) {
            return;
        }
        if (!allowedTargets().contains(target)) {
            throw new IllegalTaskStateException("任务状态不能从" + value + "流转为" + target.getValue());
        }
    }

    /**
     * 获取当前状态允许流转的目标状态集合。
     *
     * @return 允许流转的目标状态集合
     */
    public Set<TaskStatus> allowedTargets() {
        switch (this) {
            case PENDING:
                return EnumSet.of(IN_PROGRESS, CANCELLED);
            case OPEN:
                return EnumSet.of(IN_PROGRESS, CANCELLED);
            case IN_PROGRESS:
                return EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED:
                return Collections.emptySet();
            case CANCELLED:
                return Collections.emptySet();
            default:
                return Collections.emptySet();
        }
    }
}
