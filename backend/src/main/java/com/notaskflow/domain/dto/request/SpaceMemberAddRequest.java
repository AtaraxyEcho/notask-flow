package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 空间成员添加请求。
 *
 * @author LIN
 */
@Data
public class SpaceMemberAddRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;
}
