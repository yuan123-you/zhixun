<template>
  <!-- ClientOnly polyfill - 仅在客户端渲染子组件 -->
  <slot v-if="isClient" />
</template>

<script setup lang="ts">
const isClient = ref(false)
onMounted(() => { isClient.value = true })
</script>
