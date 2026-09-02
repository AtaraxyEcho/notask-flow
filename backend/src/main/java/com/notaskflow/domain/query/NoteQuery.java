package com.notaskflow.domain.query;

import com.notaskflow.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 笔记分页查询条件。
 *
 * @author LIN
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoteQuery extends PageQuery {

    private Long notebookId;

    private Long tagId;

    private String keyword;

    private Long projectId;
}
