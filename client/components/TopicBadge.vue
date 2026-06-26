<template>
  <NuxtLink :to="`/topics/${id}`" class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-medium transition"
    :class="isOfficial ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300' : 'bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-300'"
    @click.stop>
    <span class="opacity-70">#</span>
    <span class="hover:underline">{{ name }}</span>
  </NuxtLink>
</template>

<script setup lang="ts">
defineProps<{ id: number; name: string; isOfficial?: boolean }>()
</script>
