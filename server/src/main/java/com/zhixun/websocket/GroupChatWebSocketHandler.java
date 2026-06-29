package com.zhixun.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.util.JwtUtil;
import com.zhixun.dto.group.GroupMessageRequest;
import com.zhixun.service.GroupService;
import com.zhixun.vo.GroupMessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 群组聊天 WebSocket 处理器
 * 连接路径: /ws/group-chat?token=xxx&groupId=xxx
 *
 * 消息格式（客户端→服务端）:
 *   {"type":"CHAT","data":{"groupId":1,"content":"hello"}}
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

        if (content == null || content.trim().isEmpty()) {
            sendError(session, "消息内容不能为空");
            return;
        }

        GroupMessageRequest req = new GroupMessageRequest();
        req.setGroupId(groupId);
        req.setContent(content.trim());

        try {
            GroupMessageVO msgVO = groupService.sendMessage(senderId, req);
            String response = objectMapper.writeValueAsString(Map.of("type", "CHAT", "data", msgVO));
            broadcastToGroup(groupId, response, null);
        } catch (Exception e) {
            sendError(session, e.getMessage());
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

    private void broadcastToGroup(Long groupId, String message, String excludeSessionId) {
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
