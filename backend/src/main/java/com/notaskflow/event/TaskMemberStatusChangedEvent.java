package com.notaskflow.event;

import com.notaskflow.common.enums.TaskMemberStatus;
import java.io.Serializable;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务成员状态变更事件。
 *
 * @author LIN
 */
@Data
@NoArgsConstructor
public class TaskMemberStatusChangedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventId;

    private Long taskId;

    private Long taskMemberId;

    private Long spaceId;

    private TaskMemberStatus targetStatus;

    /**
     * 创建任务成员状态变更事件。
     *
     * @param taskId 任务标识
     * @param taskMemberId 任务成员标识
     * @param spaceId 空间标识
     * @param targetStatus 目标状态
     */
    public TaskMemberStatusChangedEvent(Long taskId, Long taskMemberId, Long spaceId,
                                        TaskMemberStatus targetStatus) {
        this(UUID.randomUUID().toString(), taskId, taskMemberId, spaceId, targetStatus);
    }

    /**
     * 创建任务成员状态变更事件。
     *
     * @param eventId 事件标识
     * @param taskId 任务标识
     * @param taskMemberId 任务成员标识
     * @param spaceId 空间标识
     * @param targetStatus 目标状态
     */
    public TaskMemberStatusChangedEvent(String eventId, Long taskId, Long taskMemberId, Long spaceId,
                                        TaskMemberStatus targetStatus) {
        this.eventId = eventId == null || eventId.isBlank() ? UUID.randomUUID().toString() : eventId;
        this.taskId = taskId;
        this.taskMemberId = taskMemberId;
        this.spaceId = spaceId;
        this.targetStatus = targetStatus;
    }
}
