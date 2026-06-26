package com.zhixun.controller;

import com.zhixun.common.result.PageResult;
import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.template.TemplateCreateRequest;
import com.zhixun.dto.template.TemplateQueryRequest;
import com.zhixun.service.TemplateService;
import com.zhixun.vo.TemplateVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/templates")
@RequiredArgsConstructor
public class TemplateController {
    private final TemplateService templateService;
    private final SecurityUtil securityUtil;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public R<Long> create(@Valid @RequestBody TemplateCreateRequest request) {
        return R.ok(templateService.createTemplate(securityUtil.getCurrentUserId(), request));
    }

    @GetMapping
    public R<PageResult<TemplateVO>> list(TemplateQueryRequest request) {
        Page<TemplateVO> page = templateService.getTemplateList(request);
        return R.ok(new PageResult<>(page.getRecords(), page.getTotal(), request.getPage(), request.getPageSize()));
    }

    @GetMapping("/{id}")
    public R<TemplateVO> detail(@PathVariable Long id) {
        return R.ok(templateService.getTemplateDetail(id));
    }

    @PostMapping("/{id}/use")
    @PreAuthorize("isAuthenticated()")
    public R<Void> useTemplate(@PathVariable Long id) {
        templateService.useTemplate(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public R<Void> delete(@PathVariable Long id) {
        templateService.deleteTemplate(securityUtil.getCurrentUserId(), id);
        return R.ok();
    }
}