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
 * 作品操作日志实体，对应 article_operate_log 表
 */
@Data
@TableName("article_operate_log")
public class ArticleOperateLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作品ID，对应 article_id */
    private Long articleId;

    /** 操作人ID，对应 operator_id */
    private Long operatorId;

    /** 操作类型：approve-审核通过，reject-驳回，takedown-下架 */
    private String action;

    /** 操作原因 */
    private String reason;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 操作类型常量 - 审核通过 */
    public static final String ACTION_APPROVE = "approve";

    /** 操作类型常量 - 驳回 */
    public static final String ACTION_REJECT = "reject";

    /** 操作类型常量 - 下架 */
    public static final String ACTION_TAKEDOWN = "takedown";
}
