package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 空间成员角色更新请求。
 *
 * @author LIN
 */
@Data
public class SpaceMemberRoleUpdateRequest {

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;
}
