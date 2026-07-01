<template>
  <div class="banner-management">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>轮播图管理</span>
          <el-button type="primary" @click="handleAdd()">新增轮播图</el-button>
        </div>
      </template>

      <!-- 轮播图表格 -->
      <el-table v-loading="loading" :data="bannerList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="图片" width="160">
          <template #default="{ row }">
            <el-image
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              fit="cover"
              style="width: 120px; height: 60px; border-radius: 4px"
              preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column label="链接类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.linkType === 1 ? 'primary' : 'success'" size="small">
              {{ row.linkType === 1 ? '作品' : '外链' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="linkUrl" label="链接地址" min-width="150" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" />
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
      :title="editingBanner ? '编辑轮播图' : '新增轮播图'"
      width="600px"
    >
      <el-form :model="bannerForm" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="bannerForm.title" placeholder="请输入轮播图标题" />
        </el-form-item>
        <el-form-item label="图片地址" prop="imageUrl">
          <el-input v-model="bannerForm.imageUrl" placeholder="请输入图片地址" />
        </el-form-item>
        <el-form-item label="链接类型" prop="linkType">
          <el-select v-model="bannerForm.linkType" placeholder="请选择链接类型">
            <el-option :value="1" label="作品" />
            <el-option :value="2" label="外链" />
          </el-select>
        </el-form-item>
        <el-form-item label="链接地址" prop="linkUrl">
          <el-input v-model="bannerForm.linkUrl" placeholder="请输入链接地址" />
        </el-form-item>
        <el-form-item label="排序值" prop="sortOrder">
          <el-input-number v-model="bannerForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
            v-model="bannerForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
            v-model="bannerForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="bannerForm.status" placeholder="请选择状态">
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
import type { Banner } from '@/types'
import { createBanner, updateBanner, deleteBanner } from '@/api/banner'
import { useRequestCache } from '@/composables/useRequestCache'

/** 轮播图缓存实例 */
const bannerCache = useRequestCache<Banner[]>({
  ttl: 5 * 60 * 1000,
  staleWhileRevalidate: true,
})

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingBanner = ref<Banner | null>(null)
const formRef = ref<FormInstance>()

/** 轮播图列表 */
const bannerList = ref<Banner[]>([])

/** 表单数据 */
const bannerForm = reactive({
  title: '',
  imageUrl: '',
  linkUrl: '',
  linkType: 1,
  sortOrder: 0,
  startTime: '',
  endTime: '',
  status: 1,
})

/** 表单验证规则 */
const formRules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请输入图片地址', trigger: 'blur' }],
}

/** 新增轮播图 */
function handleAdd() {
  editingBanner.value = null
  bannerForm.title = ''
  bannerForm.imageUrl = ''
  bannerForm.linkUrl = ''
  bannerForm.linkType = 1
  bannerForm.sortOrder = 0
  bannerForm.startTime = ''
  bannerForm.endTime = ''
  bannerForm.status = 1
  dialogVisible.value = true
}

/** 编辑轮播图 */
function handleEdit(banner: any) {
  editingBanner.value = banner
  bannerForm.title = banner.title
  bannerForm.imageUrl = banner.imageUrl
  bannerForm.linkUrl = banner.linkUrl
  bannerForm.linkType = banner.linkType
  bannerForm.sortOrder = banner.sortOrder
  bannerForm.startTime = banner.startTime || ''
  bannerForm.endTime = banner.endTime || ''
  bannerForm.status = banner.status
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (editingBanner.value) {
      await updateBanner(editingBanner.value.id, bannerForm)
      ElMessage.success('更新成功')
    } else {
      await createBanner(bannerForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    bannerCache.invalidateByPrefix('/admin/banners')
    loadBanners()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    submitLoading.value = false
  }
}

/** 删除轮播图 */
async function handleDelete(banner: any) {
  try {
    await ElMessageBox.confirm('确定要删除该轮播图吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteBanner(banner.id)
    ElMessage.success('删除成功')
    bannerCache.invalidateByPrefix('/admin/banners')
    loadBanners()
  } catch {
    // 用户取消或请求失败
  }
}

/** 加载轮播图列表 */
async function loadBanners(force = false) {
  loading.value = true
  try {
    const result = await bannerCache.request('/admin/banners', undefined, { force })
    bannerList.value = result
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadBanners()
})
</script>

<style scoped lang="scss">
.banner-management {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
