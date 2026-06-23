package com.zhixun.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 安全审计日志实体，对应 sys_security_audit_log 表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_security_audit_log")
public class SecurityAuditLog {

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 事件类型 */
    private String eventType;

    /** 用户ID */
    private Long userId;

    /** 请求IP */
    private String ip;

    /** 请求方法 */
    private String method;

    /** 请求路径 */
    private String path;

    /** 详情 */
    private String detail;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
