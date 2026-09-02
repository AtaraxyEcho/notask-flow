package com.notaskflow.listener;

import com.notaskflow.event.TaskMemberStatusChangedEvent;
import com.notaskflow.mq.producer.TaskEventProducer;
import com.notaskflow.service.EventFailLogService;
import com.notaskflow.service.TaskStatusRecalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 任务领域事件监听器。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventListener {

    private final TaskStatusRecalculationService taskStatusRecalculationService;

    private final TaskEventProducer taskEventProducer;

    private final EventFailLogService eventFailLogService;

    /**
     * 在事务提交后处理任务成员状态变更事件。
     *
     * @param event 任务成员状态变更事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskMemberStatusChanged(TaskMemberStatusChangedEvent event) {
        taskStatusRecalculationService.recalculate(event);
        try {
            taskEventProducer.sendMemberStatusChanged(event);
        } catch (AmqpException e) {
            log.error("任务事件发送失败，写入补偿表。任务ID: {}, 成员ID: {}", event.getTaskId(), event.getTaskMemberId(), e);
            eventFailLogService.recordFailure("TASK_MEMBER_STATUS_CHANGED", event, e.getMessage());
        }
    }
}
