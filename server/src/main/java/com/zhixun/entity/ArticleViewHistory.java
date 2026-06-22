package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章浏览历史实体，对应 article_view_history 表
 */
@Data
@TableName("article_view_history")
public class ArticleViewHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文章ID，对应 article_id */
    private Long articleId;

    /** 用户ID（未登录为null），对应 user_id */
    private Long userId;

    /** 访问IP */
    private String ip;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
