package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.notaskflow.common.enums.EventFailStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 事件发送失败补偿实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_event_fail_log")
public class EventFailLog extends BaseEntity {

    private String eventType;

    private String eventData;

    private String failReason;

    private Integer retryCount;

    private EventFailStatus status;
}
