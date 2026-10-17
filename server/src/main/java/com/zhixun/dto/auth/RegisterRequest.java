package com.zhixun.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求
 * 支持用户名选填，未填时自动使用邮箱号（带后缀）作为用户名
 */
@Data
public class RegisterRequest {

    /** 用户名（选填，支持字母、数字、下划线、中文，以及邮箱中的@和点） */
    @Size(max = 50, message = "用户名长度最长50个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_\\u4e00-\\u9fa5@.]*$", message = "用户名只能包含字母、数字、下划线、中文、@和点")
    private String username;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度为6-20个字符")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).+$", message = "密码必须包含字母和数字")
    private String password;

    /** 邮箱 */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    @Size(max = 20, message = "昵称最长20个字符")
    private String nickname;

    /** 验证码 */
    @NotBlank(message = "验证码不能为空")
    private String code;
}
