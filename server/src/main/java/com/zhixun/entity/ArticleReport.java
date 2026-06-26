package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_article_report")
public class ArticleReport implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long reporterId;
    private String reason;
    private String description;
    private Integer status = 0;
    private Long handledBy;
    private LocalDateTime handledAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(exist = false)
    private String reporterName;
    @TableField(exist = false)
    private String articleTitle;
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_IGNORED = 1;
    public static final int STATUS_DELETED = 2;
}