package com.notaskflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础配置。
 *
 * @author LIN
 */
@Configuration
public class RabbitMqConfig {

    public static final String TASK_EXCHANGE = "notask.task.exchange";

    public static final String TASK_EVENT_QUEUE = "notask.task.event.queue";

    public static final String TASK_EVENT_ROUTING_KEY = "task.event";

    public static final String NOTIFICATION_EXCHANGE = "notask.notification.exchange";

    public static final String NOTIFICATION_CREATE_QUEUE = "notask.notification.create.queue";

    public static final String NOTIFICATION_CREATE_ROUTING_KEY = "notification.create";

    public static final String MAIL_EXCHANGE = "notask.mail.exchange";

    public static final String MAIL_SEND_QUEUE = "notask.mail.queue";

    public static final String MAIL_SEND_ROUTING_KEY = "mail.send.requested";

    public static final String SEARCH_INDEX_EXCHANGE = "notask.search-index.exchange";

    public static final String SEARCH_INDEX_QUEUE = "notask.search-index.queue";

    public static final String SEARCH_INDEX_ROUTING_KEY = "search.index";

    public static final String FILE_PROCESS_EXCHANGE = "notask.file-process.exchange";

    public static final String FILE_PROCESS_QUEUE = "notask.file-process.queue";

    public static final String FILE_PROCESS_ROUTING_KEY = "file.process";

    public static final String STATS_EXCHANGE = "notask.stats.exchange";

    public static final String STATS_QUEUE = "notask.stats.queue";

    public static final String STATS_REFRESH_ROUTING_KEY = "stats.refresh";

    public static final String DEAD_LETTER_EXCHANGE = "notask.dead-letter.exchange";

    public static final String DEAD_LETTER_QUEUE = "notask.dead-letter.queue";

    public static final String DEAD_LETTER_ROUTING_KEY = "dead-letter";

    /**
     * 注册任务事件交换机。
     *
     * @return 任务事件交换机
     */
    @Bean
    public DirectExchange taskExchange() {
        return new DirectExchange(TASK_EXCHANGE, true, false);
    }

    /**
     * 注册任务事件队列。
     *
     * @return 任务事件队列
     */
    @Bean
    public Queue taskEventQueue() {
        return durableQueue(TASK_EVENT_QUEUE);
    }

    /**
     * 绑定任务事件队列。
     *
     * @param taskEventQueue 任务事件队列
     * @param taskExchange 任务事件交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding taskEventBinding(Queue taskEventQueue, DirectExchange taskExchange) {
        return BindingBuilder.bind(taskEventQueue).to(taskExchange).with(TASK_EVENT_ROUTING_KEY);
    }

    /**
     * 注册通知事件交换机。
     *
     * @return 通知事件交换机
     */
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    /**
     * 注册通知创建队列。
     *
     * @return 通知创建队列
     */
    @Bean
    public Queue notificationCreateQueue() {
        return durableQueue(NOTIFICATION_CREATE_QUEUE);
    }

    /**
     * 绑定通知创建队列。
     *
     * @param notificationCreateQueue 通知创建队列
     * @param notificationExchange 通知事件交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding notificationCreateBinding(Queue notificationCreateQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationCreateQueue)
                .to(notificationExchange)
                .with(NOTIFICATION_CREATE_ROUTING_KEY);
    }

    /**
     * 注册邮件事件交换机。
     *
     * @return 邮件事件交换机
     */
    @Bean
    public DirectExchange mailExchange() {
        return new DirectExchange(MAIL_EXCHANGE, true, false);
    }

    /**
     * 注册邮件发送队列。
     *
     * @return 邮件发送队列
     */
    @Bean
    public Queue mailSendQueue() {
        return durableQueue(MAIL_SEND_QUEUE);
    }

    /**
     * 绑定邮件发送队列。
     *
     * @param mailSendQueue 邮件发送队列
     * @param mailExchange 邮件事件交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding mailSendBinding(Queue mailSendQueue, DirectExchange mailExchange) {
        return BindingBuilder.bind(mailSendQueue).to(mailExchange).with(MAIL_SEND_ROUTING_KEY);
    }

    /**
     * 注册搜索索引事件交换机。
     *
     * @return 搜索索引事件交换机
     */
    @Bean
    public DirectExchange searchIndexExchange() {
        return new DirectExchange(SEARCH_INDEX_EXCHANGE, true, false);
    }

