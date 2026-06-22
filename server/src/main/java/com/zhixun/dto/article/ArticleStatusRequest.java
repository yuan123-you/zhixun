package com.zhixun.dto.article;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文章状态变更请求
 */
@Data
public class ArticleStatusRequest {

    /** 目标状态 */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 拒绝原因（状态为拒绝时必填） */
    private String rejectReason;
}
