<template>
  <!-- 图片网格组件 -->
  <div class="image-grid">
    <div v-for="(image, index) in images" :key="index" class="image-grid-item" @click="$emit('click', image)">
      <img :src="image.url" :alt="image.title" class="image-grid-img" />
      <div class="image-grid-overlay">
        <div class="image-grid-info">
          <p v-if="image.articleTitle" class="image-grid-title">{{ image.articleTitle }}</p>
          <p v-if="image.author" class="image-grid-author">{{ image.author }}</p>
          <p v-else class="image-grid-author">{{ image.title }}</p>
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

defineProps<{ images: ImageItem[] }>()
defineEmits<{ click: [image: ImageItem] }>()
</script>

<style scoped>
.image-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}
@media (min-width: 640px) {
  .image-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}
@media (min-width: 768px) {
  .image-grid { grid-template-columns: repeat(4, minmax(0, 1fr)); }
}

.image-grid-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}
.image-grid-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.image-grid-item:hover .image-grid-img {
  transform: scale(1.05);
}

.image-grid-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(0,0,0,0.7), rgba(0,0,0,0.2), transparent);
  opacity: 0;
  transition: opacity 0.2s;
}
.image-grid-item:hover .image-grid-overlay {
  opacity: 1;
}

.image-grid-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px;
}
.image-grid-title {
  font-size: 12px;
  color: #fff;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}
.image-grid-author {
  font-size: 12px;
  color: rgba(255,255,255,0.7);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
