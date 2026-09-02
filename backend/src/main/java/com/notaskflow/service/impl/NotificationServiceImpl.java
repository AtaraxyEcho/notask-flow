package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.notaskflow.common.PageResponse;
import com.notaskflow.common.enums.BusinessType;
import com.notaskflow.common.enums.NotificationType;
import com.notaskflow.domain.entity.Note;
import com.notaskflow.domain.entity.Notification;
import com.notaskflow.domain.entity.SpaceJoinRequest;
import com.notaskflow.domain.entity.Task;
import com.notaskflow.domain.entity.Todo;
import com.notaskflow.domain.query.NotificationQuery;
import com.notaskflow.domain.vo.NotificationVO;
import com.notaskflow.event.MailSendRequestedEvent;
import com.notaskflow.exception.ResourceNotFoundException;
import com.notaskflow.mapper.NoteMapper;
import com.notaskflow.mapper.NotificationMapper;
import com.notaskflow.mapper.SpaceJoinRequestMapper;
import com.notaskflow.mapper.TaskMapper;
import com.notaskflow.mapper.TodoMapper;
import com.notaskflow.service.NotificationService;
import com.notaskflow.utils.LoginUserUtil;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知服务实现，处理用户通知查询、已读和创建。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    private final TaskMapper taskMapper;

    private final NoteMapper noteMapper;

    private final SpaceJoinRequestMapper spaceJoinRequestMapper;

    private final TodoMapper todoMapper;

    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 分页查询当前用户通知。
     *
     * @param query 查询条件
     * @return 通知分页结果
     */
    @Override
    public PageResponse<NotificationVO> page(NotificationQuery query) {
        Page<Notification> page = new Page<>(query.safePageNum(), query.safePageSize());
        LambdaQueryWrapper<Notification> wrapper = Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, LoginUserUtil.currentUserId())
                .eq(query.getIsRead() != null, Notification::getIsRead, query.getIsRead())
                .orderByDesc(Notification::getGmtCreate);
        IPage<Notification> result = notificationMapper.selectPage(page, wrapper);
        List<NotificationVO> list = result.getRecords().stream().map(this::toVO).toList();
        return PageResponse.of(page, list);
    }

    /**
     * 查询当前用户未读通知数量。
     *
     * @return 未读通知数量
     */
    @Override
    public Long unreadCount() {
        return notificationMapper.selectCount(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, LoginUserUtil.currentUserId())
                .eq(Notification::getIsRead, false));
    }

    /**
     * 标记当前用户的指定通知为已读。
     *
     * @param id 通知标识
     * @return 通知信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationVO markRead(Long id) {
        Notification notification = findCurrentNotification(id);
        notification.setIsRead(true);
        notificationMapper.updateById(notification);
        log.info("通知标记已读，notificationId={}，userId={}", id, LoginUserUtil.currentUserId());
        return toVO(notification);
    }

    /**
     * 标记当前用户所有通知为已读。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void readAll() {
        Long currentUserId = LoginUserUtil.currentUserId();
        notificationMapper.update(null, Wrappers.<Notification>lambdaUpdate()
                .eq(Notification::getUserId, currentUserId)
                .set(Notification::getIsRead, true));
        log.info("用户通知全部标记已读，userId={}", currentUserId);
    }

    /**
     * 清空当前用户的全部已读通知。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearRead() {
        Long currentUserId = LoginUserUtil.currentUserId();
        int deletedCount = notificationMapper.delete(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getUserId, currentUserId)
                .eq(Notification::getIsRead, true));
        log.info("用户已读通知清空完成，userId={}，deletedCount={}", currentUserId, deletedCount);
    }

    /**
     * 删除当前用户的指定通知。
     *
     * @param id 通知标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Notification notification = findCurrentNotification(id);
        notificationMapper.deleteById(notification.getId());
        log.info("通知删除完成，notificationId={}，userId={}", id, LoginUserUtil.currentUserId());
    }

    /**
     * 创建用户通知。
     *
     * @param userId 用户标识
     * @param type 通知类型
     * @param businessType 业务类型
     * @param businessId 业务标识
     * @param title 通知标题
     * @param content 通知内容
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(Long userId, NotificationType type, BusinessType businessType, Long businessId, String title,
                       String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSpaceId(resolveSpaceId(businessType, businessId));
        notification.setType(type);
        notification.setBusinessType(businessType);
        notification.setBusinessId(businessId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(false);
        notificationMapper.insert(notification);
        applicationEventPublisher.publishEvent(new MailSendRequestedEvent(userId, type, businessType, title, content));
        log.info("通知创建完成，notificationId={}，userId={}，businessType={}，businessId={}",
                notification.getId(), userId, businessType, businessId);
    }

    /**
     * 查询当前用户通知。
     *
     * @param id 通知标识
     * @return 通知实体
     */
    private Notification findCurrentNotification(Long id) {
        Notification notification = notificationMapper.selectOne(Wrappers.<Notification>lambdaQuery()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, LoginUserUtil.currentUserId()));
        if (notification == null) {
            throw new ResourceNotFoundException("通知不存在");
        }
        return notification;
    }

    /**
     * 转换通知视图对象。
     *
     * @param notification 通知实体
     * @return 通知视图对象
     */
    private NotificationVO toVO(Notification notification) {
        return new NotificationVO(notification.getId(), notification.getUserId(), notification.getSpaceId(),
                notification.getType(), notification.getBusinessType(), notification.getBusinessId(),
                notification.getTitle(), notification.getContent(), notification.getIsRead(), notification.getGmtCreate());
    }

    /**
     * 根据业务对象解析通知所属空间。
     *
     * @param businessType 业务类型
     * @param businessId 业务标识
     * @return 空间标识
     */
    private Long resolveSpaceId(BusinessType businessType, Long businessId) {
        if (businessType == null || businessId == null) {
            return null;
        }
        switch (businessType) {
            case TASK:
                Task task = taskMapper.selectById(businessId);
                return task == null ? null : task.getSpaceId();
            case NOTE:
                Note note = noteMapper.selectById(businessId);
                return note == null ? null : note.getSpaceId();
            case TODO:
                Todo todo = todoMapper.selectById(businessId);
                return todo == null ? null : todo.getSpaceId();
            case SPACE_JOIN_REQUEST:
                SpaceJoinRequest request = spaceJoinRequestMapper.selectById(businessId);
                return request == null ? null : request.getTargetSpaceId();
            default:
                return null;
        }
    }
}
