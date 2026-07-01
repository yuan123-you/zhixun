package com.zhixun.controller;

import com.zhixun.common.annotation.OperationLog;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.auth.ChangePasswordRequest;
import com.zhixun.dto.auth.ForgotPasswordRequest;
import com.zhixun.dto.auth.LoginRequest;
import com.zhixun.dto.auth.OAuthBindRequest;
import com.zhixun.dto.auth.OAuthLoginRequest;
import com.zhixun.dto.auth.RegisterRequest;
import com.zhixun.dto.auth.SendCodeRequest;
import com.zhixun.service.AuthService;
import com.zhixun.service.CaptchaService;
import com.zhixun.service.GraphCaptchaService;
import com.zhixun.service.OAuthService;
import com.zhixun.vo.GraphCaptchaVO;
import com.zhixun.vo.TokenResponse;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;
    private final GraphCaptchaService graphCaptchaService;
    private final OAuthService oAuthService;
    private final SecurityUtil securityUtil;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    @SentinelResource(value = "auth-register", blockHandler = "registerBlockHandler", blockHandlerClass = AuthController.BlockHandlers.class)
    @OperationLog(module = "认证", action = "注册")
    public R<TokenResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.register(request);
        setTokenCookies(httpRequest, response, tokenResponse);
        return R.ok(tokenResponse);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @SentinelResource(value = "auth-login", blockHandler = "loginBlockHandler", blockHandlerClass = AuthController.BlockHandlers.class)
    @OperationLog(module = "认证", action = "登录")
    public R<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.login(request, httpRequest);
        setTokenCookies(httpRequest, response, tokenResponse);
        return R.ok(tokenResponse);
    }

    /**
     * 用户登出（需认证）
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest httpRequest, HttpServletResponse response) {
        authService.logout(httpRequest);
        clearTokenCookies(httpRequest, response);
        return R.ok();
    }

    /**
     * 刷新 Token
     * 优先从 httpOnly Cookie 读取 refreshToken，其次从请求体读取（兼容旧版本）
     */
    @PostMapping("/refresh")
    public R<TokenResponse> refresh(HttpServletRequest httpRequest, @RequestBody(required = false) Map<String, String> body, HttpServletResponse response) {
        String refreshToken = null;

        // 根据 X-Client-Type 确定要读取的 Cookie 名称
        String clientType = resolveClientType(httpRequest);
        String refreshTokenCookieName = getRefreshTokenCookieName(clientType);

        // 优先从 Cookie 读取 refreshToken（优先尝试对应客户端类型的 Cookie）
        Cookie[] cookies = httpRequest.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (refreshTokenCookieName.equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
            // 兜底：尝试旧版 Cookie 名称（兼容未更新前端的情况）
            if (refreshToken == null || refreshToken.isBlank()) {
                for (Cookie cookie : cookies) {
                    if ("refreshToken".equals(cookie.getName())) {
                        refreshToken = cookie.getValue();
                        break;
                    }
                }
            }
        }

        // Cookie 中无 refreshToken，尝试从请求体读取（兼容旧版本）
        if (refreshToken == null || refreshToken.isBlank()) {
            if (body != null) {
                refreshToken = body.get("refreshToken");
            }
        }

        if (refreshToken == null || refreshToken.isBlank()) {
            return R.fail(ErrorCode.BAD_REQUEST.getCode(), "刷新令牌不能为空");
        }

        TokenResponse tokenResponse = authService.refreshToken(refreshToken);
        setTokenCookies(httpRequest, response, tokenResponse);
        return R.ok(tokenResponse);
    }

    /**
     * 修改密码（需认证）
     */
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @OperationLog(module = "认证", action = "修改密码")
    public R<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        authService.changePassword(userId, request);
        return R.ok();
    }

    /**
     * 忘记密码
     */
    @PutMapping("/forgot-password")
    public R<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return R.ok();
    }

    /**
     * 获取图形验证码（发送邮箱验证码前需先通过图形验证码）
     */
    @GetMapping("/graph-captcha")
    public R<GraphCaptchaVO> getGraphCaptcha() {
        GraphCaptchaService.GraphCaptchaResult result = graphCaptchaService.generate();
        return R.ok(new GraphCaptchaVO(result.captchaKey(), result.image()));
    }

    /**
     * 发送验证码（邮箱）
     */
    @PostMapping("/send-code")
    @SentinelResource(value = "auth-send-code", blockHandler = "sendCodeBlockHandler", blockHandlerClass = AuthController.BlockHandlers.class)
    public R<Void> sendCode(@Valid @RequestBody SendCodeRequest request) {
        captchaService.sendEmailCode(request.getEmail(), request.getPurpose(),
                request.getCaptchaKey(), request.getCaptchaAnswer());
        return R.ok();
    }

    /**
     * 获取OAuth授权URL
     */
    @GetMapping("/oauth/url")
    public R<String> getOAuthUrl(@RequestParam String provider, @RequestParam String redirectUri) {
        String url = oAuthService.getAuthorizationUrl(provider, redirectUri);
        return R.ok(url);
    }

    /**
     * 第三方登录
     */
    @PostMapping("/oauth/login")
    public R<TokenResponse> oauthLogin(@Valid @RequestBody OAuthLoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        TokenResponse tokenResponse = oAuthService.oauthLogin(request.getProvider(), request.getCode());
        setTokenCookies(httpRequest, response, tokenResponse);
        return R.ok(tokenResponse);
    }

    /**
     * 绑定第三方账号（需登录）
     */
    @PostMapping("/oauth/bind")
    @PreAuthorize("isAuthenticated()")
    public R<Void> bindOAuth(@Valid @RequestBody OAuthBindRequest request) {
        Long userId = securityUtil.getCurrentUserId();
        oAuthService.bindOAuth(userId, request.getProvider(), request.getCode());
        return R.ok();
    }

    /**
     * 解绑第三方账号（需登录）
     */
    @DeleteMapping("/oauth/unbind")
    @PreAuthorize("isAuthenticated()")
    public R<Void> unbindOAuth(@RequestParam String provider) {
        Long userId = securityUtil.getCurrentUserId();
        oAuthService.unbindOAuth(userId, provider);
        return R.ok();
    }

    // ========== Cookie 工具方法 ==========

    /** 客户端类型请求头名 */
    private static final String CLIENT_TYPE_HEADER = "X-Client-Type";
    /** 客户端类型：客户端 */
    private static final String CLIENT_TYPE_CLIENT = "client";
    /** 客户端类型：管理员端 */
    private static final String CLIENT_TYPE_ADMIN = "admin";

    /**
     * 从请求头获取客户端类型
     * @return "client" 或 "admin"，默认为 "client"
     */
    private String resolveClientType(HttpServletRequest request) {
        String clientType = request.getHeader(CLIENT_TYPE_HEADER);
        if (CLIENT_TYPE_ADMIN.equalsIgnoreCase(clientType)) {
            return CLIENT_TYPE_ADMIN;
        }
        return CLIENT_TYPE_CLIENT;
    }

    /**
     * 获取 accessToken Cookie 名称
     */
    private String getAccessTokenCookieName(String clientType) {
        return CLIENT_TYPE_ADMIN.equals(clientType) ? "admin_accessToken" : "client_accessToken";
    }

    /**
     * 获取 refreshToken Cookie 名称
     */
    private String getRefreshTokenCookieName(String clientType) {
        return CLIENT_TYPE_ADMIN.equals(clientType) ? "admin_refreshToken" : "client_refreshToken";
    }

    /**
     * 设置 Token Cookie
     * accessToken: httpOnly Cookie（用于 WebSocket 连接，短期有效）
     * refreshToken: httpOnly Cookie（长期有效，安全存储）
     * 根据 X-Client-Type 请求头区分客户端和管理员端，使用不同的 Cookie 名称避免互相覆盖
     */
    private void setTokenCookies(HttpServletRequest httpRequest, HttpServletResponse response, TokenResponse tokenResponse) {
        String clientType = resolveClientType(httpRequest);
        String accessTokenName = getAccessTokenCookieName(clientType);
        String refreshTokenName = getRefreshTokenCookieName(clientType);

        // 设置 accessToken Cookie（短期有效，用于 WebSocket）
        Cookie accessTokenCookie = new Cookie(accessTokenName, tokenResponse.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true); // 生产环境强制 HTTPS
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(tokenResponse.getExpiresIn() != null ? tokenResponse.getExpiresIn().intValue() : 7 * 24 * 60 * 60);
        accessTokenCookie.setAttribute("SameSite", "Strict"); // CSRF 防护
        response.addCookie(accessTokenCookie);

        // 设置 refreshToken Cookie（长期有效，httpOnly 安全存储）
        Cookie refreshTokenCookie = new Cookie(refreshTokenName, tokenResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7天
        refreshTokenCookie.setAttribute("SameSite", "Strict");
        response.addCookie(refreshTokenCookie);
    }

    /**
     * 清除 Token Cookie
     * 根据 X-Client-Type 请求头清除对应客户端的 Cookie
     */
    private void clearTokenCookies(HttpServletRequest httpRequest, HttpServletResponse response) {
        String clientType = resolveClientType(httpRequest);
        String accessTokenName = getAccessTokenCookieName(clientType);
        String refreshTokenName = getRefreshTokenCookieName(clientType);

        Cookie accessTokenCookie = new Cookie(accessTokenName, "");
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(true);
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(0); // 立即过期
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie(refreshTokenName, "");
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(0);
        response.addCookie(refreshTokenCookie);
    }

    /**
     * Sentinel 限流降级处理
     */
    public static class BlockHandlers {
        public static R<TokenResponse> registerBlockHandler(RegisterRequest request, HttpServletResponse response, BlockException e) {
            return R.fail(429, "注册请求过于频繁，请稍后重试");
        }

        public static R<TokenResponse> loginBlockHandler(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response, BlockException e) {
            return R.fail(429, "登录请求过于频繁，请稍后重试");
        }

        public static R<Void> sendCodeBlockHandler(SendCodeRequest request, BlockException e) {
            return R.fail(429, "验证码发送请求过于频繁，请稍后重试");
        }
    }
}
