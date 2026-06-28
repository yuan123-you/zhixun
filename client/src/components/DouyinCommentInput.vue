<template>
  <!--
    抖音风格评论输入框
    - 基础态：底部玻璃态单行输入条（含表情/图片按钮）
    - 展开态：从底部弹起的全屏输入面板（含文本区、工具栏、发送按钮）
  -->
  <div class="dy-comment-root">
    <!-- ========== 基础态：底部固定输入条 ========== -->
    <div
      v-if="!expanded"
      class="dy-input-bar"
      :class="{ 'dy-input-bar--reply': !!replyTo }"
      @click="handleOpen()"
    >
      <div class="dy-input-bar-inner">
        <!-- 单行输入框（无头像） -->
        <div class="dy-input-field" :class="{ 'dy-input-field--reply': !!replyTo }">
          <span v-if="replyTo" class="dy-input-reply-tag">
            回复 @{{ replyTo.user?.nickname || '用户' }}
          </span>
          <span v-else class="dy-input-placeholder">
            {{ placeholder || '说点好听的~' }}
          </span>
        </div>

        <!-- 内嵌工具按钮 -->
        <div class="dy-input-tools">
          <button
            class="dy-tool-btn dy-tool-btn--emoji"
            type="button"
            aria-label="表情"
            @click.stop="openWithEmoji"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10" />
              <path d="M8 14s1.5 2 4 2 4-2 4-2" />
              <line x1="9" y1="9" x2="9.01" y2="9" />
              <line x1="15" y1="9" x2="15.01" y2="9" />
            </svg>
          </button>
          <button
            class="dy-tool-btn dy-tool-btn--image"
            type="button"
            aria-label="图片"
            @click.stop="openWithImage"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- ========== 展开态：全屏输入面板（Teleport 到 body） ========== -->
    <Teleport to="body" :disabled="!isClient">
      <Transition name="dy-sheet">
        <div
          v-if="expanded"
          class="dy-sheet-mask"
          @click.self="handleClose"
        >
          <div
            class="dy-sheet"
            :class="{ 'dy-sheet--reply': !!replyTo }"
            @touchstart.stop
            @click.stop
          >
            <!-- 顶部拖动条 + 操作按钮 -->
            <div class="dy-sheet-header">
              <button class="dy-sheet-action dy-sheet-action--cancel" type="button" @click="handleClose">
                取消
              </button>
              <div class="dy-sheet-drag">
                <span class="dy-sheet-drag-bar"></span>
              </div>
              <button
                class="dy-sheet-action dy-sheet-action--send"
                :class="{ 'dy-sheet-action--ready': canSend }"
                type="button"
                :disabled="!canSend || isSending"
                @click="handleSend"
              >
                <span v-if="isSending" class="dy-send-spinner"></span>
                <span v-else>发布</span>
              </button>
            </div>

            <!-- 回复提示 -->
            <div v-if="replyTo" class="dy-sheet-reply-hint">
              <span class="dy-sheet-reply-label">回复</span>
              <span class="dy-sheet-reply-user">@{{ replyTo.user?.nickname || '用户' }}</span>
              <button class="dy-sheet-reply-clear" type="button" @click="handleClearReply">×</button>
            </div>

            <!-- 文本输入区 -->
            <div class="dy-sheet-body">
              <textarea
                ref="textareaRef"
                v-model="text"
                class="dy-sheet-textarea"
                :placeholder="placeholder || '说点好听的~'"
                :maxlength="maxlength"
                rows="4"
                @input="handleInput"
                @keydown.meta.enter.prevent="handleSend"
                @keydown.ctrl.enter.prevent="handleSend"
              ></textarea>

              <!-- 已选图片预览 -->
              <div v-if="images.length" class="dy-sheet-images">
                <div
                  v-for="(img, idx) in images"
                  :key="idx"
                  class="dy-sheet-image-item"
                >
                  <img :src="img.url" :alt="`图片${idx + 1}`" />
                  <button
                    class="dy-sheet-image-remove"
                    type="button"
                    aria-label="删除"
                    @click="removeImage(idx)"
                  >×</button>
                </div>
              </div>
            </div>

            <!-- 底部工具栏 -->
            <div class="dy-sheet-toolbar">
              <button
                class="dy-sheet-tool"
                :class="{ 'dy-sheet-tool--active': showEmoji }"
                type="button"
                @click="toggleEmoji"
                aria-label="表情"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <path d="M8 14s1.5 2 4 2 4-2 4-2" />
                  <line x1="9" y1="9" x2="9.01" y2="9" />
                  <line x1="15" y1="9" x2="15.01" y2="9" />
                </svg>
              </button>
              <button
                class="dy-sheet-tool"
                type="button"
                @click="triggerImageUpload"
                aria-label="图片"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <polyline points="21 15 16 10 5 21" />
                </svg>
              </button>
              <span class="dy-sheet-count" :class="{ 'dy-sheet-count--limit': text.length > maxlength - 20 }">
                {{ text.length }} / {{ maxlength }}
              </span>
            </div>

            <!-- 表情面板（嵌入底部） -->
            <Transition name="dy-emoji">
              <div v-if="showEmoji" class="dy-emoji-panel">
                <div class="dy-emoji-grid">
                  <button
                    v-for="emoji in emojiList"
                    :key="emoji"
                    class="dy-emoji-btn"
                    type="button"
                    @click="insertEmoji(emoji)"
                  >{{ emoji }}</button>
                </div>
              </div>
            </Transition>

            <!-- 隐藏的文件上传 input -->
            <input
              ref="fileInputRef"
              type="file"
              accept="image/*"
              multiple
              class="dy-file-input"
              @change="onFileChange"
            />
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
/**
 * 抖音风格评论输入框
 * 设计要点：
 * 1. 基础态：玻璃态底部单行输入，无头像
 * 2. 展开态：底部弹起的全屏面板，含完整编辑器和工具栏
 * 3. 流畅的 spring 动画和视觉反馈
 */
