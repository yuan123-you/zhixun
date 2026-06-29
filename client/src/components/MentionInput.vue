<template>
  <!-- @提及输入框 -->
  <div class="mention-input-wrap">
    <el-input
      ref="textareaRef"
      :model-value="modelValue"
      type="textarea"
      :placeholder="placeholder || ''"
      :rows="rows || 3"
      resize="none"
      @input="onInput"
      @keydown="onKeydown"
    />
    <div v-if="showMentionList" class="mention-dropdown">
      <div v-for="(user, i) in mentionResults" :key="user.id" class="mention-item"
        :class="{ active: i === mentionIndex }" @click="selectUser(user)">
        <img :src="user.avatar || '/avatar-placeholder.png'" class="mention-avatar" />
        <div>
          <div class="mention-name">{{ user.nickname }}</div>
          <div class="mention-uid">{{ user.uid ? '@' + user.uid : '' }}</div>
        </div>
      </div>
      <div v-if="mentionResults.length === 0" class="mention-empty">未找到用户</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useMention } from '@/composables/useMention'

const props = defineProps<{
  modelValue: string
  placeholder?: string
  rows?: number
}>()

const emit = defineEmits(['update:modelValue'])
const textareaRef = ref<any>(null)
const { mentionResults, showMentionList, mentionIndex, searchUsers, insertMention, detectMentionTrigger } = useMention()

let searchTimer: ReturnType<typeof setTimeout> | null = null

function onInput(e: string | Event) {
  const val = typeof e === 'string' ? e : (e.target as HTMLTextAreaElement).value
  emit('update:modelValue', val)
  const textarea = (textareaRef.value?.$el?.querySelector('textarea') || textareaRef.value?.textarea) as HTMLTextAreaElement | undefined
  const cursorPos = textarea?.selectionStart || 0
  const { triggered, query } = detectMentionTrigger(val, cursorPos)
  if (triggered) {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(() => searchUsers(query), 200)
  } else {
    showMentionList.value = false
  }
}

function onKeydown(e: Event | KeyboardEvent) {
  const ke = e as KeyboardEvent
  if (!showMentionList.value) return
  if (ke.key === 'ArrowDown') { ke.preventDefault(); mentionIndex.value = Math.min(mentionIndex.value + 1, mentionResults.value.length - 1) }
  else if (ke.key === 'ArrowUp') { ke.preventDefault(); mentionIndex.value = Math.max(mentionIndex.value - 1, -1) }
  else if (ke.key === 'Enter' && mentionIndex.value >= 0) { ke.preventDefault(); selectUser(mentionResults.value[mentionIndex.value]) }
  else if (ke.key === 'Escape') { showMentionList.value = false }
}

function selectUser(user: any) {
  const textarea = (textareaRef.value?.$el?.querySelector('textarea') || textareaRef.value?.textarea) as HTMLTextAreaElement | undefined
  if (!textarea) return
  const cursorPos = textarea.selectionStart
  const { text, pos } = insertMention(props.modelValue, cursorPos, user)
  emit('update:modelValue', text)
  showMentionList.value = false
  setTimeout(() => { textarea.selectionStart = textarea.selectionEnd = pos }, 10)
}

defineExpose({ textareaRef })
</script>

<style scoped>
.mention-input-wrap { position: relative; }
.mention-dropdown {
  position: absolute;
  bottom: 100%;
  left: 0;
  margin-bottom: 8px;
  width: 256px;
  background: var(--el-bg-color);
  border-radius: 12px;
  box-shadow: var(--el-box-shadow-dark);
  border: 1px solid var(--el-border-color-lighter);
  max-height: 192px;
  overflow-y: auto;
  z-index: 50;
}
.mention-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  cursor: pointer;
  transition: background-color 0.15s;
}
.mention-item:hover { background: var(--el-fill-color-lighter); }
.mention-item.active { background: var(--el-color-primary-light-9); }
.mention-avatar { width: 32px; height: 32px; border-radius: 50%; }
.mention-name { font-size: 14px; font-weight: 500; color: var(--el-text-color-primary); }
.mention-uid { font-size: 12px; color: var(--el-text-color-placeholder); }
.mention-empty {
  padding: 16px;
  text-align: center;
  font-size: 14px;
  color: var(--el-text-color-placeholder);
}
</style>
