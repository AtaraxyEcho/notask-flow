package com.notaskflow.service;

import com.notaskflow.event.TaskMemberStatusChangedEvent;

/**
 * 任务状态重算服务接口。
 *
 * @author LIN
 */
public interface TaskStatusRecalculationService {

    /**
     * 根据成员状态事件重算任务整体状态。
     *
     * @param event 成员状态变更事件
     */
    void recalculate(TaskMemberStatusChangedEvent event);

    /**
     * 成员职责移除后重新计算任务整体状态。
     *
     * @param taskId 任务标识
     * @param spaceId 空间标识
     */
    void recalculateAfterMemberRemoved(Long taskId, Long spaceId);
}
