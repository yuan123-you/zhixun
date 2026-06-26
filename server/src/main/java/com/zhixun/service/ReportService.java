package com.zhixun.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.dto.report.ArticleReportRequest;
import com.zhixun.dto.report.UserReportRequest;
import com.zhixun.dto.report.ReportHandleRequest;
import com.zhixun.vo.ReportVO;

public interface ReportService {
    void reportArticle(Long reporterId, ArticleReportRequest request);
    void reportUser(Long reporterId, UserReportRequest request);
    Page<ReportVO> getPendingReports(Integer page, Integer pageSize, String type);
    void handleReport(Long adminId, Long reportId, ReportHandleRequest request, String type);
}