import type { Comment } from '@/types'
import { sanitizeText } from '@/utils/sanitize'

interface ImageItem {
  url: string
  file?: File
  uploading?: boolean
}

const props = withDefaults(defineProps<{
  /** 占位符 */
  placeholder?: string
  /** 最大字符数 */
  maxlength?: number
  /** 回复目标 */
  replyTo?: Comment | null
  /** 是否正在发送 */
  sending?: boolean
  /** 图片上传函数（可由父组件注入） */
  uploader?: (file: File) => Promise<string>
}>(), {
  placeholder: '说点好听的~',
  maxlength: 500,
  replyTo: null,
  sending: false,
  uploader: undefined,
})

const emit = defineEmits<{
  /** 提交评论 */
  submit: [data: { content: string; images: string[]; parentId?: number; replyUserId?: number }]
  /** 清除回复状态 */
  clearReply: []
}>()

// 客户端挂载标记（Teleport 需要）
const isClient = ref(false)
onMounted(() => { isClient.value = true })

// 状态
const expanded = ref(false)
const text = ref('')
const images = ref<ImageItem[]>([])
const showEmoji = ref(false)
const isSending = computed(() => props.sending)

// refs
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

// 表情数据
const emojiList = [
  '😀','😃','😄','😁','😆','😅','🤣','😂','🙂','😉','😊','😇',
  '🥰','😍','🤩','😘','😗','😚','😙','😋','😛','😜','🤪','😝',
  '🤑','🤗','🤭','🤫','🤔','🤐','🤨','😐','😑','😶','😏','😒',
  '🙄','😬','🤥','😌','😔','😪','🤤','😴','😷','🤒','🤕','🤢',
  '👍','👎','👌','✌️','🤞','🤟','🤘','🤙','👈','👉','👆','👇',
  '✋','🤚','🖐','🖖','👋','🤝','🙏','💪','🦾','🫶','💖','💗',
  '💓','💞','💝','💘','💕','❤️','🧡','💛','💚','💙','💜','🤎',
  '🖤','🤍','💯','💢','💥','💫','💦','💨','🔥','✨','🌟','⭐',
  '🌈','☀️','🌤','⛅','🌥','☁️','🌦','🌧','⛈','🌩','🌨','❄️',
]

const canSend = computed(() => {
  return (text.value.trim().length > 0 || images.value.length > 0)
})

// ==================== 交互逻辑 ====================
function handleOpen() {
  expanded.value = true
  // 锁定 body 滚动
  document.body.style.overflow = 'hidden'
  nextTick(() => {
    textareaRef.value?.focus()
  })
}

function openWithEmoji() {
  handleOpen()
  nextTick(() => {
    showEmoji.value = true
  })
}

function openWithImage() {
  handleOpen()
  nextTick(() => {
    triggerImageUpload()
  })
}

