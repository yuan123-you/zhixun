<template>
  <!-- 评论区 - 抖音风格 + 现代编辑感 -->
  <div class="comment-section">
    <!-- 评论列表区 -->
    <div class="comment-list-wrapper">
      <!-- 评论头部 -->
      <header class="comment-header">
        <div class="comment-header-left">
          <h3 class="comment-title">评论</h3>
          <span class="comment-count">
            共 <em>{{ total }}</em> 条
          </span>
        </div>
        <div class="sort-toggle" role="tablist">
          <button
            v-for="opt in sortOptions"
            :key="opt.value"
            class="sort-btn"
            :class="{ active: sortBy === opt.value }"
            type="button"
            role="tab"
            :aria-selected="sortBy === opt.value"
            @click="setSort(opt.value)"
          >
            <span>{{ opt.label }}</span>
          </button>
        </div>
      </header>

      <!-- 评论加载中 -->
      <div v-if="loading" class="comment-status">
        <div class="comment-status-spinner"></div>
        <span>加载中…</span>
      </div>

      <!-- 评论列表 -->
      <div v-else-if="comments.length > 0" class="comment-list">
        <CommentItem
          v-for="(comment, idx) in comments"
          :key="comment.id"
          :comment="comment"
          class="stagger-item"
          :style="{ animationDelay: `${Math.min(idx * 50, 500)}ms` }"
          @reply="handleReply"
          @like="handleLike"
          @delete="handleDelete"
          @report="handleReport"
        />
      </div>

      <!-- 空状态 -->
      <div v-else class="comment-empty">
        <div class="comment-empty-illustration" aria-hidden="true">
          <svg viewBox="0 0 96 96" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 28c0-4.4 3.6-8 8-8h40c4.4 0 8 3.6 8 8v32c0 4.4-3.6 8-8 8H40l-12 12V68h-0c-4.4 0-8-3.6-8-8V28z" />
            <circle cx="36" cy="44" r="2" fill="currentColor" />
            <circle cx="48" cy="44" r="2" fill="currentColor" />
            <circle cx="60" cy="44" r="2" fill="currentColor" />
          </svg>
        </div>
        <p class="comment-empty-title">还没有人评论</p>
        <p class="comment-empty-subtitle">做第一个分享想法的人吧</p>
      </div>

      <!-- 无限滚动哨兵 -->
      <div ref="sentinelRef" class="h-1"></div>
      <div v-if="loadingMore" class="comment-status">
        <div class="comment-status-spinner"></div>
        <span>加载更多…</span>
      </div>
      <div v-else-if="!hasMore && comments.length > 0" class="comment-end">
        <span class="comment-end-line"></span>
        <span class="comment-end-text">已经到底啦</span>
        <span class="comment-end-line"></span>
      </div>
    </div>

    <!-- 抖音风格评论输入框 -->
    <DouyinCommentInput
      v-if="userStore.isLoggedIn"
      :reply-to="replyTo"
      :placeholder="replyTo ? `回复 @${replyTo.user?.nickname || '用户'}` : '说点好听的~'"
      @submit="handleSubmit"
      @clearReply="replyTo = null"
    />

    <!-- 未登录提示 -->
    <div v-else class="comment-login-prompt">
      <div class="comment-login-prompt-inner">
        <p>登录后即可参与评论</p>
        <RouterLink to="/login" class="comment-login-btn">立即登录</RouterLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * 评论区主组件
 * - 集成抖音风格输入框（基础态 + 展开态）
 * - 现代编辑感列表设计
 * - 无限滚动加载
 */
import type { Comment } from '@/types'

const props = defineProps<{
  articleId: number
  comments: Comment[]
  hasMore: boolean
  total: number
  loading?: boolean
}>()
const emit = defineEmits<{
  submit: [data: { content: string; parentId?: number; replyUserId?: number; images?: string[] }]
  loadMore: []
  like: [commentId: number]
  delete: [commentId: number]
  report: [data: { commentId: number; reason?: string }]
  sortChange: [sort: string]
}>()

const userStore = useUserStore()

// 状态
const replyTo = ref<Comment | null>(null)
const sortBy = ref('latest')
const sentinelRef = ref<HTMLElement | null>(null)
const loadingMore = ref(false)
const localLoading = ref(false)

const loading = computed(() => props.loading || localLoading.value)

const sortOptions = [
  { value: 'latest', label: '最新' },
  { value: 'hot', label: '最热' },
]

// 事件处理
const handleReply = (comment: Comment) => {
  replyTo.value = comment
  // 平滑滚动到输入框位置
  nextTick(() => {
    const el = document.querySelector('.dy-input-bar')
    el?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  })
}

const handleSubmit = (data: { content: string; images: string[]; parentId?: number; replyUserId?: number }) => {
  if (!data.content.trim() && data.images.length === 0) return
  emit('submit', data)
  replyTo.value = null
}

