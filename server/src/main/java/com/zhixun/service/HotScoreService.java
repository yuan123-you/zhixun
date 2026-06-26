package com.zhixun.service;

import com.zhixun.entity.Article;

import java.util.List;

/**
 * 热度分计算服务接口
 */
public interface HotScoreService {

    /**
     * 计算单篇作品热度分
     * 公式: hot_score = (view*1 + like*5 + comment*3 + collect*8) / max(1, hours_since_publish)^1.5
     *
     * @param article 作品实体
     * @return 热度分
     */
    double calculateHotScore(Article article);

    /**
     * 更新作品热度分到 Redis
     *
     * @param articleId 作品ID
     */
    void updateHotScore(Long articleId);

    /**
     * 批量更新所有已发布作品的热度分（定时任务调用）
     */
    void batchUpdateHotScores();
}
