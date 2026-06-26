<template>
  <div class="page-header">
    <div class="flex items-center justify-between">
      <div>
        <h2 class="page-title">{{ title }}</h2>
        <p v-if="description || subtitle" class="page-subtitle">{{ description || subtitle }}</p>
      </div>
      <div v-if="$slots.actions" class="flex-shrink-0">
        <slot name="actions" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
  description?: string
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
