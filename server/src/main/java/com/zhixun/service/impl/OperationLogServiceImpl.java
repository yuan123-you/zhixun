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
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final OperationLogMapper operationLogMapper;

    private static final DateTimeFormatter CSV_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    @Override
    public String exportLogs(String startDate, String endDate, String module) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

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

        // 限制最大导出数量
        wrapper.last("LIMIT 10000");

        List<OperationLog> logs = operationLogMapper.selectList(wrapper);

        // 构建CSV
        StringBuilder csv = new StringBuilder();
        csv.append("ID,操作人ID,操作模块,操作动作,目标类型,目标ID,操作详情,请求IP,操作时间\n");

        for (OperationLog opLog : logs) {
            csv.append(escapeCsv(String.valueOf(opLog.getId()))).append(",");
            csv.append(escapeCsv(String.valueOf(opLog.getOperatorId()))).append(",");
            csv.append(escapeCsv(opLog.getModule())).append(",");
            csv.append(escapeCsv(opLog.getAction())).append(",");
            csv.append(escapeCsv(opLog.getTargetType())).append(",");
            csv.append(escapeCsv(opLog.getTargetId() != null ? String.valueOf(opLog.getTargetId()) : "")).append(",");
            csv.append(escapeCsv(opLog.getDetail())).append(",");
            csv.append(escapeCsv(opLog.getIp())).append(",");
            csv.append(escapeCsv(opLog.getCreatedAt() != null ? opLog.getCreatedAt().format(CSV_DATE_FORMATTER) : "")).append("\n");
        }

        return csv.toString();
    }

    @Override
    public Map<String, Object> getLogStats(String startDate, String endDate) {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();

        // 时间范围筛选
        if (StringUtils.hasText(startDate)) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            wrapper.ge(OperationLog::getCreatedAt, start);
        }
        if (StringUtils.hasText(endDate)) {
            LocalDateTime end = LocalDate.parse(endDate).plusDays(1).atStartOfDay();
            wrapper.lt(OperationLog::getCreatedAt, end);
        }

        List<OperationLog> logs = operationLogMapper.selectList(wrapper);

        // 按模块统计
        Map<String, Long> byModule = logs.stream()
                .collect(Collectors.groupingBy(
                        opLog -> opLog.getModule() != null ? opLog.getModule() : "未知",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        // 按动作统计
        Map<String, Long> byAction = logs.stream()
                .collect(Collectors.groupingBy(
                        opLog -> opLog.getAction() != null ? opLog.getAction() : "未知",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        // 按日期统计
        Map<String, Long> byDate = logs.stream()
                .collect(Collectors.groupingBy(
                        opLog -> opLog.getCreatedAt() != null
                                ? opLog.getCreatedAt().toLocalDate().toString()
                                : "未知",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        stats.put("total", logs.size());
        stats.put("byModule", byModule);
        stats.put("byAction", byAction);
        stats.put("byDate", byDate);

        return stats;
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

    /**
     * CSV字段转义（处理逗号、引号、换行符）
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
