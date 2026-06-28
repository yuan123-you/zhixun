<template>
  <!-- 统一用户头像组件 -->
  <div class="avatar-wrapper" :class="{ 'avatar-bordered': bordered }" :style="avatarStyle">
    <div v-if="isLoading" class="avatar-placeholder" />
    <img
      v-show="!isLoading && !hasError"
      :src="avatarSrc"
      :alt="alt"
      class="avatar-image"
      :class="{ 'avatar-image-loaded': isLoaded }"
      @load="onLoad"
      @error="onError"
    />
    <div v-if="hasError || (!src && !isLoading)" class="avatar-fallback">
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" fill="none" class="avatar-fallback-icon">
        <circle cx="100" cy="78" r="32" fill="currentColor" opacity="0.5"/>
        <ellipse cx="100" cy="170" rx="52" ry="42" fill="currentColor" opacity="0.5"/>
      </svg>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 统一用户头像组件 */
const DEFAULT_AVATAR = '/images/default-avatar.svg'

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
const isLoading = ref(true)
const isLoaded = ref(false)
const hasError = ref(false)
// 记录上一次解析后的 URL，避免相同 URL 重复触发加载动画
const lastResolvedUrl = ref<string | null>(null)

const sizeMap: Record<string, number> = { xs: 20, sm: 32, md: 40, lg: 48, xl: 80 }
const avatarSize = computed(() => props.customSize || sizeMap[props.size] || 40)
const avatarStyle = computed(() => ({ width: `${avatarSize.value}px`, height: `${avatarSize.value}px` }))

// 不根据 hasError 切换 src，否则会形成死循环：
// 真实URL加载失败 → hasError=true → src变为默认头像 → 默认头像加载成功 → hasError=false → src变回真实URL → 再次失败 → 无限闪烁
const avatarSrc = computed(() => {
  return resolveUrl(props.src) || DEFAULT_AVATAR
})

const onLoad = () => { isLoading.value = false; isLoaded.value = true; hasError.value = false }
const onError = () => { isLoading.value = false; isLoaded.value = false; hasError.value = true }

watch(() => props.src, (newSrc) => {
  const newUrl = resolveUrl(newSrc) || DEFAULT_AVATAR
  // 仅当解析后的 URL 真正变化时才重置状态，避免相同 URL 反复触发加载闪烁
  if (newUrl !== lastResolvedUrl.value) {
    lastResolvedUrl.value = newUrl
    isLoading.value = true
    isLoaded.value = false
    hasError.value = false
  }
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
  background-color: var(--el-fill-color);
}
.avatar-bordered {
  border: 2px solid var(--el-bg-color);
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.05);
}
.avatar-placeholder {
  position: absolute;
  inset: 0;
  background-color: var(--el-fill-color-dark);
  border-radius: 50%;
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
.avatar-image-loaded { opacity: 1; }
.avatar-fallback {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: var(--el-fill-color);
  color: var(--el-text-color-placeholder);
}
.avatar-fallback-icon { width: 70%; height: 70%; }
</style>
