package com.notaskflow.domain.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;
import lombok.Data;

/**
 * 系统设置更新请求。
 *
 * @author LIN
 */
@Data
public class SystemSettingUpdateRequest {

    @NotEmpty(message = "系统设置不能为空")
    private Map<String, String> settings;
}
