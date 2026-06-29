package com.zhixun.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class GroupMessageRequest {
    @NotNull(message = "缇D涓嶈兘涓虹┖")
    private Long groupId;
    @NotBlank(message = "娑堟伅鍐呭涓嶈兘涓虹┖")
    private String content;
    private String messageType = "text";
    private List<Long> mentionedUserIds;
}
