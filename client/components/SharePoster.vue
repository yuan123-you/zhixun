<template>
  <!-- 分享海报组件 -->
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50" @click.self="close">
        <div class="bg-white dark:bg-gray-800 rounded-xl shadow-2xl w-[340px] max-h-[90vh] overflow-y-auto">
          <!-- 海报预览区 -->
          <div ref="posterRef" class="p-6 bg-white">
            <!-- 封面图 -->
            <div v-if="article?.coverImage" class="mb-4 rounded-lg overflow-hidden">
              <img :src="article.coverImage" :alt="article.title" class="w-full h-40 object-cover" crossorigin="anonymous" />
            </div>

            <!-- 文章标题 -->
            <h3 class="text-lg font-bold text-gray-900 line-clamp-2 mb-2">
              {{ article?.title }}
            </h3>

            <!-- 文章摘要 -->
            <p class="text-sm text-gray-500 line-clamp-3 mb-4">
              {{ article?.summary }}
            </p>

            <!-- 作者信息 -->
            <div class="flex items-center space-x-3 mb-4">
              <img :src="article?.author?.avatar || article?.authorAvatar || '/default-avatar.png'" :alt="article?.author?.nickname || article?.authorName" class="w-10 h-10 rounded-full object-cover" crossorigin="anonymous" />
              <span class="text-sm font-medium text-gray-700">{{ article?.author?.nickname || article?.authorName }}</span>
            </div>

            <!-- 分割线 -->
            <div class="border-t border-gray-200 pt-4">
              <div class="flex items-center justify-between">
                <!-- 品牌信息 -->
                <div class="flex items-center space-x-2">
                  <div class="w-8 h-8 bg-primary rounded-lg flex items-center justify-center">
                    <span class="text-white text-xs font-bold">知</span>
                  </div>
                  <div>
                    <p class="text-xs font-semibold text-gray-800">知讯</p>
                    <p class="text-xs text-gray-400">优质内容平台</p>
                  </div>
                </div>
                <!-- 二维码 -->
                <div class="w-20 h-20">
                  <canvas ref="qrCanvasRef" class="w-full h-full"></canvas>
                </div>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="p-4 border-t border-gray-100 dark:border-gray-700 space-y-3">
            <button
              class="w-full btn btn-primary py-2.5 text-sm"
              :disabled="generating"
              @click="generatePoster"
            >
              {{ generating ? '生成中...' : '保存海报' }}
            </button>
            <button
              class="w-full btn btn-secondary py-2.5 text-sm"
              @click="close"
            >
              关闭
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/** 分享海报组件：使用 html2canvas 生成海报图片 */
import type { Article } from '~/types'

const props = defineProps<{
  visible: boolean
  article: Article | null
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'shared'): void
}>()

const posterRef = ref<HTMLElement | null>(null)
const qrCanvasRef = ref<HTMLCanvasElement | null>(null)
const generating = ref(false)

// 生成二维码
const generateQRCode = async () => {
  if (!qrCanvasRef.value || !props.article) return
  try {
    const QRCode = await import('qrcode')
    const url = `${window.location.origin}/articles/${props.article.id}`
    await QRCode.toCanvas(qrCanvasRef.value, url, {
      width: 80,
      margin: 1,
      color: {
        dark: '#1f2937',
        light: '#ffffff',
      },
    })
  } catch {
    // 二维码生成失败，静默处理
  }
}

// 生成海报图片并下载
const generatePoster = async () => {
  if (!posterRef.value || generating.value) return
  generating.value = true

  try {
    const html2canvas = (await import('html2canvas')).default
    const canvas = await html2canvas(posterRef.value, {
      scale: 2,
      useCORS: true,
      allowTaint: false,
      backgroundColor: '#ffffff',
    })

    // 下载海报
    const link = document.createElement('a')
    link.download = `知讯-${props.article?.title || '分享海报'}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()

    // 通知父组件分享完成
    emit('shared')
  } catch {
    // 海报生成失败
  } finally {
    generating.value = false
  }
}

// 关闭弹窗
const close = () => {
  emit('close')
}

// 监听弹窗显示，生成二维码
watch(() => props.visible, (val) => {
  if (val) {
    nextTick(() => {
      generateQRCode()
    })
  }
})
</script>
