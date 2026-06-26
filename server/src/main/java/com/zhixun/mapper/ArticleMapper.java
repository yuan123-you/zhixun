package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.Article;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作品 Mapper
 */
@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
