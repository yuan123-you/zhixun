package com.zhixun.dto.topic;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TopicCreateRequest {
    @NotBlank(message = "璇濋鍚嶇О涓嶈兘涓虹┖")
    private String name;
    private String description;
    private String coverImage;
}