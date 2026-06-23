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
import jakarta.servlet.http.HttpServletRequest;
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
    public R<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return R.ok(authService.register(request));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @SentinelResource(value = "auth-login", blockHandler = "loginBlockHandler", blockHandlerClass = AuthController.BlockHandlers.class)
    @OperationLog(module = "认证", action = "登录")
    public R<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return R.ok(authService.login(request, httpRequest));
    }

    /**
     * 用户登出（需认证）
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return R.ok();
    }

    /**
     * 刷新 Token
     */
    @PostMapping("/refresh")
    public R<TokenResponse> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return R.fail(ErrorCode.BAD_REQUEST.getCode(), "刷新令牌不能为空");
        }
        return R.ok(authService.refreshToken(refreshToken));
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
    public R<TokenResponse> oauthLogin(@Valid @RequestBody OAuthLoginRequest request) {
        return R.ok(oAuthService.oauthLogin(request.getProvider(), request.getCode()));
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

    /**
     * Sentinel 限流降级处理
     */
    public static class BlockHandlers {
        public static R<TokenResponse> registerBlockHandler(RegisterRequest request, BlockException e) {
            return R.fail(429, "注册请求过于频繁，请稍后重试");
        }

        public static R<TokenResponse> loginBlockHandler(LoginRequest request, HttpServletRequest httpRequest, BlockException e) {
            return R.fail(429, "登录请求过于频繁，请稍后重试");
        }

        public static R<Void> sendCodeBlockHandler(SendCodeRequest request, BlockException e) {
            return R.fail(429, "验证码发送请求过于频繁，请稍后重试");
        }
    }
}
