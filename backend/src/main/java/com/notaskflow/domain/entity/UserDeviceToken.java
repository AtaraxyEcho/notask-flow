package com.notaskflow.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.notaskflow.common.enums.PushPlatform;
import com.notaskflow.common.enums.PushProvider;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 用户移动端推送设备令牌实体。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("nt_user_device_token")
public class UserDeviceToken extends BaseEntity {

    private Long userId;

    private PushPlatform platform;

    private PushProvider provider;

    private String deviceId;

    private String deviceName;

    private String deviceToken;

    private String appVersion;

    private Boolean enabled;

    private LocalDateTime lastActiveAt;
}
