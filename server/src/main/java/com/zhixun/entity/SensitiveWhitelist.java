package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 敏感词白名单实体，对应 sys_sensitive_whitelist 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_sensitive_whitelist")
public class SensitiveWhitelist {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 白名单词汇 */
    private String word;

    /** 创建人ID */
    private Long createdBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
