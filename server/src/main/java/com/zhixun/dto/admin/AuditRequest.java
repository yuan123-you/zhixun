package com.zhixun.dto.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审核请求
 */
@Data
public class AuditRequest {

    /** 审核状态 */
    @NotNull(message = "审核状态不能为空")
    private Integer status;

    /** 审核意见/拒绝原因 */
    private String reason;
}
