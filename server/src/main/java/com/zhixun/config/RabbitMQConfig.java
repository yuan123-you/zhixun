package com.zhixun.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置
 * 定义交换机、队列和绑定关系
 */
@Configuration
public class RabbitMQConfig {

    // ========== 通知相关 ==========

    /** 通知交换机 */
    public static final String NOTIFICATION_EXCHANGE = "zhixun.notification.exchange";

    /** 通知队列 */
    public static final String NOTIFICATION_QUEUE = "zhixun.notification.queue";

    /** 通知路由键 */
    public static final String NOTIFICATION_ROUTING_KEY = "zhixun.notification.#";

    // ========== 私信相关 ==========

    /** 私信交换机 */
    public static final String CHAT_EXCHANGE = "zhixun.chat.exchange";

    /** 私信队列 */
    public static final String CHAT_QUEUE = "zhixun.chat.queue";

    /** 私信路由键 */
    public static final String CHAT_ROUTING_KEY = "zhixun.chat.#";

    // ========== 文章事件相关 ==========

    /** 文章事件交换机 */
    public static final String ARTICLE_EVENT_EXCHANGE = "zhixun.article.exchange";

    /** 文章事件队列 */
    public static final String ARTICLE_EVENT_QUEUE = "zhixun.article.queue";

    /** 文章事件路由键 */
    public static final String ARTICLE_EVENT_ROUTING_KEY = "zhixun.article.#";

    // ========== 通用配置 ==========

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter);
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setPrefetchCount(10);
        return factory;
    }

    // ========== 通知交换机和队列 ==========

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_ROUTING_KEY);
    }

    // ========== 私信交换机和队列 ==========

    @Bean
    public TopicExchange chatExchange() {
        return new TopicExchange(CHAT_EXCHANGE, true, false);
    }

    @Bean
    public Queue chatQueue() {
        return new Queue(CHAT_QUEUE, true);
    }

    @Bean
    public Binding chatBinding() {
        return BindingBuilder.bind(chatQueue())
                .to(chatExchange())
                .with(CHAT_ROUTING_KEY);
    }

    // ========== 文章事件交换机和队列 ==========

    @Bean
    public TopicExchange articleEventExchange() {
        return new TopicExchange(ARTICLE_EVENT_EXCHANGE, true, false);
    }

    @Bean
    public Queue articleEventQueue() {
        return new Queue(ARTICLE_EVENT_QUEUE, true);
    }

    @Bean
    public Binding articleEventBinding() {
        return BindingBuilder.bind(articleEventQueue())
                .to(articleEventExchange())
                .with(ARTICLE_EVENT_ROUTING_KEY);
    }
}
