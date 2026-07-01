<template>
  <div class="article-detail">
    <!-- 顶部操作栏 -->
    <div class="detail-header">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
      <div class="header-actions">
        <el-button
          v-if="article?.status === 1"
          type="success"
          @click="handleAudit"
        >
          审核
        </el-button>
        <el-button
          v-if="article?.status === 2"
          type="warning"
          @click="handleOffline"
        >
          下架
        </el-button>
        <el-button type="danger" @click="handleDelete">删除</el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <template v-if="loading">
      <el-skeleton :rows="12" animated />
    </template>

    <!-- 文章详情内容 -->
    <template v-else-if="article">
      <el-card shadow="never" class="main-card">
        <!-- 标题与状态 -->
        <div class="title-row">
          <h1 class="article-title">{{ article.title }}</h1>
          <StatusTag :status="article.status" />
        </div>

        <!-- 元信息 -->
        <div class="meta-row">
          <div class="meta-item">
            <el-icon><User /></el-icon>
            <span>{{ article.author?.nickname || article.authorName || '-' }}</span>
          </div>
          <div class="meta-item">
            <el-icon><Calendar /></el-icon>
            <span>{{ formatTime(article.createdAt) }}</span>
          </div>
          <div class="meta-item" v-if="article.categoryName">
            <el-icon><Folder /></el-icon>
            <span>{{ article.categoryName }}</span>
          </div>
          <div class="meta-item" v-if="article.location">
            <el-icon><Location /></el-icon>
            <span>{{ article.location }}</span>
          </div>
          <div class="meta-item" v-if="article.ipAddress">
            <el-icon><Monitor /></el-icon>
            <span>IP属地: {{ article.ipAddress }}</span>
          </div>
        </div>

        <!-- 统计数据 -->
        <div class="stats-row">
          <div class="stat-item">
            <span class="stat-value">{{ article.viewCount ?? 0 }}</span>
            <span class="stat-label">浏览</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ article.likeCount ?? 0 }}</span>
            <span class="stat-label">点赞</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ article.commentCount ?? 0 }}</span>
            <span class="stat-label">评论</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ article.collectCount ?? 0 }}</span>
            <span class="stat-label">收藏</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ article.shareCount ?? 0 }}</span>
            <span class="stat-label">分享</span>
          </div>
        </div>

        <!-- 标签 -->
        <div class="tags-row" v-if="article.tags?.length">
          <el-tag
            v-for="tag in article.tags"
            :key="tag.id"
            size="small"
            class="tag-item"
          >
            {{ tag.name }}
          </el-tag>
        </div>

        <!-- 可见性 -->
        <div class="visibility-row" v-if="article.visibility !== undefined">
          <span class="info-label">可见性：</span>
          <span>{{ visibilityText(article.visibility) }}</span>
        </div>

        <!-- 封面图 -->
        <div class="cover-row" v-if="article.coverImage">
          <span class="info-label">封面图：</span>
          <el-image
            :src="resolveUrl(article.coverImage)"
            fit="cover"
            class="cover-image"
            :preview-src-list="[resolveUrl(article.coverImage)]"
          />
        </div>

        <!-- 作品图片 -->
        <div class="images-row" v-if="article.images?.length">
          <span class="info-label">作品图片：</span>
          <div class="image-list">
            <el-image
              v-for="(img, index) in article.images"
              :key="index"
              :src="resolveUrl(img)"
              fit="cover"
              class="article-image"
              :preview-src-list="article.images.map(resolveUrl)"
              :initial-index="index"
            />
          </div>
        </div>

        <!-- 拒绝原因 -->
        <div class="reject-row" v-if="article.rejectReason">
          <el-alert type="error" :closable="false">
            <template #title>拒绝原因：{{ article.rejectReason }}</template>
          </el-alert>
        </div>

        <!-- 正文内容 -->
        <el-divider />
        <div class="article-content" v-html="sanitizedContent" />
      </el-card>
    </template>

    <!-- 加载失败 -->
    <template v-else>
      <el-empty description="作品不存在或加载失败">
        <el-button type="primary" @click="loadArticle">重新加载</el-button>
      </el-empty>
    </template>

    <!-- 审核对话框 -->
    <AuditDialog
      v-model="auditDialogVisible"
      :article-id="articleId"
      @success="handleAuditSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, User, Calendar, Folder, Location, Monitor } from '@element-plus/icons-vue'
