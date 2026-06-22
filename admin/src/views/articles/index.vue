<template>
  <div class="article-management">
    <!-- 搜索筛选栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索文章标题/内容"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
            <el-option label="草稿" value="draft" />
            <el-option label="待审核" value="pending" />
            <el-option label="已发布" value="published" />
            <el-option label="已下架" value="offline" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.categoryId" placeholder="全部分类" clearable>
            <el-option
              v-for="cat in categoryList"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 文章列表 -->
    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="articleList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="authorName" label="作者" width="120" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="标签" width="180">
          <template #default="{ row }">
            <el-tag
              v-for="tag in row.tags"
              :key="tag.id"
              size="small"
              class="mr-1"
            >
              {{ tag.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleView(row)">查看</el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="success"
              link
              size="small"
              @click="handleAudit(row)"
            >
              审核
            </el-button>
            <el-button
              v-if="row.status === 'published'"
              type="warning"
              link
              size="small"
              @click="handleOffline(row)"
            >
              下架
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
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadArticles"
          @current-change="loadArticles"
        />
      </div>
    </el-card>

    <!-- 审核对话框 -->
    <AuditDialog
      v-model="auditDialogVisible"
      :article-id="currentArticle?.id"
      @success="loadArticles"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Article, ArticleQuery, ArticleStatus, Category } from '@/types'
import { getArticleList, deleteArticle, offlineArticle } from '@/api/article'
import { getCategoryTree } from '@/api/category'
import StatusTag from '@/components/StatusTag.vue'
import AuditDialog from '@/components/AuditDialog.vue'

/** 加载状态 */
const loading = ref(false)

/** 文章列表 */
const articleList = ref<Article[]>([])

/** 分类列表 */
const categoryList = ref<Category[]>([])

/** 总数 */
const total = ref(0)

/** 查询参数 */
const queryParams = reactive<ArticleQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
  categoryId: undefined,
})

/** 审核对话框 */
const auditDialogVisible = ref(false)
const currentArticle = ref<Article | null>(null)

/** 搜索 */
function handleSearch() {
  queryParams.page = 1
  loadArticles()
}

/** 重置搜索 */
function handleReset() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.categoryId = undefined
  queryParams.page = 1
  loadArticles()
}

/** 查看文章 */
function handleView(article: Article) {
  // 可跳转文章详情页或打开预览对话框
  ElMessage.info(`查看文章: ${article.title}`)
}

/** 审核文章 */
function handleAudit(article: Article) {
  currentArticle.value = article
  auditDialogVisible.value = true
}

/** 下架文章 */
async function handleOffline(article: Article) {
  try {
    await ElMessageBox.confirm('确定要下架该文章吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await offlineArticle(article.id)
    ElMessage.success('下架成功')
    loadArticles()
  } catch {
    // 用户取消或请求失败
  }
}

/** 删除文章 */
async function handleDelete(article: Article) {
  try {
    await ElMessageBox.confirm('确定要删除该文章吗？此操作不可恢复', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteArticle(article.id)
    ElMessage.success('删除成功')
    loadArticles()
  } catch {
    // 用户取消或请求失败
  }
}

/** 加载文章列表 */
async function loadArticles() {
  loading.value = true
  try {
    const res = await getArticleList(queryParams)
    articleList.value = res.data.list
    total.value = res.data.total
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

/** 加载分类列表 */
async function loadCategories() {
  try {
    const res = await getCategoryTree()
    categoryList.value = res.data
  } catch {
    // 错误已在拦截器中处理
  }
}

onMounted(() => {
  loadArticles()
  loadCategories()
})
</script>

<style scoped lang="scss">
.article-management {
  .search-card {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .mr-1 {
    margin-right: 4px;
  }
}
</style>
