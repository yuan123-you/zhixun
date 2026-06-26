package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.report.ArticleReportRequest;
import com.zhixun.dto.report.ReportHandleRequest;
import com.zhixun.dto.report.UserReportRequest;
import com.zhixun.service.ReportService;
import com.zhixun.vo.ReportVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;
    private final SecurityUtil securityUtil;

    @PostMapping("/article")
    @PreAuthorize("isAuthenticated()")
    public R<Void> reportArticle(@Valid @RequestBody ArticleReportRequest request) {
        reportService.reportArticle(securityUtil.getCurrentUserId(), request);
        return R.ok();
    }

    @PostMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public R<Void> reportUser(@Valid @RequestBody UserReportRequest request) {
        reportService.reportUser(securityUtil.getCurrentUserId(), request);
        return R.ok();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public R<PageResult<ReportVO>> pending(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "20") Integer pageSize,
                                            @RequestParam(defaultValue = "article") String type) {
        Page<ReportVO> result = reportService.getPendingReports(page, pageSize, type);
        return R.ok(new PageResult<>(result.getRecords(), result.getTotal(), page, pageSize));
    }

    @PutMapping("/{id}/handle")
    @PreAuthorize("hasRole('ADMIN')")
    public R<Void> handle(@PathVariable Long id, @Valid @RequestBody ReportHandleRequest request,
                           @RequestParam(defaultValue = "article") String type) {
        reportService.handleReport(securityUtil.getCurrentUserId(), id, request, type);
        return R.ok();
    }
}