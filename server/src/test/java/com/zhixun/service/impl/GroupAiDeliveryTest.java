package com.zhixun.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.util.SensitiveWordUtil;
import com.zhixun.entity.GroupInfo;
import com.zhixun.entity.GroupMessage;
import com.zhixun.mapper.*;
import com.zhixun.service.AIService;
import com.zhixun.vo.AIResponseVO;
import com.zhixun.websocket.GroupChatWebSocketHandler;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GroupAiDeliveryTest {
    private boolean member = true, normal = true, reject = false, zeroWrite = false;
    private Runnable beforeWrite = () -> {};
    private List<Runnable> jobs;
    private List<GroupMessage> saved;
    private List<String> broadcasts;
    private List<Object[]> failures;
    private GroupMapper groups;
    private GroupMemberMapper members;
    private GroupMessageMapper messages;
    private AIService ai;
    private GroupServiceImpl service;
    private ObjectMapper json;
    private MockedStatic<GroupChatWebSocketHandler> websocket;

    @BeforeEach void setUp() {
        jobs = new ArrayList<>(); saved = new ArrayList<>(); broadcasts = new ArrayList<>(); failures = new ArrayList<>();
        groups = mock(GroupMapper.class); members = mock(GroupMemberMapper.class); ai = mock(AIService.class);
        json = new ObjectMapper().findAndRegisterModules();
        when(groups.selectById(2L)).thenAnswer(inv -> {
            GroupInfo group = new GroupInfo(); group.setId(2L); group.setStatus(normal ? 0 : 1); return group;
        });
        when(members.selectCount(any())).thenAnswer(inv -> member ? 1L : 0L);
        messages = mock(GroupMessageMapper.class, inv -> {
            if (inv.getMethod().getName().startsWith("insert")) {
                GroupMessage message = inv.getArgument(0);
                beforeWrite.run();
                if (zeroWrite || (inv.getMethod().getName().equals("insertAiIfActive") && (!member || !normal))) return 0;
                message.setId(99L); message.setCreatedAt(LocalDateTime.now()); saved.add(message); return 1;
            }
            return Answers.RETURNS_DEFAULTS.answer(inv);
        });
        var replies = new com.zhixun.service.ai.GroupAiReplies(new com.zhixun.service.GroupChatAccess(groups, members), messages, ai, json, task -> {
            if (reject) throw new RejectedExecutionException("queue full");
            jobs.add(task);
        });
        service = new GroupServiceImpl(groups, members, messages, mock(GroupJoinRequestMapper.class),
                mock(UserMapper.class), replies, mock(SensitiveWordUtil.class), json);
        websocket = mockStatic(GroupChatWebSocketHandler.class, inv -> {
            if (inv.getMethod().getName().equals("broadcastAIMessage")) broadcasts.add(inv.getArgument(1));
            if (inv.getMethod().getName().equals("sendToUser")) failures.add(inv.getArguments());
            return null;
        });
        when(ai.generateText(any())).thenReturn(response("Valid final", "tokens=5"));
    }
    @AfterEach void tearDown() { websocket.close(); }

    private AIResponseVO response(String content, String usage) {
        AIResponseVO vo = new AIResponseVO(); vo.setContent(content); vo.setUsage(usage); return vo;
    }
    private void runJob() { assertEquals(1, jobs.size()); jobs.remove(0).run(); }

    @ParameterizedTest @ValueSource(strings = {"error: missing_key", " ERROR: unavailable"})
    void explicitProviderFailureNeverBecomesNormalGroupContent(String usage) {
        when(ai.generateText(any())).thenReturn(response("Raw provider failure", usage));
        service.sendAIMessage(2L, 7L, "Question"); runJob();
        assertTrue(saved.isEmpty()); assertTrue(broadcasts.isEmpty());
        assertEquals(1, failures.size()); assertEquals(2L, failures.get(0)[0]); assertEquals(7L, failures.get(0)[1]);
    }
    @Test void blankFinalContentIsNotPersisted() {
        when(ai.generateText(any())).thenReturn(response(" ", "tokens=0"));
        service.sendAIMessage(2L, 7L, "Question"); runJob(); assertTrue(saved.isEmpty()); assertTrue(broadcasts.isEmpty());
    }
    @Test void validReplyUsesTheExistingChatWireShape() throws Exception {
        service.sendAIMessage(2L, 7L, "Question"); runJob();
        assertEquals(1, saved.size()); assertEquals("Valid final", saved.get(0).getContent());
        assertEquals(1, broadcasts.size()); var packet = json.readTree(broadcasts.get(0));
        assertEquals("CHAT", packet.path("type").asText()); assertEquals("ai_reply", packet.at("/data/messageType").asText());
        assertEquals(0, packet.at("/data/senderId").asLong()); assertEquals(99, packet.at("/data/id").asLong());
    }
    @Test void membershipRevokedWhileQueuedPreventsProviderWork() {
        service.sendAIMessage(2L, 7L, "Question"); member = false; runJob();
        verify(ai, never()).generateText(any()); assertTrue(saved.isEmpty());
    }
    @Test void memberLeavingDuringGenerationPreventsPersistence() {
        when(ai.generateText(any())).thenAnswer(inv -> { member = false; return response("Valid final", "tokens=5"); });
        service.sendAIMessage(2L, 7L, "Question"); runJob(); assertTrue(saved.isEmpty()); assertTrue(broadcasts.isEmpty());
    }
    @Test void groupDismissedWhileQueuedPreventsProviderWork() {
        service.sendAIMessage(2L, 7L, "Question"); normal = false; runJob();
        verify(ai, never()).generateText(any()); assertTrue(saved.isEmpty());
    }
    @Test void finalPermissionRaceCannotSaveOrBroadcastAReply() {
        beforeWrite = () -> member = false;
        service.sendAIMessage(2L, 7L, "Question"); runJob(); assertTrue(saved.isEmpty()); assertTrue(broadcasts.isEmpty());
    }
    @Test void zeroRowWriteCannotBroadcastAReplyWithNoSavedRecord() {
        zeroWrite = true;
        service.sendAIMessage(2L, 7L, "Question"); runJob(); assertTrue(saved.isEmpty()); assertTrue(broadcasts.isEmpty());
    }
    @Test void fullQueueReturnsExplicitUnavailableRatherThanRawRejection() {
        reject = true;
        var failure = assertThrows(BusinessException.class, () -> service.sendAIMessage(2L, 7L, "Question"));
        assertEquals(503, failure.getCode());
    }
    @Test void blankQuestionIsRejectedBeforeItCanBeQueued() {
        var failure = assertThrows(BusinessException.class, () -> service.sendAIMessage(2L, 7L, " "));
        assertEquals(400, failure.getCode()); assertTrue(jobs.isEmpty());
    }
    @Test void dismissedGroupRejectsTheRequestBeforeQueueing() {
        normal = false;
        assertThrows(BusinessException.class, () -> service.sendAIMessage(2L, 7L, "Question")); assertTrue(jobs.isEmpty());
    }
}
