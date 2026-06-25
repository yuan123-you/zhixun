<template>
  <div class="system-settings">
    <el-tabs v-model="activeTab">
      <!-- 基本设置 -->
      <el-tab-pane label="基本设置" name="basic">
        <el-form :model="basicForm" label-width="100px" class="settings-form">
          <el-form-item label="站点名称">
            <el-input v-model="basicForm.siteName" placeholder="请输入站点名称" />
          </el-form-item>
          <el-form-item label="站点描述">
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
        <el-table :data="bannerList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="图片" width="200">
            <template #default="{ row }">
              <el-image :src="row.imageUrl" style="width: 160px; height: 80px" fit="cover" />
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="150" />
          <el-table-column prop="link" label="链接" min-width="200" show-overflow-tooltip />
          <el-table-column prop="sort" label="排序" width="80" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="handleEditBanner(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDeleteBanner(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 轮播图对话框 -->
        <el-dialog v-model="bannerDialogVisible" :title="editingBanner ? '编辑轮播图' : '添加轮播图'" width="500px">
          <el-form :model="bannerForm" label-width="80px">
            <el-form-item label="图片">
              <ImageUpload v-model="bannerForm.imageUrl" />
            </el-form-item>
            <el-form-item label="标题">
              <el-input v-model="bannerForm.title" placeholder="请输入标题" />
            </el-form-item>
            <el-form-item label="链接">
              <el-input v-model="bannerForm.link" placeholder="请输入链接地址" />
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="bannerForm.sort" :min="0" :max="999" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="bannerDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSaveBanner">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 公告管理 -->
      <el-tab-pane label="公告管理" name="announcements">
        <div class="tab-header">
          <el-button type="primary" @click="handleAddAnnouncement">添加公告</el-button>
        </div>
        <el-table :data="announcementList" stripe>
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" min-width="200" />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 'warning' ? 'warning' : row.type === 'success' ? 'success' : 'info'">
                {{ row.type === 'warning' ? '警告' : row.type === 'success' ? '成功' : '通知' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'info'">
                {{ row.enabled ? '启用' : '禁用' }}
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
          <el-form :model="announcementForm" label-width="80px">
            <el-form-item label="标题">
              <el-input v-model="announcementForm.title" placeholder="请输入公告标题" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="announcementForm.content" type="textarea" :rows="4" placeholder="请输入公告内容" />
            </el-form-item>
            <el-form-item label="类型">
              <el-select v-model="announcementForm.type">
                <el-option label="通知" value="info" />
                <el-option label="警告" value="warning" />
                <el-option label="成功" value="success" />
              </el-select>
            </el-form-item>
            <el-form-item label="生效时间">
              <el-date-picker
                v-model="announcementDateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
            <el-form-item label="是否启用">
              <el-switch v-model="announcementForm.enabled" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="announcementDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSaveAnnouncement">确定</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Banner, Announcement } from '@/types'
import ImageUpload from '@/components/ImageUpload.vue'

const activeTab = ref('basic')
const saving = ref(false)

/** 基本设置表单 */
const basicForm = reactive({
  siteName: '知讯',
  siteDescription: '知讯内容管理系统',
  logo: '',
})

/** 轮播图相关 */
const bannerList = ref<Banner[]>([])
const bannerDialogVisible = ref(false)
const editingBanner = ref<Banner | null>(null)
const bannerForm = reactive({ imageUrl: '', title: '', link: '', sort: 0 })
const announcementDateRange = ref<string[]>([])

/** 公告相关 */
const announcementList = ref<Announcement[]>([])
const announcementDialogVisible = ref(false)
const editingAnnouncement = ref<Announcement | null>(null)
const announcementForm = reactive({
  title: '',
  content: '',
  type: 'info' as 'info' | 'warning' | 'success',
  startTime: '',
  endTime: '',
  enabled: true,
})

/** 保存基本设置 */
async function handleSaveBasic() {
  saving.value = true
  try {
    // 调用保存API
    ElMessage.success('保存成功')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    saving.value = false
  }
}

/** 添加轮播图 */
function handleAddBanner() {
  editingBanner.value = null
  bannerForm.imageUrl = ''
  bannerForm.title = ''
  bannerForm.link = ''
  bannerForm.sort = 0
  bannerDialogVisible.value = true
}

/** 编辑轮播图 */
function handleEditBanner(banner: Banner) {
  editingBanner.value = banner
  bannerForm.imageUrl = banner.imageUrl
  bannerForm.title = banner.title
  bannerForm.link = banner.link
  bannerForm.sort = banner.sort
  bannerDialogVisible.value = true
}

/** 保存轮播图 */
async function handleSaveBanner() {
  // 调用保存API
  ElMessage.success('保存成功')
  bannerDialogVisible.value = false
}

/** 删除轮播图 */
async function handleDeleteBanner(banner: Banner) {
  try {
    await ElMessageBox.confirm('确定要删除该轮播图吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}

/** 添加公告 */
function handleAddAnnouncement() {
  editingAnnouncement.value = null
  announcementForm.title = ''
  announcementForm.content = ''
  announcementForm.type = 'info'
  announcementForm.enabled = true
  announcementDateRange.value = []
  announcementDialogVisible.value = true
}

/** 编辑公告 */
function handleEditAnnouncement(announcement: Announcement) {
  editingAnnouncement.value = announcement
  announcementForm.title = announcement.title
  announcementForm.content = announcement.content
  announcementForm.type = announcement.type
  announcementForm.enabled = announcement.enabled
  announcementDateRange.value = [announcement.startTime, announcement.endTime]
  announcementDialogVisible.value = true
}

/** 保存公告 */
async function handleSaveAnnouncement() {
  if (announcementDateRange.value?.length === 2) {
    announcementForm.startTime = announcementDateRange.value[0]
    announcementForm.endTime = announcementDateRange.value[1]
  }
  // 调用保存API
  ElMessage.success('保存成功')
  announcementDialogVisible.value = false
}

/** 删除公告 */
async function handleDeleteAnnouncement(announcement: Announcement) {
  try {
    await ElMessageBox.confirm('确定要删除该公告吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    ElMessage.success('删除成功')
  } catch {
    // 用户取消
  }
}
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
