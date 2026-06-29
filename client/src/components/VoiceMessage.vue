<template>
  <div
    class="voice-msg"
    :class="{ 'voice-msg-mine': isMine, 'voice-msg-playing': isPlaying }"
    @click="togglePlay"
  >
    <!-- 喇叭/播放图标 -->
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
    <!-- 语音时长 -->
    <span class="voice-duration">{{ formatDuration(displayDuration) }}</span>
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

const resolvedUrl = computed(() => resolveUrl(props.url) || props.url)
const displayDuration = computed(() => props.duration || metaDuration.value || 0)

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
  if (!audio) return
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
  min-width: 90px;
  max-width: 220px;
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

/* 自己发的语音 - 白色文字 */
.voice-msg-mine .voice-duration {
  color: rgba(255, 255, 255, 0.9);
}
.voice-msg-mine .voice-play-icon,
.voice-msg-mine .wave-bar {
  color: rgba(255, 255, 255, 0.95);
  fill: rgba(255, 255, 255, 0.95);
}

/* 别人的语音 - 深色文字 */
.voice-msg:not(.voice-msg-mine) .voice-duration {
  color: var(--zh-text, #1e293b);
}
.voice-msg:not(.voice-msg-mine) .voice-play-icon,
.voice-msg:not(.voice-msg-mine) .wave-bar {
  color: var(--zh-text-secondary, #64748b);
  fill: var(--zh-text-secondary, #64748b);
}

/* 播放中 */
.voice-msg-playing .voice-play-icon {
  display: none;
}
</style>
