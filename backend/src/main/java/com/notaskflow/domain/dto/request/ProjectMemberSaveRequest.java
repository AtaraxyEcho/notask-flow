package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.ProjectMemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 项目成员保存请求。
 *
 * @author LIN
 */
@Data
public class ProjectMemberSaveRequest {

    @NotNull(message = "用户标识不能为空")
    private Long userId;

    @NotNull(message = "项目成员角色不能为空")
    private ProjectMemberRole role;
}
