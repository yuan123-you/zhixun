package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.social.MessageSendRequest;
import com.zhixun.service.MessageService;
import com.zhixun.vo.ConversationVO;
import com.zhixun.vo.MessageVO;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.validation.Valid;
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
 * 私信控制器
 */
@RestController
@RequestMapping("/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SecurityUtil securityUtil;

    /**
     * 发送私信
     */
    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    @SentinelResource(value = "message-send", blockHandler = "sendMessageBlockHandler", blockHandlerClass = MessageController.BlockHandlers.class)
    public R<MessageVO> sendMessage(@Valid @RequestBody MessageSendRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(messageService.sendMessage(userId, request));
    }

    /**
     * 会话列表
     */
    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ConversationVO>> getConversations(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(messageService.getConversations(userId, page, pageSize));
    }

    /**
     * 与某用户的私信记录
     */
    @GetMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<MessageVO>> getMessages(
            @PathVariable Long userId,
            @RequestParam(required = false) Long beforeId,
            @RequestParam(defaultValue = "20") Integer limit) {
        Long currentUserId = securityUtil.getCurrentUserId();
        return R.ok(messageService.getMessages(currentUserId, userId, beforeId, limit));
    }

    /**
     * 标记与某用户的私信已读
     */
    @PutMapping("/{userId}/read")
    @PreAuthorize("isAuthenticated()")
    public R<Void> markAsRead(@PathVariable Long userId) {
        Long currentUserId = securityUtil.getCurrentUserId();
        messageService.markAsRead(currentUserId, userId);
        return R.ok();
    }

    /**
     * 未读私信数
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public R<Map<String, Object>> getUnreadCount() {
        Long userId = securityUtil.getCurrentUserId();
        Integer count = messageService.getUnreadCount(userId);
        return R.ok(Map.of("unread_count", count));
    }

    /**
     * Sentinel 限流降级处理
     */
    public static class BlockHandlers {
        public static R<MessageVO> sendMessageBlockHandler(MessageSendRequest request, BlockException e) {
            return R.fail(429, "私信发送过于频繁，请稍后重试");
        }
    }
}
