<template>
  <div class="notification-management">
    <PageHeader title="通知管理" description="查看系统通知记录，向用户发送系统通知" />

    <el-tabs v-model="activeTab" type="card">
      <!-- 通知列表 -->
      <el-tab-pane label="通知列表" name="list">
        <!-- 搜索筛选 -->
        <el-card shadow="never" class="search-card">
          <el-form :model="queryParams" inline>
            <el-form-item label="通知类型">
              <el-select v-model="queryParams.type" placeholder="全部类型" clearable>
                <el-option :value="NotificationType.System" label="系统通知" />
                <el-option :value="NotificationType.Audit" label="审核通知" />
                <el-option :value="NotificationType.Interact" label="互动通知" />
                <el-option :value="NotificationType.Follow" label="关注通知" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch">搜索</el-button>
              <el-button @click="handleReset">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 通知表格 -->
        <el-card shadow="never">
          <!-- 骨架屏：首次加载时展示 -->
          <template v-if="isFirstLoad">
            <el-skeleton :rows="6" animated />
          </template>

          <template v-else>
            <el-table v-loading="loading" :data="notificationList" stripe>
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column label="类型" width="100">
                <template #default="{ row }">
                  <el-tag :type="typeTagMap[row.type] as 'success' | 'warning' | 'info' | 'danger' | 'primary'" size="small">
                    {{ typeLabelMap[row.type] || '未知' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
              <el-table-column prop="content" label="内容摘要" min-width="250" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ truncateContent(row.content) }}
                </template>
              </el-table-column>
              <el-table-column prop="senderName" label="发送者" width="120" show-overflow-tooltip />
              <el-table-column label="目标" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.targetAll ? 'info' : 'warning'" size="small">
                    {{ row.targetAll ? '全部用户' : '指定用户' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="170" />

              <!-- 错误重试空状态 -->
              <template #empty>
                <div class="table-empty">
                  <template v-if="hasError">
                    <el-icon :size="40" color="#909399"><CircleCloseFilled /></el-icon>
                    <p>数据加载失败</p>
                    <el-button type="primary" size="small" @click="loadNotifications()">重新加载</el-button>
                  </template>
                  <template v-else>
                    <el-empty description="暂无通知数据" />
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
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="() => loadNotifications()"
              @current-change="() => loadNotifications()"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 发送通知 -->
      <el-tab-pane label="发送通知" name="send">
        <el-card shadow="never">
          <el-form
            ref="formRef"
            :model="sendForm"
            :rules="formRules"
            label-width="100px"
            style="max-width: 600px"
          >
            <el-form-item label="通知类型" prop="type">
              <el-select v-model="sendForm.type" placeholder="请选择通知类型">
                <el-option :value="NotificationType.System" label="系统通知" />
                <el-option :value="NotificationType.Audit" label="审核通知" />
                <el-option :value="NotificationType.Interact" label="互动通知" />
                <el-option :value="NotificationType.Follow" label="关注通知" />
              </el-select>
            </el-form-item>

            <el-form-item label="标题" prop="title">
              <el-input v-model="sendForm.title" placeholder="请输入通知标题" />
            </el-form-item>

            <el-form-item label="内容" prop="content">
              <el-input
                v-model="sendForm.content"
                type="textarea"
                :rows="5"
                placeholder="请输入通知内容"
              />
            </el-form-item>

            <el-form-item label="发送目标">
              <el-radio-group v-model="targetMode">
                <el-radio value="all">全部用户</el-radio>
                <el-radio value="specific">指定用户</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item
              v-if="targetMode === 'specific'"
              label="用户ID"
              prop="targetUserIds"
            >
              <el-input
                v-model="sendForm.targetUserIdsInput"
                placeholder="请输入用户ID，多个用逗号分隔"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="sendLoading" @click="handleSend">
                发送通知
              </el-button>
              <el-button @click="handleResetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { NotificationBroadcast, NotificationQuery, PageResult } from '@/types'
import { NotificationType } from '@/types'
import { getNotificationList, sendNotification } from '@/api/notificationAdmin'
import PageHeader from '@/components/PageHeader.vue'
import { useRequestCache } from '@/composables/useRequestCache'

/** 请求缓存实例 */
const notificationCache = useRequestCache<PageResult<any>>({
  ttl: 2 * 60 * 1000,
  staleWhileRevalidate: true,
  retryCount: 2,
  retryInterval: 800,
})

const activeTab = ref('list')
const loading = ref(false)
const isFirstLoad = ref(true)
const hasError = ref(false)
const notificationList = ref<any[]>([])
const total = ref(0)

/** 通知类型映射 */
const typeLabelMap: Record<number, string> = {
  1: '系统通知',
  2: '审核通知',
  3: '互动通知',
  4: '关注通知',
}

const typeTagMap: Record<number, string> = {
  1: 'primary',
  2: 'warning',
  3: 'success',
  4: 'info',
}

/** 查询参数 */
const queryParams = reactive<NotificationQuery>({
  page: 1,
  pageSize: 10,
  type: undefined,
})

function handleSearch() {
  queryParams.page = 1
  loadNotifications()
}

function handleReset() {
  queryParams.type = undefined
  queryParams.page = 1
  loadNotifications()
}

/** 截断内容摘要 */
function truncateContent(content: string): string {
  if (!content) return '-'
  return content.length > 50 ? content.slice(0, 50) + '...' : content
}

/** 加载通知列表 */
async function loadNotifications(force = false) {
  loading.value = true
  hasError.value = false
  try {
    const result = await notificationCache.request(
      '/admin/notifications',
      queryParams as unknown as Record<string, unknown>,
      { force },
    )
    notificationList.value = result.list
    total.value = result.total
  } catch {
    hasError.value = true
    ElMessage.error('通知列表加载失败，请检查网络后重试')
  } finally {
    loading.value = false
    isFirstLoad.value = false
  }
}

// ========== 发送通知 ==========

const formRef = ref<FormInstance>()
const sendLoading = ref(false)
const targetMode = ref<'all' | 'specific'>('all')

const sendForm = reactive({
  type: NotificationType.System as NotificationType,
  title: '',
  content: '',
  targetUserIdsInput: '',
})

/** 表单验证规则 */
const formRules: FormRules = {
  type: [{ required: true, message: '请选择通知类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

/** 发送通知 */
async function handleSend() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  sendLoading.value = true
  try {
    const data: NotificationBroadcast = {
      type: sendForm.type,
      title: sendForm.title,
      content: sendForm.content,
    }

    if (targetMode.value === 'all') {
      data.targetAll = true
    } else {
      const ids = sendForm.targetUserIdsInput
        .split(',')
        .map((s) => s.trim())
        .filter(Boolean)
        .map(Number)
        .filter((n) => !isNaN(n))
      data.targetUserIds = ids
    }

    const res = await sendNotification(data)
    if (res.code === 0 || res.code === 200) {
      ElMessage.success('通知发送成功')
      handleResetForm()
      // 切换到列表页并刷新
      activeTab.value = 'list'
      notificationCache.invalidateByPrefix('/admin/notifications')
      loadNotifications(true)
    } else {
      ElMessage.error(res.message || '发送失败')
    }
  } catch {
    // 错误已在拦截器中处理
  } finally {
    sendLoading.value = false
  }
}

/** 重置发送表单 */
function handleResetForm() {
  sendForm.type = NotificationType.System
  sendForm.title = ''
  sendForm.content = ''
  sendForm.targetUserIdsInput = ''
  targetMode.value = 'all'
  formRef.value?.resetFields()
}

onMounted(() => {
  loadNotifications()
})
</script>

<style scoped lang="scss">
.notification-management {
  .search-card {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
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