const handleLike = (commentId: number) => emit('like', commentId)
const handleDelete = (commentId: number) => emit('delete', commentId)
const handleReport = (data: { commentId: number; reason?: string }) => emit('report', data)
const setSort = (sort: string) => {
  if (sortBy.value === sort) return
  sortBy.value = sort
  localLoading.value = true
  emit('sortChange', sort)
  // 父组件会更新 comments，由 watch 关闭 loading
  nextTick(() => {
    setTimeout(() => { localLoading.value = false }, 300)
  })
}

// 无限滚动
let observer: IntersectionObserver | null = null
onMounted(() => {
  nextTick(() => {
    if (sentinelRef.value) {
      observer = new IntersectionObserver(
        (entries) => {
          if (entries[0]?.isIntersecting && props.hasMore && !loadingMore.value) {
            loadingMore.value = true
            emit('loadMore')
            nextTick(() => { loadingMore.value = false })
          }
        },
        { rootMargin: '200px' },
      )
      observer.observe(sentinelRef.value)
    }
  })
})
onUnmounted(() => { observer?.disconnect() })
</script>

<style scoped>
.comment-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-list-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ========== 头部 ========== */
.comment-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--zh-border-light);
  position: relative;
}
.comment-header::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 32px;
  height: 2px;
  background: var(--zh-primary);
  border-radius: 2px;
}
.comment-header-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.comment-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--zh-text);
  letter-spacing: -0.01em;
  margin: 0;
}
.comment-count {
  font-size: 13px;
  color: var(--zh-text-tertiary);
  font-weight: 500;
}
.comment-count em {
  font-style: normal;
  color: var(--zh-text-secondary);
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  margin: 0 2px;
}

.sort-toggle {
  display: inline-flex;
  background: var(--zh-bg-hover);
  border-radius: 999px;
  padding: 3px;
  gap: 2px;
}
.sort-btn {
  position: relative;
  padding: 5px 14px;
  border: none;
  background: transparent;
  color: var(--zh-text-tertiary);
  font-size: 12px;
  font-weight: 500;
  border-radius: 999px;
  cursor: pointer;
  transition: color 200ms ease, background 200ms ease, transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}
.sort-btn:hover {
  color: var(--zh-text-secondary);
}
.sort-btn.active {
  background: var(--zh-bg-elevated);
  color: var(--zh-primary);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  font-weight: 600;
}

/* ========== 状态 ========== */
.comment-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 32px 0;
  color: var(--zh-text-tertiary);
  font-size: 13px;
}
.comment-status-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--zh-border);
  border-top-color: var(--zh-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.comment-end {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px 0 8px;
  color: var(--zh-text-tertiary);
  font-size: 12px;
}
.comment-end-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    var(--zh-border) 50%,
    transparent 100%
  );
}
.comment-end-text {
  letter-spacing: 0.05em;
}

/* ========== 列表 ========== */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* ========== 空状态 ========== */
.comment-empty {
  text-align: center;
  padding: 56px 16px 32px;
}
.comment-empty-illustration {
  width: 96px;
  height: 96px;
  margin: 0 auto 20px;
  color: var(--zh-primary);
  opacity: 0.6;
  animation: float 3s ease-in-out infinite;
}
.comment-empty-illustration svg {
  width: 100%;
  height: 100%;
}
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}
.comment-empty-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--zh-text-secondary);
  margin: 0 0 4px;
}
.comment-empty-subtitle {
  font-size: 13px;
  color: var(--zh-text-tertiary);
  margin: 0;
}

/* ========== 登录提示 ========== */
.comment-login-prompt {
  margin: 12px 0;
  padding: 0;
}
.comment-login-prompt-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  background: linear-gradient(
    135deg,
    rgba(var(--zh-primary-rgb), 0.05) 0%,
    rgba(var(--zh-primary-rgb), 0.02) 100%
  );
  border: 1px solid rgba(var(--zh-primary-rgb), 0.1);
  border-radius: var(--zh-radius-lg);
  backdrop-filter: blur(8px);
}
.comment-login-prompt p {
  margin: 0;
  font-size: 14px;
  color: var(--zh-text-secondary);
  font-weight: 500;
}
.comment-login-btn {
  display: inline-flex;
  align-items: center;
  padding: 8px 18px;
  background: var(--zh-primary-gradient);
  color: #fff;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  box-shadow: 0 4px 12px rgba(var(--zh-primary-rgb), 0.25);
  transition: transform 150ms ease, box-shadow 200ms ease;
}
.comment-login-btn:hover {
  color: #fff;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(var(--zh-primary-rgb), 0.32);
}
.comment-login-btn:active {
  transform: scale(0.97);
}
</style>