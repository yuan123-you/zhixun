package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分类实体，对应 cms_category 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_category")
public class Category implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父分类ID，对应 parent_id */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 图标 */
    private String icon;

    /** 排序值，对应 sort_order */
    private Integer sortOrder;

    /** 状态：0-禁用，1-正常 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
