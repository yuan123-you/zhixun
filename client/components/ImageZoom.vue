<template>
  <!-- 图片缩放组件 -->
  <Teleport to="body">
    <Transition name="zoom-fade">
      <div
        v-if="visible"
        class="fixed inset-0 z-[100] bg-black/90 flex items-center justify-center overflow-hidden"
        @click.self="close"
        @touchstart.passive="onTouchStart"
        @touchmove.prevent="onTouchMove"
        @touchend.passive="onTouchEnd"
        @dblclick="resetZoom"
      >
        <!-- 关闭按钮 -->
        <button
          class="absolute top-4 right-4 z-10 w-10 h-10 rounded-full bg-white/10 text-white flex items-center justify-center hover:bg-white/20 transition-colors"
          @click="close"
        >
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>

        <!-- 缩放提示 -->
        <div v-if="scale > 1" class="absolute bottom-4 left-1/2 -translate-x-1/2 text-white/60 text-sm">
          {{ Math.round(scale * 100) }}% · 双击恢复
        </div>

        <!-- 图片 -->
        <img
          ref="imageRef"
          :src="src"
          :alt="alt || ''"
          :style="{
            transform: `translate(${offsetX}px, ${offsetY}px) scale(${scale})`,
            transition: isDragging ? 'none' : 'transform 0.3s ease',
            transformOrigin: 'center center',
            maxWidth: '90vw',
            maxHeight: '90vh',
            objectFit: 'contain',
          }"
          draggable="false"
        />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
/** 图片缩放组件：双指缩放、双击恢复、拖拽平移 */

const props = withDefaults(defineProps<{
  /** 是否显示 */
  visible: boolean
  /** 图片地址 */
  src: string
  /** 图片描述 */
  alt?: string
}>(), {
  alt: '',
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const MAX_SCALE = 3
const MIN_SCALE = 1

const imageRef = ref<HTMLImageElement>()
const scale = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
const isDragging = ref(false)

// 双指缩放相关
let initialDistance = 0
let initialScale = 1

// 拖拽相关
let lastX = 0
let lastY = 0

// 双击检测
let lastTapTime = 0
const DOUBLE_TAP_DELAY = 300

const close = () => {
  emit('update:visible', false)
}

const resetZoom = () => {
  scale.value = 1
  offsetX.value = 0
  offsetY.value = 0
}

// 计算双指距离
const getDistance = (touches: TouchList): number => {
  const dx = touches[0].clientX - touches[1].clientX
  const dy = touches[0].clientY - touches[1].clientY
  return Math.sqrt(dx * dx + dy * dy)
}

const onTouchStart = (e: TouchEvent) => {
  // 双击检测
  const now = Date.now()
  if (now - lastTapTime < DOUBLE_TAP_DELAY && e.touches.length === 1) {
    resetZoom()
    lastTapTime = 0
    return
  }
  lastTapTime = now

  if (e.touches.length === 2) {
    // 双指开始
    initialDistance = getDistance(e.touches)
    initialScale = scale.value
    isDragging.value = false
  } else if (e.touches.length === 1) {
    // 单指拖拽（仅缩放状态下）
    if (scale.value > 1) {
      isDragging.value = true
      lastX = e.touches[0].clientX
      lastY = e.touches[0].clientY
    }
  }
}

const onTouchMove = (e: TouchEvent) => {
  if (e.touches.length === 2) {
    // 双指缩放
    const currentDistance = getDistance(e.touches)
    const newScale = initialScale * (currentDistance / initialDistance)
    scale.value = Math.min(MAX_SCALE, Math.max(MIN_SCALE, newScale))
  } else if (e.touches.length === 1 && isDragging.value && scale.value > 1) {
    // 单指平移
    const currentX = e.touches[0].clientX
    const currentY = e.touches[0].clientY
    offsetX.value += currentX - lastX
    offsetY.value += currentY - lastY
    lastX = currentX
    lastY = currentY
  }
}

const onTouchEnd = () => {
  isDragging.value = false
  initialDistance = 0
  initialScale = 1
}

// 监听 visible 变化，重置状态
watch(() => props.visible, (val) => {
  if (!val) {
    resetZoom()
  }
})
</script>

<style scoped>
.zoom-fade-enter-active,
.zoom-fade-leave-active {
  transition: opacity 0.25s ease;
}
.zoom-fade-enter-from,
.zoom-fade-leave-to {
  opacity: 0;
}
</style>
