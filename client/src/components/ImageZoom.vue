<template>
  <!-- 图片缩放组件 -->
  <Teleport to="body">
    <Transition name="zoom-fade">
      <div v-if="visible" class="zoom-overlay" @click.self="close"
        @touchstart.passive="onTouchStart" @touchmove="onTouchMove" @touchend.passive="onTouchEnd" @dblclick="resetZoom">
        <button class="zoom-close" @click="close">
          <el-icon :size="24"><Close /></el-icon>
        </button>
        <div v-if="scale > 1" class="zoom-hint">{{ Math.round(scale * 100) }}% · 双击恢复</div>
        <img ref="imageRef" :src="src" :alt="alt || ''" :style="imgStyle" draggable="false" />
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { Close } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{ visible: boolean; src: string; alt?: string }>(), { alt: '' })
const emit = defineEmits<{ 'update:visible': [value: boolean] }>()

const MAX_SCALE = 3; const MIN_SCALE = 1
const imageRef = ref<HTMLImageElement>()
const scale = ref(1); const offsetX = ref(0); const offsetY = ref(0); const isDragging = ref(false)
let initialDistance = 0; let initialScale = 1; let lastX = 0; let lastY = 0; let lastTapTime = 0
const DOUBLE_TAP_DELAY = 300

const imgStyle = computed(() => ({
  transform: `translate(${offsetX.value}px, ${offsetY.value}px) scale(${scale.value})`,
  transition: isDragging.value ? 'none' : 'transform 0.3s ease',
  transformOrigin: 'center center',
  maxWidth: '90vw',
  maxHeight: '90vh',
  objectFit: 'contain' as const,
}))

const close = () => emit('update:visible', false)
const resetZoom = () => { scale.value = 1; offsetX.value = 0; offsetY.value = 0 }

const getDistance = (touches: TouchList): number => {
  const dx = touches[0].clientX - touches[1].clientX
  const dy = touches[0].clientY - touches[1].clientY
  return Math.sqrt(dx * dx + dy * dy)
}

const onTouchStart = (e: TouchEvent) => {
  const now = Date.now()
  if (now - lastTapTime < DOUBLE_TAP_DELAY && e.touches.length === 1) { resetZoom(); lastTapTime = 0; return }
  lastTapTime = now
  if (e.touches.length === 2) { initialDistance = getDistance(e.touches); initialScale = scale.value }
  else if (e.touches.length === 1 && scale.value > 1) { isDragging.value = true; lastX = e.touches[0].clientX; lastY = e.touches[0].clientY }
}

const onTouchMove = (e: TouchEvent) => {
  if (e.cancelable) e.preventDefault()
  if (e.touches.length === 2) {
    const currentDistance = getDistance(e.touches)
    scale.value = Math.min(MAX_SCALE, Math.max(MIN_SCALE, initialScale * (currentDistance / initialDistance)))
  } else if (e.touches.length === 1 && isDragging.value && scale.value > 1) {
    const currentX = e.touches[0].clientX; const currentY = e.touches[0].clientY
    offsetX.value += currentX - lastX; offsetY.value += currentY - lastY
    lastX = currentX; lastY = currentY
  }
}

const onTouchEnd = () => { isDragging.value = false; initialDistance = 0; initialScale = 1 }
watch(() => props.visible, (val) => { if (!val) resetZoom() })
</script>

<style scoped>
.zoom-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: rgba(0,0,0,0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.zoom-close {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 10;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(255,255,255,0.1);
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}
.zoom-close:hover { background: rgba(255,255,255,0.2); }
.zoom-hint {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255,255,255,0.6);
  font-size: 14px;
}
.zoom-fade-enter-active,
.zoom-fade-leave-active { transition: opacity 0.25s ease; }
.zoom-fade-enter-from,
.zoom-fade-leave-to { opacity: 0; }
</style>
