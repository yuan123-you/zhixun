package com.zhixun.controller;

import com.zhixun.common.annotation.OperationLog;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.article.ArticleCreateRequest;
import com.zhixun.dto.article.ArticlePublishRequest;
import com.zhixun.dto.article.ArticleQueryRequest;
import com.zhixun.dto.article.ArticleStatusRequest;
import com.zhixun.dto.article.ArticleUpdateRequest;
import com.zhixun.dto.article.ArticleVisibilityRequest;
import com.zhixun.service.ArticleService;
import com.zhixun.vo.ArticleDetailVO;
import com.zhixun.vo.ArticleInteractionUserVO;
import com.zhixun.vo.ArticleVO;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * 作品控制器
 */
@RestController
@RequestMapping("/v1/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final SecurityUtil securityUtil;

    /**
     * 发布作品（需认证）
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "作品", action = "发布")
    @SentinelResource(value = "article-create", blockHandler = "createBlockHandler", blockHandlerClass = ArticleController.BlockHandlers.class)
    public R<Long> create(@Valid @RequestBody ArticleCreateRequest request,
                          HttpServletRequest httpRequest) {
        Long userId = securityUtil.getCurrentUserId();
        // 获取客户端真实IP
        String clientIp = getClientIp(httpRequest);
        return R.ok(articleService.createArticle(userId, request, clientIp));
    }

    /**
     * 编辑作品（需认证）
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "作品", action = "编辑")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        articleService.updateArticle(userId, id, request);
        return R.ok();
    }

    /**
     * 作品列表（公开）
     */
    @GetMapping
    public ResponseEntity<R<PageResult<ArticleVO>>> list(ArticleQueryRequest request,
                                                         HttpServletResponse response) {
        // 设置 Cache-Control：公共缓存，最大缓存60秒
        response.setHeader("Cache-Control", "public, max-age=60");
        return ResponseEntity.ok(R.ok(articleService.getArticleList(request)));
    }

    /**
     * 作品详情（公开）
     */
    @GetMapping("/{id}")
    public ResponseEntity<R<ArticleDetailVO>> detail(@PathVariable Long id,
                                                      HttpServletResponse response) {
        // 尝试获取当前登录用户ID（未登录为 null）
        Long currentUserId = null;
        try {
            currentUserId = securityUtil.getCurrentUserId();
        } catch (Exception e) {
            // 未登录用户，currentUserId 保持为 null
        }
        // 设置 Cache-Control：公共缓存，最大缓存60秒
        response.setHeader("Cache-Control", "public, max-age=60");
        return ResponseEntity.ok(R.ok(articleService.getArticleDetail(id, currentUserId)));
    }

    /**
     * 删除作品（需认证）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "作品", action = "删除")
    public R<Void> delete(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        articleService.deleteArticle(userId, id);
        return R.ok();
    }

    /**
     * 状态变更（需管理员）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ArticleStatusRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        articleService.updateArticleStatus(userId, id, request);
        return R.ok();
    }

    /**
     * 相关推荐
     */
    @GetMapping("/{id}/related")
    public ResponseEntity<R<List<ArticleVO>>> related(
            @PathVariable Long id,
            @RequestParam(defaultValue = "6") Integer limit,
            HttpServletResponse response) {
        // 设置 Cache-Control：公共缓存，最大缓存120秒
        response.setHeader("Cache-Control", "public, max-age=120");
        return ResponseEntity.ok(R.ok(articleService.getRelatedArticles(id, limit)));
    }

    /**
     * 修改作品可见性（需认证，仅作者）
     */
    @PutMapping("/{id}/visibility")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "作品", action = "修改可见性")
    public R<Void> updateVisibility(@PathVariable Long id, @Valid @RequestBody ArticleVisibilityRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        articleService.updateArticleVisibility(userId, id, request);
        return R.ok();
    }

    /**
     * 发布草稿（需认证，仅作者）
     */
    @PutMapping("/{id}/publish")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "作品", action = "发布草稿")
    public R<Void> publishDraft(@PathVariable Long id, @RequestBody ArticlePublishRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        articleService.publishDraft(userId, id, request != null ? request : new ArticlePublishRequest());
        return R.ok();
    }

    /**
     * 记录分享（公开，未登录也可分享）
     */
    @PostMapping("/{id}/share")
    public R<Void> share(@PathVariable Long id) {
        articleService.incrementShareCount(id);
        return R.ok();
    }

    /**
     * 获取作品浏览者列表（需认证，仅作者）
     */
    @GetMapping("/{id}/viewers")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ArticleInteractionUserVO>> viewers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(articleService.getArticleViewers(id, userId, page, pageSize));
    }

    /**
     * 获取作品点赞者列表（需认证，仅作者）
     */
    @GetMapping("/{id}/likers")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ArticleInteractionUserVO>> likers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(articleService.getArticleLikers(id, userId, page, pageSize));
    }

    /**
     * Sentinel 限流降级处理
     */
    public static class BlockHandlers {
        public static R<Long> createBlockHandler(ArticleCreateRequest request, BlockException e) {
            return R.fail(429, "发布请求过于频繁，请稍后重试");
        }
    }

    /**
     * 获取客户端真实IP，优先取 X-Forwarded-For / X-Real-IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // X-Forwarded-For 可能包含多个IP，取第一个
            int idx = ip.indexOf(',');
            if (idx > 0) {
                ip = ip.substring(0, idx);
            }
            return ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