function handleClose() {
  expanded.value = false
  showEmoji.value = false
  document.body.style.overflow = ''
}

function handleInput() {
  // input 事件由 v-model 处理
}

function handleClearReply() {
  emit('clearReply')
}

function toggleEmoji() {
  showEmoji.value = !showEmoji.value
  if (showEmoji.value) {
    nextTick(() => {
      // 让表情面板出现后再聚焦文本框
      textareaRef.value?.focus()
    })
  }
}

function insertEmoji(emoji: string) {
  const ta = textareaRef.value
  if (!ta) {
    text.value += emoji
    return
  }
  const start = ta.selectionStart ?? text.value.length
  const end = ta.selectionEnd ?? text.value.length
  const newVal = text.value.slice(0, start) + emoji + text.value.slice(end)
  text.value = newVal
  // 设置光标到插入位置之后
  nextTick(() => {
    const pos = start + emoji.length
    ta.focus()
    ta.setSelectionRange(pos, pos)
  })
}

function triggerImageUpload() {
  fileInputRef.value?.click()
}

async function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return

  for (const file of files) {
    if (!file.type.startsWith('image/')) continue
    if (file.size > 5 * 1024 * 1024) {
      ElMessage?.warning?.('图片大小不能超过 5MB')
      continue
    }
    // 立即生成本地预览
    const localUrl = URL.createObjectURL(file)
    const item: ImageItem = { url: localUrl, file, uploading: true }
    images.value.push(item)

    try {
      if (props.uploader) {
        // 使用父组件提供的上传器
        const remoteUrl = await props.uploader(file)
        item.url = remoteUrl
        item.uploading = false
      } else {
        // 调用 fileApi.uploadImage 实际上传到服务器
        try {
          const apiModule = await import('~/api').catch(() => null)
          const fileApi = (apiModule as any)?.fileApi
          if (fileApi?.uploadImage) {
            const fd = new FormData()
            fd.append('file', file)
            const res = await fileApi.uploadImage(fd)
            const remoteUrl = res?.data?.data || res?.data?.url || res?.data
            if (typeof remoteUrl === 'string' && remoteUrl) {
              item.url = remoteUrl
              item.uploading = false
            } else {
              // 服务端未返回有效 URL，移除该图片（本地 blob URL 无法跨页面持久化）
              const idx = images.value.indexOf(item)
              if (idx > -1) {
                URL.revokeObjectURL(localUrl)
                images.value.splice(idx, 1)
              }
              ElMessage?.error?.('图片上传失败：未返回有效URL')
            }
          } else {
            // 后端暂未提供 fileApi.uploadImage，移除该图片
            const idx = images.value.indexOf(item)
            if (idx > -1) {
              URL.revokeObjectURL(localUrl)
              images.value.splice(idx, 1)
            }
            ElMessage?.error?.('图片上传功能暂不可用')
          }
        } catch (err) {
          console.error('图片上传失败:', err)
          ElMessage?.error?.('图片上传失败')
          // 上传失败时移除该图片（避免将不可用的 blob URL 发送到服务器）
          const idx = images.value.indexOf(item)
          if (idx > -1) {
            URL.revokeObjectURL(localUrl)
            images.value.splice(idx, 1)
          }
        }
      }
    } catch (err) {
      console.error('图片处理失败:', err)
      ElMessage?.error?.('图片上传失败')
      // 移除失败项
      const idx = images.value.indexOf(item)
      if (idx > -1) images.value.splice(idx, 1)
    }
  }
  // 重置 input 以便重复选择同一文件
  input.value = ''
}

function removeImage(idx: number) {
  const item = images.value[idx]
  if (item?.file) {
    URL.revokeObjectURL(item.url)
  }
  images.value.splice(idx, 1)
}

async function handleSend() {
  if (!canSend.value || isSending.value) return
  // 净化文本内容
  const sanitized = sanitizeText(text.value.trim())
  // 收集已上传图片 URL
  const imageUrls = images.value
    .filter(i => !i.uploading)
    .map(i => i.url)
  emit('submit', {
    content: sanitized,
    images: imageUrls,
    parentId: props.replyTo?.id,
    replyUserId: props.replyTo?.userId,
  })
  // 重置状态
  text.value = ''
  images.value.forEach(i => i.file && URL.revokeObjectURL(i.url))
  images.value = []
  showEmoji.value = false
  expanded.value = false
  document.body.style.overflow = ''
}

