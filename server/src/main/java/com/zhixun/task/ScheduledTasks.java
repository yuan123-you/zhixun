package com.zhixun.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.ArticleViewHistory;
import com.zhixun.entity.OperationLog;
import com.zhixun.entity.ViewHistory;
import com.zhixun.mapper.ArticleViewHistoryMapper;
import com.zhixun.mapper.OperationLogMapper;
import com.zhixun.mapper.ViewHistoryMapper;
import com.zhixun.service.ArticleService;
import com.zhixun.service.HotScoreService;
import com.zhixun.service.RankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledTasks {

    private final HotScoreService hotScoreService;
    private final RankService rankService;
    private final ArticleService articleService;
    private final ViewHistoryMapper viewHistoryMapper;
    private final ArticleViewHistoryMapper articleViewHistoryMapper;
    private final OperationLogMapper operationLogMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /** 在线状态 Redis Key 前缀 */
    private static final String ONLINE_KEY_PREFIX = "user:online:";
    /** 在线超时时间（秒） */
    private static final long ONLINE_TIMEOUT_SECONDS = 90;

    /**
     * 每小时：批量更新热度分
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void batchUpdateHotScores() {
        log.info("===== 定时任务：批量更新热度分 =====");
        try {
            hotScoreService.batchUpdateHotScores();
        } catch (Exception e) {
            log.error("批量更新热度分失败: {}", e.getMessage());
        }
    }

    /**
     * 每小时：更新排行榜缓存
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void updateRankCache() {
        log.info("===== 定时任务：更新排行榜缓存 =====");
        try {
            // 预热日榜、周榜、月榜缓存
            rankService.getHotRank("daily", null, 50);
            rankService.getHotRank("weekly", null, 50);
            rankService.getHotRank("monthly", null, 50);
            log.info("排行榜缓存更新完成");
        } catch (Exception e) {
            log.error("更新排行榜缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 每日凌晨3点：清理过期数据
     * - 浏览记录90天
     * - 操作日志180天
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanExpiredData() {
        log.info("===== 定时任务：清理过期数据 =====");
        try {
            // 清理90天前的浏览记录
            LocalDateTime viewHistoryCutoff = LocalDateTime.now().minusDays(90);
            int viewHistoryDeleted = viewHistoryMapper.delete(
                    new LambdaQueryWrapper<ViewHistory>()
                            .lt(ViewHistory::getCreatedAt, viewHistoryCutoff));
            log.info("清理浏览记录 {} 条", viewHistoryDeleted);

            // 清理90天前的文章浏览记录
            int articleViewHistoryDeleted = articleViewHistoryMapper.delete(
                    new LambdaQueryWrapper<ArticleViewHistory>()
                            .lt(ArticleViewHistory::getCreateTime, viewHistoryCutoff));
            log.info("清理文章浏览记录 {} 条", articleViewHistoryDeleted);

            // 清理180天前的操作日志
            LocalDateTime operationLogCutoff = LocalDateTime.now().minusDays(180);
            int operationLogDeleted = operationLogMapper.delete(
                    new LambdaQueryWrapper<OperationLog>()
                            .lt(OperationLog::getCreatedAt, operationLogCutoff));
            log.info("清理操作日志 {} 条", operationLogDeleted);
        } catch (Exception e) {
            log.error("清理过期数据失败: {}", e.getMessage());
        }
    }

    /**
     * 每5分钟：更新在线状态（超过90s未心跳置离线）
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void updateOnlineStatus() {
        log.info("===== 定时任务：更新在线状态 =====");
        try {
            // 扫描所有在线用户的 Key
            Set<String> onlineKeys = stringRedisTemplate.keys(ONLINE_KEY_PREFIX + "*");
            if (onlineKeys == null || onlineKeys.isEmpty()) {
                return;
            }

            long now = System.currentTimeMillis();
            int offlineCount = 0;

            for (String key : onlineKeys) {
                String lastHeartbeatStr = stringRedisTemplate.opsForValue().get(key);
                if (lastHeartbeatStr == null) {
                    continue;
                }

                try {
                    long lastHeartbeat = Long.parseLong(lastHeartbeatStr);
                    if (now - lastHeartbeat > ONLINE_TIMEOUT_SECONDS * 1000) {
                        // 超时，删除在线状态
                        stringRedisTemplate.delete(key);
                        offlineCount++;
                    }
                } catch (NumberFormatException e) {
                    // 无效数据，删除
                    stringRedisTemplate.delete(key);
                    offlineCount++;
                }
            }

            if (offlineCount > 0) {
                log.info("将 {} 个用户置为离线", offlineCount);
            }
        } catch (Exception e) {
            log.error("更新在线状态失败: {}", e.getMessage());
        }
    }

    /**
     * 每分钟：检查定时发布文章
     * 查询所有 status=PENDING 且 publish_at <= now() 的文章，将其状态更新为 PUBLISHED
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkScheduledPublish() {
        log.info("===== 定时任务：检查定时发布文章 =====");
        try {
            articleService.publishScheduledArticles();
        } catch (Exception e) {
            log.error("定时发布检查失败: {}", e.getMessage());
        }
    }

    /**
     * 每30分钟：检查缓存一致性
     * 对比 Redis 缓存中的文章详情与数据库最新数据，
     * 发现不一致时自动清除过期缓存
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void checkCacheConsistency() {
        log.info("===== 定时任务：检查缓存一致性 =====");
        try {
            checkArticleDetailConsistency();
        } catch (Exception e) {
            log.error("缓存一致性检查失败: {}", e.getMessage());
        }
    }

    /**
     * 检查文章详情缓存一致性
     * 对比 Redis 缓存中的文章详情与数据库最新数据，发现不一致时自动清除过期缓存
     */
    private void checkArticleDetailConsistency() {
        Map<String, Object> result = articleService.checkArticleDetailConsistency();
        int checkedCount = result.get("checkedCount") instanceof Integer ? (Integer) result.get("checkedCount") : 0;
        int inconsistentCount = result.get("inconsistentCount") instanceof Integer ? (Integer) result.get("inconsistentCount") : 0;
        int fixedCount = result.get("fixedCount") instanceof Integer ? (Integer) result.get("fixedCount") : 0;
        if (inconsistentCount > 0) {
            log.warn("缓存一致性检查：检查 {} 条，不一致 {} 条，已修复 {} 条", checkedCount, inconsistentCount, fixedCount);
        } else {
            log.info("缓存一致性检查：检查 {} 条，全部一致", checkedCount);
        }
    }
}