    /**
     * 注册搜索索引队列。
     *
     * @return 搜索索引队列
     */
    @Bean
    public Queue searchIndexQueue() {
        return durableQueue(SEARCH_INDEX_QUEUE);
    }

    /**
     * 绑定搜索索引队列。
     *
     * @param searchIndexQueue 搜索索引队列
     * @param searchIndexExchange 搜索索引事件交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding searchIndexBinding(Queue searchIndexQueue, DirectExchange searchIndexExchange) {
        return BindingBuilder.bind(searchIndexQueue).to(searchIndexExchange).with(SEARCH_INDEX_ROUTING_KEY);
    }

    /**
     * 注册文件处理事件交换机。
     *
     * @return 文件处理事件交换机
     */
    @Bean
    public DirectExchange fileProcessExchange() {
        return new DirectExchange(FILE_PROCESS_EXCHANGE, true, false);
    }

    /**
     * 注册文件处理队列。
     *
     * @return 文件处理队列
     */
    @Bean
    public Queue fileProcessQueue() {
        return durableQueue(FILE_PROCESS_QUEUE);
    }

    /**
     * 绑定文件处理队列。
     *
     * @param fileProcessQueue 文件处理队列
     * @param fileProcessExchange 文件处理事件交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding fileProcessBinding(Queue fileProcessQueue, DirectExchange fileProcessExchange) {
        return BindingBuilder.bind(fileProcessQueue).to(fileProcessExchange).with(FILE_PROCESS_ROUTING_KEY);
    }

    /**
     * 注册统计刷新事件交换机。
     *
     * @return 统计刷新事件交换机
     */
    @Bean
    public DirectExchange statsExchange() {
        return new DirectExchange(STATS_EXCHANGE, true, false);
    }

    /**
     * 注册统计刷新队列。
     *
     * @return 统计刷新队列
     */
    @Bean
    public Queue statsQueue() {
        return durableQueue(STATS_QUEUE);
    }

    /**
     * 绑定统计刷新队列。
     *
     * @param statsQueue 统计刷新队列
     * @param statsExchange 统计刷新事件交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding statsBinding(Queue statsQueue, DirectExchange statsExchange) {
        return BindingBuilder.bind(statsQueue).to(statsExchange).with(STATS_REFRESH_ROUTING_KEY);
    }

    /**
     * 注册死信交换机。
     *
     * @return 死信交换机
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    /**
     * 注册死信队列。
     *
     * @return 死信队列
     */
    @Bean
    public Queue deadLetterQueue() {
        return new Queue(DEAD_LETTER_QUEUE, true);
    }

    /**
     * 绑定死信队列。
     *
     * @param deadLetterQueue 死信队列
     * @param deadLetterExchange 死信交换机
     * @return 队列绑定关系
     */
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_ROUTING_KEY);
    }

    /**
     * 注册手动 ACK 的监听容器工厂。
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @return 监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                              MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setMessageConverter(rabbitMessageConverter);
        return factory;
    }

    /**
     * 注册基于 JSON 的消息转换器。
     *
     * @param objectMapper JSON 对象映射器
     * @return JSON 消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 注册兼容历史序列化消息的转换器。
     *
     * @return 兼容历史消息的转换器
     */
    @Bean
    public SimpleMessageConverter legacyEventMessageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(List.of(
                "com.notaskflow.event.*",
                "com.notaskflow.common.enums.*",
                "java.lang.*",
                "java.util.*"));
        return converter;
    }

    /**
     * 注册按内容类型委派的消息转换器。
     *
     * @param legacyEventMessageConverter 历史消息转换器
     * @param jackson2JsonMessageConverter JSON 消息转换器
     * @return 消息转换器
     */
    @Bean
    public MessageConverter rabbitMessageConverter(SimpleMessageConverter legacyEventMessageConverter,
                                                   Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        ContentTypeDelegatingMessageConverter converter =
                new ContentTypeDelegatingMessageConverter(legacyEventMessageConverter);
        converter.addDelegate("application/json", jackson2JsonMessageConverter);
        return converter;
    }

    /**
     * 注册任务事件消息模板。
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param jackson2JsonMessageConverter JSON 消息转换器
     * @return RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter jackson2JsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter);
        return rabbitTemplate;
    }

    private Queue durableQueue(String queueName) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY)
                .build();
    }
}
