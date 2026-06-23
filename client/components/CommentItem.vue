<template>
  <!-- 单条评论项组件（供 CommentSection 内部使用） -->
  <div class="flex space-x-3">
    <!-- 用户头像 -->
    <img :src="comment.user?.avatar || '/default-avatar.png'" :alt="comment.user?.nickname" class="w-8 h-8 rounded-full object-cover shrink-0" />

    <div class="flex-1 min-w-0">
      <!-- 用户名和内容 -->
      <div>
        <span class="text-sm font-medium text-gray-900 dark:text-white">{{ comment.user?.nickname }}</span>
        <!-- 待审核标签 -->
        <span v-if="comment.status === 0" class="ml-2 inline-flex items-center px-1.5 py-0.5 rounded text-xs font-medium bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400">
          待审核
        </span>
        <!-- 回复目标用户名 -->
        <template v-if="comment.replyUser">
          <span class="text-xs text-gray-400 dark:text-gray-500 mx-1">回复</span>
          <span class="text-sm font-medium text-primary">@{{ comment.replyUser.nickname }}</span>
        </template>
        <p class="text-sm text-gray-700 dark:text-gray-300 mt-1">{{ comment.content }}</p>
      </div>

      <!-- 底部操作栏 -->
      <div class="flex items-center space-x-4 mt-2 text-xs text-gray-400 dark:text-gray-500">
        <span>{{ formatTime(comment.createdAt) }}</span>
        <!-- 点赞 -->
        <button class="flex items-center space-x-1 transition-colors" :class="comment.isLiked ? 'text-primary' : 'hover:text-primary'" @click="$emit('like', comment.id)">
          <svg class="w-3.5 h-3.5" :class="comment.isLiked ? 'fill-primary' : ''" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
          </svg>
          <span>{{ comment.likeCount }}</span>
        </button>
        <!-- 回复 -->
        <button class="hover:text-primary transition-colors" @click="$emit('reply', comment)">
          回复
        </button>
        <!-- 删除（仅自己的评论） -->
        <button v-if="isOwner" class="hover:text-red-500 transition-colors" @click="handleDelete">
          删除
        </button>
        <!-- 举报（非自己的评论） -->
        <button v-if="!isOwner && userStore.isLoggedIn" class="hover:text-primary transition-colors" @click="showReportModal = true">
          举报
        </button>
      </div>
    </div>
  </div>

  <!-- 举报弹窗 -->
  <Teleport to="body">
    <div v-if="showReportModal" class="fixed inset-0 z-50 flex items-center justify-center p-4" @click.self="showReportModal = false">
      <div class="absolute inset-0 bg-black/50" @click="showReportModal = false"></div>
      <div class="relative bg-white dark:bg-gray-800 rounded-xl shadow-xl w-full max-w-md p-6">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">举报评论</h3>
        <textarea
          v-model="reportReason"
          class="input resize-none"
          rows="3"
          placeholder="请输入举报原因（选填）"
          maxlength="500"
        ></textarea>
        <div class="flex justify-end space-x-3 mt-4">
          <button class="btn-ghost text-sm" @click="showReportModal = false">取消</button>
          <button class="btn-primary text-sm" :disabled="reportSubmitting" @click="handleReport">
            {{ reportSubmitting ? '提交中...' : '提交举报' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
/** 单条评论项组件 */
import type { Comment } from '~/types'

const props = defineProps<{
  comment: Comment
}>()

const emit = defineEmits<{
  reply: [comment: Comment]
  like: [commentId: number]
  delete: [commentId: number]
  report: [data: { commentId: number; reason?: string }]
}>()

const userStore = useUserStore()

// 是否是自己的评论
const isOwner = computed(() => userStore.userInfo?.id === props.comment.userId)

// 举报相关
const showReportModal = ref(false)
const reportReason = ref('')
const reportSubmitting = ref(false)

// 删除评论
const handleDelete = () => {
  emit('delete', props.comment.id)
}

// 提交举报
const handleReport = async () => {
  reportSubmitting.value = true
  try {
    emit('report', {
      commentId: props.comment.id,
      reason: reportReason.value.trim() || undefined,
    })
    showReportModal.value = false
    reportReason.value = ''
  } finally {
    reportSubmitting.value = false
  }
}

// 格式化时间
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
  return date.toLocaleDateString('zh-CN')
}
</script>
