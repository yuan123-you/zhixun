package com.zhixun.dto.report;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportHandleRequest {
    @NotNull(message = "处理状态不能为空")
    private Integer status;
    private String remark;
}
