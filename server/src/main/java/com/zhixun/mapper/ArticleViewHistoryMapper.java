package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.ArticleViewHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作品浏览历史 Mapper
 */
@Mapper
public interface ArticleViewHistoryMapper extends BaseMapper<ArticleViewHistory> {
}
