package com.zhixun.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 忘记密码/重置密码请求
 * 仅需提供邮箱、验证码与新密码，彻底移除用户名
 */
@Data
public class ForgotPasswordRequest {

    /** 邮箱（用于验证码校验与精准定位用户账号） */
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
