package com.zhixun.service;

import com.zhixun.vo.TokenResponse;

/**
 * OAuth 第三方登录服务接口
 */
public interface OAuthService {

    /**
     * 第三方登录
     *
     * @param provider 授权类型（wechat / qq）
     * @param code     授权码
     * @return Token 响应
     */
    TokenResponse oauthLogin(String provider, String code);

    /**
     * 绑定第三方账号
     *
     * @param userId   用户ID
     * @param provider 授权类型（wechat / qq）
     * @param code     授权码
     */
    void bindOAuth(Long userId, String provider, String code);

    /**
     * 解绑第三方账号
     *
     * @param userId   用户ID
     * @param provider 授权类型（wechat / qq）
     */
    void unbindOAuth(Long userId, String provider);

    /**
     * 获取OAuth授权URL
     *
     * @param provider    授权类型（wechat / qq）
     * @param redirectUri 回调地址
     * @return 授权URL
     */
    String getAuthorizationUrl(String provider, String redirectUri);
}
