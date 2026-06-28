<template>
  <!-- 空状态 - 友好引导 -->
  <div class="empty-state">
    <div class="empty-illustration">
      <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="20" y="30" width="80" height="60" rx="12" stroke="currentColor" stroke-width="2" fill="none" opacity="0.3"/>
        <circle cx="45" cy="52" r="8" stroke="currentColor" stroke-width="2" fill="none"/>
        <path d="M25 80h70" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.3"/>
        <path d="M25 88h50" stroke="currentColor" stroke-width="2" stroke-linecap="round" opacity="0.2"/>
        <circle cx="70" cy="95" r="3" fill="currentColor" opacity="0.2"/>
        <circle cx="80" cy="95" r="3" fill="currentColor" opacity="0.2"/>
        <circle cx="90" cy="95" r="3" fill="currentColor" opacity="0.2"/>
      </svg>
    </div>
    <h3 class="empty-title">{{ title || '暂无内容' }}</h3>
    <p class="empty-desc">{{ description || '这里还没有内容，快去发现更多精彩吧' }}</p>
    <div v-if="$slots.action" class="empty-action">
      <slot name="action" />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ title?: string; description?: string }>()
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}
.empty-illustration {
  width: 120px;
  height: 120px;
  color: var(--zh-text-placeholder);
  margin-bottom: 20px;
  animation: fade-in-up 0.5s var(--zh-transition-base) both;
}
.empty-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--zh-text);
  margin-bottom: 8px;
  animation: fade-in-up 0.5s 0.1s var(--zh-transition-base) both;
}
.empty-desc {
  font-size: 14px;
  color: var(--zh-text-tertiary);
  max-width: 280px;
  line-height: 1.6;
  animation: fade-in-up 0.5s 0.15s var(--zh-transition-base) both;
}
.empty-action {
  margin-top: 24px;
  animation: fade-in-up 0.5s 0.2s var(--zh-transition-base) both;
}
</style>
