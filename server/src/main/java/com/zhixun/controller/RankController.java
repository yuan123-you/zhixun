package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.service.RankService;
import com.zhixun.vo.HotArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 排行控制器
 */
@RestController
@RequestMapping("/v1/rank")
@RequiredArgsConstructor
public class RankController {

    private final RankService rankService;

    /**
     * 热点排行榜
     */
    @GetMapping("/hot")
    public R<List<HotArticleVO>> hotRank(
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "20") Integer limit) {
        return R.ok(rankService.getHotRank(period, categoryId, limit));
    }
}
