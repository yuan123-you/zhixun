/**
 * useShuffleRefresh 单元测试
 *
 * 测试覆盖：
 * - Fisher-Yates 洗牌算法正确性
 * - 去重逻辑（已展示内容不重复）
 * - 偏移量管理与回绕
 * - 加载状态与错误处理
 * - 防抖保护
 * - 数据耗尽时的补充获取
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'

// 模拟 Vue 响应式 API
const mockRef = <T>(val: T) => ({
  value: val,
  __v_isRef: true,
})

const mockComputed = <T>(fn: () => T) => ({
  get value() { return fn() },
  __v_isRef: true,
})

const mockWatch = vi.fn()

vi.stubGlobal('ref', mockRef)
vi.stubGlobal('computed', mockComputed)
vi.stubGlobal('watch', mockWatch)

// 导入被测模块
import { useShuffleRefresh } from '~/composables/useShuffleRefresh'

// 测试数据
interface TestItem {
  id: number
  name: string
}

const createItems = (start: number, count: number): TestItem[] =>
  Array.from({ length: count }, (_, i) => ({ id: start + i, name: `Item ${start + i}` }))

describe('useShuffleRefresh', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('基本功能', () => {
    it('应该正确初始化状态', () => {
      const fetchFn = vi.fn().mockResolvedValue(createItems(0, 5))
      const { items, loading, error, offset } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
      })

      expect(items.value).toEqual([])
      expect(loading.value).toBe(false)
      expect(error.value).toBe('')
      expect(offset.value).toBe(0)
    })

    it('应该成功获取并展示第一批数据', async () => {
      const mockData = createItems(0, 5)
      const fetchFn = vi.fn().mockResolvedValue(mockData)
      const { items, refresh } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
        shuffle: false, // 关闭洗牌以便验证顺序
      })

      await refresh()

      expect(fetchFn).toHaveBeenCalledWith(0, 5)
      expect(items.value.length).toBe(5)
      expect(items.value.every(item => mockData.some(d => d.id === item.id))).toBe(true)
    })
  })

  describe('Fisher-Yates 洗牌算法', () => {
    it('洗牌后应包含所有原始元素', async () => {
      const mockData = createItems(0, 10)
      const fetchFn = vi.fn().mockResolvedValue(mockData)
      const { items, refresh } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 10,
        shuffle: true,
      })

      await refresh()

      const resultIds = items.value.map(i => i.id).sort()
      const expectedIds = mockData.map(i => i.id).sort()
      expect(resultIds).toEqual(expectedIds)
    })

    it('多次洗牌应产生不同顺序（概率性测试）', async () => {
      const mockData = createItems(0, 20)
      const fetchFn = vi.fn().mockResolvedValue(mockData)

      // 收集多次洗牌结果
      const results: string[] = []
      for (let i = 0; i < 10; i++) {
        const { items, refresh, reset } = useShuffleRefresh<TestItem>({
          fetchFn,
          limit: 20,
          shuffle: true,
          debounceMs: 0,
        })
        await refresh()
        results.push(items.value.map(i => i.id).join(','))
        reset()
      }

      // 至少应有2种不同顺序（概率极高，20个元素的排列数极大）
      const uniqueResults = new Set(results)
      expect(uniqueResults.size).toBeGreaterThan(1)
    })

    it('单个元素不应洗牌', async () => {
      const mockData = createItems(0, 1)
      const fetchFn = vi.fn().mockResolvedValue(mockData)
      const { items, refresh } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 1,
        shuffle: true,
      })

      await refresh()
      expect(items.value).toEqual(mockData)
    })
  })

  describe('去重逻辑', () => {
    it('不应展示已展示过的内容', async () => {
      // 第一次返回 id 0-4，第二次返回 id 2-6（2,3,4重复）
      const fetchFn = vi.fn()
        .mockResolvedValueOnce(createItems(0, 5))
        .mockResolvedValueOnce(createItems(2, 5))

      const { items, refresh, shownCount } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
        shuffle: false,
        debounceMs: 0,
      })

      await refresh()
      expect(items.value.length).toBe(5)
      expect(shownCount.value).toBe(5)

      await refresh()
      // 第二次获取的 id 2-6 中，2,3,4 已展示过，应被过滤
      const newIds = items.value.map(i => i.id)
      expect(newIds).not.toContain(2)
      expect(newIds).not.toContain(3)
      expect(newIds).not.toContain(4)
      expect(items.value.length).toBe(2) // 只有 5, 6
    })

    it('全部重复时应清空去重记录重新展示', async () => {
      // 两次返回完全相同的数据
      const sameData = createItems(0, 5)
      const fetchFn = vi.fn()
        .mockResolvedValueOnce(sameData)
        .mockResolvedValueOnce(sameData)

      const { items, refresh } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
        shuffle: false,
        debounceMs: 0,
      })

      await refresh()
      expect(items.value.length).toBe(5)

      await refresh()
      // 第二次全部重复，应清空去重记录重新展示
      expect(items.value.length).toBe(5)
    })
  })

  describe('偏移量管理', () => {
    it('偏移量应随获取次数递增', async () => {
      const fetchFn = vi.fn()
        .mockResolvedValueOnce(createItems(0, 5))
        .mockResolvedValueOnce(createItems(5, 5))

      const { offset, refresh } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
        shuffle: false,
        debounceMs: 0,
      })

      await refresh()
      expect(offset.value).toBe(5)

      await refresh()
      expect(offset.value).toBe(10)
    })

    it('已知总量时偏移量应回绕', async () => {
      const fetchFn = vi.fn()
        .mockResolvedValueOnce(createItems(0, 5))
        .mockResolvedValueOnce(createItems(5, 3)) // 只有3个，总共8个

      const { offset, refresh } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
        total: 8,
        shuffle: false,
        debounceMs: 0,
      })

      await refresh()
      expect(offset.value).toBe(5)

      await refresh()
      // 5 + 3 = 8 >= total(8)，应回绕到0
      expect(offset.value).toBe(0)
    })

    it('数据耗尽时应回绕重试', async () => {
      const fetchFn = vi.fn()
        .mockResolvedValueOnce(createItems(0, 5))
        .mockResolvedValueOnce([]) // 第二次无数据
        .mockResolvedValueOnce(createItems(0, 5)) // 回绕重试

      const { items, refresh } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
        shuffle: false,
        debounceMs: 0,
      })

      await refresh()
      expect(items.value.length).toBe(5)

      await refresh()
      // 第二次无数据，应回绕重试
      expect(items.value.length).toBe(5)
    })
  })

  describe('加载状态与错误处理', () => {
    it('请求失败时应设置错误信息并保留旧数据', async () => {
      const fetchFn = vi.fn()
        .mockResolvedValueOnce(createItems(0, 5))
        .mockRejectedValueOnce(new Error('网络错误'))

      const { items, error, refresh } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
        shuffle: false,
        debounceMs: 0,
      })

      await refresh()
      expect(items.value.length).toBe(5)
      expect(error.value).toBe('')

      await refresh()
      expect(error.value).toBe('网络错误')
      expect(items.value.length).toBe(5) // 保留旧数据
    })

    it('请求期间 loading 应为 true', async () => {
      let resolvePromise!: Function
      const fetchFn = vi.fn().mockImplementation(() => new Promise(resolve => {
        resolvePromise = resolve
      }))

      const { loading, refresh } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
        debounceMs: 0,
      })

      const promise = refresh()
      expect(loading.value).toBe(true)

      resolvePromise(createItems(0, 5))
      await promise
      expect(loading.value).toBe(false)
    })
  })

  describe('防抖保护', () => {
    it('快速连续调用应被防抖', async () => {
      const fetchFn = vi.fn().mockResolvedValue(createItems(0, 5))
      const { refresh } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
        debounceMs: 1000,
      })

      await refresh()
      await refresh() // 第二次调用应在防抖间隔内

      expect(fetchFn).toHaveBeenCalledTimes(1)
    })
  })

  describe('重置功能', () => {
    it('reset 应清除所有状态', async () => {
      const fetchFn = vi.fn().mockResolvedValue(createItems(0, 5))
      const { items, offset, error, shownCount, refresh, reset } = useShuffleRefresh<TestItem>({
        fetchFn,
        limit: 5,
        shuffle: false,
        debounceMs: 0,
      })

      await refresh()
      expect(items.value.length).toBe(5)
      expect(offset.value).toBe(5)
      expect(shownCount.value).toBe(5)

      reset()
      expect(items.value).toEqual([])
      expect(offset.value).toBe(0)
      expect(shownCount.value).toBe(0)
    })
  })

  describe('自定义 getItemId', () => {
    it('应支持自定义 ID 提取函数', async () => {
      interface CustomItem { key: string; value: number }
      const mockData: CustomItem[] = [
        { key: 'a', value: 1 },
        { key: 'b', value: 2 },
      ]
      const fetchFn = vi.fn().mockResolvedValue(mockData)

      const { items, refresh, shownCount } = useShuffleRefresh<CustomItem>({
        fetchFn,
        limit: 5,
        getItemId: (item) => item.key,
        shuffle: false,
        debounceMs: 0,
      })

      await refresh()
      expect(items.value.length).toBe(2)
      expect(shownCount.value).toBe(2)
    })
  })
})
