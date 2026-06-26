package com.zhixun.dto.collaboration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CollaboratorInviteRequest {
    @NotNull(message = "鏂囩珷ID涓嶈兘涓虹┖")
    private Long articleId;
    @NotNull(message = "鐢ㄦ埛ID涓嶈兘涓虹┖")
    private Long userId;
    @NotBlank(message = "鏉冮檺涓嶈兘涓虹┖")
    private String permission;
}