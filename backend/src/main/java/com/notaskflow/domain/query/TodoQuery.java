package com.notaskflow.domain.query;

import com.notaskflow.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 待办分页查询条件。
 *
 * @author LIN
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TodoQuery extends PageQuery {

    private String keyword;

    private Boolean isCompleted;

    private Long assigneeId;
}
