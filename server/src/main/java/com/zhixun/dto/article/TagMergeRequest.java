package com.zhixun.dto.article;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 标签合并请求
 */
@Data
public class TagMergeRequest {

    /** 源标签ID（将被合并后删除） */
    @NotNull(message = "源标签ID不能为空")
    private Long sourceTagId;

    /** 目标标签ID（合并到的目标标签） */
    @NotNull(message = "目标标签ID不能为空")
    private Long targetTagId;
}
