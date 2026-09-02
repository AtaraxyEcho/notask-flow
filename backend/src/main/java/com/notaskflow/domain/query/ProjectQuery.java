package com.notaskflow.domain.query;

import com.notaskflow.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目分页查询条件。
 *
 * @author LIN
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectQuery extends PageQuery {

    private String keyword;

    private Boolean archived;
}
