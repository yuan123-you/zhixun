import { ref } from 'vue'

export function useVoiceRecorder() {
  const isRecording = ref(false)
  const recordingTime = ref(0)
  const audioBlob = ref<Blob | null>(null)
  const audioUrl = ref<string | null>(null)
  const recordError = ref('')
  let mediaRecorder: MediaRecorder | null = null
  let timer: ReturnType<typeof setInterval> | null = null
  let chunks: Blob[] = []
  let stopResolve: ((blob: Blob | null) => void) | null = null

  async function startRecording() {
    recordError.value = ''
    try {
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        recordError.value = '当前浏览器不支持语音录制，请使用 Chrome 或 Edge 浏览器'
        console.error('[VoiceRecorder] MediaDevices API 不可用')
        return
      }
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
      chunks = []
      audioBlob.value = null
      audioUrl.value = null
      mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) chunks.push(e.data) }
      mediaRecorder.onstop = () => {
        const blob = new Blob(chunks, { type: 'audio/webm' })
        audioBlob.value = blob
        audioUrl.value = URL.createObjectURL(blob)
        stream.getTracks().forEach(t => t.stop())
        // Resolve the pending stopRecording() promise
        if (stopResolve) { stopResolve(blob); stopResolve = null }
      }
      mediaRecorder.start()
      isRecording.value = true
      recordingTime.value = 0
      timer = setInterval(() => { recordingTime.value++ }, 1000)
    } catch (e: any) {
      if (e?.name === 'NotAllowedError' || e?.name === 'PermissionDeniedError') {
        recordError.value = '麦克风权限被拒绝，请在浏览器设置中允许访问麦克风'
      } else if (e?.name === 'NotFoundError') {
        recordError.value = '未检测到麦克风设备'
      } else if (e?.name === 'NotReadableError') {
        recordError.value = '麦克风被其他程序占用，请关闭后重试'
      } else {
        recordError.value = '语音录制启动失败，请稍后重试'
      }
      console.error('[VoiceRecorder] 启动录制失败:', e?.name || e)
    }
  }

  /** 停止录制并返回音频 Blob（Promise 在 onstop 回调触发后才 resolve） */
  function stopRecording(): Promise<Blob | null> {
    return new Promise((resolve) => {
      if (mediaRecorder && isRecording.value) {
        stopResolve = resolve
        mediaRecorder.stop()
        isRecording.value = false
        if (timer) { clearInterval(timer); timer = null }
      } else {
        resolve(null)
      }
    })
  }

  function cancelRecording() {
    stopResolve = null // discard pending resolve
    if (mediaRecorder && isRecording.value) {
      mediaRecorder.stop()
      isRecording.value = false
      audioBlob.value = null
      audioUrl.value = null
      recordingTime.value = 0
      if (timer) { clearInterval(timer); timer = null }
    }
  }

  function formatTime(seconds: number): string {
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  }

  return {
    isRecording, recordingTime, audioBlob, audioUrl, recordError,
    startRecording, stopRecording, cancelRecording, formatTime,
  }
}