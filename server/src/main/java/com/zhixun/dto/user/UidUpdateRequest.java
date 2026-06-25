package com.zhixun.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * UID 修改请求
 */
@Data
public class UidUpdateRequest {

    /** 新的UID，仅允许大小写字母、数字、下划线 */
    @NotBlank(message = "UID不能为空")
    @Size(min = 4, max = 20, message = "UID长度为4-20个字符")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "UID仅允许大小写字母、数字和下划线")
    private String uid;
}
