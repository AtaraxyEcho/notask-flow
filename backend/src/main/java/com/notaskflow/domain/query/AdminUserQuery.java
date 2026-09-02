package com.notaskflow.domain.query;

import com.notaskflow.common.PageQuery;
import com.notaskflow.common.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端用户分页查询条件。
 *
 * @author LIN
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserQuery extends PageQuery {

    private String keyword;

    private UserStatus status;
}
