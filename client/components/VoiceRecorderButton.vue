<template>
  <div class="inline-flex items-center">
    <button v-if="!recorder.isRecording.value && !recorder.audioBlob.value"
      @click="recorder.startRecording()"
      class="p-2 text-gray-400 hover:text-red-500 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition"
      title="语音消息">
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"/></svg>
    </button>
    <div v-else-if="recorder.isRecording.value" class="flex items-center gap-2">
      <span class="w-2 h-2 bg-red-500 rounded-full animate-pulse"></span>
      <span class="text-sm text-gray-600 dark:text-gray-300">{{ recorder.formatTime(recorder.recordingTime.value) }}</span>
      <button @click="recorder.stopRecording()" class="px-2 py-1 bg-red-600 text-white rounded text-xs">完成</button>
      <button @click="recorder.cancelRecording()" class="px-2 py-1 bg-gray-400 text-white rounded text-xs">取消</button>
    </div>
    <div v-else class="flex items-center gap-2">
      <audio :src="recorder.audioUrl.value!" controls class="h-8 max-w-[120px]"></audio>
      <button @click="sendVoice" class="px-2 py-1 bg-blue-600 text-white rounded text-xs">发送</button>
      <button @click="cancelVoice" class="px-2 py-1 bg-gray-300 text-gray-600 rounded text-xs">取消</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useVoiceRecorder } from '~/composables/useVoiceRecorder'

const emit = defineEmits(['send'])

const recorder = useVoiceRecorder()

function sendVoice() {
  if (recorder.audioBlob.value) {
    emit('send', recorder.audioBlob.value)
    cancelVoice()
  }
}

function cancelVoice() {
  if (recorder.audioUrl.value) URL.revokeObjectURL(recorder.audioUrl.value)
  recorder.audioBlob.value = null
  recorder.audioUrl.value = null
  recorder.recordingTime.value = 0
}
</script>
