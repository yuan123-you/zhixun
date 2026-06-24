<template>
  <div class="user-management">
    <!-- 搜索筛选 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="搜索用户名/昵称" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
            <el-option label="正常" value="active" />
            <el-option label="封禁" value="banned" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="queryParams.role" placeholder="全部角色" clearable>
            <el-option label="管理员" value="admin" />
            <el-option label="编辑" value="editor" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 用户列表 -->
    <el-card shadow="never">
      <!-- 骨架屏：首次加载时展示 -->
      <template v-if="isFirstLoad">
        <el-skeleton :rows="6" animated />
      </template>

      <template v-else>
        <el-table v-loading="loading" :data="userList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="用户" min-width="200">
            <template #default="{ row }">
              <div class="user-cell">
                <el-avatar :size="36" :src="row.avatar">{{ row.nickname?.charAt(0) }}</el-avatar>
                <div class="user-info">
                  <span class="nickname">{{ row.nickname }}</span>
                  <span class="username">@{{ row.username }}</span>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="email" label="邮箱" width="200" />
          <el-table-column prop="phone" label="手机号" width="130" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'admin' ? 'danger' : row.role === 'editor' ? 'warning' : 'info'">
                {{ roleMap[row.role] || row.role }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'danger'">
                {{ row.status === 'active' ? '正常' : '封禁' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="170" />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'active'"
                type="danger"
                link
                size="small"
                @click="handleBan(row)"
              >
                封禁
              </el-button>
              <el-button
                v-if="row.status === 'banned'"
                type="success"
                link
                size="small"
                @click="handleUnban(row)"
              >
                解封
              </el-button>
              <el-button type="primary" link size="small" @click="handleAssignRole(row)">
                角色分配
              </el-button>
            </template>
          </el-table-column>

          <!-- 错误重试空状态 -->
          <template #empty>
            <div class="table-empty">
              <template v-if="hasError">
                <el-icon :size="40" color="#909399"><CircleCloseFilled /></el-icon>
                <p>数据加载失败</p>
                <el-button type="primary" size="small" @click="loadUsers()">重新加载</el-button>
              </template>
              <template v-else>
                <el-empty description="暂无用户数据" />
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
          @size-change="loadUsers"
          @current-change="loadUsers"
        />
      </div>
    </el-card>

    <!-- 角色分配对话框 -->
    <el-dialog v-model="roleDialogVisible" title="角色分配" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ currentUser?.nickname }}</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="assignRole">
            <el-option label="管理员" value="admin" />
            <el-option label="编辑" value="editor" />
            <el-option label="普通用户" value="user" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="roleLoading" @click="confirmAssignRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import type { UserInfo, UserQuery, PageResult } from '@/types'
import { banUser, unbanUser, assignRole } from '@/api/user'
import { useRequestCache } from '@/composables/useRequestCache'

/** 请求缓存实例 */
const userCache = useRequestCache<PageResult<UserInfo>>({
  ttl: 2 * 60 * 1000,
  staleWhileRevalidate: true,
  retryCount: 2,
  retryInterval: 800,
})

const loading = ref(false)
const isFirstLoad = ref(true)
const hasError = ref(false)
const userList = ref<UserInfo[]>([])
const total = ref(0)

/** 角色映射 */
const roleMap: Record<string, string> = {
  admin: '管理员',
  editor: '编辑',
  user: '普通用户',
}

const queryParams = reactive<UserQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
  role: undefined,
})

/** 角色分配相关 */
const roleDialogVisible = ref(false)
const roleLoading = ref(false)
const currentUser = ref<UserInfo | null>(null)
const assignRole = ref('user')

function handleSearch() {
  queryParams.page = 1
  loadUsers()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.role = undefined
  queryParams.page = 1
  loadUsers()
}

/** 封禁用户 */
async function handleBan(user: UserInfo) {
  try {
    await ElMessageBox.confirm(`确定要封禁用户 "${user.nickname}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await banUser(user.id)
    ElMessage.success('封禁成功')
    userCache.invalidate('/users', queryParams as unknown as Record<string, unknown>)
    loadUsers()
  } catch {
    // 用户取消或请求失败
  }
}

/** 解封用户 */
async function handleUnban(user: UserInfo) {
  try {
    await unbanUser(user.id)
    ElMessage.success('解封成功')
    userCache.invalidate('/users', queryParams as unknown as Record<string, unknown>)
    loadUsers()
  } catch {
    // 错误已在拦截器中处理
  }
}

/** 角色分配 */
function handleAssignRole(user: UserInfo) {
  currentUser.value = user
  assignRole.value = user.role
  roleDialogVisible.value = true
}

/** 确认角色分配 */
async function confirmAssignRole() {
  if (!currentUser.value) return
  roleLoading.value = true
  try {
    await assignRole(currentUser.value.id, assignRole.value)
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    userCache.invalidate('/users', queryParams as unknown as Record<string, unknown>)
    loadUsers()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    roleLoading.value = false
  }
}

/** 加载用户列表 */
async function loadUsers(force = false) {
  loading.value = true
  hasError.value = false
  try {
    const result = await userCache.request('/users', queryParams as unknown as Record<string, unknown>, { force })
    userList.value = result.list
    total.value = result.total
  } catch {
    hasError.value = true
    ElMessage.error('用户列表加载失败，请稍后重试')
  } finally {
    loading.value = false
    isFirstLoad.value = false
  }
}

onMounted(() => {
  loadUsers()
})
</script>

<style scoped lang="scss">
.user-management {
  .search-card {
    margin-bottom: 16px;
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
