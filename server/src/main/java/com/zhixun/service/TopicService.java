package com.zhixun.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.dto.topic.TopicCreateRequest;
import com.zhixun.dto.topic.TopicQueryRequest;
import com.zhixun.vo.TopicVO;

import java.util.List;

public interface TopicService {
    Long createTopic(Long userId, TopicCreateRequest request);
    Page<TopicVO> getTopicList(TopicQueryRequest request);
    Page<TopicVO> getAdminTopicList(TopicQueryRequest request);
    TopicVO getTopicDetail(Long topicId, Long currentUserId);
    void toggleFollow(Long userId, Long topicId);
    void updateTopicStatus(Long topicId, Integer status);
    void deleteTopic(Long topicId);
    List<TopicVO> getHotTopics(int limit);
    List<TopicVO> searchTopics(String keyword, int limit);
    List<TopicVO> getUserFollowedTopics(Long userId);
    void updateTopicArticleCount(Long topicId);
    void linkArticleToTopics(Long articleId, String content);
}