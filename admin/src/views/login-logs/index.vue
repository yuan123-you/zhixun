<template>
  <div class="login-logs-management">
    <PageHeader
      title="登录日志"
      description="查看用户登录记录，包括登录IP、设备、浏览器等信息"
    />

    <!-- 搜索筛选栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索用户名/昵称"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="登录时间">
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

    <!-- 登录日志列表 -->
    <el-card shadow="never" class="table-card">
      <!-- 骨架屏：首次加载时展示 -->
      <template v-if="isFirstLoad">
        <el-skeleton :rows="8" animated />
      </template>

      <template v-else>
        <el-table v-loading="loading" :data="logList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" width="120" />
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="location" label="归属地" width="160" show-overflow-tooltip />
          <el-table-column prop="userAgent" label="用户代理" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.status === 1" type="success" size="small">成功</el-tag>
              <el-tooltip
                v-else
                :content="row.failReason || '未知原因'"
                placement="top"
                :show-after="300"
              >
                <el-tag type="danger" size="small">失败</el-tag>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="failReason" label="失败原因" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.failReason" class="fail-reason">{{ row.failReason }}</span>
              <span v-else class="text-muted">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="登录时间" width="170" />

          <!-- 错误重试空状态 -->
          <template #empty>
            <div class="table-empty">
              <template v-if="hasError">
                <el-icon :size="40" color="#909399"><CircleCloseFilled /></el-icon>
                <p>数据加载失败</p>
                <el-button type="primary" size="small" @click="loadLogs()">重新加载</el-button>
              </template>
              <template v-else>
                <el-empty description="暂无登录日志" />
              </template>
            </div>
          </template>
        </el-table>
      </template>

      <!-- 分页 -->
      <div v-if="!isFirstLoad" class="pagination-wrapper">
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import type { LoginLog, LoginLogQuery, PageResult } from '@/types'
import { useRequestCache } from '@/composables/useRequestCache'
import PageHeader from '@/components/PageHeader.vue'

/** 请求缓存实例 */
const logCache = useRequestCache<PageResult<LoginLog>>({
  ttl: 2 * 60 * 1000,
  staleWhileRevalidate: true,
  retryCount: 2,
  retryInterval: 800,
})

/** 加载状态 */
const loading = ref(false)

/** 是否首次加载（用于骨架屏判断） */
const isFirstLoad = ref(true)

/** 是否有错误 */
const hasError = ref(false)

/** 日志列表 */
const logList = ref<LoginLog[]>([])

/** 总数 */
const total = ref(0)

/** 日期范围（用于日期选择器绑定） */
const dateRange = ref<[string, string] | null>(null)

/** 查询参数 */
const queryParams = reactive<LoginLogQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
  startDate: undefined,
  endDate: undefined,
})

/** 日期范围变更 */
function handleDateChange(val: [string, string] | null) {
  if (val && val.length === 2) {
    queryParams.startDate = val[0]
    queryParams.endDate = val[1]
  } else {
    queryParams.startDate = undefined
    queryParams.endDate = undefined
  }
}

/** 搜索 */
function handleSearch() {
  queryParams.page = 1
  loadLogs()
}

/** 重置搜索 */
function handleReset() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.startDate = undefined
  queryParams.endDate = undefined
  dateRange.value = null
  queryParams.page = 1
  loadLogs()
}

/** 加载登录日志列表 */
async function loadLogs(force = false) {
  loading.value = true
  hasError.value = false
  try {
    const result = await logCache.request(
      '/admin/login-logs',
      queryParams as unknown as Record<string, unknown>,
      { force },
    )
    logList.value = result.list
    total.value = result.total
  } catch {
    hasError.value = true
    ElMessage.error('登录日志加载失败，请检查网络后重试')
  } finally {
    loading.value = false
    isFirstLoad.value = false
  }
}

onMounted(() => {
  loadLogs()
})
</script>

<style scoped lang="scss">
.login-logs-management {
  .search-card {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .fail-reason {
    color: #f56c6c;
  }

  .text-muted {
    color: #c0c4cc;
  }

  .table-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 32px 0;
    gap: 8px;

    p {
      color: #909399;
      font-size: 14px;
    }
  }
}
</style>