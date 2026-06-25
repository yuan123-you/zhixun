<template>
  <!-- 热榜组件 -->
  <div class="card">
    <div class="p-4 border-b border-slate-200/60">
      <h3 class="font-semibold text-slate-900">🔥 热榜</h3>
    </div>
    <!-- 空状态 -->
    <div v-if="!items || items.length === 0" class="p-4 text-center text-sm text-slate-400">
      暂无热榜数据
    </div>
    <div v-else class="divide-y divide-slate-100">
      <NuxtLink v-for="(item, index) in items" :key="item.id" :to="`/articles/${item.id}`" class="flex items-start p-2 hover:bg-slate-50 transition-colors">
        <!-- 排名 -->
        <span class="w-6 text-center font-bold text-sm shrink-0" :class="index < 3 ? 'text-danger' : 'text-gray-400'">
          {{ index + 1 }}
        </span>
        <!-- 标题 -->
        <div class="ml-2 flex-1 min-w-0">
          <p class="text-sm text-slate-900 line-clamp-2">{{ item.title }}</p>
          <div class="flex items-center space-x-2 mt-1 text-xs text-gray-400">
            <span v-if="item.authorNickname">{{ item.authorNickname }}</span>
            <span v-if="item.authorNickname && item.score != null">·</span>
            <span v-if="item.score != null">{{ formatCount(item.score) }} 热度</span>
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

// 格式化热度值：>=1，最多1位小数
const formatCount = (count: number | undefined) => {
  if (count == null || count < 1) return '1'
  const rounded = Math.round(count * 10) / 10
  if (rounded >= 10000) return `${(rounded / 10000).toFixed(1)}万`
  if (rounded >= 1000) return `${(rounded / 1000).toFixed(1)}k`
  return rounded.toString()
}
</script>
