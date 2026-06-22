package com.zhixun.service;

import com.zhixun.dto.auth.ChangePasswordRequest;
import com.zhixun.dto.auth.ForgotPasswordRequest;
import com.zhixun.dto.auth.LoginRequest;
import com.zhixun.dto.auth.RegisterRequest;
import com.zhixun.vo.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     *
     * @param request 注册请求
     * @return Token 响应
     */
    TokenResponse register(RegisterRequest request);

    /**
     * 用户登录
     *
     * @param request  登录请求
     * @param httpRequest HTTP 请求
     * @return Token 响应
     */
    TokenResponse login(LoginRequest request, HttpServletRequest httpRequest);

    /**
     * 用户登出
     *
     * @param httpRequest HTTP 请求
     */
    void logout(HttpServletRequest httpRequest);

    /**
     * 刷新令牌
     *
     * @param refreshToken 刷新令牌
     * @return Token 响应
     */
    TokenResponse refreshToken(String refreshToken);

    /**
     * 修改密码
     *
     * @param userId  用户ID
     * @param request 修改密码请求
     */
    void changePassword(Long userId, ChangePasswordRequest request);

    /**
     * 忘记密码
     *
     * @param request 忘记密码请求
     */
    void forgotPassword(ForgotPasswordRequest request);
}
