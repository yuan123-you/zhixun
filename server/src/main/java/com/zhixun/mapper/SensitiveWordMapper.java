package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词 Mapper 接口
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {
}
