package com.zhixun.controller;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.social.MessageSendRequest;
import com.zhixun.service.MessageService;
import com.zhixun.vo.ConversationVO;
import com.zhixun.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
     * 前端: GET /conversations/{userId}/messages?page=1&pageSize=30
     */
    @GetMapping("/{userId}/messages")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<MessageVO>> getMessages(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Long currentUserId = securityUtil.getCurrentUserId();
        return R.ok(messageService.getMessages(currentUserId, userId, page, pageSize));
    }

    /**
     * 发送私信
     * 前端: POST /conversations/{userId}/messages
     */
    @PostMapping("/{userId}/messages")
    @PreAuthorize("isAuthenticated()")
    public R<MessageVO> sendMessage(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        Long senderId = securityUtil.getCurrentUserId();
        // 兼容 content 字段可能是 String / Number / Boolean，统一转为 String
        Object rawContent = body == null ? null : body.get("content");
        String content = rawContent == null ? null : String.valueOf(rawContent);

        log.info("私信发送请求: senderId={}, receiverId={}, contentLength={}",
                senderId, userId, content != null ? content.length() : 0);

        // 参数校验
        if (content == null || content.trim().isEmpty()) {
            log.warn("私信发送失败: 内容为空, senderId={}, receiverId={}", senderId, userId);
            return R.fail(ErrorCode.BAD_REQUEST, "消息内容不能为空");
        }
        if (content.length() > 1000) {
            log.warn("私信发送失败: 内容超长, senderId={}, receiverId={}, length={}", senderId, userId, content.length());
            return R.fail(ErrorCode.BAD_REQUEST, "消息内容最长1000个字符");
        }

        try {
            MessageSendRequest request = new MessageSendRequest();
            request.setReceiverId(userId);
            request.setContent(content.trim());

            // 读取消息类型（text/image），默认 text
            Object rawType = body != null ? body.get("type") : null;
            String msgType = "text";
            if (rawType != null) {
                String t = String.valueOf(rawType).trim().toLowerCase();
                if ("image".equals(t) || "1".equals(t)) {
                    msgType = "image";
                }
            }
            request.setType(msgType);

            MessageVO result = messageService.sendMessage(senderId, request);
            log.info("私信发送成功: senderId={}, receiverId={}, messageId={}", senderId, userId, result.getId());
            return R.ok(result);
        } catch (BusinessException e) {
            log.warn("私信发送业务异常: senderId={}, receiverId={}, error={}", senderId, userId, e.getMessage());
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("私信发送系统异常: senderId={}, receiverId={}", senderId, userId, e);
            return R.fail(ErrorCode.BUSINESS_ERROR, "消息发送失败，请稍后重试");
        }
    }

    /**
     * 发送AI助手消息（私信场景）
     * 前端: POST /conversations/{userId}/ai
     */
    @PostMapping("/{userId}/ai")
    @PreAuthorize("isAuthenticated()")
    public R<MessageVO> sendAIMessage(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        Long senderId = securityUtil.getCurrentUserId();
        String question = (String) body.get("question");
        if (question == null || question.trim().isEmpty()) {
            return R.fail(ErrorCode.BAD_REQUEST, "提问内容不能为空");
        }
        if (question.length() > 1000) {
            return R.fail(ErrorCode.BAD_REQUEST, "提问内容最长1000个字符");
        }
        try {
            MessageVO result = messageService.sendAIMessage(senderId, userId, question.trim());
            return R.ok(result);
        } catch (BusinessException e) {
            log.warn("AI私信业务异常: senderId={}, targetUserId={}, error={}", senderId, userId, e.getMessage());
            return R.fail(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("AI私信系统异常: senderId={}, targetUserId={}", senderId, userId, e);
            return R.fail(ErrorCode.BUSINESS_ERROR, "AI回复失败，请稍后重试");
        }
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
