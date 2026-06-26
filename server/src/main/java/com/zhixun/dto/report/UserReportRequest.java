package com.zhixun.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserReportRequest {
    @NotNull(message = "琚妇鎶ョ敤鎴稩D涓嶈兘涓虹┖")
    private Long reportedUserId;
    @NotBlank(message = "涓炬姤鍘熷洜涓嶈兘涓虹┖")
    private String reason;
    private String description;
}