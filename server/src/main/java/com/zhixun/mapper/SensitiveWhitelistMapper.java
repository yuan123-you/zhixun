package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.SensitiveWhitelist;
import org.apache.ibatis.annotations.Mapper;

/**
 * 敏感词白名单 Mapper 接口
 */
@Mapper
public interface SensitiveWhitelistMapper extends BaseMapper<SensitiveWhitelist> {
}
