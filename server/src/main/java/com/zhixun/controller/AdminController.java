package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.config.cache.MultiLevelCacheManager;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.admin.AuditRequest;
import com.zhixun.dto.admin.SensitiveWhitelistRequest;
import com.zhixun.dto.admin.SensitiveWordRequest;
import com.zhixun.dto.admin.UserStatusRequest;
import com.zhixun.entity.OperationLog;
import com.zhixun.entity.SensitiveWhitelist;
import com.zhixun.entity.SensitiveWord;
import com.zhixun.entity.SecurityAuditLog;
import com.zhixun.service.AdminService;
import com.zhixun.service.ArticleService;
import com.zhixun.service.CommentService;
import com.zhixun.service.OperationLogService;
import com.zhixun.service.OpenSearchSyncService;
import com.zhixun.service.OpenSearchStatusService;
import com.zhixun.service.impl.SynonymService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.CommentVO;
import com.zhixun.vo.DashboardVO;
import com.zhixun.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后台管理控制器
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final OperationLogService operationLogService;
    private final SecurityUtil securityUtil;
    private final OpenSearchSyncService openSearchSyncService;
    private final CommentService commentService;
    private final ArticleService articleService;

    @Autowired(required = false)
    private OpenSearchStatusService openSearchStatusService;

    @Autowired(required = false)
    private SynonymService synonymService;

    @Autowired
    private org.springframework.context.ApplicationContext applicationContext;

    /**
     * 待审核作品列表
     */
    @GetMapping("/articles/pending")
    public R<PageResult<ArticleVO>> pendingArticles(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.getPendingArticles(page, pageSize));
    }

    /**
     * 审核作品
     */
    @PutMapping("/articles/{id}/audit")
    public R<Void> auditArticle(@PathVariable Long id, @Valid @RequestBody AuditRequest request) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.auditArticle(adminId, id, request);
        return R.ok();
    }

    /**
     * 数据概览
     */
    @GetMapping("/dashboard/overview")
    public R<DashboardVO> dashboardOverview(
            @RequestParam(required = false, defaultValue = "daily") String period) {
        return R.ok(adminService.getDashboardOverview(period));
    }

    /**
     * 用户列表
     */
    @GetMapping("/users")
    public R<PageResult<UserVO>> userList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.getUserList(keyword, role, status, page, pageSize));
    }

    /**
     * 封禁/解封用户
     */
    @PutMapping("/users/{id}/status")
    public R<Void> updateUserStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.updateUserStatus(adminId, id, request);
        return R.ok();
    }

    /**
     * 敏感词列表
     */
    @GetMapping("/sensitive-words")
    public R<PageResult<SensitiveWord>> sensitiveWords(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.getSensitiveWords(page, pageSize));
    }

    /**
     * 添加敏感词
     */
    @PostMapping("/sensitive-words")
    public R<Void> addSensitiveWord(@Valid @RequestBody SensitiveWordRequest request) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.addSensitiveWord(adminId, request);
        return R.ok();
    }

    /**
     * 删除敏感词
     */
    @DeleteMapping("/sensitive-words/{id}")
    public R<Void> deleteSensitiveWord(@PathVariable Long id) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.deleteSensitiveWord(adminId, id);
        return R.ok();
    }

    /**
     * 操作日志
     */
    @GetMapping("/operation-logs")
    public R<PageResult<OperationLog>> operationLogs(
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(operationLogService.getLogs(operatorId, module, startDate, endDate, page, pageSize));
    }

    /**
     * 全量同步数据到 OpenSearch
     */
    @PostMapping("/search/sync")
    public R<Void> syncOpenSearch() {
        openSearchSyncService.fullSync();
        return R.ok();
    }

    /**
     * OpenSearch 状态检查
     */
    @GetMapping("/search/status")
    public R<Map<String, Object>> openSearchStatus() {
        if (openSearchStatusService == null) {
            return R.ok(Map.of("available", false, "reason", "OpenSearch client not configured"));
        }
        return R.ok(openSearchStatusService.getStatus());
    }

    /**
     * 评论列表（管理员）
     */
    @GetMapping("/comments")
    public R<PageResult<CommentVO>> commentList(
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.getCommentList(articleId, status, page, pageSize));
    }

    /**
     * 删除评论（管理员）
     */
    @DeleteMapping("/comments/{id}")
    public R<Void> deleteComment(@PathVariable Long id) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.deleteCommentAsAdmin(adminId, id);
        return R.ok();
    }

    /**
     * 待审核评论列表
     */
    @GetMapping("/comments/pending")
    public R<PageResult<CommentVO>> pendingComments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(commentService.getPendingComments(page, pageSize));
    }

    /**
     * 敏感词白名单列表
     */
    @GetMapping("/sensitive-whitelist")
    public R<PageResult<SensitiveWhitelist>> sensitiveWhitelist(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.getSensitiveWhitelist(page, pageSize));
    }

    /**
     * 添加敏感词白名单
     */
    @PostMapping("/sensitive-whitelist")
    public R<Void> addSensitiveWhitelist(@Valid @RequestBody SensitiveWhitelistRequest request) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.addSensitiveWhitelist(adminId, request);
        return R.ok();
    }

    /**
     * 删除敏感词白名单
     */
    @DeleteMapping("/sensitive-whitelist/{id}")
    public R<Void> deleteSensitiveWhitelist(@PathVariable Long id) {
        Long adminId = securityUtil.getCurrentUserId();
        adminService.deleteSensitiveWhitelist(adminId, id);
        return R.ok();
    }

    @GetMapping("/synonyms")
    public R<List<String>> getSynonyms() {
        if (synonymService == null) return R.ok(List.of());
        return R.ok(synonymService.getAllSynonyms());
    }

    @PostMapping("/synonyms")
    public R<Void> addSynonym(@RequestBody Map<String, String> body) {
        if (synonymService == null) return R.fail(503, "同义词服务不可用");
        synonymService.addSynonym(body.get("rule"));
        return R.ok();
    }

    @DeleteMapping("/synonyms")
    public R<Void> removeSynonym(@RequestBody Map<String, String> body) {
        if (synonymService == null) return R.fail(503, "同义词服务不可用");
        synonymService.removeSynonym(body.get("rule"));
        return R.ok();
    }

    @PostMapping("/synonyms/reload")
    public R<Map<String, Object>> reloadSynonyms() {
        if (synonymService == null) return R.fail(503, "同义词服务不可用");
        boolean success = synonymService.reloadSearchAnalyzers();
        if (success) {
            return R.ok(Map.of("success", true, "method", "reload_analyzers"));
        }
        boolean rebuilt = synonymService.rebuildIndexWithSynonyms();
        return R.ok(Map.of("success", rebuilt, "method", "rebuild_index"));
    }

    /**
     * 安全审计日志查询
     */
    @GetMapping("/security-audit-logs")
    public R<PageResult<SecurityAuditLog>> securityAuditLogs(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(adminService.getSecurityAuditLogs(eventType, userId, ip, startDate, endDate, page, pageSize));
    }

    /**
     * 安全审计日志统计
     */
    @GetMapping("/security-audit-logs/stats")
    public R<Map<String, Object>> securityAuditStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return R.ok(adminService.getSecurityAuditStats(startDate, endDate));
    }

    /**
     * 导出操作日志为CSV
     */
    @GetMapping("/operation-logs/export")
    public ResponseEntity<byte[]> exportLogs(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String module) {
        String csvContent = operationLogService.exportLogs(startDate, endDate, module);
        byte[] csvBytes = csvContent.getBytes(StandardCharsets.UTF_8);

        // 添加 BOM 以支持 Excel 正确识别 UTF-8 编码
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(csvBytes, 0, result, bom.length, csvBytes.length);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=operation_logs.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(result.length)
                .body(result);
    }

    /**
     * 操作日志统计
     */
    @GetMapping("/operation-logs/stats")
    public R<Map<String, Object>> logStats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return R.ok(operationLogService.getLogStats(startDate, endDate));
    }

    /**
     * 缓存一致性检查
     * 对比 Redis 缓存中的数据与数据库中的最新数据，返回不一致的条目
     */
    @GetMapping("/cache/consistency")
    public R<Map<String, Object>> checkCacheConsistency() {
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> articleResult = articleService.checkArticleDetailConsistency();
            result.put("articleDetail", articleResult);
            result.put("consistent", articleResult.get("inconsistentCount") instanceof Integer
                    && (Integer) articleResult.get("inconsistentCount") == 0);
        } catch (Exception e) {
            result.put("consistent", false);
            result.put("error", e.getMessage());
        }
        return R.ok(result);
    }

    /**
     * 手动清空所有多级缓存（L1 Caffeine + L2 Redis）
     * 用于解决缓存与数据库不一致时无需重启服务的应急操作
     */
    @PostMapping("/cache/clear")
    public R<Map<String, Object>> clearAllCaches() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 获取 multiLevelCacheManager Bean（如果存在）
            org.springframework.cache.CacheManager cm = applicationContext.getBean(
                    "multiLevelCacheManager", org.springframework.cache.CacheManager.class);
            if (cm instanceof MultiLevelCacheManager mlcm) {
                mlcm.clearAllCachesOnStartup();
                result.put("success", true);
                result.put("message", "所有多级缓存已清空（L1 Caffeine + L2 Redis）");
            } else {
                result.put("success", false);
                result.put("message", "缓存管理器类型不支持");
            }
        } catch (Exception e) {
            // 降级：遍历所有缓存名逐一清空
            try {
                org.springframework.cache.CacheManager cm = applicationContext.getBean(
                        org.springframework.cache.CacheManager.class);
                java.util.Collection<String> names = cm.getCacheNames();
                for (String name : names) {
                    org.springframework.cache.Cache cache = cm.getCache(name);
                    if (cache != null) {
                        cache.clear();
                    }
                }
                result.put("success", true);
                result.put("message", "缓存已清空（降级模式），共处理 " + names.size() + " 个缓存");
            } catch (Exception e2) {
                result.put("success", false);
                result.put("error", e.getMessage() + "; " + e2.getMessage());
            }
        }
        return R.ok(result);
    }
}
