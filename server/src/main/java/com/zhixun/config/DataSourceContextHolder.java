package com.zhixun.config;

/**
 * 数据源上下文持有者
 * 通过 ThreadLocal 管理当前线程使用的数据源类型
 */
public class DataSourceContextHolder {

    /** 主库标识 */
    public static final String MASTER = "master";

    /** 从库标识 */
    public static final String SLAVE = "slave";

    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /** 标记是否强制走主库（从库故障回退场景） */
    private static final ThreadLocal<Boolean> FORCE_MASTER = new ThreadLocal<>();

    /**
     * 设置当前数据源类型
     *
     * @param dataSourceType 数据源类型（master / slave）
     */
    public static void setDataSourceType(String dataSourceType) {
        CONTEXT_HOLDER.set(dataSourceType);
    }

    /**
     * 获取当前数据源类型
     * 如果设置了强制主库，则返回主库
     *
     * @return 数据源类型，默认走从库
     */
    public static String getDataSourceType() {
        // 如果强制走主库，直接返回主库
        if (Boolean.TRUE.equals(FORCE_MASTER.get())) {
            return MASTER;
        }
        String dataSourceType = CONTEXT_HOLDER.get();
        return dataSourceType != null ? dataSourceType : SLAVE;
    }

    /**
     * 切换到主库
     */
    public static void master() {
        CONTEXT_HOLDER.set(MASTER);
    }

    /**
     * 切换到从库
     */
    public static void slave() {
        CONTEXT_HOLDER.set(SLAVE);
    }

    /**
     * 强制走主库（用于从库故障回退场景）
     */
    public static void forceMaster() {
        FORCE_MASTER.set(true);
        CONTEXT_HOLDER.set(MASTER);
    }

    /**
     * 是否强制走主库
     */
    public static boolean isForceMaster() {
        return Boolean.TRUE.equals(FORCE_MASTER.get());
    }

    /**
     * 清除当前数据源类型
     */
    public static void clear() {
        CONTEXT_HOLDER.remove();
        FORCE_MASTER.remove();
    }
}
