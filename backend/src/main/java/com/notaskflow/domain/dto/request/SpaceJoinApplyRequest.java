package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 团队加入申请创建请求。
 *
 * @author LIN
 */
@Data
public class SpaceJoinApplyRequest {

    @NotBlank(message = "上级账号不能为空")
    @Size(max = 100, message = "上级账号长度不能超过100")
    private String supervisorAccount;

    @Size(max = 100, message = "团队名称长度不能超过100")
    private String teamName;

    @Size(max = 500, message = "申请说明长度不能超过500")
    private String remark;
}
