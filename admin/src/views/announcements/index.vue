<template>
  <div class="announcement-management">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>公告管理</span>
          <el-button type="primary" @click="handleAdd()">新增公告</el-button>
        </div>
      </template>

      <!-- 公告表格 -->
      <el-table v-loading="loading" :data="announcementList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="250" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.type === 1 ? 'primary' : 'warning'" size="small">
              {{ row.type === 1 ? '系统公告' : '活动公告' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isTop === 1 ? 'danger' : 'info'" size="small">
              {{ row.isTop === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="展示时间" width="180">
          <template #default="{ row }">
            <div class="text-xs">
              <div>{{ row.startTime || '-' }}</div>
              <div>{{ row.endTime || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingAnnouncement ? '编辑公告' : '新增公告'"
      width="600px"
    >
      <el-form :model="announcementForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="4"
            placeholder="请输入公告内容"
          />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="announcementForm.type" placeholder="请选择公告类型">
            <el-option :value="1" label="系统公告" />
            <el-option :value="2" label="活动公告" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否置顶" prop="isTop">
          <el-select v-model="announcementForm.isTop" placeholder="请选择">
            <el-option :value="1" label="是" />
            <el-option :value="0" label="否" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="announcementForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="announcementForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="announcementForm.status" placeholder="请选择状态">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Announcement } from '@/types'
import { getAnnouncementList, createAnnouncement, updateAnnouncement, deleteAnnouncement } from '@/api/announcement'
import { useRequestCache } from '@/composables/useRequestCache'

/** 公告缓存实例 */
const announcementCache = useRequestCache<Announcement[]>({
  ttl: 5 * 60 * 1000,
  staleWhileRevalidate: true,
})

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingAnnouncement = ref<Announcement | null>(null)
const formRef = ref<FormInstance>()

/** 公告列表 */
const announcementList = ref<Announcement[]>([])

/** 表单数据 */
const announcementForm = reactive({
  title: '',
  content: '',
  type: 1,
  isTop: 0,
  startTime: '',
  endTime: '',
  status: 1,
})

/** 表单验证规则 */
const formRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

/** 新增公告 */
function handleAdd() {
  editingAnnouncement.value = null
  announcementForm.title = ''
  announcementForm.content = ''
  announcementForm.type = 1
  announcementForm.isTop = 0
  announcementForm.startTime = ''
  announcementForm.endTime = ''
  announcementForm.status = 1
  dialogVisible.value = true
}

/** 编辑公告 */
function handleEdit(announcement: Announcement) {
  editingAnnouncement.value = announcement
  announcementForm.title = announcement.title
  announcementForm.content = announcement.content
  announcementForm.type = announcement.type
  announcementForm.isTop = announcement.isTop
  announcementForm.startTime = announcement.startTime || ''
  announcementForm.endTime = announcement.endTime || ''
  announcementForm.status = announcement.status
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (editingAnnouncement.value) {
      await updateAnnouncement(editingAnnouncement.value.id, announcementForm)
      ElMessage.success('更新成功')
    } else {
      await createAnnouncement(announcementForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    announcementCache.invalidate('/admin/announcements')
    loadAnnouncements()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    submitLoading.value = false
  }
}

/** 删除公告 */
async function handleDelete(announcement: Announcement) {
  try {
    await ElMessageBox.confirm('确定要删除该公告吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteAnnouncement(announcement.id)
    ElMessage.success('删除成功')
    announcementCache.invalidate('/admin/announcements')
    loadAnnouncements()
  } catch {
    // 用户取消或请求失败
  }
}

/** 加载公告列表 */
async function loadAnnouncements(force = false) {
  loading.value = true
  try {
    const result = await announcementCache.request('/admin/announcements', undefined, { force })
    announcementList.value = result
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAnnouncements()
})
</script>

<style scoped lang="scss">
.announcement-management {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
