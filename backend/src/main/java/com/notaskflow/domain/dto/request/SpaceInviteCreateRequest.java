package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 团队邀请码创建请求。
 *
 * @author LIN
 */
@Data
public class SpaceInviteCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @Min(value = 1, message = "有效期不能小于1分钟")
    @Max(value = 1440, message = "有效期不能超过1440分钟")
    private Integer expireMinutes;
}
