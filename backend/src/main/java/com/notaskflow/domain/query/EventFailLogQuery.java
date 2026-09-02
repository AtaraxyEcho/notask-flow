package com.notaskflow.domain.query;

import com.notaskflow.common.PageQuery;
import com.notaskflow.common.enums.EventFailStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 事件失败日志查询条件。
 *
 * @author LIN
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EventFailLogQuery extends PageQuery {

    private String eventType;

    private EventFailStatus status;
}
