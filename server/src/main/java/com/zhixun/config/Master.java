package com.zhixun.config;

import java.lang.annotation.*;

/**
 * 主库注解
 * 标记在方法或类上，表示该操作走主库
 * 适用于写操作或需要强一致性的读操作
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Master {
}
