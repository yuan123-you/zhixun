package com.zhixun.common.util;

import java.util.*;

/**
 * 服务层通用工具方法
 */
public final class ServiceUtils {

    private ServiceUtils() {}

    /**
     * 安全的 null 列表转空列表
     */
    public static <T> List<T> safeToList(List<T> list) {
        return list == null ? Collections.emptyList() : list;
    }

    /**
     * 解析逗号分隔的 ID 字符串为 Long 列表
     */
    public static List<Long> parseIds(String idsStr) {
        if (idsStr == null || idsStr.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(idsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Long.parseLong(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }
}
