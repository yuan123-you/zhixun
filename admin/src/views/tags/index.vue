<template>
  <div class="tag-management">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>标签管理</span>
          <div class="flex gap-2">
            <el-button @click="handleSyncArticleCount" :loading="syncLoading">同步作品数</el-button>
            <el-button type="warning" @click="showMergeDialog">合并标签</el-button>
            <el-button type="primary" @click="handleAdd">新增标签</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索 -->
      <el-form :model="queryParams" inline class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="搜索标签名" clearable @keyup.enter="loadTags" />
        </el-form-item>
        <el-form-item label="排序">
          <el-select v-model="queryParams.sortBy" placeholder="排序方式" clearable style="width: 140px" @change="loadTags">
            <el-option label="创建时间" value="createdAt" />
            <el-option label="作品数降序" value="articleCountDesc" />
            <el-option label="作品数升序" value="articleCountAsc" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTags">搜索</el-button>
        </el-form-item>
      </el-form>

      <!-- 标签列表 -->
      <el-table v-loading="loading" :data="tagList" stripe :default-sort="defaultSort" @sort-change="handleSortChange">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="标签名称" width="200" />
        <el-table-column label="颜色" width="100">
          <template #default="{ row }">
            <el-color-picker v-model="row.color" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="articleCount" label="作品数" width="120" sortable="custom">
          <template #default="{ row }">
            <el-tag :type="row.articleCount > 0 ? 'success' : 'info'" size="small">
              {{ row.articleCount }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="热度" width="160">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <el-progress
                :percentage="getHotPercentage(row.articleCount)"
                :stroke-width="8"
                :show-text="false"
                :color="getHotColor(row.articleCount)"
                style="width: 80px"
              />
              <span class="text-xs" :class="row.articleCount >= 50 ? 'text-red-500' : row.articleCount >= 20 ? 'text-orange-500' : 'text-gray-500'">
                {{ getHotLabel(row.articleCount) }}
              </span>
            </div>
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

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadTags"
          @current-change="loadTags"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingTag ? '编辑标签' : '新增标签'" width="450px">
      <el-form :model="tagForm" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="tagForm.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签颜色" prop="color">
          <el-color-picker v-model="tagForm.color" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 合并标签对话框 -->
    <el-dialog v-model="mergeDialogVisible" title="合并标签" width="500px">
      <el-alert type="warning" :closable="false" class="mb-4">
        合并后，源标签下的所有作品将转移至目标标签，源标签将被删除。此操作不可逆，请谨慎操作。
      </el-alert>
      <el-form :model="mergeForm" :rules="mergeRules" ref="mergeFormRef" label-width="100px">
        <el-form-item label="源标签" prop="sourceTagId">
          <el-select
            v-model="mergeForm.sourceTagId"
            placeholder="选择要合并的标签（将被删除）"
            filterable
            remote
            :remote-method="searchSourceTags"
            :loading="mergeSearchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="tag in sourceTagOptions"
              :key="tag.id"
              :label="`${tag.name}（${tag.articleCount}篇作品）`"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标标签" prop="targetTagId">
          <el-select
            v-model="mergeForm.targetTagId"
            placeholder="选择目标标签（保留此标签）"
            filterable
            remote
            :remote-method="searchTargetTags"
            :loading="mergeSearchLoading"
            style="width: 100%"
          >
            <el-option
              v-for="tag in targetTagOptions"
              :key="tag.id"
              :label="`${tag.name}（${tag.articleCount}篇作品）`"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="mergeDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="mergeLoading" :disabled="!mergeForm.sourceTagId || !mergeForm.targetTagId || mergeForm.sourceTagId === mergeForm.targetTagId" @click="handleMerge">
          确认合并
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Tag, PageParams } from '@/types'
import { getTagList, createTag, updateTag, deleteTag, mergeTag, syncArticleCount, searchTags } from '@/api/tag'
import { useRequestCache } from '@/composables/useRequestCache'

/** 标签缓存实例 */
const tagCache = useRequestCache({
  ttl: 3 * 60 * 1000,
  staleWhileRevalidate: true,
})

const loading = ref(false)
const submitLoading = ref(false)
const syncLoading = ref(false)
const mergeLoading = ref(false)
const mergeSearchLoading = ref(false)
const dialogVisible = ref(false)
const mergeDialogVisible = ref(false)
const editingTag = ref<Tag | null>(null)
const formRef = ref<FormInstance>()
const mergeFormRef = ref<FormInstance>()

const tagList = ref<Tag[]>([])
const total = ref(0)
const queryParams = reactive<PageParams & { keyword?: string; sortBy?: string }>({
  page: 1,
  pageSize: 10,
  keyword: '',
  sortBy: '',
})

const defaultSort = computed(() => {
  if (queryParams.sortBy === 'articleCountDesc') return { prop: 'articleCount', order: 'descending' }
  if (queryParams.sortBy === 'articleCountAsc') return { prop: 'articleCount', order: 'ascending' }
  return {}
})

