<template>
  <div class="tag-management">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>标签管理</span>
          <el-button type="primary" @click="handleAdd">新增标签</el-button>
        </div>
      </template>

      <!-- 搜索 -->
      <el-form :model="queryParams" inline class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="搜索标签名" clearable @keyup.enter="loadTags" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadTags">搜索</el-button>
        </el-form-item>
      </el-form>

      <!-- 标签列表 -->
      <el-table v-loading="loading" :data="tagList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="标签名称" width="200" />
        <el-table-column label="颜色" width="100">
          <template #default="{ row }">
            <el-color-picker v-model="row.color" disabled />
          </template>
        </el-table-column>
        <el-table-column prop="articleCount" label="文章数" width="100" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Tag, PageParams } from '@/types'
import { getTagList, createTag, updateTag, deleteTag } from '@/api/tag'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingTag = ref<Tag | null>(null)
const formRef = ref<FormInstance>()

const tagList = ref<Tag[]>([])
const total = ref(0)
const queryParams = reactive<PageParams & { keyword?: string }>({
  page: 1,
  pageSize: 10,
  keyword: '',
})

const tagForm = reactive({
  name: '',
  color: '#409EFF',
})

const formRules: FormRules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
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
    loadTags()
  } catch {
    // 用户取消或请求失败
  }
}

async function loadTags() {
  loading.value = true
  try {
    const res = await getTagList(queryParams)
    tagList.value = res.data.list
    total.value = res.data.total
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
