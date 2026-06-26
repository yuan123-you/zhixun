package com.zhixun.dto.group;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class GroupInviteRequest {
    @NotNull(message = "群ID不能为空")
    private Long groupId;
    @NotNull(message = "邀请用户列表不能为空")
    @Size(min = 1, message = "至少邀请一个用户")
    private List<Long> userIds;
}
