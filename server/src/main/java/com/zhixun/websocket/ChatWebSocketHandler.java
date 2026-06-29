package com.zhixun.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.dto.social.MessageSendRequest;
import com.zhixun.entity.User;
import com.zhixun.mapper.UserMapper;
import com.zhixun.security.HtmlWhitelistFilter;
import com.zhixun.service.MessageService;
import com.zhixun.common.util.JwtUtil;
import com.zhixun.service.OnlineStatusService;
import com.zhixun.vo.MessageVO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 私信 WebSocket 处理器
 * 处理路径: /ws/chat?token=xxx
 * 消息格式: {"type": "CHAT/ONLINE/OFFLINE/READ", "data": {...}}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final OnlineStatusService onlineStatusService;
    private final ObjectMapper objectMapper;
    private final MessageService messageService;
    private final SensitiveWordUtil sensitiveWordUtil;
    private final UserMapper userMapper;

    /** 在线用户连接映射：userId -> WebSocketSession */
    private static final Map<Long, WebSocketSession> ONLINE_USERS = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        // 存储用户ID到会话属性
        session.getAttributes().put("userId", userId);

        // 维护在线用户连接映射
        ONLINE_USERS.put(userId, session);

        // 更新在线状态
        onlineStatusService.updateOnlineStatus(userId, true);

        log.info("WebSocket 连接建立: userId={}, sessionId={}", userId, session.getId());

        // 通知其他用户该用户上线
        broadcastOnlineStatus(userId, true);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            String type = jsonNode.path("type").asText();
            JsonNode data = jsonNode.path("data");

            switch (type) {
                case "CHAT" -> handleChatMessage(userId, data);
                case "ONLINE" -> onlineStatusService.heartbeat(userId);
                case "READ" -> handleReadMessage(userId, data);
                default -> log.warn("未知的 WebSocket 消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 消息失败: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            ONLINE_USERS.remove(userId);
            onlineStatusService.updateOnlineStatus(userId, false);
            log.info("WebSocket 连接关闭: userId={}, status={}", userId, status);

            // 通知其他用户该用户下线
            broadcastOnlineStatus(userId, false);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("WebSocket 传输错误: userId={}, error={}", userId, exception.getMessage());

        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    // ========== 内部方法 ==========

    /**
     * 从 WebSocket 连接中提取用户ID
     * 优先从 Cookie 读取 accessToken，其次从 URL 参数读取（兼容旧版本）
     */
    private Long extractUserId(WebSocketSession session) {
        try {
            String token = null;

            // 优先从握手时的 HTTP 请求属性中获取 Cookie token（已由 WebSocketConfig 拦截器验证）
            if (session.getAttributes().containsKey("handshakeCookies")) {
                Object cookiesObj = session.getAttributes().get("handshakeCookies");
                if (cookiesObj instanceof jakarta.servlet.http.Cookie[] cookies) {
                    for (jakarta.servlet.http.Cookie cookie : cookies) {
                        if ("accessToken".equals(cookie.getName())) {
                            token = cookie.getValue();
                            break;
                        }
                    }
                }
            }

            // Cookie 中无 token，尝试从 URL 参数读取（兼容旧版本）
            if (token == null || token.isEmpty()) {
                URI uri = session.getUri();
                if (uri != null) {
                    String query = uri.getQuery();
                    if (query != null && query.contains("token=")) {
                        for (String param : query.split("&")) {
                            if (param.startsWith("token=")) {
                                token = param.substring(6);
                                break;
                            }
                        }
                    }
                }
            }

            if (token == null || token.isEmpty()) {
                return null;
            }

            // 验证 JWT Token
            if (!jwtUtil.validateAccessToken(token)) {
                log.warn("WebSocket JWT 验证失败");
                return null;
            }

            return jwtUtil.getUserIdFromAccessToken(token);
        } catch (Exception e) {
            log.error("提取 WebSocket 用户ID失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 处理聊天消息
     */
    private void handleChatMessage(Long senderId, JsonNode data) {
        Long receiverId = data.path("receiverId").asLong();
        String content = data.path("content").asText();
        String messageType = data.path("messageType").asText("text");

        // 仅对文本消息做 XSS 过滤（图片消息 content 是 URL，无需过滤）
        if (!"image".equals(messageType)) {
            content = HtmlWhitelistFilter.escapePlainText(content);

            // 敏感词检查（仅文本）
            if (sensitiveWordUtil.containsSensitiveWord(content)) {
                log.warn("消息包含敏感词，拒绝发送: senderId={}, receiverId={}", senderId, receiverId);
                return;
            }
        }

        // 持久化消息
        MessageSendRequest request = new MessageSendRequest();
        request.setReceiverId(receiverId);
        request.setContent(content);
        request.setType(messageType);
        MessageVO messageVO = messageService.sendMessage(senderId, request);

        // 查询发送者信息
        User sender = userMapper.selectById(senderId);
        String senderNickname = sender != null ? sender.getNickname() : "";
        String senderAvatar = sender != null ? sender.getAvatar() : "";

        // 转发消息给目标用户
        WebSocketSession receiverSession = ONLINE_USERS.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            try {
                Map<String, Object> message = Map.of(
                        "type", "CHAT",
                        "data", Map.of(
                                "senderId", senderId,
                                "content", content,
                                "messageType", messageType,
                                "senderNickname", senderNickname,
                                "senderAvatar", senderAvatar,
                                "createdAt", messageVO.getCreatedAt().toString()
                        )
                );
                String json = objectMapper.writeValueAsString(message);
                receiverSession.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("转发 WebSocket 消息失败: senderId={}, receiverId={}", senderId, receiverId);
            }
        }
    }

    /**
     * 处理已读消息
     */
    private void handleReadMessage(Long userId, JsonNode data) {
        Long targetUserId = data.path("targetUserId").asLong();

        // 通知发送者消息已读
        WebSocketSession targetSession = ONLINE_USERS.get(targetUserId);
        if (targetSession != null && targetSession.isOpen()) {
            try {
                Map<String, Object> message = Map.of(
                        "type", "READ",
                        "data", Map.of("readBy", userId)
                );
                String json = objectMapper.writeValueAsString(message);
                targetSession.sendMessage(new TextMessage(json));
            } catch (IOException e) {
                log.error("发送已读通知失败: userId={}, targetUserId={}", userId, targetUserId);
            }
        }
    }

    /**
     * 广播用户在线/离线状态
     */
    private void broadcastOnlineStatus(Long userId, boolean online) {
        String type = online ? "ONLINE" : "OFFLINE";
        Map<String, Object> message = Map.of(
                "type", type,
                "data", Map.of("userId", userId)
        );

        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);

            for (Map.Entry<Long, WebSocketSession> entry : ONLINE_USERS.entrySet()) {
                if (!entry.getKey().equals(userId) && entry.getValue().isOpen()) {
                    try {
                        entry.getValue().sendMessage(textMessage);
                    } catch (IOException e) {
                        log.warn("广播在线状态失败: targetUserId={}", entry.getKey());
                    }
                }
            }
        } catch (Exception e) {
            log.error("广播在线状态失败: userId={}", userId);
        }
    }

    /**
     * 获取在线用户数
     */
    public static int getOnlineCount() {
        return ONLINE_USERS.size();
    }

    /**
     * 检查用户是否在线
     */
    public static boolean isUserOnline(Long userId) {
        WebSocketSession session = ONLINE_USERS.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 向指定用户发送消息（供外部调用，如 MQ 消费者）
     */
    public static void sendToUser(Long userId, TextMessage message) {
        WebSocketSession session = ONLINE_USERS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                log.error("发送 WebSocket 消息失败: userId={}", userId);
            }
        }
    }
}
