package com.zhixun.dto.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类创建/更新请求
 */
@Data
public class CategoryRequest {

    /** 父分类ID（0表示顶级分类） */
    private Long parentId = 0L;

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称最长50个字符")
    private String name;

    /** 图标 */
    private String icon;

    /** 排序值 */
    private Integer sort = 0;

    /** 状态：0-禁用，1-正常 */
    private Integer status = 1;
}
