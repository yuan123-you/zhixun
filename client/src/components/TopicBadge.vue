<template>
  <RouterLink :to="`/topics/${id}`" class="topic-badge" :class="isOfficial ? 'official' : 'normal'" @click.stop>
    <span class="hash">#</span>
    <span>{{ name }}</span>
  </RouterLink>
</template>

<script setup lang="ts">
defineProps<{ id: number; name: string; isOfficial?: boolean }>()
</script>

<style scoped>
.topic-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 500;
  text-decoration: none;
  transition: all 0.2s;
}
.topic-badge.official {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}
.dark .topic-badge.official {
  background: rgba(99, 102, 241, 0.15);
  color: var(--el-color-primary-light-3);
}
.topic-badge.normal {
  background: var(--el-color-purple-light-9, #f3e8ff);
  color: var(--el-color-purple, #7c3aed);
}
.dark .topic-badge.normal {
  background: rgba(124, 58, 237, 0.15);
  color: #c4b5fd;
}
.topic-badge:hover {
  filter: brightness(0.95);
}
.hash {
  opacity: 0.7;
}
</style>
