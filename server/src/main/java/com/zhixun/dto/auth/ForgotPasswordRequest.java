package com.zhixun.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码/重置密码请求
 * 仅需提供邮箱与验证码即可重置密码，不再依赖用户名
 */
@Data
public class ForgotPasswordRequest {

    /** 用户名（选填，向下兼容） */
    private String username;

    /** 邮箱（用于验证码校验与定位用户） */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 邮箱验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度8-32位")
    private String newPassword;
}
