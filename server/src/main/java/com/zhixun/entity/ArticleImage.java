package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhixun.enums.ImageTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文章图片实体，对应 cms_article_image 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_article_image")
public class ArticleImage {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文章ID，对应 article_id */
    private Long articleId;

    /** 图片地址 */
    private String url;

    /** 图片类型 */
    private ImageTypeEnum type;

    /** 宽度 */
    private Integer width;

    /** 高度 */
    private Integer height;

    /** 排序值，对应 sort_order */
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
