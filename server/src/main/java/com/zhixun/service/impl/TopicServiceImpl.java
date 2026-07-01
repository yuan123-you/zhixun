package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixun.dto.topic.TopicCreateRequest;
import com.zhixun.dto.topic.TopicQueryRequest;
import com.zhixun.entity.Topic;
import com.zhixun.entity.TopicArticle;
import com.zhixun.entity.TopicFollow;
import com.zhixun.mapper.TopicArticleMapper;
import com.zhixun.mapper.TopicFollowMapper;
import com.zhixun.mapper.TopicMapper;
import com.zhixun.service.TopicService;
import com.zhixun.vo.TopicVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
    private final TopicMapper topicMapper;
    private final TopicArticleMapper topicArticleMapper;
    private final TopicFollowMapper topicFollowMapper;
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#([^#\\s]{1,30})#");

    @Override @Transactional
    public Long createTopic(Long userId, TopicCreateRequest request) {
        Topic topic = new Topic();
        topic.setName(request.getName());
        topic.setDescription(request.getDescription());
        topic.setCoverImage(request.getCoverImage());
        topic.setCreatorId(userId);
        topic.setHotScore(BigDecimal.ZERO);
        if (request.getIsOfficial() != null && request.getIsOfficial()) {
            topic.setIsOfficial(1);
        }
        topicMapper.insert(topic);
        return topic.getId();
    }

    @Override
    public Page<TopicVO> getTopicList(TopicQueryRequest request) {
        Page<Topic> page = new Page<>(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Topic::getStatus, 0);
        if (request.getKeyword() != null && !request.getKeyword().isEmpty())
            wrapper.like(Topic::getName, request.getKeyword());
        if ("new".equals(request.getOrderBy())) wrapper.orderByDesc(Topic::getCreatedAt);
        else wrapper.orderByDesc(Topic::getHotScore);
        Page<Topic> result = topicMapper.selectPage(page, wrapper);
        return (Page<TopicVO>) result.convert(this::toVO);
    }

    @Override
    public Page<TopicVO> getAdminTopicList(TopicQueryRequest request) {
        Page<Topic> page = new Page<>(request.getPage(), request.getPageSize());
        LambdaQueryWrapper<Topic> wrapper = new LambdaQueryWrapper<>();
        // 管理端不按状态过滤，返回所有话题
        if (request.getKeyword() != null && !request.getKeyword().isEmpty())
            wrapper.like(Topic::getName, request.getKeyword());
        if ("new".equals(request.getOrderBy())) wrapper.orderByDesc(Topic::getCreatedAt);
        else wrapper.orderByDesc(Topic::getHotScore);
        Page<Topic> result = topicMapper.selectPage(page, wrapper);
        return (Page<TopicVO>) result.convert(this::toVO);
    }

    @Override
    public TopicVO getTopicDetail(Long topicId, Long currentUserId) {
        Topic t = topicMapper.selectById(topicId);
        if (t == null) return null;
        TopicVO vo = toVO(t);
        if (currentUserId != null && topicFollowMapper.selectByUserIdAndTopicId(currentUserId, topicId) != null)
            vo.setIsFollowed(true);
        return vo;
    }

    @Override @Transactional
    public void toggleFollow(Long userId, Long topicId) {
        Topic t = topicMapper.selectById(topicId);
        if (t == null) throw new RuntimeException("话题不存在或已被删除");
        TopicFollow f = topicFollowMapper.selectByUserIdAndTopicId(userId, topicId);
        if (f != null) {
            topicFollowMapper.deleteById(f.getId());
            t.setFollowCount(Math.max(0, t.getFollowCount() - 1));
        } else {
            f = new TopicFollow(); f.setTopicId(topicId); f.setUserId(userId);
            topicFollowMapper.insert(f);
            t.setFollowCount(t.getFollowCount() + 1);
        }
        topicMapper.updateById(t);
    }

    @Override
    public List<TopicVO> getHotTopics(int limit) {
        return topicMapper.selectHotTopics(limit).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<TopicVO> searchTopics(String keyword, int limit) {
        return topicMapper.searchTopics(keyword, limit).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<TopicVO> getUserFollowedTopics(Long userId) {
        LambdaQueryWrapper<TopicFollow> fw = new LambdaQueryWrapper<>();
        fw.eq(TopicFollow::getUserId, userId);
        List<TopicFollow> follows = topicFollowMapper.selectList(fw);
        if (follows.isEmpty()) return new ArrayList<>();
        List<Long> ids = follows.stream().map(TopicFollow::getTopicId).collect(Collectors.toList());
        return topicMapper.selectBatchIds(ids).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void updateTopicStatus(Long topicId, Integer status) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new RuntimeException("话题不存在或已被删除");
        }
        topic.setStatus(status);
        topicMapper.updateById(topic);
    }

    @Override
    @Transactional
    public void deleteTopic(Long topicId) {
        Topic topic = topicMapper.selectById(topicId);
        if (topic == null) {
            throw new RuntimeException("话题不存在或已被删除");
        }
        // 删除话题关联的文章关系
        LambdaQueryWrapper<TopicArticle> taWrapper = new LambdaQueryWrapper<>();
        taWrapper.eq(TopicArticle::getTopicId, topicId);
        topicArticleMapper.delete(taWrapper);
        // 删除话题关注关系
        LambdaQueryWrapper<TopicFollow> tfWrapper = new LambdaQueryWrapper<>();
        tfWrapper.eq(TopicFollow::getTopicId, topicId);
        topicFollowMapper.delete(tfWrapper);
        // 删除话题
        topicMapper.deleteById(topicId);
    }

    @Override
    public void updateTopicArticleCount(Long topicId) {
        LambdaQueryWrapper<TopicArticle> w = new LambdaQueryWrapper<>();
        w.eq(TopicArticle::getTopicId, topicId);
        long c = topicArticleMapper.selectCount(w);
        Topic t = topicMapper.selectById(topicId);
        if (t != null) { t.setArticleCount(c); topicMapper.updateById(t); }
    }

    @Override @Transactional
    public void linkArticleToTopics(Long articleId, String content) {
        if (content == null) return;
        Matcher m = HASHTAG_PATTERN.matcher(content);
        while (m.find()) {
            String name = m.group(1);
            LambdaQueryWrapper<Topic> w = new LambdaQueryWrapper<>();
            w.eq(Topic::getName, name);
            Topic topic = topicMapper.selectOne(w);
            if (topic == null) {
                topic = new Topic(); topic.setName(name); topic.setCreatorId(0L);
                topic.setHotScore(BigDecimal.ZERO); topicMapper.insert(topic);
            }
            LambdaQueryWrapper<TopicArticle> taW = new LambdaQueryWrapper<>();
            taW.eq(TopicArticle::getTopicId, topic.getId()).eq(TopicArticle::getArticleId, articleId);
            if (topicArticleMapper.selectCount(taW) == 0) {
                TopicArticle ta = new TopicArticle(); ta.setTopicId(topic.getId()); ta.setArticleId(articleId);
                topicArticleMapper.insert(ta);
                updateTopicArticleCount(topic.getId());
            }
        }
    }

    private TopicVO toVO(Topic t) {
        TopicVO vo = new TopicVO();
        vo.setId(t.getId()); vo.setName(t.getName()); vo.setDescription(t.getDescription());
        vo.setCoverImage(t.getCoverImage()); vo.setArticleCount(t.getArticleCount());
        vo.setFollowCount(t.getFollowCount()); vo.setHotScore(t.getHotScore());
        vo.setIsOfficial(t.getIsOfficial()); vo.setStatus(t.getStatus()); vo.setIsFollowed(false);
        return vo;
    }
}