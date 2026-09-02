package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.EventFailStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 事件失败日志视图对象。
 *
 * @author LIN
 */
@Data
public class EventFailLogVO {

    private Long id;

    private String eventType;

    private String eventData;

    private String failReason;

    private Integer retryCount;

    private EventFailStatus status;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;
}
