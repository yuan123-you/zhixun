<template>
  <!-- 返回顶部按钮 - 半透明小按钮，右下角距离底部10%位置 -->
  <Transition name="backtop-fade">
    <button
      v-show="visible"
      class="backtop-btn"
      aria-label="回到顶部"
      @click="scrollToTop"
    >
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M18 15l-6-6-6 6" />
      </svg>
    </button>
  </Transition>
</template>

<script setup lang="ts">
/** 返回顶部按钮：滚动超过视口50%时显示，半透明小按钮，右下角距离底部10%不遮挡菜单 */

const visible = ref(false)

const handleScroll = () => {
  visible.value = window.scrollY > window.innerHeight * 0.5
}

const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped>
.backtop-btn {
  position: fixed;
  right: 14px;
  bottom: 10vh;
  z-index: 90;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(128, 128, 128, 0.35);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  outline: none;
  -webkit-tap-highlight-color: transparent;
}
.backtop-btn:hover {
  background: rgba(128, 128, 128, 0.55);
  transform: translateY(-2px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.2);
}
.backtop-btn:active {
  transform: scale(0.9);
}

/* 暗色模式适配 */
:global(.dark) .backtop-btn {
  background: rgba(200, 200, 200, 0.3);
  border-color: rgba(255, 255, 255, 0.15);
}
:global(.dark) .backtop-btn:hover {
  background: rgba(200, 200, 200, 0.45);
}

/* 移动端确保不遮挡底部导航栏 */
@media (max-width: 767.98px) {
  .backtop-btn {
    bottom: calc(10vh + env(safe-area-inset-bottom, 0px));
    right: 10px;
    width: 34px;
    height: 34px;
  }
}

/* 过渡动画 */
.backtop-fade-enter-active,
.backtop-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.backtop-fade-enter-from,
.backtop-fade-leave-to {
  opacity: 0;
  transform: translateY(10px);
}
</style>
