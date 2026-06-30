<template>
  <div class="voice-recording-bar">
    <span class="voice-rec-dot" />
    <span class="voice-rec-time">{{ formattedTime }}</span>
    <button class="voice-rec-stop" @click="$emit('finish')">发送</button>
    <button class="voice-rec-cancel" @click="$emit('cancel')">取消</button>
  </div>
</template>

<script setup lang="ts">
/**
 * VoiceRecordingBar - 语音录制中状态栏（私信/群聊共用）
 * 消除三处重复的 HTML + CSS + @keyframes
 */
import { computed } from 'vue'

const props = defineProps<{
  recordingTime: number
}>()

defineEmits<{
  finish: []
  cancel: []
}>()

const formattedTime = computed(() => {
  const s = props.recordingTime
  const mm = Math.floor(s / 60).toString().padStart(2, '0')
  const ss = (s % 60).toString().padStart(2, '0')
  return `${mm}:${ss}`
})
</script>

<style scoped>
.voice-recording-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  background: var(--zh-bg-hover, #f1f5f9);
  border-radius: 24px;
  border: 1.5px solid var(--zh-border, #e5e7eb);
}
.voice-rec-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
  animation: voice-pulse 1s infinite;
  flex-shrink: 0;
}
@keyframes voice-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
.voice-rec-time {
  font-size: 14px;
  font-weight: 500;
  color: var(--zh-text, #1e293b);
  font-variant-numeric: tabular-nums;
  min-width: 40px;
}
.voice-rec-stop {
  padding: 4px 16px;
  border: none;
  border-radius: 16px;
  background: var(--zh-primary, #6366f1);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
  margin-left: auto;
}
.voice-rec-stop:hover { opacity: 0.85; }
.voice-rec-cancel {
  padding: 4px 12px;
  border: 1px solid var(--zh-border, #e5e7eb);
  border-radius: 16px;
  background: transparent;
  color: var(--zh-text-secondary, #64748b);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.15s;
}
.voice-rec-cancel:hover { background: var(--zh-bg-hover, #f1f5f9); }
</style>
