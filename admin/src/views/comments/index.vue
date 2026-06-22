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
            <el-option label="正常" value="active" />
            <el-option label="待审核" value="pending" />
            <el-option label="已删除" value="deleted" />
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
      <el-table v-loading="loading" :data="commentList" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="content" label="评论内容" min-width="250" show-overflow-tooltip />
        <el-table-column prop="articleTitle" label="所属文章" min-width="180" show-overflow-tooltip />
        <el-table-column prop="username" label="评论者" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'active'" type="success">正常</el-tag>
            <el-tag v-else-if="row.status === 'pending'" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.status === 'deleted'" type="danger">已删除</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="likeCount" label="点赞" width="80" />
        <el-table-column prop="createdAt" label="评论时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending'"
              type="success"
              link
              size="small"
              @click="handleApprove(row)"
            >
              通过
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
          @size-change="loadComments"
          @current-change="loadComments"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Comment, CommentQuery } from '@/types'
import { getCommentList, deleteComment, approveComment } from '@/api/comment'

const loading = ref(false)
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
    loadComments()
  } catch {
    // 用户取消或请求失败
  }
}

/** 加载评论列表 */
async function loadComments() {
  loading.value = true
  try {
    const res = await getCommentList(queryParams)
    commentList.value = res.data.list
    total.value = res.data.total
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
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
}
</style>
