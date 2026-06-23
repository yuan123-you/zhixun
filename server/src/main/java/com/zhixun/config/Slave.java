package com.zhixun.config;

import java.lang.annotation.*;

/**
 * 从库注解
 * 标记在方法或类上，表示该操作走从库
 * 适用于读操作，允许主从延迟场景下的数据读取
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Slave {
}
