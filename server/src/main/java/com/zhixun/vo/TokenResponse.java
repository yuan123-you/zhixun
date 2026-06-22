package com.zhixun.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * Token 响应 VO
 */
@Data
public class TokenResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 访问令牌 */
    private String accessToken;

    /** 刷新令牌 */
    private String refreshToken;

    /** 令牌类型 */
    private String tokenType = "Bearer";

    /** 过期时间（秒） */
    private Long expiresIn;

    /** 用户信息 */
    private LoginUserVO userInfo;
}