import { getArticleDetail, deleteArticle, offlineArticle } from '@/api/article'
import StatusTag from '@/components/StatusTag.vue'
import AuditDialog from '@/components/AuditDialog.vue'

const route = useRoute()
const router = useRouter()

/** 解析资源URL：将MinIO内部地址转换为后端代理路径 */
function resolveUrl(url: string | null | undefined): string {
  if (!url) return ''
  if (/^(https:|data:)/.test(url)) return url
  const minioMatch = url.match(/^https?:\/\/[^/]+\/([^/]+)\/(.+)$/)
  if (minioMatch && /^[a-zA-Z0-9._-]+$/.test(minioMatch[1])) {
    return `/api/v1/files/view/${minioMatch[1]}/${minioMatch[2]}`
  }
  if (url.startsWith('/')) return url
  return `/api/v1/${url}`
}

/** 简易HTML净化：移除script标签和事件属性 */
function sanitizeHtml(html: string): string {
  return html
    .replace(/<script[\s\S]*?<\/script>/gi, '')
    .replace(/\son\w+\s*=\s*("[^"]*"|'[^']*'|[^\s>]*)/gi, '')
}

const articleId = computed(() => Number(route.params.id))

const loading = ref(false)
const article = ref<any>(null)
const auditDialogVisible = ref(false)

/** 格式化时间 */
function formatTime(time: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

/** 可见性文本 */
function visibilityText(v: number) {
  const map: Record<number, string> = { 0: '公开', 1: '仅粉丝', 2: '仅互关', 3: '仅自己' }
  return map[v] ?? '未知'
}

/** 净化HTML内容 */
const sanitizedContent = computed(() => {
  if (!article.value?.content) return ''
  return sanitizeHtml(article.value.content)
})

/** 加载文章详情 */
async function loadArticle() {
  if (!articleId.value) return
  loading.value = true
  try {
    const res = await getArticleDetail(articleId.value)
    article.value = res
  } catch {
    article.value = null
    ElMessage.error('作品加载失败')
  } finally {
    loading.value = false
  }
}

/** 返回列表 */
function goBack() {
  router.push('/articles')
}

/** 审核 */
function handleAudit() {
  auditDialogVisible.value = true
}

/** 审核成功回调 */
function handleAuditSuccess() {
  loadArticle()
}

/** 下架 */
async function handleOffline() {
  try {
    await ElMessageBox.confirm('确定要下架该作品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await offlineArticle(articleId.value)
    ElMessage.success('下架成功')
    loadArticle()
  } catch {
    // 用户取消或请求失败
  }
}

/** 删除 */
async function handleDelete() {
  try {
    await ElMessageBox.confirm('确定要删除该作品吗？此操作不可恢复', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteArticle(articleId.value)
    ElMessage.success('删除成功')
    goBack()
  } catch {
    // 用户取消或请求失败
  }
}

/** 监听路由参数变化重新加载 */
watch(() => route.params.id, () => {
  if (route.params.id) loadArticle()
})

onMounted(() => {
  loadArticle()
})
</script>

<style scoped lang="scss">
.article-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .main-card {
    .title-row {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;

      .article-title {
        font-size: 22px;
        font-weight: 600;
        margin: 0;
      }
    }

    .meta-row {
      display: flex;
      flex-wrap: wrap;
      gap: 20px;
      margin-bottom: 16px;
      color: #909399;
      font-size: 14px;

      .meta-item {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }

    .stats-row {
      display: flex;
      gap: 32px;
      margin-bottom: 16px;

      .stat-item {
        display: flex;
        flex-direction: column;
        align-items: center;

        .stat-value {
          font-size: 18px;
          font-weight: 600;
          color: #303133;
        }

        .stat-label {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .tags-row {
      margin-bottom: 16px;

      .tag-item {
        margin-right: 6px;
      }
    }

    .visibility-row,
    .cover-row,
    .images-row {
      margin-bottom: 16px;
      font-size: 14px;

      .info-label {
        color: #909399;
        margin-right: 8px;
      }
    }

    .cover-image {
      width: 240px;
      height: 160px;
      border-radius: 6px;
    }

    .image-list {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .article-image {
        width: 120px;
        height: 120px;
        border-radius: 6px;
      }
    }

    .reject-row {
      margin-bottom: 16px;
    }

    .article-content {
      line-height: 1.8;
      font-size: 15px;
      word-break: break-word;

      :deep(img) {
        max-width: 100%;
        border-radius: 6px;
      }
    }
  }
}
</style>
