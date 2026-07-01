<template>
  <Teleport to="body">
    <Transition name="preview-fade">
      <div v-if="visible" class="file-preview-overlay" @click.self="close">
        <div class="file-preview-dialog">
          <!-- 头部 -->
          <div class="file-preview-header">
            <div class="file-preview-title-wrap">
              <div class="file-preview-type-dot" :style="{ background: typeColor }" />
              <h3 class="file-preview-title">{{ fileName }}</h3>
            </div>
            <div class="file-preview-actions">
              <button class="file-preview-btn" title="下载" @click="downloadFile">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
              </button>
              <button class="file-preview-btn" title="关闭" @click="close">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
          </div>

          <!-- 内容区域 -->
          <div class="file-preview-body">
            <!-- PDF 预览 -->
            <div v-if="fileExt === 'pdf'" class="file-preview-pdf">
              <iframe
                v-if="resolvedUrl"
                :src="resolvedUrl"
                class="file-preview-iframe"
                frameborder="0"
              />
              <div v-else class="file-preview-loading">
                <div class="file-preview-spinner" />
                <span>加载中...</span>
              </div>
            </div>

            <!-- TXT 预览 -->
            <div v-else-if="fileExt === 'txt'" class="file-preview-txt">
              <div v-if="txtLoading" class="file-preview-loading">
                <div class="file-preview-spinner" />
                <span>加载中...</span>
              </div>
              <pre v-else-if="txtContent" class="file-preview-text">{{ txtContent }}</pre>
              <div v-else class="file-preview-error">
                <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4.5c-.77-.833-2.694-.833-3.464 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z" />
                </svg>
                <p>文件内容加载失败</p>
              </div>
            </div>

            <!-- 不支持的格式 -->
            <div v-else class="file-preview-unsupported">
              <svg class="w-14 h-14" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.2" d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
              </svg>
              <p class="file-preview-unsupported-text">该文件格式暂不支持在线预览</p>
              <button class="file-preview-download-btn" @click="downloadFile">
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                </svg>
                <span>下载文件</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * FilePreviewDialog - 文件预览弹窗（私信/群聊共用）
 * 支持 PDF iframe 预览、TXT 文本内容预览、其他格式下载
 */
import { ref, watch, computed } from 'vue'
import { useResourceUrl } from '@/composables/useResourceUrl'

const props = defineProps<{
  visible: boolean
  fileUrl: string
  fileName: string
  fileExt: string
}>()

const emit = defineEmits<{
  close: []
}>()

const { resolveUrl } = useResourceUrl()

const resolvedUrl = computed(() => resolveUrl(props.fileUrl) || props.fileUrl)

/** 文件类型对应的颜色 */
const typeColor = computed(() => {
  const ext = props.fileExt.toLowerCase()
  if (['doc', 'docx'].includes(ext)) return '#2B579A'
  if (ext === 'pdf') return '#E13F34'
  if (ext === 'txt') return '#64748B'
  return 'var(--zh-primary, #6366f1)'
})

// TXT 内容加载
const txtContent = ref('')
const txtLoading = ref(false)

watch(() => props.visible, async (v) => {
  if (!v) return
  if (props.fileExt === 'txt') {
    txtContent.value = ''
    txtLoading.value = true
    try {
      const resp = await fetch(resolvedUrl.value)
      if (resp.ok) {
        txtContent.value = await resp.text()
      }
    } catch {
      txtContent.value = ''
    } finally {
      txtLoading.value = false
    }
  }
})

function close() {
  emit('close')
}

function downloadFile() {
  const link = document.createElement('a')
  link.href = resolvedUrl.value
  link.download = props.fileName
  link.target = '_blank'
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// ESC 关闭
function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.visible) close()
}
watch(() => props.visible, (v) => {
  if (v) window.addEventListener('keydown', onKeydown)
  else window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
.file-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  backdrop-filter: blur(4px);
}

.file-preview-dialog {
  background: var(--zh-bg-elevated, #fff);
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  width: 100%;
  max-width: 800px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: previewSlideIn 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes previewSlideIn {
  from { opacity: 0; transform: scale(0.92) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

/* 头部 */
.file-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid var(--zh-border, #e5e7eb);
  flex-shrink: 0;
}

.file-preview-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  flex: 1;
}

.file-preview-type-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.file-preview-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--zh-text, #1e293b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin: 0;
}

.file-preview-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 12px;
}

.file-preview-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--zh-text-secondary, #64748b);
  cursor: pointer;
  transition: all 0.15s;
}

.file-preview-btn:hover {
  background: var(--zh-bg-hover, #f1f5f9);
  color: var(--zh-text, #1e293b);
}

/* 内容区域 */
.file-preview-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

/* PDF 预览 */
.file-preview-pdf {
  flex: 1;
  min-height: 500px;
}

.file-preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
}

/* TXT 预览 */
.file-preview-txt {
  flex: 1;
  overflow: auto;
  padding: 18px;
}

.file-preview-text {
  font-family: 'JetBrains Mono', 'Fira Code', 'Consolas', monospace;
  font-size: 13px;
  line-height: 1.7;
  color: var(--zh-text, #1e293b);
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  tab-size: 4;
}

/* 加载状态 */
.file-preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px;
  color: var(--zh-text-tertiary, #94a3b8);
  font-size: 14px;
}

.file-preview-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--zh-border, #e5e7eb);
  border-top-color: var(--zh-primary, #6366f1);
  border-radius: 50%;
  animation: preview-spin 0.8s linear infinite;
}

@keyframes preview-spin {
  to { transform: rotate(360deg); }
}

/* 错误状态 */
.file-preview-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 60px;
  color: var(--zh-text-tertiary, #94a3b8);
}

.file-preview-error p {
  font-size: 14px;
  margin: 0;
}

/* 不支持的格式 */
.file-preview-unsupported {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px 40px;
  color: var(--zh-text-tertiary, #94a3b8);
}

.file-preview-unsupported-text {
  font-size: 14px;
  margin: 0;
}

.file-preview-download-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 20px;
  border: none;
  border-radius: 10px;
  background: var(--zh-primary, #6366f1);
  color: #fff;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
  margin-top: 8px;
}

.file-preview-download-btn:hover {
  opacity: 0.9;
}

/* 动画 */
.preview-fade-enter-active,
.preview-fade-leave-active {
  transition: opacity 0.2s ease;
}

.preview-fade-enter-from,
.preview-fade-leave-to {
  opacity: 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .file-preview-overlay {
    padding: 12px;
  }
  .file-preview-dialog {
    max-height: 90vh;
    border-radius: 12px;
  }
  .file-preview-pdf {
    min-height: 350px;
  }
}
</style>
