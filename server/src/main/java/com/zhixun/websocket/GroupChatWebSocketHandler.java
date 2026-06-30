package com.zhixun.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.util.JwtUtil;
import com.zhixun.dto.group.GroupMessageRequest;
import com.zhixun.entity.GroupInfo;
import com.zhixun.entity.User;
import com.zhixun.mapper.GroupMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.GroupService;
import com.zhixun.service.NotificationService;
import com.zhixun.vo.GroupMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import jakarta.servlet.http.Cookie;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 群组聊天 WebSocket 处理器
 * 连接路径: /ws/group-chat?token=xxx&groupId=xxx
 *
 * 消息格式（客户端→服务端）:
 *   {"type":"CHAT","data":{"groupId":1,"content":"hello","messageType":"text","mentionedUserIds":[1,2]}}
 *   {"type":"JOIN","data":{"groupId":1}}
 *
 * 消息格式（服务端→客户端）:
 *   {"type":"CHAT","data":{...GroupMessageVO}}
 *   {"type":"SYSTEM","data":{"content":"xxx加入了群组"}}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupChatWebSocketHandler extends TextWebSocketHandler {

    private final GroupService groupService;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final GroupMapper groupMapper;
    private final UserMapper userMapper;

    /** groupId -> (sessionId -> WebSocketSession) */
    private static final Map<Long, Map<String, WebSocketSession>> GROUP_SESSIONS = new ConcurrentHashMap<>();

    /** sessionId -> userId */
    private static final Map<String, Long> SESSION_USER = new ConcurrentHashMap<>();

    /** sessionId -> 当前加入的 groupId */
    private static final Map<String, Long> SESSION_GROUP = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        session.getAttributes().put("userId", userId);
        SESSION_USER.put(session.getId(), userId);

        // 如果连接时已携带 groupId，自动加入群组房间
        Long groupId = extractGroupId(session);
        if (groupId != null) {
            joinGroupRoom(session, groupId, userId);
        }

        log.info("群组WS连接建立: userId={}, groupId={}, sessionId={}", userId, groupId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = SESSION_USER.get(session.getId());
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        try {
            JsonNode json = objectMapper.readTree(message.getPayload());
            String type = json.path("type").asText();
            JsonNode data = json.path("data");

            switch (type) {
                case "CHAT" -> handleChatMessage(session, userId, data);
                case "JOIN" -> {
                    Long groupId = data.path("groupId").asLong();
                    joinGroupRoom(session, groupId, userId);
                }
                default -> log.warn("未知群组WS消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("群组WS消息处理失败: userId={}, error={}", userId, e.getMessage());
            sendError(session, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = SESSION_USER.remove(session.getId());
        Long groupId = SESSION_GROUP.remove(session.getId());
        if (groupId != null) {
            Map<String, WebSocketSession> room = GROUP_SESSIONS.get(groupId);
            if (room != null) {
                room.remove(session.getId());
                if (room.isEmpty()) {
                    GROUP_SESSIONS.remove(groupId);
                }
            }
        }
        log.info("群组WS连接关闭: userId={}, groupId={}, status={}", userId, groupId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("群组WS传输错误: {}", exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    // ========== 内部方法 ==========

    private void handleChatMessage(WebSocketSession session, Long senderId, JsonNode data) throws IOException {
        Long groupId = data.path("groupId").asLong();
        String content = data.path("content").asText();
        String messageType = data.path("messageType").asText("text");

        if (content == null || content.trim().isEmpty()) {
            sendError(session, "消息内容不能为空");
            return;
        }

        GroupMessageRequest req = new GroupMessageRequest();
        req.setGroupId(groupId);
        req.setContent(content.trim());
        req.setMessageType(messageType);

        // 解析 mentionedUserIds
        JsonNode mentionNode = data.path("mentionedUserIds");
        if (mentionNode.isArray() && mentionNode.size() > 0) {
            List<Long> mentionedIds = new ArrayList<>();
            for (JsonNode idNode : mentionNode) {
                mentionedIds.add(idNode.asLong());
            }
            req.setMentionedUserIds(mentionedIds);
        }

        try {
            GroupMessageVO msgVO = groupService.sendMessage(senderId, req);
            // 广播已由 groupService.sendMessage() 统一处理，此处不再重复广播

            // 发送@提及通知（异步，不阻塞消息发送）
            if (msgVO.getMentionedUserIds() != null && !msgVO.getMentionedUserIds().isEmpty()) {
                sendMentionNotifications(senderId, groupId, msgVO);
            }
        } catch (Exception e) {
            sendError(session, e.getMessage());
        }
    }

    private void sendMentionNotifications(Long senderId, Long groupId, GroupMessageVO msgVO) {
        try {
            // 获取群组名称
            GroupInfo group = groupMapper.selectById(groupId);
            String groupName = group != null ? group.getName() : "未知群组";

            // 获取发送者名称
            User sender = userMapper.selectById(senderId);
            String senderName = sender != null ? sender.getNickname() : "未知用户";

            for (Long targetUserId : msgVO.getMentionedUserIds()) {
                if (targetUserId.equals(senderId)) continue; // 不给自己发通知
                try {
                    notificationService.createNotification(
                            targetUserId,
                            7, // MENTION type
                            "有人在群组中@了你",
                            senderName + " 在群组「" + groupName + "」中@了你",
                            groupId
                    );
                } catch (Exception e) {
                    log.warn("发送@提及通知失败: targetUserId={}, error={}", targetUserId, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("发送@提及通知异常: groupId={}, error={}", groupId, e.getMessage());
        }
    }

    private void joinGroupRoom(WebSocketSession session, Long groupId, Long userId) {
        // 先离开旧房间
        Long oldGroupId = SESSION_GROUP.get(session.getId());
        if (oldGroupId != null && !oldGroupId.equals(groupId)) {
            Map<String, WebSocketSession> oldRoom = GROUP_SESSIONS.get(oldGroupId);
            if (oldRoom != null) {
                oldRoom.remove(session.getId());
                if (oldRoom.isEmpty()) GROUP_SESSIONS.remove(oldGroupId);
            }
        }

        SESSION_GROUP.put(session.getId(), groupId);
        GROUP_SESSIONS.computeIfAbsent(groupId, k -> new ConcurrentHashMap<>()).put(session.getId(), session);
        log.info("用户{}加入群组房间{}", userId, groupId);
    }

    /**
     * 公开静态方法：向群组广播AI助手回复消息
     * 供 GroupServiceImpl 异步生成AI回复后调用
     */
    public static void broadcastAIMessage(Long groupId, String jsonMessage) {
        Map<String, WebSocketSession> room = GROUP_SESSIONS.get(groupId);
        if (room == null) return;
        TextMessage textMsg = new TextMessage(jsonMessage);
        room.forEach((sid, s) -> {
            if (s.isOpen()) {
                try {
                    s.sendMessage(textMsg);
                } catch (IOException e) {
                    log.error("AI回复广播失败: groupId={}, sessionId={}", groupId, sid, e);
                }
            }
        });
    }

    /**
     * 公开静态方法：向群组广播消息（不排除任何会话）
     * 供 GroupServiceImpl HTTP 发送消息后调用
     */
    public static void broadcastToGroup(Long groupId, String message) {
        broadcastToGroup(groupId, message, null);
    }

    /**
     * 向群组广播消息（可排除指定会话）
     */
    private static void broadcastToGroup(Long groupId, String message, String excludeSessionId) {
        Map<String, WebSocketSession> room = GROUP_SESSIONS.get(groupId);
        if (room == null) return;
        TextMessage textMsg = new TextMessage(message);
        room.forEach((sid, s) -> {
            if (!sid.equals(excludeSessionId) && s.isOpen()) {
                try {
                    s.sendMessage(textMsg);
                } catch (IOException e) {
                    log.error("群组广播失败: sessionId={}", sid, e);
                }
            }
        });
    }

    private Long extractUserId(WebSocketSession session) {
        // 优先从握手拦截器设置的 attributes 中获取 userId（Cookie 鉴权场景）
        Object userIdAttr = session.getAttributes().get("userId");
        if (userIdAttr instanceof Long userId) {
            return userId;
        }

        // 降级：从 token 解析（URL 参数鉴权场景）
        String token = extractToken(session);
        if (token == null) return null;
        if (!jwtUtil.validateAccessToken(token)) return null;
        return jwtUtil.getUserIdFromAccessToken(token);
    }

    private Long extractGroupId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) return null;
        for (String param : uri.getQuery().split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "groupId".equals(kv[0])) {
                try { return Long.parseLong(kv[1]); } catch (NumberFormatException e) { return null; }
            }
        }
        return null;
    }

    private String extractToken(WebSocketSession session) {
        // 方式1: 从握手拦截器保存的 Cookie 中读取 accessToken
        Object cookiesObj = session.getAttributes().get("handshakeCookies");
        if (cookiesObj instanceof Cookie[] cookies) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 方式2: 从 URL 参数读取（兼容旧版本）
        URI uri = session.getUri();
        if (uri == null || uri.getQuery() == null) return null;
        for (String param : uri.getQuery().split("&")) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && "token".equals(kv[0])) {
                return kv[1];
            }
        }
        return null;
    }

    private void sendError(WebSocketSession session, String message) {
        if (!session.isOpen()) return;
        try {
            String json = objectMapper.writeValueAsString(Map.of("type", "ERROR", "data", Map.of("message", message)));
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("发送WS错误消息失败", e);
        }
    }
}
