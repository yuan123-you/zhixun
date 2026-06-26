package com.zhixun.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.dto.template.TemplateCreateRequest;
import com.zhixun.dto.template.TemplateQueryRequest;
import com.zhixun.vo.TemplateVO;

public interface TemplateService {
    Long createTemplate(Long userId, TemplateCreateRequest request);
    Page<TemplateVO> getTemplateList(TemplateQueryRequest request);
    TemplateVO getTemplateDetail(Long templateId);
    void useTemplate(Long templateId);
    void deleteTemplate(Long userId, Long templateId);
}