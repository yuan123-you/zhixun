package com.zhixun.vo;

import lombok.Data;

import java.util.List;

/**
 * 分类视图（含子分类树）
 */
@Data
public class CategoryVO {

    /** 分类ID */
    private Long id;

    /** 父分类ID */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 排序值 */
    private Integer sort;

    /** 图标 */
    private String icon;

    /** 子分类列表 */
    private List<CategoryVO> children;
}
