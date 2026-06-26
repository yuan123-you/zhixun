package com.zhixun.dto.group;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupCreateRequest {
    @NotBlank(message = "群名称不能为空")
    private String name;
    private String description;
    private String avatar;
    private Integer isPublic;
}
