package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.service.RankService;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开用户接口（C端）
 * 提供推荐用户等无需认证的端点
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class PublicUserController {

    private final RankService rankService;

    /**
     * 推荐用户（公开）
     * 基于近期文章浏览量推荐活跃用户
     */
    @GetMapping("/recommend")
    public R<List<UserVO>> recommend(@RequestParam(defaultValue = "5") Integer limit) {
        return R.ok(rankService.getHotUsers(limit));
    }
}
