package com.zhixun.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 标签创建/更新请求
 */
@Data
public class TagRequest {

    /** 标签名称 */
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称最长50个字符")
    private String name;
}
