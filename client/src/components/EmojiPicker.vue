<template>
  <!-- 表情选择器 -->
  <div class="emoji-picker-wrap">
    <el-button text @click="toggle" ref="triggerRef" title="插入表情" class="emoji-trigger">
      <el-icon :size="14"><Sunny /></el-icon>
    </el-button>
    <Teleport to="body">
      <div v-if="open" class="emoji-popup" :style="popupPosition">
        <div class="emoji-grid">
          <button v-for="emoji in emojis" :key="emoji" class="emoji-btn" @click="selectEmoji(emoji)">
            {{ emoji }}
          </button>
        </div>
      </div>
    </Teleport>
    <div v-if="open" class="emoji-backdrop" @click="open = false"></div>
  </div>
</template>

<script setup lang="ts">
import { Sunny } from '@element-plus/icons-vue'

const emit = defineEmits(['select'])
const open = ref(false)
const triggerRef = ref<HTMLElement | null>(null)
const popupPosition = reactive({ top: '0px', left: '0px' })

function toggle() {
  open.value = !open.value
  if (open.value) {
    nextTick(() => {
      if (triggerRef.value?.$el || triggerRef.value) {
        const el = (triggerRef.value as any)?.$el || triggerRef.value
        const rect = el.getBoundingClientRect()
        popupPosition.top = (rect.top - 230 > 0 ? rect.top - 230 : rect.bottom + 8) + 'px'
        popupPosition.left = Math.min(rect.left, window.innerWidth - 300) + 'px'
      }
    })
  }
}

const emojis = ['😀','😃','😄','😁','😅','😂','🤣','😊','😇','🙂','😉','😌','😍','🥰','😘','😗','😋','😛','😜','🤪','😎','🤩','🥳','😏','😒','😞','😔','😟','😕','🙁','😣','😖','😫','😩','🥺','😢','😭','😤','😠','😡','🤔','🤗','🤭','👍','👎','👏','🙌','🤝','💪','✌️','🤞','🤟','❤️','🧡','💛','💚','💙','💜','🖤','💖','🔥','⭐','🎉','🎊','✨','💡','📌','📍','🏠','🌍','🌸','🌺','🌈','☀️','🌙','⚡','💧','🎵','🎶','💯']

function selectEmoji(emoji: string) {
  emit('select', emoji)
  open.value = false
}
</script>

<style scoped>
.emoji-picker-wrap { position: relative; display: inline-block; }
.emoji-trigger {
  padding: 4px !important;
  min-height: auto !important;
  border-radius: 4px !important;
  transition: all 0.15s;
}
.emoji-trigger:hover {
  background: var(--zh-bg-hover, var(--el-fill-color-light)) !important;
  color: var(--zh-text, var(--el-text-color-primary)) !important;
}
.emoji-popup {
  position: fixed;
  z-index: 9999;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: var(--el-box-shadow-dark);
  border: 1px solid var(--el-border-color-lighter);
  padding: 12px;
  width: 288px;
}
.emoji-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  max-height: 192px;
  overflow-y: auto;
}
.emoji-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: none;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.15s;
}
.emoji-btn:hover { background: var(--el-fill-color-light); }
.emoji-backdrop { position: fixed; inset: 0; z-index: 9998; }
</style>
