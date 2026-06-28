<template>
  <div class="pending-articles">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索作品标题"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 待审核列表 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="articleList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="createdAt" label="提交时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handlePreview(row)">
              预览
            </el-button>
            <el-button type="success" link size="small" @click="handleApprove(row)">
              通过
            </el-button>
            <el-button type="danger" link size="small" @click="handleReject(row)">
              驳回
            </el-button>
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
          @size-change="loadPendingArticles"
          @current-change="loadPendingArticles"
        />
      </div>
    </el-card>

    <!-- 作品预览对话框 -->
    <el-dialog v-model="previewVisible" title="作品预览" width="700px" top="5vh">
      <div v-if="previewArticle" class="article-preview">
        <h2>{{ previewArticle.title }}</h2>
        <div class="meta">
          <span>作者: {{ previewArticle.authorName }}</span>
          <span>分类: {{ previewArticle.categoryName }}</span>
          <span>提交时间: {{ previewArticle.createdAt }}</span>
        </div>
        <el-divider />
        <div class="content" v-html="previewArticle.content" />
      </div>
    </el-dialog>

    <!-- 驳回原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回作品" width="500px">
      <el-form :model="rejectForm" :rules="rejectRules" ref="rejectFormRef">
        <el-form-item label="驳回原因" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="rejectLoading" @click="confirmReject">
          确定驳回
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { Article, ArticleQuery } from '@/types'
import { getPendingArticles, auditArticle } from '@/api/article'

/** 加载状态 */
const loading = ref(false)

/** 作品列表 */
const articleList = ref<Article[]>([])

/** 总数 */
const total = ref(0)

/** 查询参数 */
const queryParams = reactive<ArticleQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
})

/** 预览相关 */
const previewVisible = ref(false)
const previewArticle = ref<Article | null>(null)

/** 驳回相关 */
const rejectDialogVisible = ref(false)
const rejectLoading = ref(false)
const rejectFormRef = ref<FormInstance>()
const rejectForm = reactive({ reason: '' })
const currentRejectId = ref<number>(0)

const rejectRules: FormRules = {
  reason: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }],
}

/** 搜索 */
function handleSearch() {
  queryParams.page = 1
  loadPendingArticles()
}

/** 重置 */
function handleReset() {
  queryParams.keyword = ''
  queryParams.page = 1
  loadPendingArticles()
}

/** 预览作品 */
function handlePreview(article: Article) {
  previewArticle.value = article
  previewVisible.value = true
}

/** 通过审核 */
async function handleApprove(article: Article) {
  try {
    await auditArticle({ id: article.id, status: 2 })
    ElMessage.success('审核通过')
    loadPendingArticles()
  } catch {
    // 错误已在拦截器中处理
  }
}

/** 驳回作品 */
function handleReject(article: Article) {
  currentRejectId.value = article.id
  rejectForm.reason = ''
  rejectDialogVisible.value = true
}

/** 确认驳回 */
async function confirmReject() {
  const valid = await rejectFormRef.value?.validate().catch(() => false)
  if (!valid) return

  rejectLoading.value = true
  try {
    await auditArticle({
      id: currentRejectId.value,
      status: 3,
      reason: rejectForm.reason,
    })
    ElMessage.success('已驳回')
    rejectDialogVisible.value = false
    loadPendingArticles()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    rejectLoading.value = false
  }
}

/** 加载待审核作品 */
async function loadPendingArticles() {
  loading.value = true
  try {
    const res = await getPendingArticles(queryParams)
    articleList.value = res.data.list
    total.value = res.data.total
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPendingArticles()
})
</script>

<style scoped lang="scss">
.pending-articles {
  .search-card {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .article-preview {
    h2 {
      margin: 0 0 12px;
    }
    .meta {
      display: flex;
      gap: 16px;
      color: #999;
      font-size: 14px;
    }
    .content {
      line-height: 1.8;
    }
  }
}
</style>
