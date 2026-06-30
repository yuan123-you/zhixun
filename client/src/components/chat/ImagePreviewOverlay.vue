<template>
  <Teleport to="body">
    <div v-if="src" class="image-preview-overlay" @click="$emit('close')">
      <img :src="src" class="image-preview-img" alt="预览" @click.stop />
    </div>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * ImagePreviewOverlay - 图片全屏预览浮层（私信/群聊共用）
 */
defineProps<{
  src: string
}>()

defineEmits<{
  close: []
}>()
</script>

<style scoped>
.image-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.8);
  cursor: pointer;
}
.image-preview-img {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
  object-fit: contain;
  cursor: default;
}
</style>
