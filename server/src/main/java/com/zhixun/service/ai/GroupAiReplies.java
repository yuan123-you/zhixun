package com.zhixun.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.dto.ai.AIWriteRequest;
import com.zhixun.entity.GroupMessage;
import com.zhixun.mapper.GroupMessageMapper;
import com.zhixun.service.AIService;
import com.zhixun.service.GroupChatAccess;
import com.zhixun.vo.GroupMessageVO;
import com.zhixun.websocket.GroupChatWebSocketHandler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Receive -> execute -> validate -> guarded persist -> notify, behind the existing group API. */
@Slf4j
@Service
public class GroupAiReplies {
    private final GroupChatAccess access;
    private final GroupMessageMapper messages;
    private final AIService ai;
    private final ObjectMapper json;
    private final GroupAiExecutor executor;

    public GroupAiReplies(GroupChatAccess access, GroupMessageMapper messages,
                          AIService ai, ObjectMapper json, GroupAiExecutor executor) {
        this.access = access; this.messages = messages;
        this.ai = ai; this.json = json; this.executor = executor;
    }

    public void submit(Long groupId, Long userId, String question) {
        if (groupId == null || groupId <= 0 || question == null || question.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "群组和非空问题不能为空");
        }
        if (userId == null || userId <= 0) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        access.requireMember(groupId, userId);
        String prompt = question.trim();
        try {
            executor.execute(() -> complete(groupId, userId, prompt));
        } catch (RejectedExecutionException e) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务繁忙，请稍后重试");
        }
    }

    private void complete(Long groupId, Long userId, String question) {
        try {
            access.requireMember(groupId, userId); // Authorization may change while waiting in the queue.
            AIWriteRequest request = new AIWriteRequest(); request.setPrompt(question); request.setMode("chat");
            String content = AiResults.requireFinal(ai.generateText(request)).getContent();
            access.requireMember(groupId, userId);
            GroupMessage message = new GroupMessage();
            message.setGroupId(groupId); message.setSenderId(0L);
            message.setSenderName("AI助手"); message.setSenderAvatar("");
            message.setContent(content); message.setMessageType("ai_reply"); message.setCreatedAt(LocalDateTime.now());
            if (messages.insertAiIfActive(message, userId) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "AI 请求的群组权限已变更");
            }
            GroupMessageVO view = new GroupMessageVO();
            view.setId(message.getId()); view.setGroupId(groupId); view.setSenderId(0L);
            view.setSenderName("AI助手"); view.setSenderAvatar(""); view.setContent(content);
            view.setMessageType("ai_reply"); view.setCreatedAt(message.getCreatedAt()); view.setMentionedUserIds(List.of());
            GroupChatWebSocketHandler.broadcastAIMessage(groupId,
                    json.writeValueAsString(Map.of("type", "CHAT", "data", view)));
        } catch (Exception e) {
            log.warn("群组 AI 请求失败: groupId={}, userId={}, reason={}", groupId, userId, e.getClass().getSimpleName());
            reportFailure(groupId, userId);
        }
    }

    private void reportFailure(Long groupId, Long userId) {
        try {
            String error = json.writeValueAsString(Map.of("type", "ERROR", "data",
                    Map.of("message", "AI 请求未能完成，请稍后重试")));
            GroupChatWebSocketHandler.sendToUser(groupId, userId, error);
        } catch (Exception e) {
            log.warn("群组 AI 错误通知失败: groupId={}, userId={}", groupId, userId);
        }
    }
}
