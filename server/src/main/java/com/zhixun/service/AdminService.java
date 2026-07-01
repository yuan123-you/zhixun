package com.zhixun.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.result.PageResult;
import com.zhixun.dto.admin.AuditRequest;
import com.zhixun.dto.admin.SensitiveWhitelistRequest;
import com.zhixun.dto.admin.SensitiveWordRequest;
import com.zhixun.dto.admin.UserStatusRequest;
import com.zhixun.entity.LoginLog;
import com.zhixun.entity.SecurityAuditLog;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.CommentVO;
import com.zhixun.vo.ConversationVO;
import com.zhixun.vo.DashboardVO;
import com.zhixun.vo.GroupVO;
import com.zhixun.vo.MessageVO;
import com.zhixun.vo.NotificationVO;
import com.zhixun.vo.UserVO;

import java.util.List;
import java.util.Map;

/**
 * 后台管理服务接口
 */
public interface AdminService {

    /**
     * 待审核作品列表
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 待审核作品分页结果
     */
    PageResult<ArticleVO> getPendingArticles(Integer page, Integer pageSize);

    /**
     * 审核作品
     *
     * @param adminId   管理员ID
     * @param articleId 作品ID
     * @param request   审核请求
     */
    void auditArticle(Long adminId, Long articleId, AuditRequest request);

    /**
     * 数据概览
     *
     * @return 概览数据
     */
    DashboardVO getDashboardOverview();

    /**
     * 数据概览（支持时间维度）
     *
     * @param period 时间周期：daily/weekly/monthly
     * @return 概览数据
     */
    DashboardVO getDashboardOverview(String period);

    /**
     * 用户列表
     *
     * @param keyword  搜索关键词
     * @param role     角色筛选
     * @param status   状态筛选
     * @param page     页码
     * @param pageSize 每页大小
     * @return 用户分页结果
     */
    PageResult<UserVO> getUserList(String keyword, String role, Integer status, Integer page, Integer pageSize);

    /**
     * 封禁/解封用户
     *
     * @param adminId 管理员ID
     * @param userId  用户ID
     * @param request 状态变更请求
     */
    void updateUserStatus(Long adminId, Long userId, UserStatusRequest request);

    /**
     * 敏感词列表
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 敏感词分页结果
     */
    PageResult<com.zhixun.entity.SensitiveWord> getSensitiveWords(Integer page, Integer pageSize);

    /**
     * 添加敏感词
     *
     * @param adminId 管理员ID
     * @param request 敏感词请求
     */
    void addSensitiveWord(Long adminId, SensitiveWordRequest request);

    /**
     * 删除敏感词
     *
     * @param adminId 管理员ID
     * @param wordId 敏感词ID
     */
    void deleteSensitiveWord(Long adminId, Long wordId);

    /**
     * 更新敏感词级别
     *
     * @param adminId 管理员ID
     * @param wordId  敏感词ID
     * @param level   新级别
     */
    void updateSensitiveWordLevel(Long adminId, Long wordId, Integer level);

    /**
     * 评论列表（管理员）
     *
     * @param articleId 作品ID（可选）
     * @param status    评论状态（可选）
     * @param page      页码
     * @param pageSize  每页大小
     * @return 评论分页结果
     */
    PageResult<CommentVO> getCommentList(Long articleId, Integer status, Integer page, Integer pageSize);

    /**
     * 删除评论（管理员）
     *
     * @param adminId   管理员ID
     * @param commentId 评论ID
     */
    void deleteCommentAsAdmin(Long adminId, Long commentId);

    /**
     * 敏感词白名单列表
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 白名单分页结果
     */
    PageResult<com.zhixun.entity.SensitiveWhitelist> getSensitiveWhitelist(Integer page, Integer pageSize);

    /**
     * 添加敏感词白名单
     *
     * @param adminId 管理员ID
     * @param request 白名单请求
     */
    void addSensitiveWhitelist(Long adminId, SensitiveWhitelistRequest request);

    /**
     * 删除敏感词白名单
     *
     * @param adminId 管理员ID
     * @param id     白名单ID
     */
    void deleteSensitiveWhitelist(Long adminId, Long id);

