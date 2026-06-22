<template>
  <div class="category-management">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>分类管理</span>
          <el-button type="primary" @click="handleAdd()">新增分类</el-button>
        </div>
      </template>

      <!-- 分类树形表格 -->
      <el-table
        v-loading="loading"
        :data="categoryTree"
        row-key="id"
        :tree-props="{ children: 'children' }"
        stripe
      >
        <el-table-column prop="name" label="分类名称" min-width="200" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="articleCount" label="文章数" width="100" />
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleAdd(row)">新增子分类</el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editingCategory ? '编辑分类' : '新增分类'"
      width="500px"
    >
      <el-form :model="categoryForm" :rules="formRules" ref="formRef" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="categoryForm.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="父级分类" prop="parentId">
          <el-select v-model="categoryForm.parentId" placeholder="无（顶级分类）" clearable>
            <el-option
              v-for="cat in flatCategories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="categoryForm.description" type="textarea" :rows="3" placeholder="请输入分类描述" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="categoryForm.sort" :min="0" :max="999" />
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
import type { Category } from '@/types'
import { getCategoryTree, createCategory, updateCategory, deleteCategory } from '@/api/category'

const loading = ref(false)
const submitLoading = ref(false)
const dialogVisible = ref(false)
const editingCategory = ref<Category | null>(null)
const formRef = ref<FormInstance>()

/** 分类树数据 */
const categoryTree = ref<Category[]>([])

/** 扁平化分类列表（用于父级选择） */
const flatCategories = ref<Category[]>([])

/** 表单数据 */
const categoryForm = reactive({
  name: '',
  description: '',
  sort: 0,
  parentId: 0,
})

/** 表单验证规则 */
const formRules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

/** 扁平化分类树 */
function flattenCategories(categories: Category[]): Category[] {
  const result: Category[] = []
  for (const cat of categories) {
    result.push(cat)
    if (cat.children?.length) {
      result.push(...flattenCategories(cat.children))
    }
  }
  return result
}

/** 新增分类 */
function handleAdd(parent?: Category) {
  editingCategory.value = null
  categoryForm.name = ''
  categoryForm.description = ''
  categoryForm.sort = 0
  categoryForm.parentId = parent?.id || 0
  dialogVisible.value = true
}

/** 编辑分类 */
function handleEdit(category: Category) {
  editingCategory.value = category
  categoryForm.name = category.name
  categoryForm.description = category.description
  categoryForm.sort = category.sort
  categoryForm.parentId = category.parentId
  dialogVisible.value = true
}

/** 提交表单 */
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (editingCategory.value) {
      await updateCategory(editingCategory.value.id, categoryForm)
      ElMessage.success('更新成功')
    } else {
      await createCategory(categoryForm)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadCategories()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    submitLoading.value = false
  }
}

/** 删除分类 */
async function handleDelete(category: Category) {
  try {
    await ElMessageBox.confirm('确定要删除该分类吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteCategory(category.id)
    ElMessage.success('删除成功')
    loadCategories()
  } catch {
    // 用户取消或请求失败
  }
}

/** 加载分类列表 */
async function loadCategories() {
  loading.value = true
  try {
    const res = await getCategoryTree()
    categoryTree.value = res.data
    flatCategories.value = flattenCategories(res.data)
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped lang="scss">
.category-management {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>
