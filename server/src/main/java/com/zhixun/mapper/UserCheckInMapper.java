package com.zhixun.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixun.entity.UserCheckIn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

@Mapper
public interface UserCheckInMapper extends BaseMapper<UserCheckIn> {
    UserCheckIn selectByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);
}