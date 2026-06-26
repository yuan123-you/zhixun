package com.zhixun.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportVO {
    private Long id;
    private String type;
    private Long targetId;
    private String targetTitle;
    private Long reporterId;
    private String reporterName;
    private String reason;
    private String description;
    private Integer status;
    private Long handledBy;
    private String handlerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime handledAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}