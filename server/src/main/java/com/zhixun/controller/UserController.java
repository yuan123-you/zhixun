package com.zhixun.controller;

import com.zhixun.common.annotation.OperationLog;
import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.user.ProfileUpdateRequest;
import com.zhixun.dto.user.SettingsUpdateRequest;
import com.zhixun.dto.user.UidUpdateRequest;
import com.zhixun.service.CommentService;
import com.zhixun.service.OnlineStatusService;
import com.zhixun.service.TencentMapService;
import com.zhixun.service.UserService;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.CommentVO;
import com.zhixun.vo.UserSettingsVO;
import com.zhixun.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final CommentService commentService;
    private final OnlineStatusService onlineStatusService;
    private final SecurityUtil securityUtil;
    private final TencentMapService tencentMapService;

    /**
     * 获取个人资料
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public R<UserVO> getProfile() {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getProfile(userId));
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "用户设置", action = "修改个人资料")
    public R<Void> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        userService.updateProfile(userId, request);
        return R.ok();
    }

    /**
     * 我发布的作品
     */
    @GetMapping("/articles")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ArticleVO>> getUserArticles(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getUserArticles(userId, status, page, pageSize));
    }

    /**
     * 我的点赞
     */
    @GetMapping("/likes")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ArticleVO>> getUserLikes(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getUserLikes(userId, page, pageSize));
    }

    /**
     * 我的评论
     */
    @GetMapping("/comments")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<CommentVO>> getUserComments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(commentService.getMyComments(userId, page, pageSize));
    }

    /**
     * 浏览历史
     */
    @GetMapping("/view-history")
    @PreAuthorize("isAuthenticated()")
    public R<PageResult<ArticleVO>> getViewHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getViewHistory(userId, startDate, endDate, page, pageSize));
    }

    /**
     * 批量同步浏览历史（客户端定期同步本地浏览记录到服务器）
     */
    @PostMapping("/view-history/batch")
    @PreAuthorize("isAuthenticated()")
    public R<Void> batchSyncViewHistory(@RequestBody Map<String, List<Map<String, Object>>> body) {
        Long userId = securityUtil.getCurrentUserId();
        List<Map<String, Object>> records = body.get("records");
        userService.batchSyncViewHistory(userId, records);
        return R.ok();
    }

    /**
     * 获取全局设置
     */
    @GetMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    public R<UserSettingsVO> getSettings() {
        Long userId = securityUtil.getCurrentUserId();
        return R.ok(userService.getSettings(userId));
    }

    /**
     * 更新全局设置
     */
    @PutMapping("/settings")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "用户设置", action = "修改全局设置")
    public R<Void> updateSettings(@Valid @RequestBody SettingsUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        userService.updateSettings(userId, request);
        return R.ok();
    }

    /**
     * 更新在线状态可见性
     * 专用端点，修改后自动清除缓存
     */
    @PutMapping("/online-status-visibility")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "用户设置", action = "修改在线状态可见性")
    public R<Void> updateOnlineStatusVisibility(@RequestBody Map<String, Integer> body) {
        Long userId = securityUtil.getCurrentUserId();
        Integer showOnlineStatus = body.get("show_online_status");
        if (showOnlineStatus == null || (showOnlineStatus != 0 && showOnlineStatus != 1)) {
            return R.fail(400, "show_online_status 参数错误，仅支持 0 或 1");
        }
        onlineStatusService.updateShowOnlineStatus(userId, showOnlineStatus);
        return R.ok();
    }

    /**
     * 批量获取用户在线状态
     * 用于关注/粉丝列表等场景
     */
    @GetMapping("/online-status/batch")
    @PreAuthorize("isAuthenticated()")
    public R<Map<Long, Boolean>> batchGetOnlineStatus(@RequestParam List<Long> userIds) {
        Long requesterId = securityUtil.getCurrentUserId();
        Map<Long, Boolean> result = onlineStatusService.batchGetOnlineStatus(userIds, requesterId);
        return R.ok(result);
    }

    /**
     * 修改UID（30天内只能修改一次）
     */
    @PutMapping("/uid")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "用户设置", action = "修改UID")
    public R<Void> updateUid(@Valid @RequestBody UidUpdateRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        userService.updateUid(userId, request.getUid());
        return R.ok();
    }

    /**
     * 自动获取并更新IP属地
     * 从请求中获取客户端IP，通过腾讯地图 IP 定位服务解析为属地信息并保存
     */
    @PostMapping("/ip-location")
    @PreAuthorize("isAuthenticated()")
    public R<String> updateIpLocation(HttpServletRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        String ip = getClientIp(request);
        String location = tencentMapService.resolveIpLocation(ip);
        if (location != null) {
            userService.updateIpLocation(userId, location);
        }
        return R.ok(location);
    }

    /**
     * 匿名查询当前请求 IP 的属地（仅返回，不入库）。
     * 供前端在用户未登录时也能展示 IP 属地信息。
     */
    @GetMapping("/ip-location")
    public R<String> getIpLocation(HttpServletRequest request) {
        String ip = getClientIp(request);
        return R.ok(tencentMapService.resolveIpLocation(ip));
    }

    /**
     * 解析客户端真实 IP，兼容 Cloudflare / 普通反向代理 / 直连三种场景。
     * <p>优先级：CF-Connecting-IP（Cloudflare 专属，最可靠）
     *          → X-Forwarded-For（标准代理头，取第一个非内网/非 Cloudflare IP）
     *          → X-Real-IP（nginx 设置）
     *          → request.getRemoteAddr()（兜底）
     */
    private String getClientIp(HttpServletRequest request) {
        // 1) Cloudflare 专用头：直接给出真实客户端 IP，最可靠
        String ip = request.getHeader("CF-Connecting-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        // 2) X-Forwarded-For：可能包含多个 IP（client, proxy1, proxy2），
        //    从前往后找第一个非内网/非 Cloudflare IP
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            for (String candidate : xff.split(",")) {
                String trimmed = candidate.trim();
                if (!trimmed.isEmpty() && !isPrivateOrCloudflareIp(trimmed)) {
                    return trimmed;
                }
            }
            // 全是内网/Cloudflare IP，返回第一个
            return xff.split(",")[0].trim();
        }

        // 3) X-Real-IP
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }

        // 4) 兜底
        return request.getRemoteAddr();
    }

    /**
     * 判断 IP 是否为内网地址或 Cloudflare 边缘节点 IP。
     */
    private boolean isPrivateOrCloudflareIp(String ip) {
        if (ip == null || ip.isEmpty()) return true;
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("127.")
                || ip.equals("::1") || ip.startsWith("169.254.")) return true;
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                if (second >= 16 && second <= 31) return true;
            } catch (Exception ignored) { }
        }
        // Cloudflare IP 段（简化匹配）
        try {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                int first = Integer.parseInt(parts[0]);
                int second = Integer.parseInt(parts[1]);
                if (first == 162 && second == 158) return true;       // 162.158.0.0/15
                if (first == 172 && second >= 64 && second <= 71) return true; // 172.64.0.0/13
                if (first == 104 && second >= 16 && second <= 31) return true; // 104.16.0.0/12
                if (first == 198 && second == 41 && Integer.parseInt(parts[2]) >= 128) return true;
            }
        } catch (Exception ignored) { }
        return false;
    }
}
