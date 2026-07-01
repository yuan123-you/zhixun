package com.zhixun.dto.topic;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TopicCreateRequest {
    @NotBlank(message = "话题名称不能为空")
    private String name;
    private String description;
    private String coverImage;
    private Boolean isOfficial;
}