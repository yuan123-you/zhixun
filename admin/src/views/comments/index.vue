<template>
  <div class="comment-management">
    <!-- 搜索筛选 -->
    <el-card shadow="never" class="search-card">
      <el-form :model="queryParams" inline>
        <el-form-item label="关键词">
          <el-input v-model="queryParams.keyword" placeholder="搜索评论内容" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
            <el-option label="正常" :value="1" />
            <el-option label="待审核" :value="0" />
            <el-option label="已删除" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 评论列表 -->
    <el-card shadow="never">
      <!-- 骨架屏：首次加载时展示 -->
      <template v-if="isFirstLoad">
        <el-skeleton :rows="6" animated />
      </template>

      <template v-else>
        <el-table v-loading="loading" :data="commentList" stripe row-key="id">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="content" label="评论内容" min-width="250" show-overflow-tooltip />
          <el-table-column prop="articleTitle" label="所属作品" min-width="180" show-overflow-tooltip />
          <el-table-column label="评论者" width="120">
            <template #default="{ row }">
              {{ row.user?.nickname || row.user?.username || '-' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 1" type="success">正常</el-tag>
              <el-tag v-else-if="row.status === 0" type="warning">待审核</el-tag>
              <el-tag v-else-if="row.status === 2" type="danger">已删除</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="likeCount" label="点赞" width="80" />
          <el-table-column prop="createdAt" label="评论时间" width="170" />
          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 0"
                type="success"
                link
                size="small"
                @click="handleApprove(row as Comment)"
              >
                通过
              </el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row as Comment)">删除</el-button>
            </template>
          </el-table-column>

          <!-- 错误重试空状态 -->
          <template #empty>
            <div class="table-empty">
              <template v-if="hasError">
                <el-icon :size="40" color="#909399"><CircleCloseFilled /></el-icon>
                <p>数据加载失败</p>
                <el-button type="primary" size="small" @click="loadComments()">重新加载</el-button>
              </template>
              <template v-else>
                <el-empty description="暂无评论数据" />
              </template>
            </div>
          </template>
        </el-table>
      </template>

      <!-- 分页 -->
      <div v-if="!isFirstLoad" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="() => loadComments()"
          @current-change="() => loadComments()"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCloseFilled } from '@element-plus/icons-vue'
import type { Comment, CommentQuery, PageResult } from '@/types'
import { deleteComment, approveComment } from '@/api/comment'
import { useRequestCache } from '@/composables/useRequestCache'

/** 请求缓存实例 */
const commentCache = useRequestCache<PageResult<Comment>>({
  ttl: 2 * 60 * 1000,
  staleWhileRevalidate: true,
  retryCount: 2,
  retryInterval: 800,
})

const loading = ref(false)
const isFirstLoad = ref(true)
const hasError = ref(false)
const commentList = ref<Comment[]>([])
const total = ref(0)

const queryParams = reactive<CommentQuery>({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: undefined,
})

function handleSearch() {
  queryParams.page = 1
  loadComments()
}

function handleReset() {
  queryParams.keyword = ''
  queryParams.status = undefined
  queryParams.page = 1
  loadComments()
}

/** 通过审核 */
async function handleApprove(comment: Comment) {
  try {
    await approveComment(comment.id)
    ElMessage.success('审核通过')
    commentCache.invalidate('/admin/comments', queryParams as unknown as Record<string, unknown>)
    loadComments()
  } catch {
    // 错误已在拦截器中处理
  }
}

/** 删除评论 */
async function handleDelete(comment: Comment) {
  try {
    await ElMessageBox.confirm('确定要删除该评论吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteComment(comment.id)
    ElMessage.success('删除成功')
    commentCache.invalidate('/admin/comments', queryParams as unknown as Record<string, unknown>)
    loadComments()
  } catch {
    // 用户取消或请求失败
  }
}

/** 删除评论 */
async function loadComments(force = false) {
  loading.value = true
  hasError.value = false
  try {
    // 注：管理端评论列表走 /admin/comments（公开接口 /comments 不存在，否则 500）
  const result = await commentCache.request('/admin/comments', queryParams as unknown as Record<string, unknown>, { force })
  commentList.value = (result as any).list || (result as any).data || []
  total.value = (result as any).total || 0
  } catch {
    hasError.value = true
    ElMessage.error('评论列表加载失败，请检查网络后重试')
  } finally {
    loading.value = false
    isFirstLoad.value = false
  }
}

onMounted(() => {
  loadComments()
})
</script>

<style scoped lang="scss">
.comment-management {
  .search-card {
    margin-bottom: 16px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 16px;
  }

  .table-empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 32px 0;
    gap: 8px;

    p {
      color: #909399;
      font-size: 14px;
    }
  }
}
</style>
