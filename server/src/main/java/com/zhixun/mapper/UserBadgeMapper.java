package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.UserBadge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBadgeMapper extends BaseMapper<UserBadge> {
    List<UserBadge> selectByUserId(@Param("userId") Long userId);
}