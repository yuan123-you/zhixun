<template>
  <!-- 单条评论项组件（递归支持多级嵌套） -->
  <div class="comment-item-wrapper">
    <div class="comment-item">
      <RouterLink v-if="comment.userId" :to="`/user/${comment.userId}`" class="comment-avatar-link">
        <UserAvatar :src="comment.user?.avatar" :alt="comment.user?.nickname" size="sm" />
      </RouterLink>
      <UserAvatar v-else :src="comment.user?.avatar" :alt="comment.user?.nickname" size="sm" />

      <div class="comment-body">
        <div class="comment-meta">
          <RouterLink v-if="comment.userId" :to="`/user/${comment.userId}`" class="comment-author">
            {{ comment.user?.nickname }}
          </RouterLink>
          <span v-else class="comment-author">{{ comment.user?.nickname }}</span>
          <el-tag v-if="comment.status === 0" size="small" type="warning" class="comment-pending-tag">待审核</el-tag>
          <template v-if="comment.replyUser">
            <span class="comment-reply-label">回复</span>
            <span class="comment-reply-target">@{{ comment.replyUser.nickname }}</span>
          </template>
          <span class="comment-time">· {{ formatTime(comment.createdAt) }}</span>
        </div>

        <p v-if="comment.content" class="comment-content">{{ comment.content }}</p>

        <!-- 评论图片 -->
        <div v-if="comment.images?.length" class="comment-images">
          <div
            v-for="(img, idx) in comment.images.slice(0, 9)"
            :key="idx"
            class="comment-image-item"
            @click="openImageZoom(idx)"
          >
            <img :src="resolveUrl(img) || img" :alt="`图片${idx + 1}`" loading="lazy" />
          </div>
        </div>

        <div class="comment-actions">
          <button
            class="comment-action"
            :class="{ liked: comment.isLiked }"
            type="button"
            @click="$emit('like', comment.id)"
          >
            <svg class="action-icon" :class="{ fill: comment.isLiked }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
            </svg>
            <span>{{ comment.likeCount || '点赞' }}</span>
          </button>
          <button class="comment-action" type="button" @click="$emit('reply', comment)">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="9 17 4 12 9 7"/>
              <path d="M20 18v-2a4 4 0 0 0-4-4H4"/>
            </svg>
            <span>回复</span>
          </button>
          <button v-if="!isOwner && !isAdmin && userStore.isLoggedIn" class="comment-action" type="button" @click="showReportModal = true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 15s1-1 4-1 5 2 8 2 4-1 4-1V3s-1 1-4 1-5-2-8-2-4 1-4 1z"/>
              <line x1="4" y1="22" x2="4" y2="15"/>
            </svg>
            <span>举报</span>
          </button>
          <button v-if="canDelete" class="comment-action comment-action--danger" type="button" @click="handleDelete">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="3 6 5 6 21 6"/>
              <path d="M19 6l-2 14a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L5 6"/>
              <path d="M10 11v6M14 11v6"/>
            </svg>
            <span>删除</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 多级子回复 -->
    <div v-if="hasReplies" class="comment-replies">
      <button v-if="collapsible" class="toggle-replies" type="button" @click="expanded = !expanded">
        <span class="toggle-replies-line" :class="{ rotated: expanded }"></span>
        <span>{{ expanded ? '收起回复' : `展开 ${totalReplyCount} 条回复` }}</span>
      </button>

      <div v-show="expanded" class="reply-list">
        <CommentItem
          v-for="reply in comment.replies"
          :key="reply.id"
          :comment="reply"
          @reply="$emit('reply', $event)"
          @like="$emit('like', $event)"
          @delete="$emit('delete', $event)"
          @report="$emit('report', $event)"
        />
      </div>
    </div>

    <!-- 评论图片缩放查看 -->
    <ImageZoom
      v-model:visible="imageZoomVisible"
      :src="imageZoomSrc"
      :alt="imageZoomAlt"
    />
  </div>
</template>

<script setup lang="ts">
/** 单条评论项组件（支持多级嵌套递归） */
import type { Comment } from '@/types'

const props = defineProps<{
  comment: Comment
  collapsible?: boolean
}>()

const emit = defineEmits<{
  reply: [comment: Comment]
  like: [commentId: number]
  delete: [commentId: number]
  report: [data: { commentId: number; reason?: string }]
}>()

const userStore = useUserStore()

const hasReplies = computed(() => (props.comment.replies?.length ?? 0) > 0)

const totalReplyCount = computed(() => {
  const countReplies = (list: Comment[]): number => {
    let count = 0
    for (const c of list) { count += 1; if (c.replies?.length) count += countReplies(c.replies) }
    return count
  }
  return countReplies(props.comment.replies ?? [])
})

const expanded = ref(true)
const isOwner = computed(() => userStore.isLoggedIn && userStore.userInfo?.id == props.comment.userId)
const isAdmin = computed(() => {
  if (!userStore.isLoggedIn) return false
  const role = userStore.userInfo?.role
  return role === 'ADMIN' || role === 'SUPER_ADMIN'
})
const canDelete = computed(() => isOwner.value || isAdmin.value)

