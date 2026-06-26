package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 作品-标签关联实体，对应 cms_article_tag 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("cms_article_tag")
public class ArticleTag implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作品ID，对应 article_id */
    private Long articleId;

    /** 标签ID，对应 tag_id */
    private Long tagId;
}
