<template>
  <div class="role-management">
    <PageHeader title="角色管理" description="管理平台管理员角色和权限，仅超级管理员可操作" />

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

    <!-- 管理员列表 -->
    <el-card shadow="never" class="table-card">
      <template v-if="isFirstLoad">
        <el-skeleton :rows="8" animated />
      </template>

      <template v-else>
        <el-table v-loading="loading" :data="adminList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="头像" width="80">
            <template #default="{ row }">
              <el-avatar :size="40" :src="row.avatar">
                {{ row.nickname?.charAt(0) || '管' }}
              </el-avatar>
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名" width="140" show-overflow-tooltip />
          <el-table-column prop="nickname" label="昵称" width="140" show-overflow-tooltip />
          <el-table-column label="角色" width="180">
            <template #default="{ row }">
              <el-select
                v-if="row.id !== currentUserId"
                :model-value="row.role"
                size="small"
                style="width: 140px"
                @change="(val: any) => handleRoleChange(row, val)"
              >
                <el-option label="运营管理员" value="ADMIN" />
                <el-option label="超级管理员" value="SUPER_ADMIN" />
              </el-select>
              <el-tag v-else type="danger">{{ getRoleLabel(row.role) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-switch
                v-if="row.id !== currentUserId"
                :model-value="row.status === 1"
                active-text="正常"
                inactive-text="禁用"
                inline-prompt
                @change="(val: any) => handleStatusChange(row, val)"
              />
              <el-tag v-else type="success">正常</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" width="170" />
          <el-table-column label="备注" min-width="120">
            <template #default="{ row }">
              <el-tag v-if="row.id === currentUserId" type="info" size="small">当前账号</el-tag>
            </template>
          </el-table-column>

          <template #empty>
            <div class="table-empty">
              <template v-if="hasError">
                <el-icon :size="40" color="#909399"><CircleCloseFilled /></el-icon>
                <p>数据加载失败</p>
                <el-button type="primary" size="small" @click="loadAdmins()">重新加载</el-button>
              </template>
              <template v-else>
                <el-empty description="暂无管理员数据" />
              </template>
            </div>
          </template>
        </el-table>
      </template>

      <div v-if="!isFirstLoad" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadAdmins"
          @current-change="loadAdmins"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getAdminList, updateAdminRole, updateAdminStatus, type AdminUser } from '@/api/roleManagement'
import PageHeader from '@/components/PageHeader.vue'

const userStore = useUserStore()

const currentUserId = computed(() => userStore.userInfo?.id)

const loading = ref(false)
const isFirstLoad = ref(true)
const hasError = ref(false)
const adminList = ref<AdminUser[]>([])
const total = ref(0)

const queryParams = reactive({
  keyword: '',
  status: undefined as number | undefined,
  page: 1,
  pageSize: 10,
})

function handleSearch() {
  queryParams.page = 1
  loadAdmins()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.page = 1
  loadAdmins()
}

async function loadAdmins() {
  loading.value = true
  hasError.value = false
  try {
    const res = await getAdminList(queryParams)
    adminList.value = res.data.list
    total.value = res.data.total
  } catch {
    hasError.value = true
    ElMessage.error('管理员列表加载失败')
  } finally {
    loading.value = false
    isFirstLoad.value = false
  }
}

function getRoleLabel(role: string) {
  const map: Record<string, string> = {
    ADMIN: '运营管理员',
    SUPER_ADMIN: '超级管理员',
  }
  return map[role] || role
}

async function handleRoleChange(row: any, newRole: string) {
  const oldLabel = getRoleLabel(row.role)
  const newLabel = getRoleLabel(newRole)
  try {
    await ElMessageBox.confirm(
      `确定要将 "${row.nickname || row.username}" 的角色从「${oldLabel}」修改为「${newLabel}」吗？`,
      '修改角色',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await updateAdminRole(row.id, newRole)
    ElMessage.success('角色修改成功')
    loadAdmins()
  } catch {
    // User cancelled or error
  }
}

async function handleStatusChange(row: any, newVal: boolean) {
  const newStatus = newVal ? 1 : 0
  const action = newVal ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确定要${action}管理员 "${row.nickname || row.username}" 吗？`,
      '提示',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    await updateAdminStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    loadAdmins()
  } catch {
    // User cancelled or error
  }
}

onMounted(() => {
  loadAdmins()
})
</script>

<style scoped lang="scss">
.role-management {
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
