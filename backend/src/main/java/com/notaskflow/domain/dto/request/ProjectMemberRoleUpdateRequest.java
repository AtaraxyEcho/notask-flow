package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 项目成员角色更新请求。
 *
 * @author LIN
 */
@Data
public class ProjectMemberRoleUpdateRequest {

    @NotNull(message = "项目成员角色不能为空")
    private ProjectMemberRole role;
}
