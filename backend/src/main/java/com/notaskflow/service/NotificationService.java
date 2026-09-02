package com.notaskflow.service;

import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.NotificationType;
import com.notaskflow.domain.query.NotificationQuery;
import com.notaskflow.domain.vo.NotificationVO;

/**
 * 通知服务接口。
 *
 * @author LIN
 */
public interface NotificationService {

    /**
     * 分页查询当前用户通知。
     *
     * @param query 查询条件
     * @return 分页通知
     */
    PageResponse<NotificationVO> page(NotificationQuery query);

    /**
     * 查询当前用户未读通知数量。
     *
     * @return 未读数量
     */
    Long unreadCount();

    /**
     * 标记通知已读。
     *
     * @param id 通知标识
     * @return 通知详情
     */
    NotificationVO markRead(Long id);

    /**
     * 标记当前用户全部通知已读。
     */
    void readAll();

    /**
     * 清空当前用户的全部已读通知。
     */
    void clearRead();

    /**
     * 删除通知。
     *
     * @param id 通知标识
     */
    void delete(Long id);

    /**
     * 创建站内通知。
     *
     * @param userId 接收用户标识
     * @param type 通知类型
     * @param businessType 业务类型
     * @param businessId 业务标识
     * @param title 通知标题
     * @param content 通知内容
     */
    void create(Long userId, NotificationType type, BusinessType businessType, Long businessId, String title,
                String content);
}
