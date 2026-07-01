<template>
  <div
    class="voice-msg"
    :class="{ 'voice-msg-mine': isMine, 'voice-msg-playing': isPlaying }"
    :style="{ width: barWidth }"
    @click="togglePlay"
  >
    <!-- 自己发的语音：时长在左，图标在右（QQ风格） -->
    <template v-if="isMine">
      <span class="voice-duration">{{ formatDuration(displayDuration) }}</span>
      <div class="voice-icon">
        <svg v-if="isPlaying" class="voice-wave" viewBox="0 0 24 24" fill="currentColor">
          <rect class="wave-bar wave-bar-1" x="3" y="8" width="2.5" height="8" rx="1.25" />
          <rect class="wave-bar wave-bar-2" x="7.5" y="5" width="2.5" height="14" rx="1.25" />
          <rect class="wave-bar wave-bar-3" x="12" y="7" width="2.5" height="10" rx="1.25" />
          <rect class="wave-bar wave-bar-4" x="16.5" y="9" width="2.5" height="6" rx="1.25" />
        </svg>
        <svg v-else class="voice-play-icon" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8 5v14l11-7z"/>
        </svg>
      </div>
    </template>
    <!-- 对方语音：图标在左，时长在右 -->
    <template v-else>
      <div class="voice-icon">
        <svg v-if="isPlaying" class="voice-wave" viewBox="0 0 24 24" fill="currentColor">
          <rect class="wave-bar wave-bar-1" x="3" y="8" width="2.5" height="8" rx="1.25" />
          <rect class="wave-bar wave-bar-2" x="7.5" y="5" width="2.5" height="14" rx="1.25" />
          <rect class="wave-bar wave-bar-3" x="12" y="7" width="2.5" height="10" rx="1.25" />
          <rect class="wave-bar wave-bar-4" x="16.5" y="9" width="2.5" height="6" rx="1.25" />
        </svg>
        <svg v-else class="voice-play-icon" viewBox="0 0 24 24" fill="currentColor">
          <path d="M8 5v14l11-7z"/>
        </svg>
      </div>
      <span class="voice-duration">{{ formatDuration(displayDuration) }}</span>
    </template>
    <!-- 隐藏的 audio 元素 -->
    <audio ref="audioRef" :src="resolvedUrl" preload="metadata" @ended="onEnded" @loadedmetadata="onMetaLoaded" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onBeforeUnmount, watch } from 'vue'

const props = withDefaults(defineProps<{
  url: string
  duration?: number
  isMine?: boolean
}>(), {
  duration: 0,
  isMine: false,
})

const { resolveUrl } = useResourceUrl()

const audioRef = ref<HTMLAudioElement | null>(null)
const isPlaying = ref(false)
const metaDuration = ref(0)

/**
 * 解析语音 URL：
 * 1. 如果 props.url 是 JSON 字符串（历史脏数据），先尝试提取其中的 url 字段
 * 2. 通过 resolveUrl 统一转换 MinIO / 相对路径
 * 3. 最终校验：URL 不能包含 { } 等非法字符，否则返回空字符串避免 audio 加载畸形 URL
 */
const resolvedUrl = computed(() => {
  let raw = props.url
  if (!raw) return ''
  // 兼容历史脏数据：props.url 本身是 JSON 字符串
  if (raw.startsWith('{')) {
    try {
      const parsed = JSON.parse(raw)
      raw = parsed.url || raw
    } catch { /* 解析失败保留原值，后续校验会拦截 */ }
  }
  const resolved = resolveUrl(raw) || raw
  // 校验：合法 URL 不应包含花括号（JSON 残留）
  if (typeof resolved !== 'string' || resolved.includes('{') || resolved.includes('}')) {
    return ''
  }
  return resolved
})
const displayDuration = computed(() => props.duration || metaDuration.value || 0)

/** 根据语音时长计算语音条宽度（类似QQ：短语音窄，长语音宽） */
const barWidth = computed(() => {
  const dur = displayDuration.value
  if (!dur || dur <= 0) return '90px'
  // 1秒≈80px, 最短90px, 最长220px
  const minW = 90
  const maxW = 220
  const w = Math.min(maxW, Math.max(minW, minW + (dur - 1) * 12))
  return w + 'px'
})

/** 格式化秒数为 mm:ss */
function formatDuration(seconds: number): string {
  if (!seconds || seconds < 0) return "0''"
  const s = Math.round(seconds)
  if (s < 60) return `${s}''`
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}'${sec.toString().padStart(2, '0')}''`
}

function onMetaLoaded() {
  if (audioRef.value && !props.duration) {
    metaDuration.value = audioRef.value.duration || 0
  }
}

function togglePlay() {
  const audio = audioRef.value
  if (!audio || !resolvedUrl.value) return
  if (isPlaying.value) {
    audio.pause()
    audio.currentTime = 0
    isPlaying.value = false
  } else {
    audio.play().then(() => {
      isPlaying.value = true
    }).catch((err) => {
      console.warn('[VoiceMessage] 播放失败:', err)
    })
  }
}

function onEnded() {
  isPlaying.value = false
}

// 组件卸载时停止播放
onBeforeUnmount(() => {
  if (audioRef.value) {
    audioRef.value.pause()
    audioRef.value = null
  }
})

// URL 变化时停止播放
watch(() => props.url, () => {
  if (audioRef.value) {
    audioRef.value.pause()
    isPlaying.value = false
  }
})
</script>

<style scoped>
.voice-msg {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px 8px 12px;
  border-radius: 18px;
  cursor: pointer;
  transition: background 0.15s, opacity 0.15s;
  user-select: none;
  -webkit-user-select: none;
}
.voice-msg:hover {
  opacity: 0.85;
}
.voice-msg:active {
  opacity: 0.7;
}

.voice-icon {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.voice-play-icon {
  width: 18px;
  height: 18px;
}

.voice-wave {
  width: 22px;
  height: 22px;
}

.wave-bar {
  animation: waveAnim 0.8s ease-in-out infinite;
  transform-origin: center;
}
.wave-bar-1 { animation-delay: 0s; }
.wave-bar-2 { animation-delay: 0.15s; }
.wave-bar-3 { animation-delay: 0.3s; }
.wave-bar-4 { animation-delay: 0.45s; }

@keyframes waveAnim {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1); }
}

.voice-duration {
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  line-height: 1;
}

/* 语音条文字色：统一深色（适配 .chat-bubble-voice 浅灰背景） - 2026-07-02 v7 */
.voice-msg .voice-duration {
  color: #1e293b;
}
.voice-msg .voice-play-icon,
.voice-msg .wave-bar {
  color: #64748b;
  fill: #64748b;
}

/* 播放中 */
.voice-msg-playing .voice-play-icon {
  display: none;
}
</style>
