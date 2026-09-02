package com.notaskflow.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 事件补偿状态。
 *
 * @author LIN
 */
@Getter
public enum EventFailStatus {

    PENDING("PENDING", "待重试"),
    RETRYING("RETRYING", "重试中"),
    FAILED("FAILED", "已失败"),
    SUCCESS("SUCCESS", "已成功");

    @EnumValue
    private final String value;

    private final String description;

    EventFailStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }
}
