package com.zhixun.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标记在Controller方法上，自动记录用户操作日志
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作模块（如：文章、评论、关注、点赞、收藏、用户设置）
     */
    String module();

    /**
     * 操作动作（如：发布、编辑、删除、关注、取消关注、点赞、取消点赞、收藏、取消收藏、修改设置）
     */
    String action();
}
