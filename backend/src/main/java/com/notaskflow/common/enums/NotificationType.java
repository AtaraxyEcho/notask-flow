package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 站内通知类型。
 *
 * @author LIN
 */
@Getter
public enum NotificationType {

    TASK_CREATED("TASK_CREATED", "任务创建"),
    TASK_CLAIMED("TASK_CLAIMED", "任务认领"),
    TASK_MEMBER_COMPLETED("TASK_MEMBER_COMPLETED", "成员任务完成"),
    TASK_COMPLETED("TASK_COMPLETED", "任务完成"),
    TODO_CREATED("TODO_CREATED", "待办生成"),
    COMMENT_MENTIONED("COMMENT_MENTIONED", "评论提及"),
    SPACE_JOIN_APPLIED("SPACE_JOIN_APPLIED", "团队加入申请"),
    SPACE_JOIN_APPROVED("SPACE_JOIN_APPROVED", "团队加入申请已通过"),
    SPACE_JOIN_REJECTED("SPACE_JOIN_REJECTED", "团队加入申请已拒绝"),
    SYSTEM_ANNOUNCEMENT("SYSTEM_ANNOUNCEMENT", "系统公告");

    @EnumValue
    private final String value;

    private final String description;

    NotificationType(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
