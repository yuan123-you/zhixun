package com.zhixun.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupMessageRequest {
    @NotNull(message = "缇D涓嶈兘涓虹┖")
    private Long groupId;
    @NotBlank(message = "娑堟伅鍐呭涓嶈兘涓虹┖")
    private String content;
    private String messageType = "text";
}