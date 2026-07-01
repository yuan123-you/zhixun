<template>
  <div class="security-audit">
    <PageHeader title="安全审计日志" description="查看系统安全事件记录，包括登录失败、越权访问、XSS/SQL注入尝试等安全事件" />

    <!-- 统计摘要 -->
    <el-row :gutter="20" class="stats-section">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card" v-loading="statsLoading">
          <div class="stat-content">
            <div class="stat-info">
              <p class="stat-label">总事件数</p>
              <p class="stat-value">{{ formatNumber(statsData?.total ?? 0) }}</p>
            </div>
            <el-icon class="stat-icon" style="color: #409eff"><Warning /></el-icon>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card" v-loading="statsLoading">
          <template #header><span>事件类型分布</span></template>
          <div class="event-type-list">
            <div v-for="item in statsData?.eventTypeStats" :key="item.eventType" class="event-type-item">
              <el-tag size="small">{{ eventTypeLabel(item.eventType) }}</el-tag>
              <span class="count">{{ item.count }}</span>
            </div>
            <el-empty v-if="!statsData?.eventTypeStats?.length" description="暂无数据" :image-size="40" />
          </div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card" v-loading="statsLoading">
          <template #header><span>Top IP</span></template>
          <div class="top-ip-list">
            <div v-for="(item, index) in statsData?.topIps" :key="item.ip" class="top-ip-item">
              <span class="rank" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
              <span class="ip">{{ item.ip }}</span>
              <span class="count">{{ item.count }}</span>
            </div>
            <el-empty v-if="!statsData?.topIps?.length" description="暂无数据" :image-size="40" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="filterForm" inline>
        <el-form-item label="事件类型">
          <el-select v-model="filterForm.eventType" placeholder="全部类型" clearable>
            <el-option v-for="item in eventTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input v-model="filterForm.userId" placeholder="输入用户ID" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="IP地址">
          <el-input v-model="filterForm.ip" placeholder="输入IP地址" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="filterForm.riskLevel" placeholder="全部等级" clearable>
            <el-option v-for="item in riskLevelOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 日志表格 -->
    <el-card shadow="never">
      <el-table v-loading="loading" :data="logList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="eventType" label="事件类型" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ eventTypeLabel(row.eventType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="ip" label="IP地址" width="140" />
        <el-table-column prop="requestMethod" label="请求方法" width="100" />
        <el-table-column prop="requestUri" label="请求URI" min-width="200" show-overflow-tooltip />
        <el-table-column prop="responseStatus" label="响应状态" width="100" />
        <el-table-column prop="riskLevel" label="风险等级" width="100">
          <template #default="{ row }">
            <el-tag :color="riskLevelColor(row.riskLevel)" size="small" effect="dark">
              {{ riskLevelLabel(row.riskLevel) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="详情" min-width="150" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="() => loadLogs()"
          @current-change="() => loadLogs()"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Warning } from '@element-plus/icons-vue'
import type { SecurityAuditLog, SecurityAuditStats } from '@/types'
import { useRequestCache } from '@/composables/useRequestCache'
import PageHeader from '@/components/PageHeader.vue'

/** 统计缓存 */
const statsCache = useRequestCache<SecurityAuditStats>({
  ttl: 5 * 60 * 1000,
  staleWhileRevalidate: true,
})

/** 日志列表缓存 */
const logCache = useRequestCache({
  ttl: 2 * 60 * 1000,
  staleWhileRevalidate: true,
})

const loading = ref(false)
const statsLoading = ref(false)
const logList = ref<SecurityAuditLog[]>([])
const total = ref(0)
const statsData = ref<SecurityAuditStats | null>(null)
const dateRange = ref<string[]>([])

/** 筛选表单 */
const filterForm = reactive({
  eventType: '',
  userId: '',
  ip: '',
  riskLevel: '',
})

/** 分页参数 */
const queryParams = reactive({
  page: 1,
  pageSize: 10,
})

/** 事件类型选项 */
const eventTypeOptions = [
  { label: '登录失败', value: 'LOGIN_FAILURE' },
  { label: '越权访问', value: 'UNAUTHORIZED_ACCESS' },
  { label: 'XSS攻击', value: 'XSS_ATTEMPT' },
  { label: 'SQL注入', value: 'SQL_INJECTION' },
  { label: 'CSRF攻击', value: 'CSRF_ATTEMPT' },
  { label: '频率限制', value: 'RATE_LIMIT_EXCEEDED' },
]

/** 风险等级选项 */
const riskLevelOptions = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
  { label: '严重', value: 'critical' },
]

