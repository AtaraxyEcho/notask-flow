package com.notaskflow.domain.query;

import com.notaskflow.common.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件管理查询条件。
 *
 * @author LIN
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ManagedFileQuery extends PageQuery {

    private Long folderId;

    private String keyword;

    private String mimeType;

    private Long uploaderId;

    private Boolean trashed;
}
