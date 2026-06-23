package com.zhixun.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * OAuth 绑定请求
 */
@Data
public class OAuthBindRequest {

    /** 授权类型：wechat / qq */
    @NotBlank(message = "授权类型不能为空")
    @Pattern(regexp = "wechat|qq", message = "授权类型仅支持wechat或qq")
    private String provider;

    /** 授权码 */
    @NotBlank(message = "授权码不能为空")
    private String code;
}
