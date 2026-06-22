package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.common.result.PageResult;
import com.zhixun.entity.OperationLog;
import com.zhixun.mapper.OperationLogMapper;
import com.zhixun.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    @Override
    @Async
    public void log(Long operatorId, String module, String action, String targetType, Long targetId, String detail, String ip) {
        try {
            OperationLog operationLog = new OperationLog();
            operationLog.setOperatorId(operatorId);
            operationLog.setModule(module);
            operationLog.setAction(action);
            operationLog.setIp(ip);
            operationLog.setDetail(buildDetail(targetType, targetId, detail));
            operationLogMapper.insert(operationLog);
        } catch (Exception e) {
            log.error("记录操作日志失败: {}", e.getMessage());
        }
    }

    @Override
    public PageResult<OperationLog> getLogs(Long operatorId, String module, String startDate, String endDate, Integer page, Integer pageSize) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        // 操作人筛选
        if (operatorId != null) {
            wrapper.eq(OperationLog::getOperatorId, operatorId);
        }

        // 模块筛选
        if (StringUtils.hasText(module)) {
            wrapper.eq(OperationLog::getModule, module);
        }

        // 时间范围筛选
        if (StringUtils.hasText(startDate)) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(OperationLog::getCreatedAt, start);
        }
        if (StringUtils.hasText(endDate)) {
            LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            wrapper.lt(OperationLog::getCreatedAt, end);
        }

        wrapper.orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> logPage = new Page<>(page, pageSize);
        Page<OperationLog> result = operationLogMapper.selectPage(logPage, wrapper);

        return new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize);
    }

    /**
     * 构建操作详情
     */
    private String buildDetail(String targetType, Long targetId, String detail) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(targetType)) {
            sb.append("目标类型:").append(targetType).append(";");
        }
        if (targetId != null) {
            sb.append("目标ID:").append(targetId).append(";");
        }
        if (StringUtils.hasText(detail)) {
            sb.append("详情:").append(detail);
        }
        return sb.toString();
    }
}
