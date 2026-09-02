package com.notaskflow.service;

import com.notaskflow.common.PageQuery;
import com.notaskflow.common.PageResponse;
import com.notaskflow.domain.dto.request.AdminSystemNotificationRequest;
import com.notaskflow.domain.vo.NotificationVO;

/**
 * 管理端系统通知服务。
 *
 * @author LIN
 */
public interface AdminSystemNotificationService {

    /**
     * 向全部普通用户发送系统通知。
     *
     * @param request 系统通知请求
     */
    void sendToAllUsers(AdminSystemNotificationRequest request);

    /**
     * 查询系统通知发送历史。
     *
     * @param query 分页查询
     * @return 系统通知历史
     */
    PageResponse<NotificationVO> history(PageQuery query);
}
