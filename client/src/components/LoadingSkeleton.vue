<template>
  <!-- 骨架屏 - 自定义交错闪烁 -->
  <div v-if="type === 'article'" class="skeleton-article" aria-hidden="true">
    <div v-for="i in count" :key="i" class="article-card-layout skeleton-card card-base">
      <div class="card-author">
        <div class="sk-avatar skeleton" />
        <div class="skeleton" style="width: 100px; height: 12px;" />
        <div class="skeleton" style="width: 60px; height: 11px;" />
      </div>
      <div class="card-content">
        <div class="sk-text">
          <div class="sk-title skeleton" />
          <div class="sk-summary skeleton" />
          <div class="sk-summary skeleton" style="width: 75%;" />
        </div>
        <div class="cover-image-wrap skeleton" />
      </div>
      <div class="card-actions">
        <div v-for="action in 3" :key="action" class="sk-action skeleton" />
      </div>
    </div>
  </div>

  <div v-else-if="type === 'user'" class="skeleton-user">
    <div v-for="i in count" :key="i" class="sk-user-row">
      <div class="sk-avatar skeleton" />
      <div class="sk-text-col">
        <div class="skeleton skeleton-text" style="width: 100px;" />
        <div class="skeleton skeleton-text" style="width: 160px;" />
      </div>
      <div class="skeleton" style="width: 64px; height: 32px; border-radius: 16px; margin-left: auto;" />
    </div>
  </div>

  <div v-else-if="type === 'comment'" class="skeleton-comment">
    <div v-for="i in count" :key="i" class="sk-comment-row">
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
withDefaults(defineProps<{ type?: 'article' | 'user' | 'comment' | 'default'; count?: number }>(), { type: 'default', count: 1 })
</script>

<style src="@/assets/css/article-card-layout.css"></style>

<style scoped>
.skeleton-user, .skeleton-comment, .skeleton-default { padding: 16px; }
.skeleton-card { height: auto; }
.skeleton-card .sk-avatar { width: 32px; height: 32px; transform: scale(0.85); margin-right: -5px; }
.skeleton-card .sk-text { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 5px; }
.sk-title { width: 85%; height: 18px; border-radius: 6px; }
.sk-summary { width: 100%; height: 12px; }
.sk-action { justify-self: center; width: 40px; height: 14px; }
.cover-image-wrap { border-radius: var(--zh-radius-md); }
.sk-avatar { width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0; }
@media (min-width: 768px) { .sk-title { height: 22px; margin-bottom: 3px; } }
.sk-user-row, .sk-comment-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding: 12px;
  background: var(--zh-bg-elevated);
  border: 1px solid var(--zh-border-light);
  border-radius: var(--zh-radius-lg);
}
.sk-user-row { align-items: center; }
.sk-user-row .sk-avatar { width: 48px; height: 48px; }
.sk-text-col { display: flex; flex-direction: column; gap: 8px; flex: 1; min-width: 0; }
.sk-comment-row { padding: 14px; }
.skeleton-default { padding: 24px 16px; }
</style>
