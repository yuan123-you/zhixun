<template>
  <div class="system-settings">
    <el-tabs v-model="activeTab">
      <!-- 基本设置 -->
      <el-tab-pane label="基本设置" name="basic">
        <el-form :model="basicForm" :rules="basicRules" ref="basicFormRef" label-width="100px" class="settings-form">
          <el-form-item label="站点名称" prop="siteName">
            <el-input v-model="basicForm.siteName" placeholder="请输入站点名称" />
          </el-form-item>
          <el-form-item label="站点描述" prop="siteDescription">
            <el-input v-model="basicForm.siteDescription" type="textarea" :rows="3" placeholder="请输入站点描述" />
          </el-form-item>
          <el-form-item label="站点Logo">
            <ImageUpload v-model="basicForm.logo" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="saving" @click="handleSaveBasic">保存设置</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 轮播图管理 -->
      <el-tab-pane label="轮播图管理" name="banners">
        <div class="tab-header">
          <el-button type="primary" @click="handleAddBanner">添加轮播图</el-button>
        </div>
        <el-table v-loading="bannerLoading" :data="bannerList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="图片" width="200">
            <template #default="{ row }">
              <el-image :src="row.imageUrl" style="width: 160px; height: 80px" fit="cover" :preview-src-list="[row.imageUrl]" />
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="150" />
          <el-table-column prop="linkUrl" label="链接" min-width="200" show-overflow-tooltip />
          <el-table-column prop="sortOrder" label="排序" width="80" />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEditBanner(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDeleteBanner(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 轮播图对话框 -->
        <el-dialog v-model="bannerDialogVisible" :title="editingBanner ? '编辑轮播图' : '添加轮播图'" width="500px">
          <el-form :model="bannerForm" :rules="bannerFormRules" ref="bannerFormRef" label-width="80px">
            <el-form-item label="图片" prop="imageUrl">
              <ImageUpload v-model="bannerForm.imageUrl" />
            </el-form-item>
            <el-form-item label="标题" prop="title">
              <el-input v-model="bannerForm.title" placeholder="请输入标题" />
            </el-form-item>
            <el-form-item label="链接地址" prop="linkUrl">
              <el-input v-model="bannerForm.linkUrl" placeholder="请输入链接地址" />
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="bannerForm.sortOrder" :min="0" :max="999" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="bannerDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="bannerSaving" @click="handleSaveBanner">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 公告管理 -->
      <el-tab-pane label="公告管理" name="announcements">
        <div class="tab-header">
          <el-button type="primary" @click="handleAddAnnouncement">添加公告</el-button>
        </div>
        <el-table v-loading="announcementLoading" :data="announcementList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 1 ? 'primary' : 'warning'" size="small">
                {{ row.type === 1 ? '系统公告' : '活动公告' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="startTime" label="开始时间" width="170" />
          <el-table-column prop="endTime" label="结束时间" width="170" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEditAnnouncement(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDeleteAnnouncement(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 公告对话框 -->
        <el-dialog v-model="announcementDialogVisible" :title="editingAnnouncement ? '编辑公告' : '添加公告'" width="550px">
          <el-form :model="announcementForm" :rules="announcementFormRules" ref="announcementFormRef" label-width="80px">
            <el-form-item label="标题" prop="title">
              <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
            </el-form-item>
            <el-form-item label="内容" prop="content">
              <el-input v-model="announcementForm.content" type="textarea" :rows="4" placeholder="请输入公告内容" />
            </el-form-item>
            <el-form-item label="类型" prop="type">
              <el-select v-model="announcementForm.type">
                <el-option :value="1" label="系统公告" />
                <el-option :value="2" label="活动公告" />
              </el-select>
            </el-form-item>
            <el-form-item label="是否置顶">
              <el-select v-model="announcementForm.isTop">
                <el-option :value="1" label="是" />
                <el-option :value="0" label="否" />
              </el-select>
            </el-form-item>
            <el-form-item label="生效时间">
              <el-date-picker
                v-model="announcementDateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="announcementForm.status">
                <el-option :value="1" label="启用" />
                <el-option :value="0" label="禁用" />
              </el-select>
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="announcementDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="announcementSaving" @click="handleSaveAnnouncement">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Banner, Announcement } from '@/types'
import { getBannerList, createBanner, updateBanner, deleteBanner } from '@/api/banner'
import { getAnnouncementList, createAnnouncement, updateAnnouncement, deleteAnnouncement } from '@/api/announcement'
import ImageUpload from '@/components/ImageUpload.vue'

const activeTab = ref('basic')

/* ========== 基本设置 ========== */
const saving = ref(false)
const basicFormRef = ref<FormInstance>()

const basicForm = reactive({
  siteName: '知讯',
  siteDescription: '知讯内容管理系统',
  logo: '',
})

const basicRules: FormRules = {
  siteName: [{ required: true, message: '请输入站点名称', trigger: 'blur' }],
}

async function handleSaveBasic() {
  const valid = await basicFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    ElMessage.success('基本设置保存成功')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    saving.value = false
  }
}

/* ========== 轮播图管理 ========== */
const bannerLoading = ref(false)
const bannerSaving = ref(false)
const bannerList = ref<Banner[]>([])
const bannerDialogVisible = ref(false)
const editingBanner = ref<Banner | null>(null)
const bannerFormRef = ref<FormInstance>()

const bannerForm = reactive({
  imageUrl: '',
  title: '',
  linkUrl: '',
  sortOrder: 0,
})

const bannerFormRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请上传图片', trigger: 'change' }],
}

function handleAddBanner() {
  editingBanner.value = null
  bannerForm.imageUrl = ''
  bannerForm.title = ''
  bannerForm.linkUrl = ''
  bannerForm.sortOrder = 0
  bannerDialogVisible.value = true
}

function handleEditBanner(banner: any) {
  editingBanner.value = banner
  bannerForm.imageUrl = banner.imageUrl
  bannerForm.title = banner.title
  bannerForm.linkUrl = banner.linkUrl
  bannerForm.sortOrder = banner.sortOrder
  bannerDialogVisible.value = true
}

async function handleSaveBanner() {
  const valid = await bannerFormRef.value?.validate().catch(() => false)
  if (!valid) return

  bannerSaving.value = true
  try {
    const data = {
      title: bannerForm.title,
      imageUrl: bannerForm.imageUrl,
      linkUrl: bannerForm.linkUrl,
      linkType: 2,
      sortOrder: bannerForm.sortOrder,
    }
    if (editingBanner.value) {
      await updateBanner(editingBanner.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createBanner(data)
      ElMessage.success('创建成功')
    }
    bannerDialogVisible.value = false
    loadBanners()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    bannerSaving.value = false
  }
}

async function handleDeleteBanner(banner: any) {
  try {
    await ElMessageBox.confirm('确定要删除该轮播图吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteBanner(banner.id)
    ElMessage.success('删除成功')
    loadBanners()
  } catch {
    // 用户取消或请求失败
  }
}

async function loadBanners() {
  bannerLoading.value = true
  try {
    const res = await getBannerList()
    bannerList.value = res.data || []
  } catch {
    // 错误已在拦截器中处理
  } finally {
    bannerLoading.value = false
  }
}

/* ========== 公告管理 ========== */
const announcementLoading = ref(false)
const announcementSaving = ref(false)
const announcementList = ref<Announcement[]>([])
const announcementDialogVisible = ref(false)
const editingAnnouncement = ref<Announcement | null>(null)
const announcementFormRef = ref<FormInstance>()
const announcementDateRange = ref<string[]>([])

const announcementForm = reactive({
  title: '',
  content: '',
  type: 1 as number,
  isTop: 0 as number,
  startTime: '',
  endTime: '',
  status: 1 as number,
})

const announcementFormRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

function handleAddAnnouncement() {
  editingAnnouncement.value = null
  announcementForm.title = ''
  announcementForm.content = ''
  announcementForm.type = 1
  announcementForm.isTop = 0
  announcementForm.startTime = ''
  announcementForm.endTime = ''
  announcementForm.status = 1
  announcementDateRange.value = []
  announcementDialogVisible.value = true
}

function handleEditAnnouncement(announcement: any) {
  editingAnnouncement.value = announcement
  announcementForm.title = announcement.title
  announcementForm.content = announcement.content
  announcementForm.type = announcement.type
  announcementForm.isTop = announcement.isTop
  announcementForm.startTime = announcement.startTime || ''
  announcementForm.endTime = announcement.endTime || ''
  announcementForm.status = announcement.status
  announcementDateRange.value = [announcement.startTime, announcement.endTime].filter(Boolean) as string[]
  announcementDialogVisible.value = true
}

async function handleSaveAnnouncement() {
  const valid = await announcementFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (announcementDateRange.value?.length === 2) {
    announcementForm.startTime = announcementDateRange.value[0]
    announcementForm.endTime = announcementDateRange.value[1]
  }

  announcementSaving.value = true
  try {
    const data = {
      title: announcementForm.title,
      content: announcementForm.content,
      type: announcementForm.type,
      isTop: announcementForm.isTop,
      startTime: announcementForm.startTime,
      endTime: announcementForm.endTime,
      status: announcementForm.status,
    }
    if (editingAnnouncement.value) {
      await updateAnnouncement(editingAnnouncement.value.id, data)
      ElMessage.success('更新成功')
    } else {
      await createAnnouncement(data)
      ElMessage.success('创建成功')
    }
    announcementDialogVisible.value = false
    loadAnnouncements()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    announcementSaving.value = false
  }
}

async function handleDeleteAnnouncement(announcement: any) {
  try {
    await ElMessageBox.confirm('确定要删除该公告吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteAnnouncement(announcement.id)
    ElMessage.success('删除成功')
    loadAnnouncements()
  } catch {
    // 用户取消或请求失败
  }
}

async function loadAnnouncements() {
  announcementLoading.value = true
  try {
    const res = await getAnnouncementList()
    announcementList.value = res.data || []
  } catch {
    // 错误已在拦截器中处理
  } finally {
    announcementLoading.value = false
  }
}

onMounted(() => {
  loadBanners()
  loadAnnouncements()
})
</script>

<style scoped lang="scss">
.system-settings {
  .settings-form {
    max-width: 600px;
    padding: 20px 0;
  }

  .tab-header {
    margin-bottom: 16px;
  }
}
</style>
