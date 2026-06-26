<template>
  <!-- 骨架屏组件 -->
  <div class="animate-pulse">
    <!-- 作品类型骨架屏 -->
    <template v-if="type === 'article'">
      <div class="card p-4">
        <div class="flex gap-4">
          <div class="flex-1 space-y-3">
            <!-- 标题 -->
            <div class="h-5 bg-slate-200 rounded w-3/4"></div>
            <!-- 正文 -->
            <div class="space-y-2">
              <div class="h-4 bg-slate-200 rounded w-full"></div>
              <div class="h-4 bg-slate-200 rounded w-2/3"></div>
            </div>
            <!-- 底部信息 -->
            <div class="flex items-center space-x-4">
              <div class="w-5 h-5 bg-slate-200 rounded-full"></div>
              <div class="h-3 bg-slate-200 rounded w-16"></div>
              <div class="h-3 bg-slate-200 rounded w-12"></div>
              <div class="h-3 bg-slate-200 rounded w-12"></div>
            </div>
          </div>
          <!-- 封面图 -->
          <div class="hidden sm:block w-32 h-24 bg-slate-200 rounded-lg shrink-0"></div>
        </div>
      </div>
    </template>

    <!-- 用户类型骨架屏 -->
    <template v-else-if="type === 'user'">
      <div class="card p-4 flex items-center space-x-3">
        <div class="w-12 h-12 bg-slate-200 rounded-full shrink-0"></div>
        <div class="flex-1 space-y-2">
          <div class="h-4 bg-slate-200 rounded w-24"></div>
          <div class="h-3 bg-slate-200 rounded w-32"></div>
        </div>
        <div class="w-16 h-8 bg-slate-200 rounded-lg"></div>
      </div>
    </template>

    <!-- 评论类型骨架屏 -->
    <template v-else-if="type === 'comment'">
      <div class="flex space-x-3">
        <div class="w-8 h-8 bg-slate-200 rounded-full shrink-0"></div>
        <div class="flex-1 space-y-2">
          <div class="h-4 bg-slate-200 rounded w-20"></div>
          <div class="h-3 bg-slate-200 rounded w-full"></div>
          <div class="h-3 bg-slate-200 rounded w-2/3"></div>
        </div>
      </div>
    </template>

    <!-- 默认单行骨架屏 -->
    <template v-else>
      <div class="space-y-2">
        <div class="h-4 bg-slate-200 rounded w-full"></div>
        <div class="h-4 bg-slate-200 rounded w-3/4"></div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/** 骨架屏组件 */

withDefaults(defineProps<{
  type?: 'article' | 'user' | 'comment' | 'default'
}>(), {
  type: 'default',
})
</script>
