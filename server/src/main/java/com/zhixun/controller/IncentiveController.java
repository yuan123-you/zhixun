package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.common.util.SecurityUtil;
import com.zhixun.service.IncentiveService;
import com.zhixun.vo.BadgeVO;
import com.zhixun.vo.CheckInVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/v1/incentive")
@RequiredArgsConstructor
public class IncentiveController {
    private final IncentiveService incentiveService;
    private final SecurityUtil securityUtil;

    @PostMapping("/checkin")
    @PreAuthorize("isAuthenticated()")
    public R<CheckInVO> checkIn() {
        return R.ok(incentiveService.checkIn(securityUtil.getCurrentUserId()));
    }

    @GetMapping("/checkin/status")
    @PreAuthorize("isAuthenticated()")
    public R<CheckInVO> checkInStatus() {
        return R.ok(incentiveService.getCheckInStatus(securityUtil.getCurrentUserId()));
    }

    @GetMapping("/badges")
    @PreAuthorize("isAuthenticated()")
    public R<List<BadgeVO>> allBadges() {
        return R.ok(incentiveService.getAllBadges(securityUtil.getCurrentUserId()));
    }

    @GetMapping("/my-badges")
    @PreAuthorize("isAuthenticated()")
    public R<List<BadgeVO>> myBadges() {
        return R.ok(incentiveService.getUserBadges(securityUtil.getCurrentUserId()));
    }
}