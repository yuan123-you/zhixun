package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.ArticleOperateLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作品操作日志 Mapper
 */
@Mapper
public interface ArticleOperateLogMapper extends BaseMapper<ArticleOperateLog> {
}