/** 事件类型中文映射 */
function eventTypeLabel(type: string): string {
  const map: Record<string, string> = {
    LOGIN_FAILURE: '登录失败',
    UNAUTHORIZED_ACCESS: '越权访问',
    XSS_ATTEMPT: 'XSS攻击',
    SQL_INJECTION: 'SQL注入',
    CSRF_ATTEMPT: 'CSRF攻击',
    RATE_LIMIT_EXCEEDED: '频率限制',
  }
  return map[type] || type
}

/** 风险等级中文映射 */
function riskLevelLabel(level: string): string {
  const map: Record<string, string> = {
    low: '低',
    medium: '中',
    high: '高',
    critical: '严重',
  }
  return map[level] || level
}

/** 风险等级颜色映射 */
function riskLevelColor(level: string): string {
  const colors: Record<string, string> = {
    low: '#67c23a',
    medium: '#e6a23c',
    high: '#f56c6c',
    critical: '#8b0000',
  }
  return colors[level] || '#909399'
}

/** 数字格式化 */
function formatNumber(num: number): string {
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + '万'
  }
  return num.toLocaleString()
}

/** 构建查询参数 */
function buildQueryParams(): Record<string, unknown> {
  const params: Record<string, unknown> = {
    page: queryParams.page,
    pageSize: queryParams.pageSize,
  }
  if (filterForm.eventType) params.eventType = filterForm.eventType
  if (filterForm.userId) params.userId = Number(filterForm.userId)
  if (filterForm.ip) params.ip = filterForm.ip
  if (filterForm.riskLevel) params.riskLevel = filterForm.riskLevel
  if (dateRange.value?.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  return params
}

/** 加载统计数据 */
async function loadStats() {
  statsLoading.value = true
  try {
    const result = await statsCache.request('/admin/security-audit-logs/stats')
    statsData.value = result
  } catch {
    // 错误已在拦截器中处理
  } finally {
    statsLoading.value = false
  }
}

/** 加载日志列表 */
async function loadLogs(force = false) {
  loading.value = true
  try {
    const result = await logCache.request('/admin/security-audit-logs', buildQueryParams(), { force })
    logList.value = result.list
    total.value = result.total
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  queryParams.page = 1
  loadLogs(true)
}

/** 重置 */
function handleReset() {
  filterForm.eventType = ''
  filterForm.userId = ''
  filterForm.ip = ''
  filterForm.riskLevel = ''
  dateRange.value = []
  queryParams.page = 1
  loadLogs(true)
}

onMounted(() => {
  loadStats()
  loadLogs()
})
</script>

<style scoped lang="scss">
.security-audit {
  .stats-section {
    margin-bottom: 16px;

    .stat-card {
      .stat-content {
        display: flex;
        align-items: center;
        justify-content: space-between;
      }

      .stat-info {
        .stat-label {
          font-size: 14px;
          color: #999;
          margin: 0 0 4px;
        }

        .stat-value {
          font-size: 28px;
          font-weight: 600;
          color: #333;
          margin: 0;
        }
      }

      .stat-icon {
        font-size: 48px;
        opacity: 0.3;
      }

      .event-type-list {
        max-height: 200px;
        overflow-y: auto;

        .event-type-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          padding: 6px 0;

          &:not(:last-child) {
            border-bottom: 1px solid #f0f0f0;
          }

          .count {
            font-size: 14px;
            font-weight: 500;
            color: #666;
          }
        }
      }

      .top-ip-list {
        max-height: 200px;
        overflow-y: auto;

        .top-ip-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 6px 0;

          &:not(:last-child) {
            border-bottom: 1px solid #f0f0f0;
          }

          .rank {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 22px;
            height: 22px;
            border-radius: 50%;
            background: #f0f0f0;
            font-size: 12px;
            color: #999;
            flex-shrink: 0;

            &.top3 {
              background: #409eff;
              color: #fff;
            }
          }

          .ip {
            flex: 1;
            font-size: 13px;
            color: #333;
            font-family: monospace;
          }

          .count {
            font-size: 14px;
            font-weight: 500;
            color: #666;
            flex-shrink: 0;
          }
        }
      }
    }
  }

  .search-card {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>