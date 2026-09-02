package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端用户状态更新请求。
 *
 * @author LIN
 */
@Data
public class AdminUserStatusUpdateRequest {

    @NotNull(message = "用户状态不能为空")
    private UserStatus status;
}
