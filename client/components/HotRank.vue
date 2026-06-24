<template>
  <!-- 热榜组件 -->
  <div class="card">
    <div class="p-4 border-b border-gray-200 dark:border-gray-700">
      <h3 class="font-semibold text-gray-900 dark:text-white">🔥 {{ t('hotRank.title') }}</h3>
    </div>
    <!-- 空状态 -->
    <div v-if="!items || items.length === 0" class="p-6 text-center text-sm text-gray-400 dark:text-gray-500">
      {{ t('hotRank.empty') }}
    </div>
    <div v-else class="divide-y divide-gray-100 dark:divide-gray-700">
      <NuxtLink v-for="(item, index) in items" :key="item.id" :to="`/articles/${item.id}`" class="flex items-start p-4 hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors">
        <!-- 排名 -->
        <span class="w-6 text-center font-bold text-sm shrink-0" :class="index < 3 ? 'text-danger' : 'text-gray-400'">
          {{ index + 1 }}
        </span>
        <!-- 标题 -->
        <div class="ml-3 flex-1 min-w-0">
          <p class="text-sm text-gray-900 dark:text-white line-clamp-2">{{ item.title }}</p>
          <div class="flex items-center space-x-2 mt-1 text-xs text-gray-400">
            <span v-if="item.authorNickname">{{ item.authorNickname }}</span>
            <span v-if="item.authorNickname && item.score != null">·</span>
            <span v-if="item.score != null">{{ formatCount(item.score) }} {{ t('article.heat') }}</span>
          </div>
        </div>
      </NuxtLink>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 热榜组件 */
import type { RankItem } from '~/types'

defineProps<{
  items: RankItem[]
}>()

const { t } = useI18n()

// 格式化数量
const formatCount = (count: number | undefined) => {
  if (count == null) return '0'
  if (count >= 10000) return `${(count / 10000).toFixed(1)}万`
  if (count >= 1000) return `${(count / 1000).toFixed(1)}k`
  return count.toString()
}
</script>