// 组件卸载时清理
onUnmounted(() => {
  document.body.style.overflow = ''
  images.value.forEach(i => i.file && URL.revokeObjectURL(i.url))
})
</script>

<style scoped>
/* ==================== 基础态：底部输入条 ==================== */
.dy-input-bar {
  position: fixed;
  left: 0;
  right: 0;
  /* 紧贴移动端 tabbar 上方（tabbar 高度 60px + 安全区） */
  bottom: calc(60px + env(safe-area-inset-bottom, 0px));
  z-index: 60;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(24px) saturate(180%);
  -webkit-backdrop-filter: blur(24px) saturate(180%);
  border-top: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 -2px 16px rgba(15, 23, 42, 0.04);
  animation: dy-bar-in 0.32s cubic-bezier(0.16, 1, 0.3, 1);
}
:global(.dark) .dy-input-bar {
  background: rgba(15, 23, 42, 0.85);
  border-top-color: rgba(51, 65, 85, 0.6);
  box-shadow: 0 -2px 16px rgba(0, 0, 0, 0.3);
}
@keyframes dy-bar-in {
  from { transform: translateY(120%); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}

/* 桌面端：正常文档流，不固定 */
@media (min-width: 768px) {
  .dy-input-bar {
    position: sticky;
    bottom: auto;
    background: var(--zh-bg-elevated);
    backdrop-filter: none;
    -webkit-backdrop-filter: none;
    border-top: 1px solid var(--zh-border);
    border-radius: var(--zh-radius-lg);
    box-shadow: var(--zh-shadow-sm);
    margin-top: 12px;
    animation: none;
  }
  :global(.dark) .dy-input-bar {
    background: var(--zh-bg-elevated);
    border-top-color: var(--zh-border);
  }
}

.dy-input-bar-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  max-width: 920px;
  margin: 0 auto;
}

.dy-input-field {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  height: 40px;
  padding: 0 14px;
  background: rgba(241, 245, 249, 0.85);
  border-radius: 999px;
  border: 1px solid transparent;
  cursor: text;
  transition: background-color 200ms cubic-bezier(0.4, 0, 0.2, 1),
              border-color 200ms cubic-bezier(0.4, 0, 0.2, 1);
}
:global(.dark) .dy-input-field {
  background: rgba(51, 65, 85, 0.5);
}
.dy-input-field:active {
  background: rgba(241, 245, 249, 1);
  border-color: var(--zh-primary);
}
:global(.dark) .dy-input-field:active {
  background: rgba(51, 65, 85, 0.8);
}

.dy-input-placeholder,
.dy-input-reply-tag {
  font-size: 14px;
  color: var(--zh-text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dy-input-reply-tag {
  color: var(--zh-primary);
  font-weight: 500;
}

.dy-input-tools {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.dy-tool-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--zh-text-secondary);
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 150ms ease, color 150ms ease, transform 120ms ease;
}
.dy-tool-btn:active {
  background: var(--zh-bg-hover);
  transform: scale(0.92);
  color: var(--zh-primary);
}
.dy-tool-btn svg {
  width: 22px;
  height: 22px;
}

/* ==================== 展开态：全屏输入面板 ==================== */
.dy-sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(6px);
  -webkit-backdrop-filter: blur(6px);
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.dy-sheet {
  width: 100%;
  max-width: 640px;
  background: var(--zh-bg-elevated);
  border-radius: 18px 18px 0 0;
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  padding-bottom: env(safe-area-inset-bottom, 0px);
  position: relative;
  box-shadow: 0 -8px 32px rgba(15, 23, 42, 0.16);
}
:global(.dark) .dy-sheet {
  background: var(--zh-bg-elevated);
  box-shadow: 0 -8px 32px rgba(0, 0, 0, 0.5);
}
@media (min-width: 768px) {
  .dy-sheet {
    border-radius: 18px;
    margin-bottom: 8vh;
    max-height: 75vh;
  }
}

.dy-sheet-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--zh-border-light);
  flex-shrink: 0;
  position: relative;
}

.dy-sheet-drag {
  position: absolute;
  top: 6px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  pointer-events: none;
}
.dy-sheet-drag-bar {
  display: block;
  width: 36px;
  height: 4px;
  background: var(--zh-border);
  border-radius: 2px;
}

