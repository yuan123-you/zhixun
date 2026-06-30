package com.zhixun.dto.group;

import lombok.Data;

@Data
public class GroupUpdateRequest {
    private String name;
    private String avatar;
    private String description;
}
