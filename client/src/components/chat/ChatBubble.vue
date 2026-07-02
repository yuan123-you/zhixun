<!-- ChatBubble 2026-07-02-v9: 强制 chunk hash 变，刷新用户浏览器缓存 --><template>
  <div class="chat-bubble-wrap">
    <!-- 调试标识：直接在页面上显示版本号，方便用户确认是否加载到最新代码 -->
    <div data-chatbubble-version="v10" style="font-size:9px;color:#ef4444;font-weight:700;padding:1px 4px;background:#fef2f2;border-radius:3px;align-self:flex-start;margin-bottom:2px">ChatBubble v10 ✓</div>

    <!-- 群聊发送者名称（可选） -->
    <span v-if="showSender && senderName" class="bubble-sender-name" :class="{ 'bubble-sender-name-mine': isMine }">
      {{ senderName }}
    </span>

    <!-- AI回复气泡 -->
    <div v-if="messageType === 'ai_reply'" class="chat-bubble chat-bubble-ai" :class="isMine ? 'chat-bubble-mine' : 'chat-bubble-other'">
      <span class="ai-label">AI助手</span>
      <p class="ai-text">{{ content }}</p>
    </div>

    <!-- 图片消息 -->
    <div v-else-if="messageType === 'image'" class="chat-bubble chat-bubble-image" @click="$emit('preview-image', content)">
      <img :src="resolvedUrl" alt="图片" class="chat-msg-image" loading="lazy" />
    </div>

    <!-- 文件消息 -->
    <div v-else-if="messageType === 'file'" class="chat-bubble chat-bubble-file" :class="isMine ? 'chat-bubble-mine' : 'chat-bubble-other'">
      <FileCard :content="content" @preview="onFilePreview" />
    </div>

    <!-- 语音消息 -->
    <div v-else-if="messageType === 'voice'" class="chat-bubble chat-bubble-voice" :class="isMine ? 'chat-bubble-mine' : 'chat-bubble-other'">
      <VoiceMessage :url="voiceUrl" :duration="voiceDuration" :is-mine="isMine" />
    </div>

    <!-- 文本消息（默认） -->
    <div v-else class="chat-bubble" :class="isMine ? 'chat-bubble-mine' : 'chat-bubble-other'">
      <slot>{{ content }}</slot>
    </div>

    <!-- 时间戳 -->
    <span v-if="time" class="bubble-time" :class="{ 'bubble-time-mine': isMine }">{{ time }}</span>

    <!-- 文件预览弹窗 -->
    <FilePreviewDialog
      :visible="filePreviewVisible"
      :file-url="filePreviewUrl"
      :file-name="filePreviewName"
      :file-ext="filePreviewExt"
      @close="filePreviewVisible = false"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * ChatBubble - 消息气泡组件（私信/群聊共用）
 * 统一 QQ 蓝 #498FE8 气泡样式、文件/图片/语音/AI 渲染
 */
import { computed, ref } from 'vue'
import FileCard from './FileCard.vue'
import VoiceMessage from '@/components/VoiceMessage.vue'
import FilePreviewDialog from './FilePreviewDialog.vue'
import { useChatMedia } from '@/composables/chat/useChatMedia'

const props = defineProps<{
  content: string
  messageType: string
  isMine: boolean
  time?: string
  showSender?: boolean
  senderName?: string
  resolveUrl?: (url: string) => string
}>()

defineEmits<{
  'preview-image': [url: string]
}>()

// 调试标识：v9 — 强制 chunk hash 变以刷新用户浏览器缓存
if (typeof window !== 'undefined') {
  ;(window as any).__CHATBUBBLE_VERSION__ = 'v9'
}

const { resolveMsgUrl, getVoiceUrl, getVoiceDuration } = useChatMedia()

const resolvedUrl = computed(() => {
  if (props.resolveUrl) return props.resolveUrl(props.content)
  return resolveMsgUrl(props.content)
})

const voiceUrl = computed(() => getVoiceUrl(props.content))
const voiceDuration = computed(() => getVoiceDuration(props.content))

// 文件预览状态
const filePreviewVisible = ref(false)
const filePreviewUrl = ref('')
const filePreviewName = ref('')
const filePreviewExt = ref('')

const onFilePreview = (url: string, name: string, ext: string) => {
  filePreviewUrl.value = url
  filePreviewName.value = name
  filePreviewExt.value = ext
  filePreviewVisible.value = true
}
</script>

<style scoped>
.chat-bubble-wrap {
  max-width: 75%;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* 发送者名称（群聊） */
.bubble-sender-name {
  font-size: 11px;
  color: var(--zh-text-tertiary, #94a3b8);
  padding-left: 2px;
}
.bubble-sender-name-mine {
  text-align: right;
  padding-right: 2px;
  padding-left: 0;
}

/* 基础气泡 */
.chat-bubble {
  padding: 10px 14px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.5;
  word-break: break-word;
  white-space: pre-wrap;
}

/* 对方气泡 - QQ蓝 */
.chat-bubble-other {
  background: #498FE8;
  color: #fff;
  border-bottom-left-radius: 6px;
}

/* 自己气泡 - 白色 */
.chat-bubble-mine {
  background: #fff;
  color: #1e293b;
  border: 1px solid #e2e8f0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  border-bottom-right-radius: 6px;
}

/* 时间戳 */
.bubble-time {
  display: block;
  font-size: 10px;
  color: var(--zh-text-placeholder, #94a3b8);
  margin-top: 4px;
}
.bubble-time-mine {
  text-align: right;
}

/* 图片消息 */
.chat-bubble-image {
  padding: 4px !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  cursor: pointer;
}
.chat-msg-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 12px;
  object-fit: cover;
  display: block;
  transition: transform 0.15s ease;
}
.chat-msg-image:hover {
  transform: scale(1.02);
}

/* AI回复气泡 */
.chat-bubble-ai {
  border-left: 3px solid var(--zh-primary, #6366f1) !important;
  background: rgba(99, 102, 241, 0.06) !important;
  color: var(--zh-text, #1e293b) !important;
}
.ai-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: var(--zh-primary, #6366f1);
  margin-bottom: 4px;
}
.ai-text {
  white-space: pre-wrap;
  line-height: 1.6;
  margin: 0;
}

/* 文件消息 */
.chat-bubble-file {
  padding: 4px 6px !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
}

/* 语音消息 - 2026-07-02 v7: 独立背景，修复自己发的语音在白底上看不见 */
.chat-bubble-voice {
  padding: 4px 6px !important;
  min-width: 100px;
  background: #f1f5f9 !important;
  color: #1e293b !important;
  border: 1px solid #e2e8f0 !important;
  box-shadow: none !important;
}
.chat-bubble-voice.chat-bubble-other {
  background: #f1f5f9 !important;
  color: #1e293b !important;
  border: 1px solid #e2e8f0 !important;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .chat-bubble {
    font-size: 13.5px;
    padding: 8px 12px;
  }
  .chat-msg-image {
    max-width: 160px;
    max-height: 160px;
  }
}
</style>
