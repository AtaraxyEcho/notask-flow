package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.notaskflow.exception.IllegalTaskStateException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;

/**
 * 任务成员工作项状态。
 *
 * @author LIN
 */
@Getter
public enum TaskMemberStatus {

    PENDING("PENDING", "未开始"),
    IN_PROGRESS("IN_PROGRESS", "进行中"),
    COMPLETED("COMPLETED", "已完成");

    @EnumValue
    private final String value;

    private final String description;

    TaskMemberStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 校验成员工作项状态流转是否合法。
     *
     * @param target 目标状态
     */
    public void checkTransition(TaskMemberStatus target) {
        if (target == null) {
            throw new IllegalTaskStateException("目标成员状态不能为空");
        }
        if (this == target) {
            return;
        }
        if (!allowedTargets().contains(target)) {
            throw new IllegalTaskStateException("成员状态不能从" + value + "流转为" + target.getValue());
        }
    }

    /**
     * 获取当前状态允许流转的目标状态集合。
     *
     * @return 允许流转的目标状态集合
     */
    public Set<TaskMemberStatus> allowedTargets() {
        switch (this) {
            case PENDING:
                return EnumSet.of(IN_PROGRESS);
            case IN_PROGRESS:
                return EnumSet.of(COMPLETED);
            case COMPLETED:
                return Collections.emptySet();
            default:
                return Collections.emptySet();
        }
    }
}
