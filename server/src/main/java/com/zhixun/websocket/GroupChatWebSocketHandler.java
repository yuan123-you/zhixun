package com.zhixun.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.service.GroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GroupChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private GroupService groupService;

    private static final Map<Long, Set<WebSocketSession>> groupSessions = new ConcurrentHashMap<>();
    private static final Map<String, Long> sessionUserMap = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String userId = getUserId(session);
        if (userId != null) {
            sessionUserMap.put(session.getId(), Long.parseLong(userId));
            log.info("Group chat WS connected: user={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        Long senderId = sessionUserMap.get(session.getId());
        if (senderId == null) return;

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            Long groupId = Long.valueOf(payload.get("groupId").toString());
            String content = (String) payload.get("content");

            var msgVO = groupService.sendMessage(senderId, new com.zhixun.dto.group.GroupMessageRequest() {{
                setGroupId(groupId);
                setContent(content);
            }});

            String response = objectMapper.writeValueAsString(msgVO);
            broadcastToGroup(groupId, response, session.getId());
        } catch (Exception e) {
            log.error("Group chat message error", e);
            session.sendMessage(new TextMessage("{\"error\":\"" + e.getMessage() + "\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = sessionUserMap.remove(session.getId());
        groupSessions.values().forEach(sessions -> sessions.remove(session));
        log.info("Group chat WS disconnected: user={}", userId);
    }

    public void registerSession(Long groupId, WebSocketSession session) {
        groupSessions.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    private void broadcastToGroup(Long groupId, String message, String excludeSessionId) {
        Set<WebSocketSession> sessions = groupSessions.get(groupId);
        if (sessions != null) {
            sessions.forEach(s -> {
                if (!s.getId().equals(excludeSessionId) && s.isOpen()) {
                    try { s.sendMessage(new TextMessage(message)); } catch (IOException e) { log.error("broadcast error", e); }
                }
            });
        }
    }

    private String getUserId(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2 && "userId".equals(kv[0])) return kv[1];
            }
        }
        return null;
    }
}