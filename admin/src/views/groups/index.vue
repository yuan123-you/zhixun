<template>
  <div class="group-management">
    <!-- 页面标题 -->
    <PageHeader title="群组管理" description="管理平台所有群组，查看群组信息和聊天记录" />

    <!-- 搜索筛选栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索群组名称/编号"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="已禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 群组列表 -->
    <el-card shadow="never" class="table-card">
      <!-- 骨架屏：首次加载时展示 -->
      <template v-if="isFirstLoad">
        <el-skeleton :rows="8" animated />
      </template>

      <template v-else>
        <el-table v-loading="loading" :data="groupList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="头像" width="80">
            <template #default="{ row }">
              <el-avatar :size="40" :src="row.avatar">
                {{ row.name?.charAt(0) }}
              </el-avatar>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="群组名称" min-width="160" show-overflow-tooltip />
          <el-table-column prop="groupNumber" label="群组编号" width="140" show-overflow-tooltip />
          <el-table-column prop="ownerName" label="群主" width="120" />
          <el-table-column label="成员数" width="100">
            <template #default="{ row }">
              <span>{{ row.memberCount }} / {{ row.maxMembers }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'">
                {{ row.status === 1 ? '正常' : '已禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="260" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleViewMembers(row)">
                成员
              </el-button>
              <el-button type="info" link size="small" @click="handleViewMessages(row)">
                聊天记录
              </el-button>
              <el-button
                v-if="row.status === 1"
                type="warning"
                link
                size="small"
                @click="handleToggleStatus(row)"
              >
                禁用
              </el-button>
              <el-button
                v-else
                type="success"
                link
                size="small"
                @click="handleToggleStatus(row)"
              >
                启用
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>

          <!-- 错误重试空状态 -->
          <template #empty>
            <div class="table-empty">
              <template v-if="hasError">
                <el-icon :size="40" color="#909399"><CircleCloseFilled /></el-icon>
                <p>数据加载失败</p>
                <el-button type="primary" size="small" @click="loadGroups()">重新加载</el-button>
              </template>
              <template v-else>
                <el-empty description="暂无群组数据" />
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
          @size-change="loadGroups"
          @current-change="loadGroups"
        />
      </div>
    </el-card>

    <!-- 成员列表对话框 -->
    <el-dialog
      v-model="memberDialogVisible"
      :title="`群成员 - ${currentGroup?.name}`"
      width="700px"
      destroy-on-close
    >
      <el-table v-loading="memberLoading" :data="memberList" stripe max-height="400">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar">
                {{ row.nickname?.charAt(0) }}
              </el-avatar>
              <div class="user-info">
                <span class="nickname">{{ row.nickname }}</span>
                <span class="username">@{{ row.username }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'info'" size="small">
              {{ row.role === 1 ? '群主' : row.role === 2 ? '管理员' : '成员' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="joinedAt" label="加入时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.joinedAt) }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 成员分页 -->
      <div class="dialog-pagination">
        <el-pagination
          v-model:current-page="memberPage"
          v-model:page-size="memberPageSize"
          :total="memberTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          small
          @size-change="loadMembers"
          @current-change="loadMembers"
        />
      </div>
    </el-dialog>

    <!-- 聊天记录对话框 -->
    <el-dialog
      v-model="messageDialogVisible"
      :title="`聊天记录 - ${currentGroup?.name}`"
      width="800px"
      destroy-on-close
    >
      <el-table v-loading="messageLoading" :data="messageList" stripe max-height="400">
        <el-table-column label="发送者" width="140">
          <template #default="{ row }">
            <div class="sender-cell">
              <el-avatar :size="28" :src="row.senderAvatar">
                {{ row.senderName?.charAt(0) }}
              </el-avatar>
              <span>{{ row.senderName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="消息内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="消息类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getMessageTypeTag(row.messageType)">
              {{ getMessageTypeLabel(row.messageType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="发送时间" width="170">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <!-- 消息分页 -->
      <div class="dialog-pagination">
        <el-pagination
          v-model:current-page="messagePage"
          v-model:page-size="messagePageSize"
          :total="messageTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          small
          @size-change="loadMessages"
          @current-change="loadMessages"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import type { GroupInfo, GroupMember, GroupMessage, GroupQuery, PageResult } from '@/types'
import { getGroupList, getGroupMembers, getGroupMessages, disbandGroup, toggleGroupStatus } from '@/api/group'
import { useRequestCache } from '@/composables/useRequestCache'
import { formatDate } from '@/utils/format'
import PageHeader from '@/components/PageHeader.vue'

/** 请求缓存实例 */
const groupCache = useRequestCache<PageResult<GroupInfo>>({
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

/** 群组列表 */
const groupList = ref<GroupInfo[]>([])

/** 总数 */
const total = ref(0)

/** 查询参数 */
const queryParams = reactive<GroupQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
})

/** 搜索 */
function handleSearch() {
  queryParams.page = 1
  loadGroups()
}

/** 重置搜索 */
function handleReset() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.page = 1
  loadGroups()
}

/** 加载群组列表 */
async function loadGroups(force = false) {
  loading.value = true
  hasError.value = false
  try {
    const result = await groupCache.request(
      '/admin/groups',
      queryParams as unknown as Record<string, unknown>,
      { force },
    )
    groupList.value = result.list
    total.value = result.total
  } catch {
    hasError.value = true
    ElMessage.error('群组列表加载失败，请检查网络后重试')
  } finally {
    loading.value = false
    isFirstLoad.value = false
  }
}

// ========== 成员相关 ==========

const memberDialogVisible = ref(false)
const memberLoading = ref(false)
const memberList = ref<GroupMember[]>([])
const memberTotal = ref(0)
const memberPage = ref(1)
const memberPageSize = ref(20)
const currentGroup = ref<GroupInfo | null>(null)

/** 查看群成员 */
function handleViewMembers(group: GroupInfo) {
  currentGroup.value = group
  memberPage.value = 1
  memberPageSize.value = 20
  memberDialogVisible.value = true
  loadMembers()
}

/** 加载成员列表 */
async function loadMembers() {
  if (!currentGroup.value) return
  memberLoading.value = true
  try {
    const res = await getGroupMembers(currentGroup.value.id, memberPage.value, memberPageSize.value)
    memberList.value = res.data.list
    memberTotal.value = res.data.total
  } catch {
    ElMessage.error('成员列表加载失败')
  } finally {
    memberLoading.value = false
  }
}

// ========== 聊天记录相关 ==========

const messageDialogVisible = ref(false)
const messageLoading = ref(false)
const messageList = ref<GroupMessage[]>([])
const messageTotal = ref(0)
const messagePage = ref(1)
const messagePageSize = ref(20)

/** 查看聊天记录 */
function handleViewMessages(group: GroupInfo) {
  currentGroup.value = group
  messagePage.value = 1
  messagePageSize.value = 20
  messageDialogVisible.value = true
  loadMessages()
}

/** 加载消息列表 */
async function loadMessages() {
  if (!currentGroup.value) return
  messageLoading.value = true
  try {
    const res = await getGroupMessages(currentGroup.value.id, messagePage.value, messagePageSize.value)
    messageList.value = res.data.list
    messageTotal.value = res.data.total
  } catch {
    ElMessage.error('消息列表加载失败')
  } finally {
    messageLoading.value = false
  }
}

/** 获取消息类型标签颜色 */
function getMessageTypeTag(type: string) {
  const map: Record<string, string> = {
    text: '',
    image: 'success',
    video: 'warning',
    file: 'info',
    system: 'danger',
  }
  return map[type] || ''
}

/** 获取消息类型中文标签 */
function getMessageTypeLabel(type: string) {
  const map: Record<string, string> = {
    text: '文本',
    image: '图片',
    video: '视频',
    file: '文件',
    system: '系统',
  }
  return map[type] || type
}

// ========== 操作相关 ==========

/** 启用/禁用群组 */
async function handleToggleStatus(group: GroupInfo) {
  const newStatus = group.status === 1 ? 0 : 1
  const actionLabel = newStatus === 0 ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${actionLabel}群组 "${group.name}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await toggleGroupStatus(group.id, newStatus)
    ElMessage.success(`${actionLabel}成功`)
    groupCache.invalidate('/admin/groups', queryParams as unknown as Record<string, unknown>)
    loadGroups()
  } catch {
    // 用户取消或请求失败
  }
}

/** 删除群组 */
async function handleDelete(group: GroupInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要删除群组 "${group.name}" 吗？此操作将解散该群组且不可恢复。`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await disbandGroup(group.id)
    ElMessage.success('群组已解散')
    groupCache.invalidate('/admin/groups', queryParams as unknown as Record<string, unknown>)
    loadGroups()
  } catch {
    // 用户取消或请求失败
  }
}

onMounted(() => {
  loadGroups()
})
</script>

<style scoped lang="scss">
.group-management {
  .search-card {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .dialog-pagination {
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

  .user-cell {
    display: flex;
    align-items: center;
    gap: 10px;

    .user-info {
      display: flex;
      flex-direction: column;

      .nickname {
        font-size: 14px;
        color: #333;
      }

      .username {
        font-size: 12px;
        color: #999;
      }
    }
  }

  .sender-cell {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
</style>