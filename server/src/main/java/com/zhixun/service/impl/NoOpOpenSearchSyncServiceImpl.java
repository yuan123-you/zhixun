package com.zhixun.service.impl;

import com.zhixun.service.OpenSearchSyncService;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.OpenSearchClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * OpenSearch同步服务Fallback实现（当OpenSearchClient不可用时自动生效）
 */
@Slf4j
@Service
@ConditionalOnMissingBean(OpenSearchClient.class)
public class NoOpOpenSearchSyncServiceImpl implements OpenSearchSyncService {

    @Override
    public void syncArticle(Long articleId) {
        log.debug("OpenSearch不可用，跳过作品同步: {}", articleId);
    }

    @Override
    public void syncArticles(List<Long> articleIds) {
        log.debug("OpenSearch不可用，跳过作品批量同步");
    }

    @Override
    public void deleteArticle(Long articleId) {
        log.debug("OpenSearch不可用，跳过作品删除: {}", articleId);
    }

    @Override
    public void syncUser(Long userId) {
        log.debug("OpenSearch不可用，跳过用户同步: {}", userId);
    }

    @Override
    public void syncUsers(List<Long> userIds) {
        log.debug("OpenSearch不可用，跳过用户批量同步");
    }

    @Override
    public void deleteUser(Long userId) {
        log.debug("OpenSearch不可用，跳过用户删除: {}", userId);
    }

    @Override
    public void syncImage(Long imageId) {
        log.debug("OpenSearch不可用，跳过图片同步: {}", imageId);
    }

    @Override
    public void syncImagesByArticle(Long articleId) {
        log.debug("OpenSearch不可用，跳过图片批量同步");
    }

    @Override
    public void deleteImage(Long imageId) {
        log.debug("OpenSearch不可用，跳过图片删除: {}", imageId);
    }

    @Override
    public void deleteImagesByArticle(Long articleId) {
        log.debug("OpenSearch不可用，跳过图片批量删除");
    }

    @Override
    public void fullSync() {
        log.debug("OpenSearch不可用，跳过全量同步");
    }

    @Override
    public void fullSyncArticles() {
        log.debug("OpenSearch不可用，跳过作品全量同步");
    }

    @Override
    public void fullSyncUsers() {
        log.debug("OpenSearch不可用，跳过用户全量同步");
    }

    @Override
    public void fullSyncImages() {
        log.debug("OpenSearch不可用，跳过图片全量同步");
    }
}
