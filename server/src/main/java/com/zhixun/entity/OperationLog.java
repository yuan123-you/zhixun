package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志实体，对应 sys_operation_log 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_operation_log")
public class OperationLog {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户ID，对应 operator_id */
    private Long operatorId;

    /** 操作模块 */
    private String module;

    /** 操作动作 */
    private String action;

    /** 目标类型，对应 target_type */
    private String targetType;

    /** 目标ID，对应 target_id */
    private Long targetId;

    /** 操作详情 */
    private String detail;

    /** 请求IP */
    private String ip;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
