package com.notaskflow.listener;

import com.notaskflow.event.NotificationCreateEvent;
import com.notaskflow.mq.producer.NotificationEventProducer;
import com.notaskflow.service.EventFailLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 通知领域事件监听器，负责在业务事务提交后投递 MQ。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationEventProducer notificationEventProducer;

    private final EventFailLogService eventFailLogService;

    /**
     * 事务提交后投递通知创建事件。
     *
     * @param event 通知创建事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreate(NotificationCreateEvent event) {
        try {
            notificationEventProducer.sendCreateEvent(event);
        } catch (AmqpException exception) {
            log.error("通知创建事件发送失败，userId={}, businessType={}, businessId={}",
                    event.getUserId(), event.getBusinessType(), event.getBusinessId(), exception);
            eventFailLogService.recordFailure("NOTIFICATION_CREATE", event, exception.getMessage());
        }
    }
}
