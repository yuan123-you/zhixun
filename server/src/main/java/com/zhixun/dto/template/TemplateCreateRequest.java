package com.zhixun.dto.template;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemplateCreateRequest {
    @NotBlank(message = "妯℃澘鍚嶇О涓嶈兘涓虹┖")
    private String name;
    private String description;
    private String coverImage;
    @NotBlank(message = "妯℃澘鍐呭涓嶈兘涓虹┖")
    private String content;
    private String category;
    private String tags;
}