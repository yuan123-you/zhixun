<template>
  <!-- 空状态组件 -->
  <div class="flex flex-col items-center justify-center py-12 px-4">
    <!-- 图标 -->
    <div class="w-24 h-24 mb-4 text-gray-300 dark:text-gray-600">
      <svg class="w-full h-full" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
      </svg>
    </div>

    <!-- 标题 -->
    <h3 class="text-lg font-medium text-gray-900 dark:text-white mb-1">{{ title }}</h3>

    <!-- 描述 -->
    <p v-if="description" class="text-sm text-gray-500 dark:text-gray-400 mb-6">{{ description }}</p>

    <!-- 操作按钮插槽 -->
    <slot name="action" />
  </div>
</template>

<script setup lang="ts">
/** 空状态组件 */

withDefaults(defineProps<{
  title?: string
  description?: string
}>(), {
  title: '',
  description: '',
})
</script>
