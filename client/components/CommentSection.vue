<template>
  <!-- 评论区组件 -->
  <div class="space-y-6">
    <!-- 评论输入框 -->
    <div v-if="userStore.isLoggedIn" class="flex space-x-3">
      <img :src="userStore.userInfo?.avatar || '/default-avatar.png'" class="w-10 h-10 rounded-full object-cover shrink-0" alt="头像" />
      <div class="flex-1">
        <textarea
          v-model="commentContent"
          class="input resize-none"
          rows="3"
          :placeholder="replyTo ? `回复 @${replyTo.user.nickname}...` : '写下你的评论...'"
        ></textarea>
        <div class="flex justify-end mt-2 space-x-2">
          <button v-if="replyTo" class="btn-ghost text-sm" @click="cancelReply">取消回复</button>
          <button class="btn-primary text-sm" :disabled="!commentContent.trim()" @click="submitComment">
            发表评论
          </button>
        </div>
      </div>
    </div>

    <!-- 未登录提示 -->
    <div v-else class="text-center py-6">
      <p class="text-gray-500 dark:text-gray-400 mb-2">登录后即可评论</p>
      <NuxtLink to="/login" class="btn-primary text-sm">去登录</NuxtLink>
    </div>

    <!-- 评论列表 -->
    <div class="space-y-6">
      <div v-for="comment in comments" :key="comment.id" class="space-y-4">
        <!-- 一级评论 -->
        <CommentItem :comment="comment" @reply="handleReply" @like="handleLike" />

        <!-- 二级回复 -->
        <div v-if="comment.replies?.length" class="ml-12 space-y-4">
          <CommentItem v-for="reply in comment.replies" :key="reply.id" :comment="reply" @reply="handleReply" @like="handleLike" />
        </div>
      </div>
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
}>()

const emit = defineEmits<{
  submit: [data: { content: string; parentId?: number }]
  loadMore: []
  like: [commentId: number]
}>()

const userStore = useUserStore()
const commentContent = ref('')
const replyTo = ref<Comment | null>(null)

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
  })
  commentContent.value = ''
  replyTo.value = null
}

// 点赞评论
const handleLike = (commentId: number) => {
  emit('like', commentId)
}

// 加载更多
const loadMore = () => {
  emit('loadMore')
}
</script>
