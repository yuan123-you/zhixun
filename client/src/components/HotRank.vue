<template>
  <!-- 热榜组件 - 卡片 + 编号渐变 -->
  <div class="hot-rank card-base">
    <div class="hot-rank-header">
      <h3 class="hot-rank-title">🔥 热榜</h3>
      <RouterLink to="/rank" class="hot-rank-more">查看全部 →</RouterLink>
    </div>

    <div v-if="!items || items.length === 0" class="hot-rank-empty">暂无热榜数据</div>

    <div v-else class="hot-rank-list">
      <RouterLink
        v-for="(item, index) in items"
        :key="item.id"
        :to="`/articles/${item.id}`"
        class="hot-rank-item"
      >
        <span class="rank-index" :class="`rank-${index + 1}`">
          {{ index + 1 }}
        </span>
        <div class="rank-info">
          <p class="rank-title">{{ item.title }}</p>
          <div class="rank-meta">
            <span v-if="item.authorNickname">{{ item.authorNickname }}</span>
            <span v-if="item.score != null" class="rank-score">{{ formatCount(item.score) }} 热度</span>
          </div>
        </div>
      </RouterLink>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { RankItem } from '@/types'

defineProps<{ items: RankItem[] }>()

const formatCount = (count: number | undefined) => {
  if (count == null || count < 1) return '1'
  const rounded = Math.round(count * 10) / 10
  if (rounded >= 10000) return `${(rounded / 10000).toFixed(1)}万`
  if (rounded >= 1000) return `${(rounded / 1000).toFixed(1)}k`
  return rounded.toString()
}
</script>

<style scoped>
.hot-rank {
  padding: 16px;
  overflow: hidden;
}

.hot-rank-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--zh-border-light);
}

.hot-rank-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--zh-text);
}

.hot-rank-more {
  font-size: 12px;
  color: var(--zh-text-tertiary);
  text-decoration: none;
  transition: color var(--zh-transition-fast);
}
.hot-rank-more:hover { color: var(--zh-primary); }

.hot-rank-empty {
  text-align: center;
  padding: 20px;
  font-size: 13px;
  color: var(--zh-text-placeholder);
}

.hot-rank-list { display: flex; flex-direction: column; gap: 2px; }

.hot-rank-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 10px 10px;
  text-decoration: none;
  border-radius: var(--zh-radius-md);
  transition: background var(--zh-transition-fast);
}
.hot-rank-item:hover { background: var(--zh-bg-hover); }

.rank-index {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: var(--zh-text-tertiary);
  border-radius: 6px;
  flex-shrink: 0;
  margin-top: 1px;
}
.rank-1 { background: linear-gradient(135deg, #fbbf24, #f59e0b); color: #fff; }
.rank-2 { background: linear-gradient(135deg, #94a3b8, #64748b); color: #fff; }
.rank-3 { background: linear-gradient(135deg, #d97706, #92400e); color: #fff; }

.rank-info { flex: 1; min-width: 0; }

.rank-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--zh-text);
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-height: 1.45;
}

.rank-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--zh-text-tertiary);
}

.rank-score {
  background: var(--zh-bg-hover);
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
  color: var(--zh-primary);
}
</style>
