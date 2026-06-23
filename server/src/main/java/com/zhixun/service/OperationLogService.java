package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.entity.OperationLog;

import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 */
public interface OperationLogService {

    /**
     * 记录操作日志（异步）
     *
     * @param operatorId 操作人ID
     * @param module     操作模块
     * @param action     操作动作
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @param detail     操作详情
     * @param ip         请求IP
     */
    void log(Long operatorId, String module, String action, String targetType, Long targetId, String detail, String ip);

    /**
     * 查询操作日志列表
     *
     * @param operatorId 操作人ID（可选）
     * @param module     操作模块（可选）
     * @param startDate  起始日期（可选）
     * @param endDate    结束日期（可选）
     * @param page       页码
     * @param pageSize   每页大小
     * @return 操作日志分页结果
     */
    PageResult<OperationLog> getLogs(Long operatorId, String module, String startDate, String endDate, Integer page, Integer pageSize);

    /**
     * 导出操作日志为CSV格式
     *
     * @param startDate 起始日期（可选）
     * @param endDate    结束日期（可选）
     * @param module     操作模块（可选）
     * @return CSV格式字符串
     */
    String exportLogs(String startDate, String endDate, String module);

    /**
     * 获取操作日志统计信息
     *
     * @param startDate 起始日期（可选）
     * @param endDate    结束日期（可选）
     * @return 统计信息（按模块、按动作、按日期的计数）
     */
    Map<String, Object> getLogStats(String startDate, String endDate);
}
