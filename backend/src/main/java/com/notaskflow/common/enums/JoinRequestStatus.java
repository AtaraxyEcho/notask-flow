package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 团队加入申请状态。
 *
 * @author LIN
 */
@Getter
public enum JoinRequestStatus {

    PENDING("PENDING", "待审批"),
    APPROVED("APPROVED", "已通过"),
    REJECTED("REJECTED", "已拒绝"),
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    private final String value;

    private final String description;

    JoinRequestStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
