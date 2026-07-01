<template>
  <div class="chat-file-card" @click="handleClick">
    <div class="chat-file-icon" :style="{ color: iconColor }">
      <!-- Word 文档 -->
      <svg v-if="fileCategory === 'word'" class="w-7 h-7" viewBox="0 0 24 24" fill="currentColor">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zM6 20V4h7v5h5v11H6z"/>
        <path d="M8 12h1.5l1 3.5 1-3.5H13l-1.75 5H10.7L9 13.5 7.3 17H5.75L4 12h1.5l1 3.5z" opacity="0"/>
        <text x="9" y="16.5" font-size="6" font-weight="700" fill="currentColor">W</text>
      </svg>
      <!-- PDF 文档 -->
      <svg v-else-if="fileCategory === 'pdf'" class="w-7 h-7" viewBox="0 0 24 24" fill="currentColor">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zM6 20V4h7v5h5v11H6z"/>
        <text x="8.5" y="16.5" font-size="5.5" font-weight="700" fill="currentColor">PDF</text>
      </svg>
      <!-- TXT 文本 -->
      <svg v-else-if="fileCategory === 'txt'" class="w-7 h-7" viewBox="0 0 24 24" fill="currentColor">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zM6 20V4h7v5h5v11H6z"/>
        <line x1="8" y1="11" x2="16" y2="11" stroke="currentColor" stroke-width="1.2" fill="none"/>
        <line x1="8" y1="14" x2="16" y2="14" stroke="currentColor" stroke-width="1.2" fill="none"/>
        <line x1="8" y1="17" x2="12" y2="17" stroke="currentColor" stroke-width="1.2" fill="none"/>
      </svg>
      <!-- Excel 表格 -->
      <svg v-else-if="fileCategory === 'excel'" class="w-7 h-7" viewBox="0 0 24 24" fill="currentColor">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zM6 20V4h7v5h5v11H6z"/>
        <path d="M8 11h8v7H8z" fill="none" stroke="currentColor" stroke-width="0.8"/>
        <line x1="8" y1="13.5" x2="16" y2="13.5" stroke="currentColor" stroke-width="0.6"/>
        <line x1="8" y1="15.8" x2="16" y2="15.8" stroke="currentColor" stroke-width="0.6"/>
        <line x1="12" y1="11" x2="12" y2="18" stroke="currentColor" stroke-width="0.6"/>
      </svg>
      <!-- PPT 演示 -->
      <svg v-else-if="fileCategory === 'ppt'" class="w-7 h-7" viewBox="0 0 24 24" fill="currentColor">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zM6 20V4h7v5h5v11H6z"/>
        <rect x="8" y="11" width="8" height="7" rx="1" fill="none" stroke="currentColor" stroke-width="0.8"/>
        <rect x="10" y="13" width="4" height="1.5" rx="0.5" fill="currentColor" opacity="0.5"/>
      </svg>
      <!-- 压缩包 -->
      <svg v-else-if="fileCategory === 'archive'" class="w-7 h-7" viewBox="0 0 24 24" fill="currentColor">
        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zM6 20V4h7v5h5v11H6z"/>
        <rect x="10" y="11" width="4" height="5" rx="0.5" fill="none" stroke="currentColor" stroke-width="0.8"/>
        <circle cx="12" cy="13.5" r="0.8" fill="currentColor"/>
      </svg>
      <!-- 通用文件（默认） -->
      <svg v-else class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
      </svg>
      <!-- 扩展名标签 -->
      <span v-if="fileExtension" class="chat-file-ext-badge" :style="{ background: iconColor }">{{ fileExtension }}</span>
    </div>
    <div class="chat-file-info">
      <span class="chat-file-name">{{ fileName }}</span>
      <span v-if="fileSize" class="chat-file-size">{{ fileSize }}</span>
    </div>
    <div class="chat-file-action">
      <!-- 可预览的文件显示眼睛图标 -->
      <svg v-if="canPreview" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
      </svg>
      <!-- 不可预览的文件显示下载图标 -->
      <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * FileCard - 聊天文件卡片组件（私信/群聊共用）
 * 支持文件类型图标、扩展名标签、点击预览/下载
 */
import { computed } from 'vue'
import { useChatMedia } from '@/composables/chat/useChatMedia'

const props = defineProps<{
  content: string
}>()

const emit = defineEmits<{
  preview: [url: string, name: string, ext: string]
}>()

const { getFileUrl, getFileName, getFileSize } = useChatMedia()

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

/** 获取文件扩展名 */
const fileExtension = computed(() => {
  const name = fileName.value
  const dotIndex = name.lastIndexOf('.')
  if (dotIndex < 0) return ''
  return name.substring(dotIndex + 1).toUpperCase()
})

/** 文件类别 */
const fileCategory = computed<'word' | 'pdf' | 'txt' | 'excel' | 'ppt' | 'archive' | 'default'>(() => {
  const ext = fileExtension.value.toLowerCase()
  if (['doc', 'docx'].includes(ext)) return 'word'
  if (ext === 'pdf') return 'pdf'
  if (ext === 'txt') return 'txt'
  if (['xls', 'xlsx'].includes(ext)) return 'excel'
  if (['ppt', 'pptx'].includes(ext)) return 'ppt'
  if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) return 'archive'
  return 'default'
})

/** 文件类型对应的图标颜色 */
const iconColor = computed(() => {
  switch (fileCategory.value) {
    case 'word': return '#2B579A'
    case 'pdf': return '#E13F34'
    case 'txt': return '#64748B'
    case 'excel': return '#217346'
    case 'ppt': return '#D04423'
    case 'archive': return '#8B5CF6'
    default: return 'var(--zh-primary, #6366f1)'
  }
})

/** 是否可预览（PDF和TXT支持在线预览） */
const canPreview = computed(() => {
  return ['pdf', 'txt'].includes(fileCategory.value)
})

/** 点击文件卡片 */
const handleClick = () => {
  const url = getFileUrl(props.content)
  const name = getFileName(props.content)
  const ext = fileExtension.value.toLowerCase()

  if (canPreview.value) {
    emit('preview', url, name, ext)
  } else {
    // 不可预览的文件，直接下载
    const link = document.createElement('a')
    link.href = url
    link.download = name
    link.target = '_blank'
    link.rel = 'noopener'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  }
}
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
  max-width: 280px;
  cursor: pointer;
  transition: background 0.15s, transform 0.1s;
}
.chat-file-card:hover {
  background: var(--zh-bg, #e2e8f0);
  transform: translateY(-1px);
}
.chat-file-card:active {
  transform: translateY(0);
}
.chat-file-icon {
  flex-shrink: 0;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}
.chat-file-ext-badge {
  position: absolute;
  bottom: -3px;
  right: -6px;
  font-size: 7px;
  font-weight: 700;
  color: #fff;
  padding: 1px 3px;
  border-radius: 3px;
  line-height: 1.2;
  letter-spacing: 0.02em;
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
.chat-file-action {
  color: var(--zh-text-tertiary, #94a3b8);
  flex-shrink: 0;
  transition: color 0.15s;
}
.chat-file-card:hover .chat-file-action {
  color: var(--zh-primary, #6366f1);
}
</style>
