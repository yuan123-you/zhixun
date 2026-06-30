<template>
  <a :href="fileUrl" target="_blank" rel="noopener" class="chat-file-card">
    <div class="chat-file-icon">
      <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
    </div>
    <div class="chat-file-info">
      <span class="chat-file-name">{{ fileName }}</span>
      <span v-if="fileSize" class="chat-file-size">{{ fileSize }}</span>
    </div>
    <div class="chat-file-download">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
      </svg>
    </div>
  </a>
</template>

<script setup lang="ts">
/**
 * FileCard - 聊天文件卡片组件（私信/群聊共用）
 * 消除三处重复的文件卡片模板 + SVG 图标 + CSS
 */
import { computed } from 'vue'

const props = defineProps<{
  content: string
}>()

const parsed = computed(() => {
  try {
    return JSON.parse(props.content)
  } catch {
    return null
  }
})

const fileUrl = computed(() => {
  if (parsed.value?.url) return parsed.value.url
  return props.content
})

const fileName = computed(() => parsed.value?.name || '未知文件')

const fileSize = computed(() => {
  const size = parsed.value?.size
  if (!size) return ''
  return size > 1048576
    ? (size / 1048576).toFixed(1) + ' MB'
    : (size / 1024).toFixed(1) + ' KB'
})
</script>

<style scoped>
.chat-file-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--zh-bg-hover, #f1f5f9);
  border-radius: 10px;
  text-decoration: none;
  color: inherit;
  min-width: 200px;
  transition: background 0.15s;
}
.chat-file-card:hover {
  background: var(--zh-bg, #e2e8f0);
}
.chat-file-icon {
  color: var(--zh-primary, #6366f1);
  flex-shrink: 0;
}
.chat-file-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.chat-file-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--zh-text, #1e293b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.chat-file-size {
  font-size: 11px;
  color: var(--zh-text-tertiary, #94a3b8);
}
.chat-file-download {
  color: var(--zh-text-tertiary, #94a3b8);
  flex-shrink: 0;
}
</style>
