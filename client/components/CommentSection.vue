<template>
  <!-- 评论区组件 -->
  <div class="space-y-3">
    <!-- 评论输入框（移动端固定在底部Tab栏上方） -->
    <div class="sticky bottom-14 md:static z-30 bg-white dark:bg-gray-900 pt-2 pb-1 -mx-1.5 px-1.5 border-t border-slate-100 dark:border-gray-800">
    <ClientOnly>
      <div v-if="userStore.isLoggedIn" class="flex space-x-3">
        <UserAvatar :src="userStore.userInfo?.avatar" alt="头像" size="md" />
        <div class="flex-1">
          <textarea
            v-model="commentContent"
            class="input resize-none"
            rows="3"
            :placeholder="replyTo ? `回复 @${replyTo.user.nickname}...` : '写下你的评论...'"
          ></textarea>
          <div class="flex justify-end mt-1.5 space-x-2">
            <button v-if="replyTo" class="btn-ghost text-sm" @click="cancelReply">取消回复</button>
            <button class="btn-primary text-sm" :disabled="!commentContent.trim()" @click="submitComment">
              发表评论
            </button>
          </div>
        </div>
      </div>

      <!-- 未登录提示 -->
      <div v-else class="text-center py-6">
        <p class="text-slate-500 mb-2">登录后即可评论</p>
        <NuxtLink to="/login" class="btn-primary text-sm">去登录</NuxtLink>
      </div>
    </ClientOnly>
    </div>

    <!-- 评论列表头部：总数 + 排序 -->
    <div class="flex items-center justify-between">
      <span class="text-sm text-slate-500">共 {{ total }} 条评论</span>
      <div class="flex items-center space-x-1 text-sm">
        <button
          class="px-2 py-1 rounded transition-colors"
          :class="sortBy === 'latest' ? 'text-primary font-medium' : 'text-slate-400 hover:text-slate-600'"
          @click="changeSort('latest')"
        >
          最新
        </button>
        <span class="text-slate-300">|</span>
        <button
          class="px-2 py-1 rounded transition-colors"
          :class="sortBy === 'hot' ? 'text-primary font-medium' : 'text-slate-400 hover:text-slate-600'"
          @click="changeSort('hot')"
        >
          最热
        </button>
      </div>
    </div>

    <!-- 评论列表（多级嵌套由 CommentItem 递归渲染） -->
    <div class="space-y-6">
      <CommentItem
        v-for="comment in comments"
        :key="comment.id"
        :comment="comment"
        @reply="handleReply"
        @like="handleLike"
        @delete="handleDelete"
        @report="handleReport"
      />
    </div>

    <!-- 加载更多 -->
    <div v-if="hasMore" class="text-center py-4">
      <button class="btn-ghost text-sm" @click="loadMore">加载更多评论</button>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 评论区组件 */
import type { Comment } from '~/types'

const props = defineProps<{
  articleId: number
  comments: Comment[]
  hasMore: boolean
  total: number
}>()

const emit = defineEmits<{
  submit: [data: { content: string; parentId?: number; replyUserId?: number }]
  loadMore: []
  like: [commentId: number]
  delete: [commentId: number]
  report: [data: { commentId: number; reason?: string }]
  sortChange: [sort: string]
}>()

const userStore = useUserStore()
const commentContent = ref('')
const replyTo = ref<Comment | null>(null)
const sortBy = ref('latest')

// 切换排序
const changeSort = (sort: string) => {
  if (sortBy.value === sort) return
  sortBy.value = sort
  emit('sortChange', sort)
}

// 回复评论
const handleReply = (comment: Comment) => {
  replyTo.value = comment
  commentContent.value = ''
}

// 取消回复
const cancelReply = () => {
  replyTo.value = null
  commentContent.value = ''
}

// 提交评论
const submitComment = () => {
  if (!commentContent.value.trim()) return
  emit('submit', {
    content: commentContent.value.trim(),
    parentId: replyTo.value?.id,
    replyUserId: replyTo.value?.userId,
  })
  commentContent.value = ''
  replyTo.value = null
}

// 点赞评论
const handleLike = (commentId: number) => {
  emit('like', commentId)
}

// 删除评论
const handleDelete = (commentId: number) => {
  emit('delete', commentId)
}

// 举报评论
const handleReport = (data: { commentId: number; reason?: string }) => {
  emit('report', data)
}

// 加载更多
const loadMore = () => {
  emit('loadMore')
}
</script>
