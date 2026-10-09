package com.zhixun.service.impl;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.entity.User;
import com.zhixun.mapper.UserMapper;
import com.zhixun.mapper.UserMessageMapper;
import com.zhixun.service.AIService;
import com.zhixun.vo.AIResponseVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageAiResultTest {
    @Mock UserMapper users;
    @Mock UserMessageMapper messages;
    @Mock AIService ai;
    @InjectMocks MessageServiceImpl service;

    @Test void blankFinalResponseCannotBecomeAPersistedFallbackMessage() {
        User receiver = new User(); receiver.setId(8L);
        when(users.selectById(anyLong())).thenReturn(receiver);
        AIResponseVO empty = new AIResponseVO(); empty.setContent(" "); empty.setUsage("tokens=0");
        when(ai.generateText(any())).thenReturn(empty);
        var failure = assertThrows(BusinessException.class, () -> service.sendAIMessage(7L, 8L, "Question"));
        assertEquals(503, failure.getCode());
        verify(messages, never()).insert(any());
    }

    @Test void normalizedErrorMarkerCannotEnterConversationHistory() {
        User receiver = new User(); receiver.setId(8L);
        when(users.selectById(anyLong())).thenReturn(receiver);
        AIResponseVO error = new AIResponseVO(); error.setContent("Provider failed"); error.setUsage(" ERROR: unavailable");
        when(ai.generateText(any())).thenReturn(error);
        assertThrows(BusinessException.class, () -> service.sendAIMessage(7L, 8L, "Question"));
        verify(messages, never()).insert(any());
    }
}
