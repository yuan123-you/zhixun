package com.zhixun.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ArticleReportRequest {
    @NotNull(message = "鏂囩珷ID涓嶈兘涓虹┖")
    private Long articleId;
    @NotBlank(message = "涓炬姤鍘熷洜涓嶈兘涓虹┖")
    private String reason;
    private String description;
}