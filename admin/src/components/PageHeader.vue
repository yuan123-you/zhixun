<template>
  <div class="page-header">
    <h2 class="page-title">{{ title }}</h2>
    <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
  </div>
</template>

<script setup lang="ts">
/** 页面标题组件 */
defineProps<{
  /** 页面标题 */
  title: string
  /** 副标题 */
  subtitle?: string
}>()
</script>

<style scoped lang="scss">
.page-header {
  margin-bottom: 20px;

  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: #333;
    margin: 0 0 4px;
  }

  .page-subtitle {
    font-size: 14px;
    color: #999;
    margin: 0;
  }
}
</style>
