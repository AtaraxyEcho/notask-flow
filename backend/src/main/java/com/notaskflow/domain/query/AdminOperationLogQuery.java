package com.notaskflow.domain.query;

import com.notaskflow.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端操作日志查询条件。
 *
 * @author LIN
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminOperationLogQuery extends PageQuery {

    private String keyword;

    private Boolean success;
}
