package com.zhixun.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 发送验证码请求
 */
@Data
public class SendCodeRequest {

    /** 邮箱地址 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 用途：register-注册，login-登录，resetPassword-重置密码 */
    @NotBlank(message = "用途不能为空")
    @Pattern(regexp = "^(register|login|resetPassword)$", message = "用途只能是register、login或resetPassword")
    private String purpose;

    /** 图形验证码键 */
    @NotBlank(message = "图形验证码键不能为空")
    private String captchaKey;

    /** 图形验证码答案 */
    @NotBlank(message = "图形验证码答案不能为空")
    private String captchaAnswer;
}
