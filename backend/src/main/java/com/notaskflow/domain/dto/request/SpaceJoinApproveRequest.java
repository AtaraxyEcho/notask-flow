package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 团队加入申请审批通过请求。
 *
 * @author LIN
 */
@Data
public class SpaceJoinApproveRequest {

    @NotNull(message = "目标空间不能为空")
    private Long spaceId;

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;
}
