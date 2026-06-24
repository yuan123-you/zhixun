package com.zhixun.service.impl;

import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.util.AesUtil;
import com.zhixun.common.util.JwtUtil;
import com.zhixun.dto.auth.ChangePasswordRequest;
import com.zhixun.dto.auth.ForgotPasswordRequest;
import com.zhixun.dto.auth.LoginRequest;
import com.zhixun.dto.auth.RegisterRequest;
import com.zhixun.entity.LoginLog;
import com.zhixun.entity.User;
import com.zhixun.entity.UserSettings;
import com.zhixun.enums.RoleEnum;
import com.zhixun.mapper.LoginLogMapper;
import com.zhixun.mapper.UserMapper;
import com.zhixun.mapper.UserSettingsMapper;
import com.zhixun.service.AuthService;
import com.zhixun.service.CaptchaService;
import com.zhixun.service.LoginLogService;
import com.zhixun.service.OpenSearchSyncService;
import com.zhixun.vo.LoginUserVO;
import com.zhixun.vo.TokenResponse;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserSettingsMapper userSettingsMapper;
    private final LoginLogService loginLogService;
    private final JwtUtil jwtUtil;
    private final AesUtil aesUtil;
    private final PasswordEncoder passwordEncoder;
    private final OpenSearchSyncService openSearchSyncService;
    private final StringRedisTemplate stringRedisTemplate;
    private final CaptchaService captchaService;

    /** 登录失败次数 Redis Key 前缀 */
    private static final String LOGIN_FAIL_PREFIX = "auth:login:fail:";

    /** 刷新令牌 Redis Key 前缀 */
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    /** 令牌黑名单 Redis Key 前缀 */
    private static final String TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";

    /** 最大登录失败次数 */
    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    /** 账号锁定时长（分钟） */
    private static final long LOCK_MINUTES = 30;

    /** 访问令牌过期时间（秒） */
    @Value("${jwt.access-expiration:7200}")
    private Long accessExpiration;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TokenResponse register(RegisterRequest request) {
        // 校验邮箱验证码（邮箱注册必须验证）
        if (StringUtils.hasText(request.getEmail())) {
            if (!StringUtils.hasText(request.getCode())) {
                throw new BusinessException(ErrorCode.AUTH_CAPTCHA_ERROR, "邮箱注册需要验证码");
            }
            if (!captchaService.verifyCode(request.getEmail(), request.getCode())) {
                throw new BusinessException(ErrorCode.AUTH_CAPTCHA_ERROR, "验证码错误或已过期");
            }
        }

        // 校验用户名唯一性
        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (usernameCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }

        // 校验邮箱唯一性（如提供）- 使用SHA-256哈希校验
        if (StringUtils.hasText(request.getEmail())) {
            String emailHash = DigestUtils.md5DigestAsHex(request.getEmail().toLowerCase().getBytes(StandardCharsets.UTF_8));
            Long emailCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmailHash, emailHash));
            if (emailCount > 0) {
                throw new BusinessException(ErrorCode.USER_EMAIL_EXISTS);
            }
        }

        // 校验手机号唯一性（如提供）- 使用SHA-256哈希校验
        if (StringUtils.hasText(request.getPhone())) {
            String phoneHash = DigestUtils.md5DigestAsHex(request.getPhone().getBytes(StandardCharsets.UTF_8));
            Long phoneCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getPhoneHash, phoneHash));
            if (phoneCount > 0) {
                throw new BusinessException(ErrorCode.USER_PHONE_EXISTS);
            }
        }

        // 构建用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setStatus(User.STATUS_NORMAL);
        user.setRole(RoleEnum.USER);

        // 邮箱/手机号 AES 加密存储，同时存储哈希用于唯一性校验
        if (StringUtils.hasText(request.getEmail())) {
            user.setEmail(aesUtil.encrypt(request.getEmail()));
            user.setEmailHash(DigestUtils.md5DigestAsHex(request.getEmail().toLowerCase().getBytes(StandardCharsets.UTF_8)));
        }
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhone(aesUtil.encrypt(request.getPhone()));
            user.setPhoneHash(DigestUtils.md5DigestAsHex(request.getPhone().getBytes(StandardCharsets.UTF_8)));
        }

        // 创建用户记录
        userMapper.insert(user);

        // 同步到 OpenSearch
        openSearchSyncService.syncUser(user.getId());

        // 创建默认用户设置记录
        UserSettings userSettings = new UserSettings();
        userSettings.setUserId(user.getId());
        userSettings.setNotifySystem(1);
        userSettings.setNotifyInteract(1);
        userSettingsMapper.insert(userSettings);

        // 生成双 Token
        return buildTokenResponse(user);
    }

    @Override
    public TokenResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        // 查询用户（使用自定义 SQL 查询包含 passwordHash，因为 @TableField(select = false) 会阻止 MyBatis-Plus 查询该字段）
        User user = userMapper.selectByUsernameWithPassword(request.getUsername());

        // 记录登录日志
        LoginLog loginLog = buildLoginLog(request.getUsername(), httpRequest);

        // 用户不存在
        if (user == null) {
            loginLog.setStatus(0);
            loginLog.setFailReason("用户不存在");
            loginLogService.saveAsync(loginLog);
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }

        // 检查账号状态
        if (user.getStatus() != null && user.getStatus() == User.STATUS_DISABLED) {
            loginLog.setUserId(user.getId());
            loginLog.setStatus(0);
            loginLog.setFailReason("账号已被禁用");
            loginLogService.saveAsync(loginLog);
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        // 检查登录失败次数（5次锁定30分钟）
        String failKey = LOGIN_FAIL_PREFIX + request.getUsername();
        String failCountStr = stringRedisTemplate.opsForValue().get(failKey);
        if (failCountStr != null) {
            int failCount = Integer.parseInt(failCountStr);
            if (failCount >= MAX_LOGIN_FAIL_COUNT) {
                loginLog.setUserId(user.getId());
                loginLog.setStatus(0);
                loginLog.setFailReason("账号已被锁定");
                loginLogService.saveAsync(loginLog);
                throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED, "账号已被锁定，请" + LOCK_MINUTES + "分钟后再试");
            }
        }

        // 校验密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            // 登录失败次数 +1
            Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
            if (failCount != null && failCount == 1) {
                // 第一次失败，设置过期时间
                stringRedisTemplate.expire(failKey, LOCK_MINUTES, TimeUnit.MINUTES);
            }
            loginLog.setUserId(user.getId());
            loginLog.setStatus(0);
            loginLog.setFailReason("密码错误");
            loginLogService.saveAsync(loginLog);
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED, "用户名或密码错误");
        }

        // 登录成功，清除失败次数
        stringRedisTemplate.delete(failKey);

        // 更新最后登录时间
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(updateUser);

        // 记录登录日志
        loginLog.setUserId(user.getId());
        loginLog.setStatus(1);
        loginLogService.saveAsync(loginLog);

        // 生成双 Token
        return buildTokenResponse(user);
    }

    @Override
    public void logout(HttpServletRequest httpRequest) {
        String token = resolveToken(httpRequest);
        if (!StringUtils.hasText(token)) {
            return;
        }

        // 将 AccessToken 加入 Redis 黑名单（TTL = Token 剩余有效期）
        Claims claims = jwtUtil.parseAccessToken(token);
        if (claims != null) {
            long remainingMillis = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingMillis > 0) {
                stringRedisTemplate.opsForValue().set(
                        TOKEN_BLACKLIST_PREFIX + token,
                        "1",
                        remainingMillis,
                        TimeUnit.MILLISECONDS);
            }
        }
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        // 验证 RefreshToken 有效性
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new BusinessException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);
        }

        // 检查 RefreshToken 是否在 Redis 中（轮转机制）
        String refreshKey = REFRESH_TOKEN_PREFIX + refreshToken;
        String userIdStr = stringRedisTemplate.opsForValue().get(refreshKey);
        if (userIdStr == null) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "刷新令牌已失效");
        }

        // 从 JWT 中获取用户ID
        Claims claims = jwtUtil.parseRefreshToken(refreshToken);
        Long userId = null;
        if (claims != null) {
            Object userIdObj = claims.get(JwtUtil.CLAIM_USER_ID);
            if (userIdObj instanceof Number) {
                userId = ((Number) userIdObj).longValue();
            } else if (userIdObj != null) {
                userId = Long.parseLong(userIdObj.toString());
            }
        }
        if (userId == null) {
            throw new BusinessException(ErrorCode.AUTH_TOKEN_INVALID, "刷新令牌已失效");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 检查账号状态
        if (user.getStatus() != null && user.getStatus() == User.STATUS_DISABLED) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        // 旧 RefreshToken 失效（轮转机制）
        stringRedisTemplate.delete(refreshKey);

        // 生成新的 AccessToken 和 RefreshToken
        return buildTokenResponse(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userMapper.selectByIdWithPassword(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED, "旧密码错误");
        }
        // Validate new password strength (must contain letters and digits)
        String newPassword = request.getNewPassword();
        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须包含字母和数字");
        }
        User updateUser = new User();
        updateUser.setId(userId);
        updateUser.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(updateUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 验证验证码 - 使用 CaptchaService
        String verifyTarget = StringUtils.hasText(request.getEmail()) ? request.getEmail() : request.getUsername();
        if (!captchaService.verifyCode(verifyTarget, request.getCode())) {
            throw new BusinessException(ErrorCode.AUTH_CAPTCHA_ERROR, "验证码错误或已过期");
        }

        String newPassword = request.getNewPassword();
        if (!newPassword.matches(".*[a-zA-Z].*") || !newPassword.matches(".*\\d.*")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "密码必须包含字母和数字");
        }

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(updateUser);
    }

    /**
     * 构建 Token 响应
     */
    private TokenResponse buildTokenResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole() != null ? user.getRole().getValue() : User.ROLE_USER);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole() != null ? user.getRole().getValue() : User.ROLE_USER);

        // 将 RefreshToken 存入 Redis（用于轮转机制）
        stringRedisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + refreshToken,
                String.valueOf(user.getId()),
                accessExpiration * 7,
                TimeUnit.SECONDS);

        // 构建用户信息
        LoginUserVO userInfo = new LoginUserVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRole(user.getRole() != null ? user.getRole().getValue() : User.ROLE_USER);

        // 构建权限列表
        List<String> permissions = new ArrayList<>();
        permissions.add("ROLE_USER");
        if (RoleEnum.ADMIN.equals(user.getRole()) || RoleEnum.SUPER_ADMIN.equals(user.getRole())) {
            permissions.add("ROLE_ADMIN");
        }
        userInfo.setPermissions(permissions);

        // 构建 Token 响应
        TokenResponse response = new TokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(accessExpiration);
        response.setUserInfo(userInfo);

        return response;
    }

    /**
     * 构建登录日志
     */
    private LoginLog buildLoginLog(String username, HttpServletRequest request) {
        LoginLog loginLog = new LoginLog();
        loginLog.setUsername(username);

        // 获取 IP
        String ip = getClientIp(request);
        loginLog.setIp(ip);

        // 解析用户代理
        String userAgentStr = request.getHeader("User-Agent");
        if (StringUtils.hasText(userAgentStr)) {
            loginLog.setUserAgent(userAgentStr);
        }

        return loginLog;
    }

    /**
     * 从请求头中解析令牌
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (StringUtils.hasText(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
