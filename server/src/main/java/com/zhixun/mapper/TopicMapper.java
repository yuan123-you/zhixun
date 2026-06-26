package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.Topic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {
    List<Topic> selectHotTopics(@Param("limit") int limit);
    List<Topic> searchTopics(@Param("keyword") String keyword, @Param("limit") int limit);
}