package com.notaskflow.domain.vo;

import com.notaskflow.common.enums.PushPlatform;
import com.notaskflow.common.enums.PushProvider;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户移动端推送设备令牌视图对象。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDeviceTokenVO {

    private Long id;

    private PushPlatform platform;

    private PushProvider provider;

    private String deviceId;

    private String deviceName;

    private String appVersion;

    private Boolean enabled;

    private LocalDateTime lastActiveAt;
}
