<template>
  <div class="sensitive-words">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>敏感词管理</span>
          <div>
            <el-button type="primary" @click="handleAdd">添加敏感词</el-button>
            <el-button @click="handleBatchAdd">批量添加</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索 -->
      <el-form :model="queryParams" inline class="search-form">
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="搜索敏感词" clearable @keyup.enter="loadWords" />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="queryParams.level" placeholder="全部级别" clearable>
            <el-option label="替换" value="replace" />
            <el-option label="审核" value="review" />
            <el-option label="禁止" value="block" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadWords">搜索</el-button>
        </el-form-item>
      </el-form>

      <!-- 敏感词列表 -->
      <el-table v-loading="loading" :data="wordList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="word" label="敏感词" min-width="200" />
        <el-table-column label="级别" width="120">
          <template #default="{ row }">
            <el-tag :type="levelTypeMap[row.level]">
              {{ levelNameMap[row.level] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEditLevel(row)">
              修改级别
            </el-button>
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
          @size-change="loadWords"
          @current-change="loadWords"
        />
      </div>
    </el-card>

    <!-- 添加敏感词对话框 -->
    <el-dialog v-model="addDialogVisible" title="添加敏感词" width="450px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="80px">
        <el-form-item label="敏感词" prop="word">
          <el-input v-model="addForm.word" placeholder="请输入敏感词" />
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select v-model="addForm.level" placeholder="请选择级别">
            <el-option label="替换" value="replace" />
            <el-option label="审核" value="review" />
            <el-option label="禁止" value="block" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="confirmAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量添加对话框 -->
    <el-dialog v-model="batchDialogVisible" title="批量添加敏感词" width="500px">
      <el-form :model="batchForm" label-width="80px">
        <el-form-item label="敏感词">
          <el-input
            v-model="batchForm.words"
            type="textarea"
            :rows="6"
            placeholder="每行一个敏感词"
          />
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="batchForm.level" placeholder="请选择级别">
            <el-option label="替换" value="replace" />
            <el-option label="审核" value="review" />
            <el-option label="禁止" value="block" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchLoading" @click="confirmBatchAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- 修改级别对话框 -->
    <el-dialog v-model="levelDialogVisible" title="修改级别" width="400px">
      <el-form label-width="80px">
        <el-form-item label="敏感词">
          <span>{{ currentWord?.word }}</span>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="editLevel">
            <el-option label="替换" value="replace" />
            <el-option label="审核" value="review" />
            <el-option label="禁止" value="block" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="levelDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="levelLoading" @click="confirmEditLevel">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { SensitiveWord, SensitiveWordLevel } from '@/types'
import {
  getSensitiveWordList,
  createSensitiveWord,
  batchCreateSensitiveWords,
  deleteSensitiveWord,
  updateSensitiveWordLevel,
} from '@/api/sensitiveWord'

const loading = ref(false)
const wordList = ref<SensitiveWord[]>([])
const total = ref(0)

/** 级别名称映射 */
const levelNameMap: Record<string, string> = {
  replace: '替换',
  review: '审核',
  block: '禁止',
}

/** 级别标签类型映射 */
const levelTypeMap: Record<string, string> = {
  replace: 'info',
  review: 'warning',
  block: 'danger',
}

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  level: undefined as SensitiveWordLevel | undefined,
})

/** 添加相关 */
const addDialogVisible = ref(false)
const addLoading = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({ word: '', level: 'review' as SensitiveWordLevel })
const addRules: FormRules = {
  word: [{ required: true, message: '请输入敏感词', trigger: 'blur' }],
  level: [{ required: true, message: '请选择级别', trigger: 'change' }],
}

/** 批量添加相关 */
const batchDialogVisible = ref(false)
const batchLoading = ref(false)
const batchForm = reactive({ words: '', level: 'review' as SensitiveWordLevel })

/** 修改级别相关 */
const levelDialogVisible = ref(false)
const levelLoading = ref(false)
const currentWord = ref<SensitiveWord | null>(null)
const editLevel = ref<SensitiveWordLevel>('review')

function handleAdd() {
  addForm.word = ''
  addForm.level = 'review'
  addDialogVisible.value = true
}

function handleBatchAdd() {
  batchForm.words = ''
  batchForm.level = 'review'
  batchDialogVisible.value = true
}

/** 确认添加 */
async function confirmAdd() {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return

  addLoading.value = true
  try {
    await createSensitiveWord(addForm)
    ElMessage.success('添加成功')
    addDialogVisible.value = false
    loadWords()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    addLoading.value = false
  }
}

/** 确认批量添加 */
async function confirmBatchAdd() {
  if (!batchForm.words.trim()) {
    ElMessage.warning('请输入敏感词')
    return
  }
  const words = batchForm.words.split('\n').map((w) => w.trim()).filter(Boolean)
  if (!words.length) {
    ElMessage.warning('请输入有效的敏感词')
    return
  }

  batchLoading.value = true
  try {
    await batchCreateSensitiveWords({ words, level: batchForm.level })
    ElMessage.success('批量添加成功')
    batchDialogVisible.value = false
    loadWords()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    batchLoading.value = false
  }
}

/** 修改级别 */
function handleEditLevel(word: SensitiveWord) {
  currentWord.value = word
  editLevel.value = word.level
  levelDialogVisible.value = true
}

/** 确认修改级别 */
async function confirmEditLevel() {
  if (!currentWord.value) return
  levelLoading.value = true
  try {
    await updateSensitiveWordLevel(currentWord.value.id, editLevel.value)
    ElMessage.success('修改成功')
    levelDialogVisible.value = false
    loadWords()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    levelLoading.value = false
  }
}

/** 删除敏感词 */
async function handleDelete(word: SensitiveWord) {
  try {
    await ElMessageBox.confirm('确定要删除该敏感词吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteSensitiveWord(word.id)
    ElMessage.success('删除成功')
    loadWords()
  } catch {
    // 用户取消或请求失败
  }
}

/** 加载敏感词列表 */
async function loadWords() {
  loading.value = true
  try {
    const res = await getSensitiveWordList(queryParams)
    wordList.value = res.data.list
    total.value = res.data.total
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadWords()
})
</script>

<style scoped lang="scss">
.sensitive-words {
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
