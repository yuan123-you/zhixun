package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.social.MessageSendRequest;
import com.zhixun.service.MessageService;
import com.zhixun.vo.ConversationVO;
import com.zhixun.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 会话控制器
 * 对齐前端 social.ts 的 /conversations 系列端点
 */
@RestController
@RequestMapping("/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final MessageService messageService;
    private final SecurityUtil securityUtil;

    /**
     * 获取会话列表
     * 前端: GET /conversations
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ConversationVO>> getConversations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(messageService.getConversations(userId, page, pageSize));
    }

    /**
     * 获取与某用户的私信记录
     * 前端: GET /conversations/{userId}/messages
     */
    @GetMapping("/{userId}/messages")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<MessageVO>> getMessages(
            @PathVariable Long userId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "20") Integer limit) {
        Long currentUserId = securityUtil.getCurrentUserId();
        return R.ok(messageService.getMessages(currentUserId, userId, beforeId, limit));
    }

    /**
     * 发送私信
     * 前端: POST /conversations/{userId}/messages
     */
    @PostMapping("/{userId}/messages")
    @PreAuthorize("isAuthenticated()")
    public R<MessageVO> sendMessage(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body) {
        Long senderId = securityUtil.getCurrentUserId();
        String content = body.get("content");
        MessageSendRequest request = new MessageSendRequest();
        request.setReceiverId(userId);
        request.setContent(content);
        return R.ok(messageService.sendMessage(senderId, request));
    }

    /**
     * 标记与某用户的私信已读
     * 前端: PUT /conversations/{userId}/read
     */
    @PutMapping("/{userId}/read")
    @PreAuthorize("isAuthenticated()")
    public R<Void> markAsRead(@PathVariable Long userId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        messageService.markAsRead(currentUserId, userId);
        return R.ok();
    }

    /**
     * 获取未读私信数
     * 前端: GET /conversations/unread-count
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getUnreadCount() {
        Long userId = securityUtil.getCurrentUserId();
        Integer count = messageService.getUnreadCount(userId);
        return R.ok(Map.of("count", count));
    }
}
