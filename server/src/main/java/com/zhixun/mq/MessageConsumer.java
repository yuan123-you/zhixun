package com.zhixun.mq;

import com.zhixun.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * RabbitMQ 消息消费者
 * 接收队列消息并通过 WebSocket 推送给前端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 消费通知消息
     * 接收通知队列消息，通过 WebSocket 推送给对应用户
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(Map<String, Object> message) {
        try {
            String type = (String) message.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.get("data");
            Long userId = data != null ? Long.valueOf(data.get("userId").toString()) : null;

            if (userId != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        userId.toString(), "/queue/notifications", message);
                log.debug("WebSocket 推送通知给用户 {}: type={}", userId, type);
            }
        } catch (Exception e) {
            log.error("处理通知消息失败: {}", e.getMessage());
        }
    }

    /**
     * 消费私信消息
     * 接收私信队列消息，通过 WebSocket 推送给接收者
     */
    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
    public void handleChatMessage(Map<String, Object> message) {
        try {
            String type = (String) message.get("type");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.get("data");
            Long receiverId = data != null ? Long.valueOf(data.get("receiverId").toString()) : null;

            if (receiverId != null) {
                simpMessagingTemplate.convertAndSendToUser(
                        receiverId.toString(), "/queue/messages", message);
                log.debug("WebSocket 推送私信给用户 {}: type={}", receiverId, type);
            }
        } catch (Exception e) {
            log.error("处理私信消息失败: {}", e.getMessage());
        }
    }

    /**
     * 消费文章事件消息
     * 文章状态变更等事件的处理
     */
    @RabbitListener(queues = RabbitMQConfig.ARTICLE_EVENT_QUEUE)
    public void handleArticleEvent(Map<String, Object> message) {
        try {
            String action = (String) message.get("action");
            Long articleId = message.get("articleId") != null ? Long.valueOf(message.get("articleId").toString()) : null;
            Long authorId = message.get("authorId") != null ? Long.valueOf(message.get("authorId").toString()) : null;

            log.info("处理文章事件: action={}, articleId={}, authorId={}", action, articleId, authorId);

            // 文章状态变更通知作者
            if (authorId != null && message.get("data") != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) message.get("data");
                simpMessagingTemplate.convertAndSendToUser(
                        authorId.toString(), "/queue/notifications", Map.of(
                                "type", "ARTICLE_STATUS",
                                "data", data
                        ));
            }
        } catch (Exception e) {
            log.error("处理文章事件失败: {}", e.getMessage());
        }
    }
}
