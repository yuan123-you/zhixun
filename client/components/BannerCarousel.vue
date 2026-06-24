<template>
  <div v-if="banners.length > 0" class="banner-carousel relative overflow-hidden rounded-xl">
    <div
      class="flex transition-transform duration-500 ease-in-out"
      :style="{ transform: `translateX(-${currentIndex * 100}%)` }"
    >
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
    <div v-if="banners.length > 1" class="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-2">
      <button
        v-for="(_, index) in banners"
        :key="index"
        class="w-2 h-2 rounded-full transition-colors"
        :class="currentIndex === index ? 'bg-white' : 'bg-white/50'"
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

const currentIndex = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const next = () => {
  currentIndex.value = (currentIndex.value + 1) % props.banners.length
}

const prev = () => {
  currentIndex.value = (currentIndex.value - 1 + props.banners.length) % props.banners.length
}

const goTo = (index: number) => {
  currentIndex.value = index
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
  currentIndex.value = 0
  stopAutoPlay()
  startAutoPlay()
})
</script>
