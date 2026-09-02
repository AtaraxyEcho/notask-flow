package com.notaskflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.notaskflow.common.enums.SpaceRealtimeEventType;
import com.notaskflow.common.enums.TaskMemberStatus;
import com.notaskflow.common.enums.TaskStatus;
import com.notaskflow.domain.entity.Task;
import com.notaskflow.domain.entity.TaskMember;
import com.notaskflow.event.TaskMemberStatusChangedEvent;
import com.notaskflow.mapper.TaskMapper;
import com.notaskflow.mapper.TaskMemberMapper;
import com.notaskflow.service.SpaceRealtimeEventService;
import com.notaskflow.service.TaskStatusRecalculationService;
import com.notaskflow.utils.RedisUtil;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务状态重算服务实现。
 *
 * @author LIN
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskStatusRecalculationServiceImpl implements TaskStatusRecalculationService {

    private final TaskMapper taskMapper;

    private final TaskMemberMapper taskMemberMapper;

    private final RedisUtil redisUtil;

    private final SpaceRealtimeEventService spaceRealtimeEventService;

    /**
     * 根据成员状态变更事件重新计算任务整体状态。
     *
     * @param event 成员状态变更事件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculate(TaskMemberStatusChangedEvent event) {
        if (TaskMemberStatus.IN_PROGRESS.equals(event.getTargetStatus())) {
            markTaskInProgress(event.getTaskId(), event.getSpaceId());
        }
        if (TaskMemberStatus.COMPLETED.equals(event.getTargetStatus())) {
            markTaskCompletedIfReady(event.getTaskId(), event.getSpaceId());
        }
        clearStatsCache(event.getSpaceId());
    }

    /**
     * 成员职责移除后重新计算任务整体状态。
     *
     * @param taskId 任务标识
     * @param spaceId 空间标识
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateAfterMemberRemoved(Long taskId, Long spaceId) {
        markTaskCompletedIfReady(taskId, spaceId);
        clearStatsCache(spaceId);
        log.info("成员移除后完成任务状态重算，spaceId={}，taskId={}", spaceId, taskId);
    }

    /**
     * 使用条件更新将任务从开放状态推进到进行中。
     *
     * @param taskId 任务标识
     */
    private void markTaskInProgress(Long taskId, Long spaceId) {
        LambdaUpdateWrapper<Task> wrapper = Wrappers.<Task>lambdaUpdate()
                .eq(Task::getId, taskId)
                .in(Task::getStatus, List.of(TaskStatus.OPEN, TaskStatus.PENDING))
                .set(Task::getStatus, TaskStatus.IN_PROGRESS);
        int updated = taskMapper.update(null, wrapper);
        if (updated > 0) {
            publishTaskStatusChanged(spaceId, taskId, TaskStatus.IN_PROGRESS);
            log.info("任务状态自动推进为进行中，taskId={}", taskId);
        }
    }

    /**
     * 所有必须完成项完成后使用条件更新完成任务。
     *
     * @param taskId 任务标识
     */
    private void markTaskCompletedIfReady(Long taskId, Long spaceId) {
        Long unfinishedRequiredCount = taskMemberMapper.selectCount(Wrappers.<TaskMember>lambdaQuery()
                .eq(TaskMember::getTaskId, taskId)
                .eq(TaskMember::getIsRequired, true)
                .ne(TaskMember::getStatus, TaskMemberStatus.COMPLETED));
        if (unfinishedRequiredCount > 0) {
            return;
        }
        LambdaUpdateWrapper<Task> wrapper = Wrappers.<Task>lambdaUpdate()
                .eq(Task::getId, taskId)
                .in(Task::getStatus, List.of(TaskStatus.IN_PROGRESS, TaskStatus.PENDING))
                .set(Task::getStatus, TaskStatus.COMPLETED)
                .set(Task::getCompletedAt, LocalDateTime.now());
        int updated = taskMapper.update(null, wrapper);
        if (updated > 0) {
            publishTaskStatusChanged(spaceId, taskId, TaskStatus.COMPLETED);
            log.info("任务状态自动推进为已完成，taskId={}", taskId);
        }
    }

    /**
     * 发布任务重算后的实时事件。
     *
     * @param spaceId 空间标识
     * @param taskId 任务标识
     * @param status 任务状态
     */
    private void publishTaskStatusChanged(Long spaceId, Long taskId, TaskStatus status) {
        if (spaceId == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("taskId", taskId);
        payload.put("status", status);
        payload.put("recalculated", true);
        spaceRealtimeEventService.publishSystem(spaceId, SpaceRealtimeEventType.TASK_UPDATED, payload);
    }

    /**
     * 清理任务相关统计缓存。
     *
     * @param spaceId 空间标识
     */
    private void clearStatsCache(Long spaceId) {
        if (spaceId == null) {
            return;
        }
        redisUtil.delete(List.of(
                "space:" + spaceId + ":stats:load",
                "space:" + spaceId + ":stats:trend",
                "space:" + spaceId + ":stats:role-completion"));
    }
}
