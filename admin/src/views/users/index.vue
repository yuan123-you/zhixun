<template>
  <div class="user-management">
    <!-- 搜索筛选 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="搜索用户名/昵称/UID" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
            <el-option label="正常" value="active" />
            <el-option label="封禁" value="banned" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="queryParams.role" placeholder="全部角色" clearable>
            <el-option label="管理员" value="ADMIN" />
            <el-option label="超级管理员" value="SUPER_ADMIN" />
            <el-option label="普通用户" value="USER" />
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
        <el-table v-loading="loading" :data="userList" stripe @row-click="handleRowClick" style="cursor: pointer">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="uid" label="UID" width="150" />
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
              <el-tag :type="row.role?.toUpperCase() === 'ADMIN' || row.role?.toUpperCase() === 'SUPER_ADMIN' ? 'danger' : row.role?.toUpperCase() === 'EDITOR' ? 'warning' : 'info'">
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
          <el-table-column label="IP属地" width="120">
            <template #default="{ row }">
              {{ (row as any).province || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="关注/粉丝" width="100">
            <template #default="{ row }">
              {{ (row as any).followCount ?? '-' }} / {{ (row as any).followerCount ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="文章数" width="80">
            <template #default="{ row }">
              {{ (row as any).articleCount ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="最后登录" width="170">
            <template #default="{ row }">
              {{ (row as any).lastLoginAt || '-' }}
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
                @click.stop="handleBan(row)"
              >
                封禁
              </el-button>
              <el-button
                v-if="row.status === 'banned'"
                type="success"
                link
                size="small"
                @click.stop="handleUnban(row)"
              >
                解封
              </el-button>
              <el-button type="primary" link size="small" @click.stop="handleAssignRole(row)">
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

    <!-- 用户详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="用户详情" width="680px" @close="userDetail = null">
      <template v-if="detailLoading">
        <el-skeleton :rows="10" animated />
      </template>
      <template v-else-if="userDetail">
        <!-- 基本信息 -->
        <div class="detail-header">
          <el-avatar :size="64" :src="userDetail.avatar">{{ userDetail.nickname?.charAt(0) }}</el-avatar>
          <div class="detail-header-info">
            <h3>{{ userDetail.nickname }}</h3>
            <p>@{{ userDetail.username }} · UID: {{ userDetail.uid }}</p>
          </div>
        </div>
        <el-divider />

        <!-- 基本信息 -->
        <el-descriptions :column="2" border size="small" title="基本信息">
          <el-descriptions-item label="邮箱">{{ userDetail.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ userDetail.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="userDetail.role?.toUpperCase() === 'ADMIN' || userDetail.role?.toUpperCase() === 'SUPER_ADMIN' ? 'danger' : userDetail.role?.toUpperCase() === 'EDITOR' ? 'warning' : 'info'" size="small">
              {{ roleMap[userDetail.role] || userDetail.role }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="userDetail.status === 'active' ? 'success' : 'danger'" size="small">
              {{ userDetail.status === 'active' ? '正常' : '封禁' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 统计数据 -->
        <el-descriptions :column="4" border size="small" title="统计数据" style="margin-top: 16px">
          <el-descriptions-item label="关注">{{ userDetail.followCount }}</el-descriptions-item>
          <el-descriptions-item label="粉丝">{{ userDetail.followerCount }}</el-descriptions-item>
          <el-descriptions-item label="文章">{{ userDetail.articleCount }}</el-descriptions-item>
          <el-descriptions-item label="获赞">{{ userDetail.totalLikeCount }}</el-descriptions-item>
        </el-descriptions>

        <!-- 位置信息 -->
        <el-descriptions :column="2" border size="small" title="位置信息" style="margin-top: 16px">
          <el-descriptions-item label="省份">{{ userDetail.province || '-' }}</el-descriptions-item>
          <el-descriptions-item label="IP属地">{{ userDetail.ipLocation || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 个人资料 -->
        <el-descriptions :column="2" border size="small" title="个人资料" style="margin-top: 16px">
          <el-descriptions-item label="性别">{{ userDetail.gender === 1 ? '男' : userDetail.gender === 2 ? '女' : '未知' }}</el-descriptions-item>
          <el-descriptions-item label="生日">{{ userDetail.birthday || '-' }}</el-descriptions-item>
          <el-descriptions-item label="简介" :span="2">{{ userDetail.bio || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 活动信息 -->
        <el-descriptions :column="2" border size="small" title="活动信息" style="margin-top: 16px">
          <el-descriptions-item label="最后登录">{{ userDetail.lastLoginAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="最后登录IP">{{ userDetail.lastLoginIp || '-' }}</el-descriptions-item>
          <el-descriptions-item label="登录次数">{{ userDetail.loginCount }}</el-descriptions-item>
        </el-descriptions>

        <!-- 隐私设置 -->
        <el-descriptions :column="2" border size="small" title="隐私设置" style="margin-top: 16px">
          <el-descriptions-item label="在线状态可见">{{ userDetail.showOnlineStatus ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="消息权限">
            {{ userDetail.messagePermission === 1 ? '所有人' : userDetail.messagePermission === 2 ? '关注的人' : '关闭' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <el-button @click="showLoginHistory">登录历史</el-button>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 用户登录历史对话框 -->
    <el-dialog v-model="loginHistoryDialogVisible" title="用户登录历史" width="800px">
      <el-table v-loading="loginHistoryLoading" :data="loginHistoryList" stripe>
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="location" label="登录地" width="150" />
        <el-table-column prop="userAgent" label="用户代理" min-width="250" show-overflow-tooltip />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
      </el-table>
      <div style="display: flex; justify-content: flex-end; margin-top: 16px">
        <el-pagination
          v-model:current-page="loginHistoryPage"
          v-model:page-size="loginHistoryPageSize"
          :total="loginHistoryTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          small
          @size-change="loadLoginHistory"
          @current-change="loadLoginHistory"
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import type { UserInfo, UserDetail, UserQuery, PageResult } from '@/types'
import { banUser, unbanUser, assignRole, getUserDetail, getUserLoginHistory } from '@/api/user'
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
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
  EDITOR: '编辑',
  USER: '普通用户',
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

/** 用户详情相关 */
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const userDetail = ref<UserDetail | null>(null)

/** 登录历史相关 */
const loginHistoryDialogVisible = ref(false)
const loginHistoryLoading = ref(false)
const loginHistoryList = ref<any[]>([])
const loginHistoryTotal = ref(0)
const loginHistoryPage = ref(1)
const loginHistoryPageSize = ref(20)
const loginHistoryUserId = ref<number | null>(null)

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
    userCache.invalidate('/admin/users', queryParams as unknown as Record<string, unknown>)
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
    userCache.invalidate('/admin/users', queryParams as unknown as Record<string, unknown>)
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
    userCache.invalidate('/admin/users', queryParams as unknown as Record<string, unknown>)
    loadUsers()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    roleLoading.value = false
  }
}

/** 行点击 - 打开用户详情 */
async function handleRowClick(row: UserInfo) {
  detailDialogVisible.value = true
  detailLoading.value = true
  userDetail.value = null
  try {
    const result = await getUserDetail(row.id)
    userDetail.value = result
  } catch {
    ElMessage.error('用户详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

/** 打开登录历史对话框 */
function showLoginHistory() {
  if (!userDetail.value) return
  loginHistoryUserId.value = userDetail.value.id
  loginHistoryPage.value = 1
  loginHistoryDialogVisible.value = true
  loadLoginHistory()
}

/** 加载登录历史 */
async function loadLoginHistory() {
  if (!loginHistoryUserId.value) return
  loginHistoryLoading.value = true
  try {
    const result = await getUserLoginHistory(
      loginHistoryUserId.value,
      loginHistoryPage.value,
      loginHistoryPageSize.value
    )
    loginHistoryList.value = result.list
    loginHistoryTotal.value = result.total
  } catch {
    ElMessage.error('登录历史加载失败')
  } finally {
    loginHistoryLoading.value = false
  }
}

/** 加载用户列表 */
async function loadUsers(force = false) {
  loading.value = true
  hasError.value = false
  try {
    const result = await userCache.request('/admin/users', queryParams as unknown as Record<string, unknown>, { force })
    userList.value = result.list
    total.value = result.total
  } catch {
    hasError.value = true
    ElMessage.error('用户列表加载失败，请检查网络后重试')
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

  .detail-header {
    display: flex;
    align-items: center;
    gap: 16px;

    .detail-header-info {
      h3 {
        margin: 0 0 4px 0;
        font-size: 18px;
      }

      p {
        margin: 0;
        color: #999;
        font-size: 13px;
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
