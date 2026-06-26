package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.ContentTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ContentTemplateMapper extends BaseMapper<ContentTemplate> {
    List<ContentTemplate> selectByCategory(@Param("category") String category);
}