package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 团队加入申请拒绝请求。
 *
 * @author LIN
 */
@Data
public class SpaceJoinRejectRequest {

    @Size(max = 500, message = "拒绝原因长度不能超过500")
    private String reason;
}
