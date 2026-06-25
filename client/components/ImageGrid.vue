<template>
  <!-- 图片网格组件（搜索图片Tab用） -->
  <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-2">
    <div
      v-for="(image, index) in images"
      :key="index"
      class="relative aspect-square rounded-lg overflow-hidden cursor-pointer group"
      @click="$emit('click', image)"
    >
      <img :src="image.url" :alt="image.title" class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
      <!-- 遮罩层 -->
      <div class="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent opacity-0 group-hover:opacity-100 transition-opacity">
        <div class="absolute bottom-0 left-0 right-0 p-2.5">
          <p v-if="image.articleTitle" class="text-white text-xs font-medium line-clamp-1 mb-0.5">{{ image.articleTitle }}</p>
          <p v-if="image.author" class="text-white/70 text-xs line-clamp-1">{{ image.author }}</p>
          <p v-else class="text-white/70 text-xs line-clamp-1">{{ image.title }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/** 图片网格组件 */

interface ImageItem {
  url: string
  title: string
  articleTitle?: string
  author?: string
  [key: string]: any
}

defineProps<{
  images: ImageItem[]
}>()

defineEmits<{
  click: [image: ImageItem]
}>()
</script>
