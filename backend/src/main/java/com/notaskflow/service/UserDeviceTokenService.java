package com.notaskflow.service;

import com.notaskflow.common.enums.PushPlatform;
import com.notaskflow.domain.dto.request.DeviceTokenRegisterRequest;
import com.notaskflow.domain.vo.UserDeviceTokenVO;

/**
 * 用户移动端推送设备令牌服务接口。
 *
 * @author LIN
 */
public interface UserDeviceTokenService {

    /**
     * 注册或刷新当前用户的设备推送令牌。
     *
     * @param request 注册请求
     * @return 设备推送令牌信息
     */
    UserDeviceTokenVO registerCurrent(DeviceTokenRegisterRequest request);

    /**
     * 解绑当前用户的设备推送令牌。
     *
     * @param platform 推送平台
     * @param deviceId 设备标识
     */
    void unbindCurrent(PushPlatform platform, String deviceId);

    /**
     * 查询当前用户指定设备的推送令牌状态。
     *
     * @param platform 推送平台
     * @param deviceId 设备标识
     * @return 设备推送令牌信息
     */
    UserDeviceTokenVO current(PushPlatform platform, String deviceId);
}
