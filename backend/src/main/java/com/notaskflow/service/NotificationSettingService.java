package com.notaskflow.service;

import com.notaskflow.domain.dto.request.NotificationSettingUpdateRequest;
import com.notaskflow.domain.vo.NotificationSettingVO;

/**
 * 通知偏好服务接口。
 *
 * @author LIN
 */
public interface NotificationSettingService {

    /**
     * 获取当前用户通知偏好。
     *
     * @return 通知偏好
     */
    NotificationSettingVO getCurrentSetting();

    /**
     * 更新当前用户通知偏好。
     *
     * @param request 通知偏好更新请求
     * @return 通知偏好
     */
    NotificationSettingVO updateCurrentSetting(NotificationSettingUpdateRequest request);
}
