<template>
  <!-- 骨架屏 - 自定义交错闪烁 -->
  <div v-if="type === 'article'" class="skeleton-article">
    <div v-for="i in 3" :key="i" class="skeleton-card">
      <div class="sk-author">
        <div class="sk-avatar skeleton" />
        <div class="sk-name skeleton skeleton-text" style="width: 100px;" />
        <div class="sk-time skeleton skeleton-text" style="width: 60px;" />
      </div>
      <div class="sk-title skeleton" style="height: 20px; width: 85%; margin-bottom: 10px;" />
      <div class="sk-body">
        <div class="sk-text" style="flex: 1;">
          <div class="skeleton skeleton-text" style="width: 100%;" />
          <div class="skeleton skeleton-text" style="width: 75%;" />
          <div class="skeleton skeleton-text" style="width: 55%;" />
        </div>
        <div class="sk-img skeleton" style="width: 120px; height: 88px; border-radius: 10px;" />
      </div>
      <div class="sk-actions">
        <div class="skeleton" style="width: 60px; height: 14px;" />
        <div class="skeleton" style="width: 60px; height: 14px;" />
        <div class="skeleton" style="width: 60px; height: 14px;" />
      </div>
    </div>
  </div>

  <div v-else-if="type === 'user'" class="skeleton-user">
    <div v-for="i in 5" :key="i" class="sk-user-row">
      <div class="sk-avatar skeleton" />
      <div class="sk-text-col">
        <div class="skeleton skeleton-text" style="width: 100px;" />
        <div class="skeleton skeleton-text" style="width: 160px;" />
      </div>
      <div class="skeleton" style="width: 64px; height: 32px; border-radius: 16px; margin-left: auto;" />
    </div>
  </div>

  <div v-else-if="type === 'comment'" class="skeleton-comment">
    <div v-for="i in 3" :key="i" class="sk-comment-row">
      <div class="sk-avatar skeleton" style="width: 32px; height: 32px;" />
      <div class="sk-text-col">
        <div class="skeleton skeleton-text" style="width: 80px;" />
        <div class="skeleton skeleton-text" style="width: 100%;" />
        <div class="skeleton skeleton-text" style="width: 65%;" />
      </div>
    </div>
  </div>

  <div v-else class="skeleton-default">
    <div class="skeleton skeleton-title" />
    <div class="skeleton skeleton-text" style="width: 100%;" />
    <div class="skeleton skeleton-text" style="width: 80%;" />
    <div class="skeleton skeleton-text" style="width: 60%;" />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ type?: 'article' | 'user' | 'comment' | 'default' }>(), { type: 'default' })
</script>

<style scoped>
.skeleton-article, .skeleton-user, .skeleton-comment, .skeleton-default {
  padding: 16px;
}
.skeleton-card, .sk-user-row, .sk-comment-row {
  margin-bottom: 20px;
}
.skeleton-card {
  padding: 16px;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border-light);
  border-radius: var(--zh-radius-lg);
}

.sk-author {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.sk-avatar { width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0; }
.sk-title { border-radius: 6px; }
.sk-body {
  display: flex;
  gap: 16px;
  margin-bottom: 14px;
}
.sk-text { display: flex; flex-direction: column; gap: 8px; }
.sk-actions {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid var(--zh-border-light);
}

.sk-user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border-light);
  border-radius: var(--zh-radius-lg);
}
.sk-user-row .sk-avatar { width: 48px; height: 48px; }
.sk-text-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.sk-comment-row {
  display: flex;
  gap: 12px;
  padding: 14px;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border-light);
  border-radius: var(--zh-radius-lg);
}
.sk-comment-row .sk-text-col { gap: 8px; }

.skeleton-default {
  padding: 24px 16px;
}
</style>
