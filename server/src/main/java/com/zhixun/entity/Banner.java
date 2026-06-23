package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 轮播图实体，对应 cms_banner 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_banner")
public class Banner implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 图片地址 */
    private String imageUrl;

    /** 链接地址 */
    private String linkUrl;

    /** 链接类型：1-文章，2-外链 */
    private Integer linkType;

    /** 排序值 */
    private Integer sortOrder;

    /** 开始展示时间 */
    private LocalDateTime startTime;

    /** 结束展示时间 */
    private LocalDateTime endTime;

    /** 状态：0-禁用，1-启用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
