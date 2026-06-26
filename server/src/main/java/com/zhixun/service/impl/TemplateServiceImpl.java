package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.dto.template.TemplateCreateRequest;
import com.zhixun.dto.template.TemplateQueryRequest;
import com.zhixun.entity.ContentTemplate;
import com.zhixun.mapper.ContentTemplateMapper;
import com.zhixun.service.TemplateService;
import com.zhixun.vo.TemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemplateServiceImpl implements TemplateService {
    private final ContentTemplateMapper contentTemplateMapper;

    @Override @Transactional
    public Long createTemplate(Long userId, TemplateCreateRequest request) {
        ContentTemplate t = new ContentTemplate();
        t.setName(request.getName()); t.setDescription(request.getDescription());
        t.setCoverImage(request.getCoverImage()); t.setContent(request.getContent());
        t.setCategory(request.getCategory()); t.setTags(request.getTags());
        t.setCreatorId(userId);
        contentTemplateMapper.insert(t);
        return t.getId();
    }

    @Override
    public Page<TemplateVO> getTemplateList(TemplateQueryRequest request) {
        Page<ContentTemplate> page = new Page<>(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<ContentTemplate> w = new LambdaQueryWrapper<>();
        w.eq(ContentTemplate::getStatus, 0);
        if (request.getCategory() != null && !request.getCategory().isEmpty())
            w.eq(ContentTemplate::getCategory, request.getCategory());
        if (request.getKeyword() != null && !request.getKeyword().isEmpty())
            w.and(qw -> qw.like(ContentTemplate::getName, request.getKeyword()).or().like(ContentTemplate::getTags, request.getKeyword()));
        w.orderByDesc(ContentTemplate::getUseCount);
        Page<ContentTemplate> result = contentTemplateMapper.selectPage(page, w);
        return (Page<TemplateVO>) result.convert(this::toVO);
    }

    @Override
    public TemplateVO getTemplateDetail(Long templateId) {
        ContentTemplate t = contentTemplateMapper.selectById(templateId);
        return t != null ? toVO(t) : null;
    }

    @Override @Transactional
    public void useTemplate(Long templateId) {
        ContentTemplate t = contentTemplateMapper.selectById(templateId);
        if (t != null) { t.setUseCount(t.getUseCount() + 1); contentTemplateMapper.updateById(t); }
    }

    @Override @Transactional
    public void deleteTemplate(Long userId, Long templateId) {
        ContentTemplate t = contentTemplateMapper.selectById(templateId);
        if (t == null || (!t.getCreatorId().equals(userId) && t.getIsOfficial() == 0))
            throw new RuntimeException("无权删除此模板");
        t.setStatus(1); contentTemplateMapper.updateById(t);
    }

    private TemplateVO toVO(ContentTemplate t) {
        TemplateVO vo = new TemplateVO(); vo.setId(t.getId()); vo.setName(t.getName());
        vo.setDescription(t.getDescription()); vo.setCoverImage(t.getCoverImage());
        vo.setCategory(t.getCategory()); vo.setContent(t.getContent());
        vo.setTags(t.getTags()); vo.setUseCount(t.getUseCount());
        vo.setCreatorName(t.getCreatorName()); return vo;
    }
}