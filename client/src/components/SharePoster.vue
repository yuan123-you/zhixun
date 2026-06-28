<template>
  <!-- 分享海报组件 -->
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="poster-overlay" @click.self="close">
        <div class="poster-dialog">
          <div ref="posterRef" class="poster-body">
            <div v-if="article?.coverImage" class="poster-cover">
              <img :src="resolveUrl(article?.coverImage) || ''" :alt="article?.title" crossorigin="anonymous" />
            </div>
            <h3 class="poster-title">{{ article?.title }}</h3>
            <p class="poster-content">{{ posterContent }}</p>
            <div class="poster-author">
              <UserAvatar :src="article?.author?.avatar || article?.authorAvatar" :alt="article?.author?.nickname || article?.authorName" size="md" />
              <span>{{ article?.author?.nickname || article?.authorName }}</span>
            </div>
            <div class="poster-footer">
              <div class="poster-brand">
                <div class="brand-logo">知</div>
                <div>
                  <p>知讯</p>
                  <span>优质内容平台</span>
                </div>
              </div>
              <div class="poster-qr">
                <canvas ref="qrCanvasRef" />
              </div>
            </div>
          </div>
          <div class="poster-actions">
            <el-button type="primary" :loading="generating" @click="generatePoster">
              {{ generating ? '生成中...' : '保存海报' }}
            </el-button>
            <el-button @click="close">关闭</el-button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import type { Article } from '@/types'

const props = defineProps<{ visible: boolean; article: Article | null }>()
const emit = defineEmits<{ 'close': []; 'shared': [] }>()

const { resolveUrl } = useResourceUrl()
const posterRef = ref<HTMLElement | null>(null)
const qrCanvasRef = ref<HTMLCanvasElement | null>(null)
const generating = ref(false)

const stripHtml = (html: string) => html?.replace(/<[^>]*>/g, '') || ''
const posterContent = computed(() => {
  const content = props.article?.content
  return content ? stripHtml(content) : (props.article?.summary || '')
})

const generateQRCode = async () => {
  if (!qrCanvasRef.value || !props.article) return
  try {
    const QRCode = await import('qrcode')
    await QRCode.toCanvas(qrCanvasRef.value, `${window.location.origin}/articles/${props.article.id}`, {
      width: 80, margin: 1, color: { dark: '#1f2937', light: '#ffffff' },
    })
  } catch { /* ignore */ }
}

const generatePoster = async () => {
  if (!posterRef.value || generating.value) return
  generating.value = true
  try {
    const html2canvas = (await import('html2canvas')).default
    const canvas = await html2canvas(posterRef.value, { scale: 2, useCORS: true, allowTaint: false, backgroundColor: '#ffffff' })
    const link = document.createElement('a')
    link.download = `知讯-${props.article?.title || '分享海报'}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
    emit('shared')
  } catch { /* ignore */ }
  generating.value = false
}

const close = () => emit('close')

watch(() => props.visible, (val) => { if (val) nextTick(() => generateQRCode()) })
</script>

<style scoped>
.poster-overlay {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0,0,0,0.5);
}
.poster-dialog {
  background: var(--el-bg-color);
  border-radius: 16px;
  box-shadow: var(--el-box-shadow-dark);
  width: 340px;
  max-height: 90vh;
  overflow-y: auto;
}
.poster-body { padding: 16px; background: #fff; }
.poster-cover { border-radius: 8px; overflow: hidden; margin-bottom: 12px; }
.poster-cover img { width: 100%; height: 160px; object-fit: cover; }
.poster-title { font-size: 18px; font-weight: 700; color: var(--el-text-color-primary); margin-bottom: 8px; overflow: hidden; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.poster-content { font-size: 14px; color: var(--el-text-color-secondary); margin-bottom: 16px; overflow: hidden; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 3; }
.poster-author { display: flex; align-items: center; gap: 8px; margin-bottom: 12px; }
.poster-author span { font-size: 14px; font-weight: 500; color: var(--el-text-color-primary); }
.poster-footer { display: flex; align-items: center; justify-content: space-between; }
.poster-brand { display: flex; align-items: center; gap: 8px; }
.brand-logo {
  width: 32px; height: 32px; background: var(--el-color-primary); border-radius: 8px;
  display: flex; align-items: center; justify-content: center; color: #fff; font-size: 12px; font-weight: 700;
}
.poster-brand p { font-size: 12px; font-weight: 600; color: var(--el-text-color-primary); }
.poster-brand span { font-size: 10px; color: var(--el-text-color-placeholder); }
.poster-qr { width: 80px; height: 80px; }
.poster-qr canvas { width: 100%; height: 100%; border-radius: 4px; }
.poster-actions {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.poster-actions .el-button { height: 44px; border-radius: 12px; }
.fade-enter-active,
.fade-leave-active { transition: opacity 0.25s ease; }
.fade-enter-from,
.fade-leave-to { opacity: 0; }
</style>
