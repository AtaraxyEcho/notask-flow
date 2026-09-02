package com.notaskflow.listener;

import com.notaskflow.event.MailSendRequestedEvent;
import com.notaskflow.mq.producer.MailEventProducer;
import com.notaskflow.service.EventFailLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 邮件领域事件监听器，负责在业务事务提交后投递 MQ。
 *
 * @author LIN
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailEventListener {

    private final MailEventProducer mailEventProducer;

    private final EventFailLogService eventFailLogService;

    /**
     * 事务提交后投递邮件发送请求事件。
     *
     * @param event 邮件发送请求事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMailSendRequested(MailSendRequestedEvent event) {
        try {
            mailEventProducer.sendRequestedEvent(event);
        } catch (AmqpException exception) {
            log.error("邮件发送请求事件投递失败，userId={}, type={}, businessType={}",
                    event.getUserId(), event.getType(), event.getBusinessType(), exception);
            eventFailLogService.recordFailure("MAIL_SEND_REQUESTED", event, exception.getMessage());
        }
    }
}
