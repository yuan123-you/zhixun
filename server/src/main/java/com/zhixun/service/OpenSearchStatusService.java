package com.zhixun.service;

import java.util.Map;

/**
 * OpenSearch 状态服务接口
 */
public interface OpenSearchStatusService {

    /**
     * 获取 OpenSearch 状态信息
     * 包括连接性、索引健康状况、文档数等
     */
    Map<String, Object> getStatus();
}