const tagForm = reactive({
  name: '',
  color: '#409EFF',
})

const mergeForm = reactive({
  sourceTagId: null as number | null,
  targetTagId: null as number | null,
})

const sourceTagOptions = ref<Tag[]>([])
const targetTagOptions = ref<Tag[]>([])

const formRules: FormRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
}

const mergeRules: FormRules = {
  sourceTagId: [{ required: true, message: '请选择源标签', trigger: 'change' }],
  targetTagId: [{ required: true, message: '请选择目标标签', trigger: 'change' }],
}

function handleAdd() {
  editingTag.value = null
  tagForm.name = ''
  tagForm.color = '#409EFF'
  dialogVisible.value = true
}

function handleEdit(tag: Tag) {
  editingTag.value = tag
  tagForm.name = tag.name
  tagForm.color = tag.color
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (editingTag.value) {
      await updateTag(editingTag.value.id, tagForm)
      ElMessage.success('更新成功')
    } else {
      await createTag(tagForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    tagCache.invalidateByPrefix('/tags')
    loadTags()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(tag: Tag) {
  try {
    await ElMessageBox.confirm('确定要删除该标签吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteTag(tag.id)
    ElMessage.success('删除成功')
    tagCache.invalidateByPrefix('/tags')
    loadTags()
  } catch {
    // 用户取消或请求失败
  }
}

async function handleSyncArticleCount() {
  try {
    await ElMessageBox.confirm('确定要同步所有标签的作品数吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    syncLoading.value = true
    await syncArticleCount()
    ElMessage.success('同步成功')
    tagCache.invalidateByPrefix('/tags')
    loadTags()
  } catch {
    // 用户取消或请求失败
  } finally {
    syncLoading.value = false
  }
}

function showMergeDialog() {
  mergeForm.sourceTagId = null
  mergeForm.targetTagId = null
  sourceTagOptions.value = []
  targetTagOptions.value = []
  mergeDialogVisible.value = true
}

async function searchSourceTags(keyword: string) {
  if (!keyword) return
  mergeSearchLoading.value = true
  try {
    const res = await searchTags(keyword)
    sourceTagOptions.value = res.data || []
  } catch {
    sourceTagOptions.value = []
  } finally {
    mergeSearchLoading.value = false
  }
}

async function searchTargetTags(keyword: string) {
  if (!keyword) return
  mergeSearchLoading.value = true
  try {
    const res = await searchTags(keyword)
    targetTagOptions.value = res.data || []
  } catch {
    targetTagOptions.value = []
  } finally {
    mergeSearchLoading.value = false
  }
}

async function handleMerge() {
  const valid = await mergeFormRef.value?.validate().catch(() => false)
  if (!valid) return

  if (mergeForm.sourceTagId === mergeForm.targetTagId) {
    ElMessage.warning('源标签和目标标签不能相同')
    return
  }

  const sourceTag = sourceTagOptions.value.find(t => t.id === mergeForm.sourceTagId)
  const targetTag = targetTagOptions.value.find(t => t.id === mergeForm.targetTagId)

  try {
    await ElMessageBox.confirm(
      `确定将标签「${sourceTag?.name}」合并到「${targetTag?.name}」吗？源标签将被删除，其下所有作品将转移至目标标签。`,
      '确认合并',
      {
        confirmButtonText: '确定合并',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    mergeLoading.value = true
    await mergeTag(mergeForm.sourceTagId!, mergeForm.targetTagId!)
    ElMessage.success('合并成功')
    mergeDialogVisible.value = false
    tagCache.invalidateByPrefix('/tags')
    loadTags()
  } catch {
    // 用户取消或请求失败
  } finally {
    mergeLoading.value = false
  }
}

function handleSortChange({ prop, order }: { prop: string; order: string | null }) {
  if (prop === 'articleCount') {
    queryParams.sortBy = order === 'descending' ? 'articleCountDesc' : 'articleCountAsc'
  } else {
    queryParams.sortBy = ''
  }
  loadTags()
}

function getHotPercentage(count: number): number {
  const maxCount = Math.max(...tagList.value.map(t => t.articleCount), 1)
  return Math.min(Math.round((count / maxCount) * 100), 100)
}

function getHotColor(count: number): string {
  if (count >= 50) return '#f56c6c'
  if (count >= 20) return '#e6a23c'
  if (count >= 5) return '#409eff'
  return '#909399'
}

function getHotLabel(count: number): string {
  if (count >= 50) return '热门'
  if (count >= 20) return '较热'
  if (count >= 5) return '一般'
  return '冷门'
}

async function loadTags(force = false) {
  loading.value = true
  try {
    const result = await tagCache.request('/tags', queryParams as unknown as Record<string, unknown>, { force })
    tagList.value = result.list
    total.value = result.total
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadTags()
})
</script>

<style scoped lang="scss">
.tag-management {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-form {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }
}
</style>
