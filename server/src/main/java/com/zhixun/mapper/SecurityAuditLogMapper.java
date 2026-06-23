package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.SecurityAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 安全审计日志 Mapper 接口
 */
@Mapper
public interface SecurityAuditLogMapper extends BaseMapper<SecurityAuditLog> {
}
