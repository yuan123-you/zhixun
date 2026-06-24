<template>
  <div v-if="banners.length > 0" class="banner-carousel relative overflow-hidden rounded-xl">
    <div
      class="flex"
      :class="skipTransition ? '' : 'transition-transform duration-500 ease-in-out'"
      :style="{ transform: `translateX(-${displayIndex * 100}%)` }"
      @transitionend="onTransitionEnd"
    >
      <!-- 末尾克隆（最后一张的副本，放在最前面） -->
      <div
        v-if="banners.length > 1"
        class="w-full shrink-0 cursor-pointer"
        @click="handleClick(banners[banners.length - 1])"
      >
        <div class="relative w-full" style="padding-bottom: 40%">
          <img
            :src="banners[banners.length - 1].imageUrl"
            :alt="banners[banners.length - 1].title"
            class="absolute inset-0 w-full h-full object-cover"
          />
          <div class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/60 to-transparent p-4">
            <h3 class="text-white text-lg font-medium truncate">{{ banners[banners.length - 1].title }}</h3>
          </div>
        </div>
      </div>
      <!-- 真实幻灯片 -->
      <div
        v-for="banner in banners"
        :key="banner.id"
        class="w-full shrink-0 cursor-pointer"
        @click="handleClick(banner)"
      >
        <div class="relative w-full" style="padding-bottom: 40%">
          <img
            :src="banner.imageUrl"
            :alt="banner.title"
            class="absolute inset-0 w-full h-full object-cover"
          />
          <div class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/60 to-transparent p-4">
            <h3 class="text-white text-lg font-medium truncate">{{ banner.title }}</h3>
          </div>
        </div>
      </div>
      <!-- 开头克隆（第一张的副本，放在最后面） -->
      <div
        v-if="banners.length > 1"
        class="w-full shrink-0 cursor-pointer"
        @click="handleClick(banners[0])"
      >
        <div class="relative w-full" style="padding-bottom: 40%">
          <img
            :src="banners[0].imageUrl"
            :alt="banners[0].title"
            class="absolute inset-0 w-full h-full object-cover"
          />
          <div class="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/60 to-transparent p-4">
            <h3 class="text-white text-lg font-medium truncate">{{ banners[0].title }}</h3>
          </div>
        </div>
      </div>
    </div>

    <!-- 左右箭头 -->
    <button
      v-if="banners.length > 1"
      class="absolute left-3 top-1/2 -translate-y-1/2 w-8 h-8 flex items-center justify-center rounded-full bg-black/30 hover:bg-black/50 text-white transition-colors"
      @click="prev"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
      </svg>
    </button>
    <button
      v-if="banners.length > 1"
      class="absolute right-3 top-1/2 -translate-y-1/2 w-8 h-8 flex items-center justify-center rounded-full bg-black/30 hover:bg-black/50 text-white transition-colors"
      @click="next"
    >
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
      </svg>
    </button>

    <!-- 指示器 -->
    <div v-if="banners.length > 1" class="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5 md:gap-2">
      <button
        v-for="(_, index) in banners"
        :key="index"
        class="w-1.5 h-1.5 md:w-2 md:h-2 rounded-full transition-colors"
        :class="realIndex === index ? 'bg-white' : 'bg-white/50'"
        @click="goTo(index)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { BannerItem } from '~/api/banner'

const props = defineProps<{
  banners: BannerItem[]
}>()

// realIndex: 真实幻灯片索引 (0 ~ banners.length - 1)
// displayIndex: 实际渲染位置，前面有一个克隆项所以偏移 +1
const realIndex = ref(0)
const displayIndex = ref(1) // 初始显示第一张真实幻灯片
const skipTransition = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

const next = () => {
  if (props.banners.length <= 1) return
  skipTransition.value = false
  displayIndex.value++
  // realIndex 在 transitionend 中更新（无缝跳转时）
  // 但对于普通滑动，立即更新
  realIndex.value = (displayIndex.value - 1) % props.banners.length
  if (realIndex.value < 0) realIndex.value += props.banners.length
}

const prev = () => {
  if (props.banners.length <= 1) return
  skipTransition.value = false
  displayIndex.value--
  realIndex.value = (displayIndex.value - 1 + props.banners.length) % props.banners.length
}

const goTo = (index: number) => {
  if (props.banners.length <= 1) return
  skipTransition.value = false
  displayIndex.value = index + 1
  realIndex.value = index
}

const onTransitionEnd = () => {
  if (props.banners.length <= 1) return
  const total = props.banners.length
  // 滑到了末尾克隆（第一张的副本）→ 瞬间跳到真正的第一张
  if (displayIndex.value === total + 1) {
    skipTransition.value = true
    displayIndex.value = 1
    realIndex.value = 0
    nextTick(() => {
      skipTransition.value = false
    })
  }
  // 滑到了开头克隆（最后一张的副本）→ 瞬间跳到真正的最后一张
  else if (displayIndex.value === 0) {
    skipTransition.value = true
    displayIndex.value = total
    realIndex.value = total - 1
    nextTick(() => {
      skipTransition.value = false
    })
  }
}

const handleClick = (banner: BannerItem) => {
  if (banner.linkUrl) {
    if (banner.linkType === 2) {
      window.open(banner.linkUrl, '_blank')
    } else {
      navigateTo(banner.linkUrl)
    }
  }
}

const startAutoPlay = () => {
  if (props.banners.length > 1) {
    timer = setInterval(next, 4000)
  }
}

const stopAutoPlay = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(() => {
  startAutoPlay()
})

onUnmounted(() => {
  stopAutoPlay()
})

watch(() => props.banners, () => {
  realIndex.value = 0
  displayIndex.value = 1
  stopAutoPlay()
  startAutoPlay()
})
</script>