const showReportModal = ref(false)
const reportReason = ref('')
const reportSubmitting = ref(false)

// 评论图片缩放
const imageZoomVisible = ref(false)
const imageZoomSrc = ref('')
const imageZoomAlt = ref('')

const { resolveUrl } = useResourceUrl()

const openImageZoom = (idx: number) => {
  const img = props.comment.images?.[idx]
  if (!img) return
  imageZoomSrc.value = resolveUrl(img) || img
  imageZoomAlt.value = `图片${idx + 1}`
  imageZoomVisible.value = true
}

const handleDelete = () => emit('delete', props.comment.id)

const handleReport = async () => {
  reportSubmitting.value = true
  try {
    emit('report', { commentId: props.comment.id, reason: reportReason.value.trim() || undefined })
    showReportModal.value = false
    reportReason.value = ''
  } finally { reportSubmitting.value = false }
}

const formatTime = (time: string) => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (hours < 24) return `${hours}小时前`
  if (days < 30) return `${days}天前`
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}
</script>

<style scoped>
.comment-item-wrapper { display: flex; flex-direction: column; }

.comment-item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}
.comment-avatar-link {
  flex-shrink: 0;
  cursor: pointer;
  transition: transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}
.comment-avatar-link:hover { transform: scale(1.05); }

.comment-body {
  flex: 1;
  min-width: 0;
  display: block;
  width: 100%;
}

.comment-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 13px;
  color: var(--zh-text-tertiary);
  line-height: 1.4;
  width: 100%;
}
.comment-author {
  font-size: 13px;
  font-weight: 600;
  color: var(--zh-text-secondary);
  text-decoration: none;
  letter-spacing: 0.01em;
  transition: color 150ms ease;
}
.comment-author:hover { color: var(--zh-primary); }
.comment-pending-tag {
  margin-left: 4px;
  height: 18px !important;
  padding: 0 6px !important;
  font-size: 10px !important;
  line-height: 16px !important;
}
.comment-reply-label { font-size: 12px; color: var(--zh-text-tertiary); margin-left: 4px; }
.comment-reply-target {
  font-size: 12px;
  font-weight: 500;
  color: var(--zh-primary);
}
.comment-time {
  font-size: 12px;
  color: var(--zh-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.comment-content {
  display: block;
  font-size: 14.5px;
  color: var(--zh-text);
  margin: 4px 0 0;
  line-height: 1.65;
  word-break: break-word;
  white-space: pre-wrap;
}

/* 评论图片 */
.comment-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
  gap: 6px;
  margin-top: 8px;
  max-width: 360px;
}
.comment-image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 6px;
  overflow: hidden;
  background: var(--zh-bg-hover);
  cursor: zoom-in;
  transition: transform 200ms cubic-bezier(0.16, 1, 0.3, 1);
}
.comment-image-item:hover { transform: scale(1.03); }
.comment-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.comment-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  flex-wrap: wrap;
  width: 100%;
}
.comment-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: none;
  background: transparent;
  color: var(--zh-text-tertiary);
  font-size: 12px;
  font-weight: 500;
  border-radius: 999px;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease;
}
.comment-action svg {
  width: 13px;
  height: 13px;
}
.comment-action:hover {
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
}
.comment-action.liked {
  color: var(--zh-primary);
}
.comment-action.liked .action-icon {
  fill: var(--zh-primary);
  stroke: var(--zh-primary);
}
.comment-action--danger:hover {
  color: var(--zh-danger);
  background: var(--zh-danger-bg);
}

/* 多级回复 */
.comment-replies {
  margin-left: 20px;
  padding-left: 12px;
  border-left: 2px solid var(--zh-border-light);
  margin-top: 4px;
  position: relative;
}
.comment-replies::before {
  content: '';
  position: absolute;
  left: -2px;
  top: 0;
  width: 2px;
  height: 24px;
  background: linear-gradient(180deg, var(--zh-primary) 0%, transparent 100%);
  border-radius: 2px;
  opacity: 0.4;
}

.toggle-replies {
  margin: 8px 0 4px -12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px 4px 6px;
  border: none;
  background: var(--zh-bg-hover);
  color: var(--zh-text-secondary);
  font-size: 12px;
  font-weight: 500;
  border-radius: 999px;
  cursor: pointer;
  transition: background 150ms ease, color 150ms ease;
}
.toggle-replies:hover {
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
}
.toggle-replies-line {
  display: inline-block;
  width: 0;
  height: 0;
  border-left: 4px solid currentColor;
  border-top: 4px solid transparent;
  border-bottom: 4px solid transparent;
  transition: transform 200ms ease;
}
.toggle-replies-line.rotated {
  transform: rotate(90deg);
}

.reply-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 4px;
  animation: reply-in 240ms cubic-bezier(0.16, 1, 0.3, 1);
}
@keyframes reply-in {
  from { opacity: 0; transform: translateY(-4px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>