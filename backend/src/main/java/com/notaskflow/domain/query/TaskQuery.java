package com.notaskflow.domain.query;

import com.notaskflow.common.PageQuery;
import com.notaskflow.common.enums.TaskMode;
import com.notaskflow.common.enums.TaskStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务分页查询条件。
 *
 * @author LIN
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskQuery extends PageQuery {

    private String keyword;

    private TaskStatus status;

    private TaskMode mode;

    private Long assigneeId;

    private Long projectId;
}
