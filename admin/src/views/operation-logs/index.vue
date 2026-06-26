<template>
  <div class="operation-logs">
    <!-- 筛选条件 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="搜索操作内容" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="操作模块">
          <el-select v-model="queryParams.module" placeholder="全部模块" clearable>
            <el-option label="作品" value="article" />
            <el-option label="评论" value="comment" />
            <el-option label="用户" value="user" />
            <el-option label="分类" value="category" />
            <el-option label="标签" value="tag" />
            <el-option label="敏感词" value="sensitive_word" />
            <el-option label="系统" value="system" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            @change="handleDateChange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志列表 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="logList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="操作人" width="120" />
        <el-table-column prop="module" label="操作模块" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ moduleNameMap[row.module] || row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作类型" width="120" />
        <el-table-column prop="target" label="操作对象" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="createdAt" label="操作时间" width="170" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailVisible" title="操作日志详情" width="600px">
      <el-descriptions v-if="currentLog" :column="2" border>
        <el-descriptions-item label="操作人">{{ currentLog.username }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ moduleNameMap[currentLog.module] || currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ currentLog.action }}</el-descriptions-item>
        <el-descriptions-item label="操作对象">{{ currentLog.target }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ip }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ currentLog.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="详细信息" :span="2">
          <pre class="detail-content">{{ currentLog.detail }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import type { OperationLog, OperationLogQuery } from '@/types'
import { getOperationLogList } from '@/api/operationLog'
import { useRequestCache } from '@/composables/useRequestCache'

/** 操作日志缓存实例 */
const logCache = useRequestCache({
  ttl: 2 * 60 * 1000,
  staleWhileRevalidate: true,
})

const loading = ref(false)
const logList = ref<OperationLog[]>([])
const total = ref(0)
const dateRange = ref<string[]>([])

/** 模块名称映射 */
const moduleNameMap: Record<string, string> = {
  article: '作品',
  comment: '评论',
  user: '用户',
  category: '分类',
  tag: '标签',
  sensitive_word: '敏感词',
  system: '系统',
}

const queryParams = reactive<OperationLogQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  module: undefined,
  startDate: undefined,
  endDate: undefined,
})

/** 详情相关 */
const detailVisible = ref(false)
const currentLog = ref<OperationLog | null>(null)

function handleSearch() {
  queryParams.page = 1
  loadLogs()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.module = undefined
  queryParams.startDate = undefined
  queryParams.endDate = undefined
  dateRange.value = []
  queryParams.page = 1
  loadLogs()
}

/** 日期范围变化 */
function handleDateChange(val: string[] | null) {
  if (val) {
    queryParams.startDate = val[0]
    queryParams.endDate = val[1]
  } else {
    queryParams.startDate = undefined
    queryParams.endDate = undefined
  }
}

/** 查看详情 */
function handleDetail(log: OperationLog) {
  currentLog.value = log
  detailVisible.value = true
}

/** 加载日志列表 */
async function loadLogs(force = false) {
  loading.value = true
  try {
    const result = await logCache.request('/operation-logs', queryParams as unknown as Record<string, unknown>, { force })
    logList.value = result.list
    total.value = result.total
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped lang="scss">
.operation-logs {
  .search-card {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .detail-content {
    margin: 0;
    padding: 8px;
    background: #f5f5f5;
    border-radius: 4px;
    font-size: 12px;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
