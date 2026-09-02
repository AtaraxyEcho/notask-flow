package com.notaskflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色任务完成统计视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleCompletionVO {

    private Long roleId;

    private String roleCode;

    private String roleName;

    private Long completedCount;
}
