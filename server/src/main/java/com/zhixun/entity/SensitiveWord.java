package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.zhixun.enums.SensitiveLevelEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 敏感词实体，对应 sys_sensitive_word 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_sensitive_word")
public class SensitiveWord {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 敏感词内容 */
    private String word;

    /** 敏感词级别 */
    private SensitiveLevelEnum level;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
