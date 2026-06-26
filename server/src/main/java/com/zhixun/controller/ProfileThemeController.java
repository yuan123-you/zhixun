package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.dto.profile.ProfileThemeRequest;
import com.zhixun.service.ProfileThemeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/profile/theme")
@RequiredArgsConstructor
public class ProfileThemeController {
    private final ProfileThemeService profileThemeService;
    private final SecurityUtil securityUtil;

    @GetMapping("/{userId}")
    public R<?> getTheme(@PathVariable Long userId) {
        return R.ok(profileThemeService.getTheme(userId));
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    public R<Void> saveTheme(@Valid @RequestBody ProfileThemeRequest request) {
        profileThemeService.saveTheme(securityUtil.getCurrentUserId(), request);
        return R.ok();
    }
}