<template>
  <div v-if="announcements.length > 0" class="announcement-bar">
    <div
      class="flex items-center gap-3 px-4 py-2.5 rounded-lg bg-blue-50 dark:bg-blue-900/20 border border-blue-100 dark:border-blue-800/30"
    >
      <!-- 图标 -->
      <div class="shrink-0">
        <svg class="w-5 h-5 text-blue-500 dark:text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5.882V19.24a1.76 1.76 0 01-3.417.592l-2.147-6.15M18 13a3 3 0 100-6M5.436 13.683A4.001 4.001 0 017 6h1.832c4.1 0 7.625-1.234 9.168-3v14c-1.543-1.766-5.067-3-9.168-3H7a3.988 3.988 0 01-1.564-.317z" />
        </svg>
      </div>

      <!-- 滚动公告 -->
      <div class="flex-1 overflow-hidden relative h-5">
        <TransitionGroup name="announcement-slide">
          <div
            v-for="(item, index) in announcements"
            :key="item.id"
            v-show="currentIndex === index"
            class="absolute inset-0 flex items-center"
          >
            <span
              class="text-sm text-blue-700 dark:text-blue-300 truncate cursor-pointer hover:underline"
              @click="handleClick(item)"
            >
              <span
                v-if="item.type === 2"
                class="inline-flex items-center mr-1.5 px-1.5 py-0.5 text-xs font-medium rounded bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400"
              >活动</span>
              <span
                v-else
                class="inline-flex items-center mr-1.5 px-1.5 py-0.5 text-xs font-medium rounded bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400"
              >公告</span>
              {{ item.title }}
            </span>
          </div>
        </TransitionGroup>
      </div>

      <!-- 展开/收起 -->
      <button
        v-if="currentItem"
        class="shrink-0 text-xs text-blue-500 hover:text-blue-700 dark:text-blue-400 dark:hover:text-blue-300"
        @click="showDetail = !showDetail"
      >
        {{ showDetail ? '收起' : '详情' }}
      </button>
    </div>

    <!-- 公告详情 -->
    <Transition name="announcement-detail">
      <div
        v-if="showDetail && currentItem"
        class="mt-2 px-4 py-3 rounded-lg bg-blue-50/50 dark:bg-blue-900/10 border border-blue-100 dark:border-blue-800/20"
      >
        <h4 class="font-medium text-blue-800 dark:text-blue-200 mb-1">{{ currentItem.title }}</h4>
        <p class="text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap">{{ currentItem.content }}</p>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import type { AnnouncementItem } from '~/api/banner'

const props = defineProps<{
  announcements: AnnouncementItem[]
}>()

const currentIndex = ref(0)
const showDetail = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

const currentItem = computed(() => props.announcements[currentIndex.value])

const handleClick = (item: AnnouncementItem) => {
  showDetail.value = !showDetail.value
}

const startAutoPlay = () => {
  if (props.announcements.length > 1) {
    timer = setInterval(() => {
      currentIndex.value = (currentIndex.value + 1) % props.announcements.length
      showDetail.value = false
    }, 5000)
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

watch(() => props.announcements, () => {
  currentIndex.value = 0
  showDetail.value = false
  stopAutoPlay()
  startAutoPlay()
})
</script>

<style scoped>
.announcement-slide-enter-active,
.announcement-slide-leave-active {
  transition: all 0.3s ease;
}
.announcement-slide-enter-from {
  opacity: 0;
  transform: translateY(100%);
}
.announcement-slide-leave-to {
  opacity: 0;
  transform: translateY(-100%);
}

.announcement-detail-enter-active,
.announcement-detail-leave-active {
  transition: all 0.2s ease;
}
.announcement-detail-enter-from,
.announcement-detail-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