    /**
     * 查询安全审计日志
     *
     * @param eventType 事件类型（可选）
     * @param userId    用户ID（可选）
     * @param ip        请求IP（可选）
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @param page      页码
     * @param pageSize  每页大小
     * @return 审计日志分页结果
     */
    PageResult<SecurityAuditLog> getSecurityAuditLogs(String eventType, Long userId, String ip,
                                                        String startDate, String endDate,
                                                        Integer page, Integer pageSize);

    /**
     * 获取安全审计日志统计
     *
     * @param startDate 开始日期（可选）
     * @param endDate   结束日期（可选）
     * @return 统计数据
     */
    java.util.Map<String, Object> getSecurityAuditStats(String startDate, String endDate);

    /**
     * 群组列表（管理员）
     */
    PageResult<GroupVO> getGroupList(String keyword, Integer status, Integer page, Integer pageSize);

    /**
     * 禁言/恢复群组
     */
    void toggleGroupStatus(Long adminId, Long groupId, Integer status);

    /**
     * 会话列表（管理员）
     */
    PageResult<ConversationVO> getConversationList(Integer page, Integer pageSize);

    /**
     * 会话消息（管理员查看）
     */
    PageResult<MessageVO> getConversationMessages(Long conversationId, Integer page, Integer pageSize);

    /**
     * 删除消息（管理员）
     */
    void deleteMessageAsAdmin(Long adminId, Long messageId);

    /**
     * 登录日志列表
     */
    PageResult<LoginLog> getLoginLogs(String keyword, Integer status, String startDate, String endDate,
                                       Integer page, Integer pageSize);

    /**
     * 通知列表（管理员）
     */
    PageResult<NotificationVO> getNotificationList(Integer type, Integer page, Integer pageSize);

    /**
     * 发送系统通知
     */
    void sendNotification(Long adminId, Integer type, String title, String content,
                          Boolean targetAll, List<Long> targetUserIds);

    /**
     * 用户详情（管理员查看，含登录统计）
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    UserVO getUserDetail(Long userId);

    /**
     * 用户登录历史
     */
    PageResult<LoginLog> getUserLoginHistory(Long userId, Integer page, Integer pageSize);

    /**
     * 群组成员列表（管理员）
     */
    PageResult<com.zhixun.entity.GroupMember> getGroupMembers(Long groupId, Integer page, Integer pageSize);

    /**
     * 群组消息列表（管理员，不限用户）
     */
    PageResult<com.zhixun.vo.GroupMessageVO> getGroupMessagesAsAdmin(Long groupId, Integer page, Integer pageSize);

    /**
     * AI 使用统计
     */
    java.util.Map<String, Object> getAIUsageStats(String period);

    /**
     * 协作列表（管理员）
     */
    PageResult<com.zhixun.vo.CollaboratorVO> getCollaborationList(Integer page, Integer pageSize);

    /**
     * 删除协作关系（管理员）
     */
    void deleteCollaboration(Long adminId, Long id);

    /**
     * 标签列表（管理员分页）
     *
     * @param keyword  搜索关键词
     * @param sortBy   排序方式
     * @param page     页码
     * @param pageSize 每页大小
     * @return 标签分页结果
     */
    PageResult<com.zhixun.entity.Tag> getTagList(String keyword, String sortBy, Integer page, Integer pageSize);

    /**
     * 管理员列表（仅返回 ADMIN 和 SUPER_ADMIN 角色的用户）
     */
    PageResult<UserVO> getAdminList(String keyword, Integer status, Integer page, Integer pageSize);

    /**
     * 修改管理员角色（仅限超级管理员操作）
     *
     * @param operatorId 操作者ID
     * @param targetId   目标用户ID
     * @param newRole    新角色（ADMIN 或 SUPER_ADMIN）
     */
    void updateAdminRole(Long operatorId, Long targetId, String newRole);

    /**
     * 启用/禁用管理员账号（仅限超级管理员操作）
     *
     * @param operatorId 操作者ID
     * @param targetId   目标用户ID
     * @param status     新状态（0-禁用，1-正常）
     */
    void updateAdminStatus(Long operatorId, Long targetId, Integer status);
}
