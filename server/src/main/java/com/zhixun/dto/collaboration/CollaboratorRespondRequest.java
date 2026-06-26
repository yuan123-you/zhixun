package com.zhixun.dto.collaboration;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CollaboratorRespondRequest {
    @NotNull(message = "状态不能为空")
    private Integer status;
}
