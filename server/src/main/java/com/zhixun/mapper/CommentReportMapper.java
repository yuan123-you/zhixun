package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.CommentReport;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论举报 Mapper 接口
 */
@Mapper
public interface CommentReportMapper extends BaseMapper<CommentReport> {
}
