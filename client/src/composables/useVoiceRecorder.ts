import { ref } from 'vue'

export function useVoiceRecorder() {
  const isRecording = ref(false)
  const recordingTime = ref(0)
  const audioBlob = ref<Blob | null>(null)
  const audioUrl = ref<string | null>(null)
  let mediaRecorder: MediaRecorder | null = null
  let timer: ReturnType<typeof setInterval> | null = null
  let chunks: Blob[] = []
  let stopResolve: ((blob: Blob | null) => void) | null = null

  async function startRecording() {
    try {
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
    } catch (e) {
      console.error('麦克风权限未开启，请在浏览器设置中允许访问麦克风')
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
    isRecording, recordingTime, audioBlob, audioUrl,
    startRecording, stopRecording, cancelRecording, formatTime,
  }
}