package com.zhixun.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 个人资料更新请求
 */
@Data
public class ProfileUpdateRequest {

    /** 昵称 */
    @Size(max = 20, message = "昵称最长20个字符")
    private String nickname;

    /** 头像 */
    private String avatar;

    /** 邮箱 */
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 手机号 */
    private String phone;

    /** 简介 */
    @Size(max = 500, message = "简介最长500个字符")
    private String bio;
}
