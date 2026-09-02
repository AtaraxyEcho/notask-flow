package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.PageQuery;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.NotificationType;
import com.notaskflow.domain.dto.request.AdminSystemNotificationRequest;
import com.notaskflow.domain.entity.Notification;
import com.notaskflow.domain.entity.User;
import com.notaskflow.domain.vo.NotificationVO;
import com.notaskflow.event.NotificationCreateEvent;
import com.notaskflow.mapper.NotificationMapper;
import com.notaskflow.mapper.UserMapper;
import com.notaskflow.service.AdminSystemNotificationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端系统通知服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSystemNotificationServiceImpl implements AdminSystemNotificationService {

    private final UserMapper userMapper;

    private final NotificationMapper notificationMapper;

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 向全部普通用户发送系统通知。
     *
     * @param request 系统通知请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendToAllUsers(AdminSystemNotificationRequest request) {
        List<User> users = userMapper.selectList(Wrappers.<User>lambdaQuery());
        for (User user : users) {
            applicationEventPublisher.publishEvent(new NotificationCreateEvent(
                    user.getId(),
                    NotificationType.SYSTEM_ANNOUNCEMENT,
                    BusinessType.SYSTEM,
                    null,
                    request.getTitle(),
                    request.getContent()
            ));
        }
        log.info("管理端发送全员系统通知完成，userCount={}", users.size());
    }

    /**
     * 查询系统通知发送历史。
     *
     * @param query 分页查询
     * @return 系统通知历史
     */
    @Override
    public PageResponse<NotificationVO> history(PageQuery query) {
        Page<Notification> page = new Page<>(query.safePageNum(), query.safePageSize());
        LambdaQueryWrapper<Notification> wrapper = Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getType, NotificationType.SYSTEM_ANNOUNCEMENT)
                .orderByDesc(Notification::getGmtCreate);
        IPage<Notification> result = notificationMapper.selectPage(page, wrapper);
        List<NotificationVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResponse.of(page, list);
    }

    private NotificationVO toVO(Notification notification) {
        return new NotificationVO(notification.getId(), notification.getUserId(), notification.getSpaceId(),
                notification.getType(), notification.getBusinessType(), notification.getBusinessId(),
                notification.getTitle(), notification.getContent(), notification.getIsRead(), notification.getGmtCreate());
    }
}
