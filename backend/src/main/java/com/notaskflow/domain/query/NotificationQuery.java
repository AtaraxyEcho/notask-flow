package com.notaskflow.domain.query;

import com.notaskflow.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知分页查询条件。
 *
 * @author LIN
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotificationQuery extends PageQuery {

    private Boolean isRead;
}
