package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.ArticleLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 点赞 Mapper 接口
 */
@Mapper
public interface ArticleLikeMapper extends BaseMapper<ArticleLike> {
}
