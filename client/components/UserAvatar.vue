<template>
  <!-- 统一用户头像组件 -->
  <div
    class="avatar-wrapper"
    :class="[sizeClass, { 'avatar-bordered': bordered }]"
    :style="customSizeStyle"
  >
    <!-- 加载占位 -->
    <div v-if="isLoading" class="avatar-placeholder animate-pulse"></div>

    <!-- 实际头像图片 -->
    <img
      v-show="!isLoading && !hasError"
      :src="avatarSrc"
      :alt="alt"
      class="avatar-image"
      :class="{ 'avatar-image-loaded': isLoaded }"
      @load="onLoad"
      @error="onError"
    />

    <!-- 错误/默认头像回退 -->
    <div v-if="hasError || (!src && !isLoading)" class="avatar-fallback">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" fill="none" class="avatar-fallback-icon">
        <circle cx="100" cy="78" r="32" fill="currentColor" opacity="0.5"/>
        <ellipse cx="100" cy="170" rx="52" ry="42" fill="currentColor" opacity="0.5"/>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 统一用户头像组件 - 支持默认头像、加载过渡、错误回退 */

const DEFAULT_AVATAR = '/images/default-avatar.svg'

const props = withDefaults(defineProps<{
  /** 头像 URL */
  src?: string | null
  /** 替代文本 */
  alt?: string
  /** 预设尺寸: xs(20px) sm(32px) md(40px) lg(48px) xl(80px) */
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  /** 自定义尺寸（px），优先级高于 size */
  customSize?: number
  /** 是否显示边框 */
  bordered?: boolean
}>(), {
  src: null,
  alt: '',
  size: 'md',
  customSize: undefined,
  bordered: false,
})

const config = useRuntimeConfig()
const { resolveUrl } = useResourceUrl()

const isLoading = ref(true)
const isLoaded = ref(false)
const hasError = ref(false)

// 解析头像URL：将内部 MinIO 地址替换为公网可访问的地址
const resolveAvatarUrl = (url: string | null | undefined): string | null => {
  return resolveUrl(url)
}

// 计算实际头像源
const avatarSrc = computed(() => {
  if (hasError.value) return DEFAULT_AVATAR
  return resolveAvatarUrl(props.src) || DEFAULT_AVATAR
})

// 预设尺寸映射
const sizeMap: Record<string, string> = {
  xs: 'w-5 h-5',
  sm: 'w-8 h-8',
  md: 'w-10 h-10',
  lg: 'w-12 h-12',
  xl: 'w-20 h-20',
}

const sizeClass = computed(() => {
  if (props.customSize) return ''
  return sizeMap[props.size] || sizeMap.md
})

const customSizeStyle = computed(() => {
  if (!props.customSize) return {}
  return {
    width: `${props.customSize}px`,
    height: `${props.customSize}px`,
  }
})

// 图片加载成功
const onLoad = () => {
  isLoading.value = false
  isLoaded.value = true
  hasError.value = false
}

// 图片加载失败
const onError = () => {
  isLoading.value = false
  isLoaded.value = false
  hasError.value = true
}

// 监听 src 变化，重置加载状态
watch(() => props.src, () => {
  isLoading.value = true
  isLoaded.value = false
  hasError.value = false
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
  background-color: #E5E7EB;
}

.dark .avatar-wrapper {
  background-color: #374151;
}

.avatar-bordered {
  border: 2px solid white;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.05);
}

.dark .avatar-bordered {
  border-color: #1F2937;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.05);
}

.avatar-placeholder {
  position: absolute;
  inset: 0;
  background-color: #D1D5DB;
  border-radius: 50%;
}

.dark .avatar-placeholder {
  background-color: #4B5563;
}

.avatar-image {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s ease-in-out;
}

.avatar-image-loaded {
  opacity: 1;
}

.avatar-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: #E5E7EB;
  color: #9CA3AF;
}

.dark .avatar-fallback {
  background-color: #374151;
  color: #6B7280;
}

.avatar-fallback-icon {
  width: 70%;
  height: 70%;
}
</style>
