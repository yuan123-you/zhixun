<template>
  <!-- 语音录制按钮 -->
  <div class="voice-btn-wrap">
    <el-button v-if="!recorder.isRecording.value && !recorder.audioBlob.value" text title="语音消息" @click="handleStartRecording">
      <el-icon :size="20"><Microphone /></el-icon>
    </el-button>
    <div v-else-if="recorder.isRecording.value" class="voice-recording">
      <span class="recording-dot" />
      <span class="recording-time">{{ recorder.formatTime(recorder.recordingTime.value) }}</span>
      <el-button size="small" type="danger" @click="recorder.stopRecording()">完成</el-button>
      <el-button size="small" @click="recorder.cancelRecording()">取消</el-button>
    </div>
    <div v-else class="voice-preview">
      <audio :src="recorder.audioUrl.value!" controls />
      <el-button size="small" type="primary" @click="sendVoice">发送</el-button>
      <el-button size="small" @click="cancelVoice">取消</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Microphone } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useVoiceRecorder } from '@/composables/useVoiceRecorder'

const emit = defineEmits(['send'])
const recorder = useVoiceRecorder()

async function handleStartRecording() {
  await recorder.startRecording()
  if (recorder.recordError.value) {
    ElMessage.error(recorder.recordError.value)
  }
}

function sendVoice() {
  if (recorder.audioBlob.value) {
    emit('send', { blob: recorder.audioBlob.value, duration: recorder.recordingTime.value })
    cancelVoice()
  }
}

function cancelVoice() {
  if (recorder.audioUrl.value) URL.revokeObjectURL(recorder.audioUrl.value)
  recorder.audioBlob.value = null; recorder.audioUrl.value = null; recorder.recordingTime.value = 0
}
</script>

<style scoped>
.voice-btn-wrap { display: inline-flex; align-items: center; }
.voice-recording { display: flex; align-items: center; gap: 8px; }
.recording-dot {
  width: 8px; height: 8px; background: var(--el-color-danger); border-radius: 50%;
  animation: pulse 1s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.recording-time { font-size: 14px; color: var(--el-text-color-secondary); }
.voice-preview { display: flex; align-items: center; gap: 8px; }
.voice-preview audio { height: 32px; max-width: 120px; }
</style>
