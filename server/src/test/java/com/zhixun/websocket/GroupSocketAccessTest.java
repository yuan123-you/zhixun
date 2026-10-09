package com.zhixun.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.util.JwtUtil;
import com.zhixun.entity.GroupInfo;
import com.zhixun.entity.GroupMember;
import com.zhixun.mapper.GroupMapper;
import com.zhixun.mapper.GroupMemberMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.GroupService;
import com.zhixun.service.NotificationService;
import java.net.URI;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.web.socket.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GroupSocketAccessTest {
    private Set<Long> members;
    private boolean connectedActorIsMember, normal;
    private GroupMapper groups;
    private GroupMemberMapper memberMapper;
    private GroupChatWebSocketHandler handler;
    private final List<WebSocketSession> sessions = new ArrayList<>();

    @BeforeEach void setUp() {
        members = new HashSet<>(Set.of(7L, 8L)); normal = true;
        groups = mock(GroupMapper.class); memberMapper = mock(GroupMemberMapper.class);
        when(groups.selectById(2L)).thenAnswer(inv -> { GroupInfo group = new GroupInfo(); group.setId(2L); group.setStatus(normal ? 0 : 1); return group; });
        when(memberMapper.selectCount(any())).thenAnswer(inv -> connectedActorIsMember ? 1L : 0L);
        when(memberMapper.selectList(any())).thenAnswer(inv -> members.stream().map(id -> {
            GroupMember member = new GroupMember(); member.setGroupId(2L); member.setUserId(id); return member;
        }).toList());
        handler = new GroupChatWebSocketHandler(mock(GroupService.class), mock(JwtUtil.class), new ObjectMapper(),
                mock(NotificationService.class), groups, mock(UserMapper.class),
                new com.zhixun.service.GroupChatAccess(groups, memberMapper));
        handler.initializeChatAccess();
    }
    private WebSocketSession connect(long userId) throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-" + userId);
        when(session.getAttributes()).thenReturn(new HashMap<>(Map.of("userId", userId)));
        when(session.getUri()).thenReturn(URI.create("ws://unit.invalid/ws/group-chat?groupId=2"));
        when(session.isOpen()).thenReturn(true);
        sessions.add(session); connectedActorIsMember = members.contains(userId);
        handler.afterConnectionEstablished(session); clearInvocations(session); return session;
    }
    @AfterEach void cleanup() throws Exception {
        for (WebSocketSession session : sessions) handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        handler.releaseChatAccess();
    }
    @Test void nonMemberCannotSubscribeToGroupReplies() throws Exception {
        members.remove(8L);
        WebSocketSession allowed = connect(7), denied = connect(8);
        GroupChatWebSocketHandler.broadcastAIMessage(2L, "{\"type\":\"CHAT\"}");
        verify(allowed).sendMessage(any(TextMessage.class)); verify(denied, never()).sendMessage(any());
    }
    @Test void leavingAfterConnectionRemovesTheRightToReceiveNormalChat() throws Exception {
        WebSocketSession allowed = connect(7), formerMember = connect(8); members.remove(8L);
        GroupChatWebSocketHandler.broadcastToGroup(2L, "{\"type\":\"CHAT\"}");
        verify(allowed).sendMessage(any(TextMessage.class)); verify(formerMember, never()).sendMessage(any());
    }
    @Test void dismissedGroupCannotContinueBroadcastingContentToOldConnections() throws Exception {
        WebSocketSession first = connect(7), second = connect(8); normal = false;
        GroupChatWebSocketHandler.broadcastAIMessage(2L, "{\"type\":\"CHAT\"}");
        verify(first, never()).sendMessage(any()); verify(second, never()).sendMessage(any());
    }
    @Test void errorReplyIsDeliveredOnlyToItsRequester() throws Exception {
        WebSocketSession requester = connect(7), other = connect(8);
        GroupChatWebSocketHandler.sendToUser(2L, 7L, "{\"type\":\"ERROR\",\"data\":{\"message\":\"Unavailable\"}}");
        verify(requester).sendMessage(any(TextMessage.class)); verify(other, never()).sendMessage(any());
    }
}
