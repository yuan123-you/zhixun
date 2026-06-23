package com.zhixun.service.impl;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 搜索质量评估工具类
 * 用于离线评估搜索质量，计算以下指标：
 * - Precision@K: 前K条结果中相关结果占比
 * - Recall@K: 前K条结果覆盖相关结果比例
 * - NDCG@K: 归一化折损累积增益
 */
@Slf4j
public class SearchQualityEvaluator {

    /**
     * 评估结果
     */
    public static class EvaluationResult {

        /** 评估关键词 */
        private String keyword;

        /** Precision@10 */
        private double precisionAt10;

        /** Recall@100 */
        private double recallAt100;

        /** NDCG@10 */
        private double ndcgAt10;

        /** 参与评估的结果数 */
        private int evaluatedCount;

        /** 相关结果总数 */
        private int relevantCount;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public double getPrecisionAt10() {
            return precisionAt10;
        }

        public void setPrecisionAt10(double precisionAt10) {
            this.precisionAt10 = precisionAt10;
        }

        public double getRecallAt100() {
            return recallAt100;
        }

        public void setRecallAt100(double recallAt100) {
            this.recallAt100 = recallAt100;
        }

        public double getNdcgAt10() {
            return ndcgAt10;
        }

        public void setNdcgAt10(double ndcgAt10) {
            this.ndcgAt10 = ndcgAt10;
        }

        public int getEvaluatedCount() {
            return evaluatedCount;
        }

        public void setEvaluatedCount(int evaluatedCount) {
            this.evaluatedCount = evaluatedCount;
        }

        public int getRelevantCount() {
            return relevantCount;
        }

        public void setRelevantCount(int relevantCount) {
            this.relevantCount = relevantCount;
        }

        @Override
        public String toString() {
            return String.format(
                    "EvaluationResult{keyword='%s', P@10=%.4f, R@100=%.4f, NDCG@10=%.4f, evaluated=%d, relevant=%d}",
                    keyword, precisionAt10, recallAt100, ndcgAt10, evaluatedCount, relevantCount);
        }
    }

    /**
     * 带相关性评分的搜索结果项
     */
    public static class RatedItem {

        /** 结果ID */
        private String id;

        /** 相关性评分（0-不相关，1-部分相关，2-高度相关） */
        private int relevanceScore;

        public RatedItem() {
        }

        public RatedItem(String id, int relevanceScore) {
            this.id = id;
            this.relevanceScore = relevanceScore;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public int getRelevanceScore() {
            return relevanceScore;
        }

        public void setRelevanceScore(int relevanceScore) {
            this.relevanceScore = relevanceScore;
        }
    }

    /**
     * 计算Precision@K：前K条结果中相关结果占比
     *
     * @param results       搜索结果列表（按排名排序）
     * @param relevantIds   相关结果的ID集合
     * @param k             截断位置
     * @return Precision@K 值
     */
    public double precisionAtK(List<String> results, Set<String> relevantIds, int k) {
        if (results == null || results.isEmpty() || relevantIds == null || relevantIds.isEmpty()) {
            return 0.0;
        }

        int limit = Math.min(k, results.size());
        long relevantInTopK = results.subList(0, limit).stream()
                .filter(relevantIds::contains)
                .count();

        return (double) relevantInTopK / limit;
    }

    /**
     * 计算Recall@K：前K条结果覆盖相关结果比例
     *
     * @param results       搜索结果列表（按排名排序）
     * @param relevantIds   相关结果的ID集合
     * @param k             截断位置
     * @return Recall@K 值
     */
    public double recallAtK(List<String> results, Set<String> relevantIds, int k) {
        if (results == null || results.isEmpty() || relevantIds == null || relevantIds.isEmpty()) {
            return 0.0;
        }

        int limit = Math.min(k, results.size());
        long relevantInTopK = results.subList(0, limit).stream()
                .filter(relevantIds::contains)
                .count();

        return (double) relevantInTopK / relevantIds.size();
    }

