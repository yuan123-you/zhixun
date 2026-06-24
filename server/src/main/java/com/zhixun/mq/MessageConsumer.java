package com.zhixun.mq;

import com.zhixun.config.RabbitMQConfig;
import com.zhixun.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.util.Map;

/**
 * RabbitMQ 消息消费者
 * 接收队列消息并通过原始 WebSocket 推送给前端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessageConsumer {

    private final ObjectMapper objectMapper;

    /**
     * 消费通知消息
     * 接收通知队列消息，通过 WebSocket 推送给对应用户
     */
    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(Map<String, Object> message) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.get("data");
            Long userId = data != null ? Long.valueOf(data.get("userId").toString()) : null;

            if (userId != null && ChatWebSocketHandler.isUserOnline(userId)) {
                String json = objectMapper.writeValueAsString(message);
                ChatWebSocketHandler.sendToUser(userId, new TextMessage(json));
                log.debug("WebSocket 推送通知给用户 {}", userId);
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
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) message.get("data");
            Long receiverId = data != null ? Long.valueOf(data.get("receiverId").toString()) : null;

            if (receiverId != null && ChatWebSocketHandler.isUserOnline(receiverId)) {
                String json = objectMapper.writeValueAsString(message);
                ChatWebSocketHandler.sendToUser(receiverId, new TextMessage(json));
                log.debug("WebSocket 推送私信给用户 {}", receiverId);
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
            if (authorId != null && ChatWebSocketHandler.isUserOnline(authorId)) {
                Map<String, Object> wsMessage = Map.of(
                        "type", "NOTIFICATION",
                        "data", Map.of(
                                "userId", authorId,
                                "type", 1,
                                "title", "文章状态变更",
                                "content", "您的文章状态已更新"
                        )
                );
                String json = objectMapper.writeValueAsString(wsMessage);
                ChatWebSocketHandler.sendToUser(authorId, new TextMessage(json));
            }
        } catch (Exception e) {
            log.error("处理文章事件失败: {}", e.getMessage());
        }
    }
}
