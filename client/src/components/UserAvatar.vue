<template>
  <!-- 统一用户头像组件（带本地持久化缓存与秒级秒开加速） -->
  <div class="avatar-wrapper" :class="{ 'avatar-bordered': bordered }" :style="avatarStyle">
    <!-- 底层占位/兜底（始终存在，杜绝空洞与闪烁） -->
    <div v-if="!isLoaded || hasError" class="avatar-fallback">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" fill="none" class="avatar-fallback-icon">
        <rect width="200" height="200" rx="100" fill="#E2E8F0"/>
        <circle cx="100" cy="78" r="32" fill="#94A3B8"/>
        <ellipse cx="100" cy="170" rx="52" ry="42" fill="#94A3B8"/>
      </svg>
    </div>

    <!-- 真实头像图片（加载成功后平滑淡入） -->
    <img
      v-if="!hasError && displaySrc"
      :src="displaySrc"
      :alt="alt"
      class="avatar-image"
      :class="{ 'avatar-image-loaded': isLoaded }"
      loading="lazy"
      @load="onLoad"
      @error="onError"
    />
  </div>
</template>

<script setup lang="ts">
/**
 * 统一用户头像组件
 * 特性：
 * 1. 0ms 同步秒开：内存/IndexedDB/localStorage 命中直接呈现
 * 2. 坏链快速熔断：对失效/404生成图拦截并走内联 SVG，免去多余网络等待
 * 3. 本地持久化：已加载的头像自动持久化到用户本地存储
 */
import { ref, computed, watch, onMounted } from 'vue'
import { useResourceUrl } from '@/composables/useResourceUrl'
import {
  DEFAULT_AVATAR_DATA_URI,
  isBrokenAvatarUrl,
  getMemoryCachedAvatar,
  getLocalCachedAvatar,
  preloadAndCacheAvatar,
} from '@/utils/avatarCache'

const props = withDefaults(defineProps<{
  src?: string | null
  alt?: string
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  customSize?: number
  bordered?: boolean
}>(), {
  src: null,
  alt: '',
  size: 'md',
  customSize: undefined,
  bordered: false,
})

const { resolveUrl } = useResourceUrl()

const sizeMap: Record<string, number> = { xs: 20, sm: 32, md: 40, lg: 48, xl: 80 }
const avatarSize = computed(() => props.customSize || sizeMap[props.size] || 40)
const avatarStyle = computed(() => ({ width: `${avatarSize.value}px`, height: `${avatarSize.value}px` }))

const displaySrc = ref<string>('')
const isLoaded = ref(false)
const hasError = ref(false)

/**
 * 解析并加载头像源
 */
const syncAvatar = async () => {
  const resolved = resolveUrl(props.src)

  // 1. 无有效地址或为坏链，立即走兜底，不发网络请求
  if (!resolved || isBrokenAvatarUrl(resolved)) {
    displaySrc.value = DEFAULT_AVATAR_DATA_URI
    isLoaded.value = true
    hasError.value = false
    return
  }

  // 2. 检查同步内存缓存
  const memoryHit = getMemoryCachedAvatar(resolved)
  if (memoryHit) {
    displaySrc.value = memoryHit
    isLoaded.value = true
    hasError.value = false
    return
  }

  // 3. 初始使用解析地址，避免等待
  displaySrc.value = resolved
  hasError.value = false

  // 4. 异步从本地持久化数据库恢复
  const localHit = await getLocalCachedAvatar(resolved)
  if (localHit) {
    displaySrc.value = localHit
    isLoaded.value = true
    return
  }

  // 5. 本地无缓存，在后台预载并写入本地
  preloadAndCacheAvatar(resolved).then((cached) => {
    if (cached && !hasError.value) {
      displaySrc.value = cached
      isLoaded.value = true
    }
  }).catch(() => {})
}

const onLoad = () => {
  isLoaded.value = true
  hasError.value = false
}

const onError = () => {
  hasError.value = true
  isLoaded.value = false
  displaySrc.value = DEFAULT_AVATAR_DATA_URI
}

watch(() => props.src, () => {
  isLoaded.value = false
  hasError.value = false
  syncAvatar()
})

onMounted(() => {
  syncAvatar()
})
</script>

<style scoped>
.avatar-wrapper {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  overflow: hidden;
  flex-shrink: 0;
  background-color: var(--el-fill-color, #f1f5f9);
}

.avatar-bordered {
  border: 2px solid var(--el-bg-color, #ffffff);
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.08);
}

.avatar-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #f1f5f9;
  color: #94a3b8;
}

.avatar-fallback-icon {
  width: 100%;
  height: 100%;
  display: block;
}

.avatar-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.avatar-image-loaded {
  opacity: 1 !important;
}
</style>
