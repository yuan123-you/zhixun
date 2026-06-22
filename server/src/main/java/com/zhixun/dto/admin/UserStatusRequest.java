package com.zhixun.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户状态变更请求
 */
@Data
public class UserStatusRequest {

    /** 用户状态：0-禁用，1-正常 */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 变更原因 */
    private String reason;
}
