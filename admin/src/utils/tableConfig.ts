import { storage, STORAGE_KEYS } from '@/utils/storage'

/** 表格列配置 */
export interface TableColumnConfig {
  /** 列的唯一标识 */
  prop: string
  /** 是否显示 */
  visible: boolean
  /** 排序 */
  sortOrder?: number
}

/** 表格配置 */
export interface TableConfig {
  /** 列配置列表 */
  columns: TableColumnConfig[]
  /** 分页大小 */
  pageSize: number
}

/** 默认分页大小 */
const DEFAULT_PAGE_SIZE = 20

/**
 * 管理端表格配置本地存储
 * - 记住每张表格的列显示/隐藏偏好
 * - 记住分页大小偏好
 * - 筛选条件记忆
 */
export const useTableConfig = () => {
  /**
   * 获取表格配置
   * @param tableKey 表格唯一标识（如 'article-list', 'user-list'）
   */
  const getTableConfig = (tableKey: string): TableConfig | null => {
    return storage.get<TableConfig>(`${STORAGE_KEYS.TABLE_CONFIG_PREFIX}${tableKey}`)
  }

  /**
   * 保存表格配置
   * @param tableKey 表格唯一标识
   * @param config 配置
   */
  const saveTableConfig = (tableKey: string, config: TableConfig): void => {
    storage.set(`${STORAGE_KEYS.TABLE_CONFIG_PREFIX}${tableKey}`, config)
  }

  /**
   * 获取分页大小偏好
   * @param tableKey 表格唯一标识
   */
  const getPageSize = (tableKey: string): number => {
    const config = getTableConfig(tableKey)
    return config?.pageSize || DEFAULT_PAGE_SIZE
  }

  /**
   * 保存分页大小偏好
   * @param tableKey 表格唯一标识
   * @param pageSize 分页大小
   */
  const savePageSize = (tableKey: string, pageSize: number): void => {
    const config = getTableConfig(tableKey) || {
      columns: [],
      pageSize: DEFAULT_PAGE_SIZE,
    }
    config.pageSize = pageSize
    saveTableConfig(tableKey, config)
  }

  /**
   * 更新列可见性
   * @param tableKey 表格唯一标识
   * @param prop 列标识
   * @param visible 是否可见
   */
  const updateColumnVisibility = (tableKey: string, prop: string, visible: boolean): void => {
    const config = getTableConfig(tableKey) || {
      columns: [],
      pageSize: DEFAULT_PAGE_SIZE,
    }
    const column = config.columns.find((c) => c.prop === prop)
    if (column) {
      column.visible = visible
    } else {
      config.columns.push({ prop, visible })
    }
    saveTableConfig(tableKey, config)
  }

  /**
   * 初始化表格列配置（仅当本地无配置时使用默认值）
   * @param tableKey 表格唯一标识
   * @param defaultColumns 默认列配置
   */
  const initTableConfig = (tableKey: string, defaultColumns: TableColumnConfig[]): TableConfig => {
    const existing = getTableConfig(tableKey)
    if (existing) return existing

    const config: TableConfig = {
      columns: defaultColumns,
      pageSize: DEFAULT_PAGE_SIZE,
    }
    saveTableConfig(tableKey, config)
    return config
  }

  return {
    getTableConfig,
    saveTableConfig,
    getPageSize,
    savePageSize,
    updateColumnVisibility,
    initTableConfig,
  }
}
