package com.notaskflow.domain.dto.request;

import com.notaskflow.common.enums.PushPlatform;
import com.notaskflow.common.enums.PushProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 设备推送令牌注册请求。
 *
 * @author LIN
 */
@Data
public class DeviceTokenRegisterRequest {

    @NotNull(message = "推送平台不能为空")
    private PushPlatform platform;

    private PushProvider provider;

    @NotBlank(message = "设备标识不能为空")
    @Size(max = 100, message = "设备标识长度不能超过100")
    private String deviceId;

    @Size(max = 120, message = "设备名称长度不能超过120")
    private String deviceName;

    @NotBlank(message = "设备推送令牌不能为空")
    @Size(max = 1024, message = "设备推送令牌长度不能超过1024")
    private String deviceToken;

    @Size(max = 40, message = "应用版本长度不能超过40")
    private String appVersion;
}