    /**
     * 计算NDCG@K（归一化折损累积增益）
     *
     * @param results       搜索结果列表（按排名排序，带相关性评分）
     * @param k             截断位置
     * @return NDCG@K 值
     */
    public double ndcgAtK(List<RatedItem> results, int k) {
        if (results == null || results.isEmpty()) {
            return 0.0;
        }

        int limit = Math.min(k, results.size());
        List<RatedItem> topK = results.subList(0, limit);

        // 计算 DCG@K
        double dcg = dcgAtK(topK, limit);

        // 计算 IDCG@K（理想排序下的 DCG）
        List<RatedItem> idealOrder = new ArrayList<>(results);
        idealOrder.sort(Comparator.comparingInt(RatedItem::getRelevanceScore).reversed());
        List<RatedItem> idealTopK = idealOrder.subList(0, Math.min(limit, idealOrder.size()));
        double idcg = dcgAtK(idealTopK, limit);

        if (idcg == 0.0) {
            return 0.0;
        }

        return dcg / idcg;
    }

    /**
     * 计算 DCG@K（折损累积增益）
     * DCG@K = sum_{i=1}^{K} (2^{rel_i} - 1) / log2(i + 1)
     */
    private double dcgAtK(List<RatedItem> items, int k) {
        double dcg = 0.0;
        for (int i = 0; i < Math.min(k, items.size()); i++) {
            int relevanceScore = items.get(i).getRelevanceScore();
            double gain = Math.pow(2, relevanceScore) - 1;
            double discount = Math.log(i + 2) / Math.log(2); // log2(i+1), i从0开始所以用i+2
            dcg += gain / discount;
        }
        return dcg;
    }

    /**
     * 综合评估搜索质量
     *
     * @param keyword       搜索关键词
     * @param results       搜索结果ID列表（按排名排序）
     * @param relevantIds   相关结果的ID集合
     * @param ratedResults  带相关性评分的搜索结果（用于NDCG计算）
     * @return 评估结果
     */
    public EvaluationResult evaluate(String keyword, List<String> results,
                                     Set<String> relevantIds, List<RatedItem> ratedResults) {
        EvaluationResult result = new EvaluationResult();
        result.setKeyword(keyword);

        // Precision@10
        result.setPrecisionAt10(precisionAtK(results, relevantIds, 10));

        // Recall@100
        result.setRecallAt100(recallAtK(results, relevantIds, 100));

        // NDCG@10
        result.setNdcgAt10(ndcgAtK(ratedResults, 10));

        // 统计信息
        result.setEvaluatedCount(results != null ? results.size() : 0);
        result.setRelevantCount(relevantIds != null ? relevantIds.size() : 0);

        log.info("搜索质量评估: {}", result);

        return result;
    }

    /**
     * 批量评估多个关键词的搜索质量，计算平均指标
     *
     * @param evaluationResults 多个关键词的评估结果列表
     * @return 平均评估指标
     */
    public EvaluationResult averageEvaluation(List<EvaluationResult> evaluationResults) {
        if (evaluationResults == null || evaluationResults.isEmpty()) {
            EvaluationResult empty = new EvaluationResult();
            empty.setKeyword("AVERAGE");
            return empty;
        }

        EvaluationResult avg = new EvaluationResult();
        avg.setKeyword("AVERAGE");

        double sumP10 = 0, sumR100 = 0, sumNdcg10 = 0;
        for (EvaluationResult r : evaluationResults) {
            sumP10 += r.getPrecisionAt10();
            sumR100 += r.getRecallAt100();
            sumNdcg10 += r.getNdcgAt10();
        }

        int n = evaluationResults.size();
        avg.setPrecisionAt10(sumP10 / n);
        avg.setRecallAt100(sumR100 / n);
        avg.setNdcgAt10(sumNdcg10 / n);
        avg.setEvaluatedCount(n);

        log.info("搜索质量平均评估: P@10={}, R@100={}, NDCG@10={}, keywords={}",
                String.format("%.4f", avg.getPrecisionAt10()),
                String.format("%.4f", avg.getRecallAt100()),
                String.format("%.4f", avg.getNdcgAt10()), n);

        return avg;
    }
}
