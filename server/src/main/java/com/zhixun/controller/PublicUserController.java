package com.zhixun.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.entity.User;
import com.zhixun.entity.UserFollow;
import com.zhixun.mapper.UserFollowMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.RankService;
import com.zhixun.service.TencentMapService;
import com.zhixun.service.TencentMapService.IpLocationDetail;
import com.zhixun.service.TencentMapService.ReverseGeocodeResult;
import com.zhixun.service.impl.RedisRateLimiterService;
import com.zhixun.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 公开用户接口（C端）
 */
@Slf4j
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class PublicUserController {

    private final RankService rankService;
    private final UserMapper userMapper;
    private final TencentMapService tencentMapService;
    private final RedisRateLimiterService rateLimiterService;
    private final SecurityUtil securityUtil;
    private final UserFollowMapper userFollowMapper;

    /**
     * 匿名查询当前请求 IP 的属地（公开，仅返回，不入库）
     * 基于腾讯地图 IP 定位服务（/ws/location/v1/ip）实现
     * <p>仅返回 "省·市" 字符串，保留旧语义。
     */
    @GetMapping("/ip-location")
    public R<String> getIpLocation(HttpServletRequest request) {
        String ip = resolveClientIp(request);
        return R.ok(tencentMapService.resolveIpLocation(ip));
    }

    /**
     * 匿名查询当前请求 IP 的结构化属地信息（新增）。
     * <p>供前端创作页/编辑资料页的"自动定位"使用：
     * <ul>
     *   <li>Key 仅在服务端持有，前端无腾讯域名白名单/CORS/referer 限制；</li>
     *   <li>IP → 定位结果走 Redis 缓存，相同 IP 重复请求不消耗腾讯配额；</li>
     *   <li>失败兜底 ip-api.com；</li>
     *   <li>按 IP 限流：每 IP 每秒 5 个令牌（容量 5），防御被刷。</li>
     * </ul>
     * <p>返回 {@code R<IpLocationDetail>}：{@code province/city/district/lat/lng/ip}。
     */
    @GetMapping("/ip-location-detail")
    public R<IpLocationDetail> getIpLocationDetail(HttpServletRequest request) {
        String ip = resolveClientIp(request);
        if (ip != null && !ip.isEmpty()
                && !rateLimiterService.isAllowedByIp(ip, "ip_location_detail", 5, 5)) {
            return R.fail(429, "定位请求过于频繁，请稍后再试");
        }
        IpLocationDetail detail = tencentMapService.resolveIpLocationDetail(ip);
        if (detail == null) {
            // 不暴露"失败原因"给前端，统一返回 null，让前端走"手动选择"分支
            return R.ok(null);
        }
        return R.ok(detail);
    }

    /**
     * 逆地理编码（经纬度 → 省市区 + 详细地址），供前端浏览器 GPS 定位后的精确解析。
     * <p>Key 仅在服务端持有；坐标 → 结果走 Redis 缓存。
     * <p>按 IP 限流：每 IP 每秒 10 个令牌（容量 10）。
     */
    @GetMapping("/reverse-geocode")
    public R<ReverseGeocodeResult> reverseGeocode(
            @RequestParam Double lat,
            @RequestParam Double lng,
            HttpServletRequest request) {
        if (lat == null || lng == null || !Double.isFinite(lat) || !Double.isFinite(lng)) {
            return R.fail(400, "经纬度参数无效");
        }
        if (Math.abs(lat) > 90 || Math.abs(lng) > 180) {
            return R.fail(400, "经纬度超出有效范围");
        }
        String ip = resolveClientIp(request);
        if (ip != null && !ip.isEmpty()
                && !rateLimiterService.isAllowedByIp(ip, "reverse_geocode", 10, 10)) {
            return R.fail(429, "逆地理编码请求过于频繁，请稍后再试");
        }
        ReverseGeocodeResult result = tencentMapService.reverseGeocode(lat, lng);
        return R.ok(result);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 通过UID查找用户（公开）
     */
    @GetMapping("/by-uid")
    public R<UserVO> findByUid(@RequestParam String uid) {
        User user = userMapper.selectByUid(uid);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUid(user.getUid());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setGender(user.getGender());
        vo.setShowGenderOnProfile(user.getShowGenderOnProfile() != null && user.getShowGenderOnProfile() == 1);
        vo.setProvince(user.getProvince());
        vo.setIpLocation(user.getIpLocation());
        vo.setRole(user.getRole() != null ? user.getRole().name() : null);
        vo.setFollowCount(user.getFollowCount());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setArticleCount(user.getArticleCount());
        return R.ok(vo);
    }

    /**
     * 推荐用户列表（公开）
     * <p>支持匿名访问：未登录时返回一批活跃用户；登录时会优先排除自己及已关注用户。
     * <p>实现思路：
     *  <ol>
     *    <li>查询状态正常的用户（status=1）且非管理员，拉取足够候选后随机打乱；</li>
     *    <li>若当前已登录，排除自己与已关注的人；</li>
     *    <li>使用 Java 端 Collections.shuffle 随机排序并截取 limit 条；</li>
     *    <li>仅做随机推荐，不依赖用户偏好表（内容推荐走 FeedController 的 /feed/recommend）。</li>
     *  </ol>
     * <p>支持分页：客户端可通过 offset + limit 实现"换一批"功能。
     */
    @GetMapping("/recommend")
    public R<List<UserVO>> recommendUsers(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(defaultValue = "0") Integer offset) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        int safeOffset = Math.max(0, offset);

        Long currentUserId = null;
        try {
            currentUserId = securityUtil.getCurrentUserId();
        } catch (Exception ignored) {
            // 匿名访问时 SecurityUtil.getCurrentUserId() 会抛未授权异常，这里吞掉即可
        }

        // 已关注用户ID集合（仅登录时计算）
        Set<Long> followingIdSet = Collections.emptySet();
        if (currentUserId != null) {
            try {
                LambdaQueryWrapper<UserFollow> followWrapper = new LambdaQueryWrapper<>();
                followWrapper.eq(UserFollow::getFollowerId, currentUserId)
                        .select(UserFollow::getFollowingId);
                followingIdSet = userFollowMapper.selectList(followWrapper).stream()
                        .map(UserFollow::getFollowingId)
                        .collect(Collectors.toSet());
            } catch (Exception e) {
                log.warn("获取关注列表失败，按未登录处理: {}", e.getMessage());
                followingIdSet = Collections.emptySet();
            }
        }

        // 拉一批候选用户（status=1 且非管理员），按创建时间倒序，取上限以保证打乱后仍有足够数据
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStatus, User.STATUS_NORMAL)
                .ne(User::getRole, com.zhixun.enums.RoleEnum.ADMIN)
                .orderByDesc(User::getCreatedAt)
                .last("LIMIT " + (safeLimit * 8 + safeOffset + 50));
        List<User> candidates = userMapper.selectList(wrapper);
        if (candidates.isEmpty()) {
            return R.ok(Collections.emptyList());
        }

        // 打乱顺序
        List<User> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);

        // 过滤自己与已关注
        List<User> filtered = new ArrayList<>();
        for (User u : shuffled) {
            if (currentUserId != null && u.getId().equals(currentUserId)) continue;
            if (followingIdSet.contains(u.getId())) continue;
            filtered.add(u);
        }

        // 处理分页偏移
        List<User> pageList;
        if (safeOffset >= filtered.size()) {
            pageList = Collections.emptyList();
        } else {
            pageList = filtered.subList(safeOffset, Math.min(filtered.size(), safeOffset + safeLimit));
        }

        // 转换为 VO
        List<UserVO> result = new ArrayList<>(pageList.size());
        for (User u : pageList) {
            result.add(toUserVO(u, currentUserId));
        }
        return R.ok(result);
    }

    /**
     * User 实体 -> UserVO（轻量脱敏，不解密敏感信息，性能更好）
     */
    private UserVO toUserVO(User user, Long currentUserId) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUid(user.getUid());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole() != null ? user.getRole().name() : null);
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        vo.setFollowCount(user.getFollowCount());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setArticleCount(user.getArticleCount());
        vo.setBio(user.getBio());
        vo.setGender(user.getGender());
        vo.setShowGenderOnProfile(user.getShowGenderOnProfile() != null && user.getShowGenderOnProfile() == 1);
        vo.setProvince(user.getProvince());
        vo.setIpLocation(user.getIpLocation());
        // 当前用户是否关注了此用户（仅登录态有意义）
        if (currentUserId != null && !currentUserId.equals(user.getId())) {
            try {
                LambdaQueryWrapper<UserFollow> fw = new LambdaQueryWrapper<>();
                fw.eq(UserFollow::getFollowerId, currentUserId)
                        .eq(UserFollow::getFollowingId, user.getId());
                vo.setIsFollowing(userFollowMapper.selectCount(fw) > 0);
            } catch (Exception e) {
                vo.setIsFollowing(false);
            }
        } else {
            vo.setIsFollowing(false);
        }
        return vo;
    }
}
