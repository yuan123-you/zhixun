package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.dto.report.ArticleReportRequest;
import com.zhixun.dto.report.ReportHandleRequest;
import com.zhixun.dto.report.UserReportRequest;
import com.zhixun.entity.ArticleReport;
import com.zhixun.entity.UserReport;
import com.zhixun.mapper.ArticleReportMapper;
import com.zhixun.mapper.UserReportMapper;
import com.zhixun.service.ReportService;
import com.zhixun.vo.ReportVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ArticleReportMapper articleReportMapper;
    private final UserReportMapper userReportMapper;

    @Override @Transactional
    public void reportArticle(Long reporterId, ArticleReportRequest request) {
        ArticleReport r = new ArticleReport();
        r.setArticleId(request.getArticleId()); r.setReporterId(reporterId);
        r.setReason(request.getReason()); r.setDescription(request.getDescription());
        articleReportMapper.insert(r);
    }

    @Override @Transactional
    public void reportUser(Long reporterId, UserReportRequest request) {
        UserReport r = new UserReport();
        r.setReportedUserId(request.getReportedUserId()); r.setReporterId(reporterId);
        r.setReason(request.getReason()); r.setDescription(request.getDescription());
        userReportMapper.insert(r);
    }

    @Override
    public Page<ReportVO> getPendingReports(Integer page, Integer pageSize, String type) {
        if ("user".equals(type)) {
            Page<UserReport> pg = new Page<>(page, pageSize);
            LambdaQueryWrapper<UserReport> w = new LambdaQueryWrapper<>();
            w.eq(UserReport::getStatus, 0).orderByDesc(UserReport::getCreatedAt);
            Page<UserReport> result = userReportMapper.selectPage(pg, w);
            return (Page<ReportVO>) result.convert(r -> toReportVO(r));
        }
        Page<ArticleReport> pg = new Page<>(page, pageSize);
        LambdaQueryWrapper<ArticleReport> w = new LambdaQueryWrapper<>();
        w.eq(ArticleReport::getStatus, 0).orderByDesc(ArticleReport::getCreatedAt);
        Page<ArticleReport> result = articleReportMapper.selectPage(pg, w);
        return (Page<ReportVO>) result.convert(r -> toReportVO(r));
    }

    @Override @Transactional
    public void handleReport(Long adminId, Long reportId, ReportHandleRequest request, String type) {
        if ("user".equals(type)) {
            UserReport r = userReportMapper.selectById(reportId);
            if (r != null) { r.setStatus(request.getStatus()); r.setHandledBy(adminId); userReportMapper.updateById(r); }
        } else {
            ArticleReport r = articleReportMapper.selectById(reportId);
            if (r != null) { r.setStatus(request.getStatus()); r.setHandledBy(adminId); articleReportMapper.updateById(r); }
        }
    }

    private ReportVO toReportVO(ArticleReport r) {
        ReportVO vo = new ReportVO(); vo.setId(r.getId()); vo.setType("article");
        vo.setTargetId(r.getArticleId()); vo.setTargetTitle(r.getArticleTitle());
        vo.setReporterId(r.getReporterId()); vo.setReporterName(r.getReporterName());
        vo.setReason(r.getReason()); vo.setDescription(r.getDescription());
        vo.setStatus(r.getStatus()); vo.setHandledBy(r.getHandledBy());
        vo.setHandledAt(r.getHandledAt()); vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }

    private ReportVO toReportVO(UserReport r) {
        ReportVO vo = new ReportVO(); vo.setId(r.getId()); vo.setType("user");
        vo.setTargetId(r.getReportedUserId()); vo.setTargetTitle(r.getReportedUserName());
        vo.setReporterId(r.getReporterId()); vo.setReporterName(r.getReporterName());
        vo.setReason(r.getReason()); vo.setDescription(r.getDescription());
        vo.setStatus(r.getStatus()); vo.setHandledBy(r.getHandledBy());
        vo.setHandledAt(r.getHandledAt()); vo.setCreatedAt(r.getCreatedAt());
        return vo;
    }
}