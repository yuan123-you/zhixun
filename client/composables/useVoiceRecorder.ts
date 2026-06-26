import { ref } from 'vue'

export function useVoiceRecorder() {
  const isRecording = ref(false)
  const recordingTime = ref(0)
  const audioBlob = ref<Blob | null>(null)
  const audioUrl = ref<string | null>(null)
  let mediaRecorder: MediaRecorder | null = null
  let timer: ReturnType<typeof setInterval> | null = null
  let chunks: Blob[] = []

  async function startRecording() {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm' })
      chunks = []
      mediaRecorder.ondataavailable = (e) => { if (e.data.size > 0) chunks.push(e.data) }
      mediaRecorder.onstop = () => {
        audioBlob.value = new Blob(chunks, { type: 'audio/webm' })
        audioUrl.value = URL.createObjectURL(audioBlob.value)
        stream.getTracks().forEach(t => t.stop())
      }
      mediaRecorder.start()
      isRecording.value = true
      recordingTime.value = 0
      timer = setInterval(() => { recordingTime.value++ }, 1000)
    } catch (e) {
      console.error('Microphone access denied:', e)
    }
  }

  function stopRecording() {
    if (mediaRecorder && isRecording.value) {
      mediaRecorder.stop()
      isRecording.value = false
      if (timer) { clearInterval(timer); timer = null }
    }
  }

  function cancelRecording() {
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