package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.TopicFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TopicFollowMapper extends BaseMapper<TopicFollow> {
    int countByTopicId(@Param("topicId") Long topicId);
    TopicFollow selectByUserIdAndTopicId(@Param("userId") Long userId, @Param("topicId") Long topicId);
}