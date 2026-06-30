<template>
  <div class="chat-toolbar">
    <EmojiPicker @select="(emoji: string) => $emit('emoji', emoji)" />
    <button class="chat-tool-btn" @click="$emit('image')" title="发送图片">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
      </svg>
    </button>
    <button class="chat-tool-btn" @click="$emit('file')" title="发送文件">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13" />
      </svg>
    </button>
    <button class="chat-tool-btn" :class="{ active: isRecording }" @click="$emit('voice')" title="语音消息">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
      </svg>
    </button>
    <button class="chat-tool-btn" :class="{ active: aiMode }" @click="$emit('ai')" title="AI助手">
      <svg class="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <rect x="5" y="8" width="14" height="11" rx="3"/>
        <circle cx="9.5" cy="13" r="1.5" fill="currentColor" stroke="none"/>
        <circle cx="14.5" cy="13" r="1.5" fill="currentColor" stroke="none"/>
        <path d="M10 16.5h4"/>
        <path d="M12 3v3"/>
        <circle cx="12" cy="2.5" r="1.5" fill="currentColor" stroke="none"/>
        <path d="M3 12.5h2M19 12.5h2"/>
        <path d="M20 4l.5 1.5L22 6l-1.5.5L20 8l-.5-1.5L18 6l1.5-.5z" fill="currentColor" stroke="none" opacity="0.6"/>
        <path d="M3.5 18l.4 1L5 19.4l-1.1.4L3.5 21l-.4-1.2L2 19.4l1.1-.4z" fill="currentColor" stroke="none" opacity="0.5"/>
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
/**
 * ChatToolbar - 聊天工具栏（表情/图片/文件/语音/AI）
 * 消除三处重复的 5 个按钮 + SVG 图标
 */
import EmojiPicker from '@/components/EmojiPicker.vue'

defineProps<{
  aiMode?: boolean
  isRecording?: boolean
}>()

defineEmits<{
  emoji: [emoji: string]
  image: []
  file: []
  voice: []
  ai: []
}>()
</script>

<style scoped>
.chat-toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding-bottom: 6px;
}
.chat-tool-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--zh-text-tertiary, #94a3b8);
  cursor: pointer;
  transition: all 0.15s;
  min-height: 0 !important;
  min-width: 0 !important;
}
.chat-tool-btn:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text-secondary, #64748b);
}
.chat-tool-btn.active {
  background: var(--zh-primary-bg, rgba(99, 102, 241, 0.1));
  color: var(--zh-primary, #6366f1);
}
</style>