.dy-sheet-action {
  border: none;
  background: transparent;
  font-size: 14px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 999px;
  transition: background-color 150ms ease;
}
.dy-sheet-action--cancel {
  color: var(--zh-text-secondary);
}
.dy-sheet-action--cancel:active {
  background: var(--zh-bg-hover);
}
.dy-sheet-action--send {
  color: #fff;
  background: var(--zh-primary);
  font-weight: 600;
  min-width: 64px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  opacity: 0.5;
  pointer-events: none;
  transition: opacity 200ms ease, transform 120ms ease, background 200ms ease;
}
.dy-sheet-action--send.dy-sheet-action--ready {
  opacity: 1;
  pointer-events: auto;
  background: var(--zh-primary-gradient);
  box-shadow: 0 4px 16px rgba(var(--zh-primary-rgb), 0.35);
}
.dy-sheet-action--send.dy-sheet-action--ready:active {
  transform: scale(0.96);
}
.dy-sheet-action--send:disabled {
  cursor: not-allowed;
}

.dy-send-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.dy-sheet-reply-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: var(--zh-primary-bg);
  font-size: 13px;
  color: var(--zh-text-secondary);
  border-bottom: 1px solid var(--zh-border-light);
  flex-shrink: 0;
}
.dy-sheet-reply-label { color: var(--zh-text-tertiary); }
.dy-sheet-reply-user {
  color: var(--zh-primary);
  font-weight: 500;
  flex: 1;
}
.dy-sheet-reply-clear {
  border: none;
  background: transparent;
  color: var(--zh-text-tertiary);
  font-size: 20px;
  line-height: 1;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dy-sheet-reply-clear:active {
  background: rgba(0, 0, 0, 0.06);
}

.dy-sheet-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  -webkit-overflow-scrolling: touch;
}

.dy-sheet-textarea {
  width: 100%;
  min-height: 100px;
  border: none;
  outline: none;
  background: transparent;
  resize: none;
  font-size: 15px;
  line-height: 1.6;
  color: var(--zh-text);
  font-family: inherit;
}
.dy-sheet-textarea::placeholder {
  color: var(--zh-text-placeholder);
}

.dy-sheet-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
  gap: 8px;
  margin-top: 12px;
}
.dy-sheet-image-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  background: var(--zh-bg-hover);
}
.dy-sheet-image-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.dy-sheet-image-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  border: none;
  font-size: 14px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  backdrop-filter: blur(4px);
}

.dy-sheet-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-top: 1px solid var(--zh-border-light);
  flex-shrink: 0;
}
.dy-sheet-tool {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--zh-text-secondary);
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 150ms ease, color 150ms ease;
}
.dy-sheet-tool:active,
.dy-sheet-tool--active {
  background: var(--zh-primary-bg);
  color: var(--zh-primary);
}
.dy-sheet-tool svg {
  width: 22px;
  height: 22px;
}
.dy-sheet-count {
  margin-left: auto;
  font-size: 12px;
  color: var(--zh-text-tertiary);
  font-variant-numeric: tabular-nums;
  padding-right: 4px;
}
.dy-sheet-count--limit {
  color: var(--zh-warning);
}

.dy-emoji-panel {
  border-top: 1px solid var(--zh-border-light);
  background: var(--zh-bg);
  max-height: 220px;
  overflow-y: auto;
  flex-shrink: 0;
}
.dy-emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 2px;
  padding: 8px;
}
.dy-emoji-btn {
  width: 100%;
  aspect-ratio: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  background: transparent;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 120ms ease, transform 120ms ease;
}
.dy-emoji-btn:active {
  background: var(--zh-bg-hover);
  transform: scale(0.9);
}

.dy-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

/* ==================== 动画 ==================== */
.dy-sheet-enter-active,
.dy-sheet-leave-active {
  transition: opacity 240ms cubic-bezier(0.4, 0, 0.2, 1);
}
.dy-sheet-enter-active .dy-sheet,
.dy-sheet-leave-active .dy-sheet {
  transition: transform 320ms cubic-bezier(0.16, 1, 0.3, 1);
}
.dy-sheet-enter-from,
.dy-sheet-leave-to {
  opacity: 0;
}
.dy-sheet-enter-from .dy-sheet,
.dy-sheet-leave-to .dy-sheet {
  transform: translateY(100%);
}

.dy-emoji-enter-active,
.dy-emoji-leave-active {
  transition: opacity 200ms ease, transform 240ms cubic-bezier(0.16, 1, 0.3, 1);
}
.dy-emoji-enter-from,
.dy-emoji-leave-to {
  opacity: 0;
  transform: translateY(8px);
}
</style>