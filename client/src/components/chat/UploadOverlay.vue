<template>
  <Teleport to="body">
    <div v-if="uploading" class="upload-overlay">
      <div class="upload-card">
        <div class="upload-spinner"></div>
        <p class="upload-text">{{ progress > 0 ? `上传中 ${progress}%` : '上传中...' }}</p>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
/**
 * UploadOverlay - 文件上传进度遮罩（私信/群聊共用）
 */
defineProps<{
  uploading: boolean
  progress?: number
}>()
</script>

<style scoped>
.upload-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}
.upload-card {
  background: var(--zh-bg-elevated, #fff);
  border-radius: 16px;
  padding: 24px 32px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
.upload-spinner {
  width: 32px;
  height: 32px;
  margin: 0 auto 12px;
  border: 3px solid var(--zh-border, #e5e7eb);
  border-top-color: var(--zh-primary, #6366f1);
  border-radius: 50%;
  animation: chat-upload-spin 0.8s linear infinite;
}
.upload-text {
  font-size: 14px;
  color: var(--zh-text, #1e293b);
}
@keyframes chat-upload-spin {
  to { transform: rotate(360deg); }
}
</style>
