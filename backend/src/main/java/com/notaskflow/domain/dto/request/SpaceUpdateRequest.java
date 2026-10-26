package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 空间更新请求。
 *
 * @author LIN
 */
@Data
public class SpaceUpdateRequest {

    @NotBlank(message = "空间名称不能为空")
    @Size(max = 100, message = "空间名称长度不能超过100")
    private String name;

    /** 新成员加入是否需要审核，为空表示保持原值。 */
    private Boolean joinApprovalRequired;

    /** 是否允许普通成员邀请新成员，为空表示保持原值。 */
    private Boolean memberInviteEnabled;
}
