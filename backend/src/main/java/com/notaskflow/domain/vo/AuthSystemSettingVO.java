package com.notaskflow.domain.vo;

/**
 * 认证相关系统设置视图。
 *
 * @param registrationEnabled 是否开放新用户注册
 * @param registerEmailVerificationRequired 注册是否必须验证邮箱
 * @param singleDeviceLoginOnly 是否仅允许同一账号单设备登录
 * @author LIN
 */
public record AuthSystemSettingVO(
        Boolean registrationEnabled,
        Boolean registerEmailVerificationRequired,
        Boolean singleDeviceLoginOnly
) {
}
