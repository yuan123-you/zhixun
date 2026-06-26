<template>
  <div class="relative">
    <textarea ref="textareaRef" :value="modelValue" @input="onInput" @keydown="onKeydown"
      :placeholder="placeholder" :rows="rows"
      class="w-full px-4 py-2.5 rounded-xl border border-gray-200 dark:border-gray-600 bg-white dark:bg-gray-800 text-gray-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 resize-none"></textarea>

    <!-- Mention dropdown -->
    <div v-if="showMentionList" class="absolute bottom-full left-0 mb-2 w-64 bg-white dark:bg-gray-800 rounded-xl shadow-xl border border-gray-200 dark:border-gray-700 max-h-48 overflow-y-auto z-50">
      <div v-for="(user, i) in mentionResults" :key="user.id"
        @click="selectUser(user)"
        :class="i === mentionIndex ? 'bg-blue-50 dark:bg-blue-900/20' : ''"
        class="flex items-center gap-3 px-3 py-2.5 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700 transition">
        <img :src="user.avatar || '/avatar-placeholder.png'" class="w-8 h-8 rounded-full" />
        <div>
          <div class="text-sm font-medium text-gray-900 dark:text-white">{{ user.nickname }}</div>
          <div class="text-xs text-gray-400">{{ user.uid ? '@' + user.uid : '' }}</div>
        </div>
      </div>
      <div v-if="mentionResults.length === 0" class="px-3 py-4 text-center text-gray-400 text-sm">未找到用户</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useMention } from '~/composables/useMention'

const props = defineProps<{
  modelValue: string
  placeholder?: string
  rows?: number
}>()

const emit = defineEmits(['update:modelValue'])

const textareaRef = ref<HTMLTextAreaElement | null>(null)
const { mentionResults, showMentionList, mentionIndex, searchUsers, insertMention, detectMentionTrigger } = useMention()

let searchTimer: ReturnType<typeof setTimeout> | null = null

function onInput(e: Event) {
  const target = e.target as HTMLTextAreaElement
  emit('update:modelValue', target.value)
  const cursorPos = target.selectionStart
  const { triggered, query } = detectMentionTrigger(target.value, cursorPos)
  if (triggered) {
    if (searchTimer) clearTimeout(searchTimer)
    searchTimer = setTimeout(() => searchUsers(query), 200)
  } else {
    showMentionList.value = false
  }
}

function onKeydown(e: KeyboardEvent) {
  if (!showMentionList.value) return
  if (e.key === 'ArrowDown') { e.preventDefault(); mentionIndex.value = Math.min(mentionIndex.value + 1, mentionResults.value.length - 1) }
  else if (e.key === 'ArrowUp') { e.preventDefault(); mentionIndex.value = Math.max(mentionIndex.value - 1, -1) }
  else if (e.key === 'Enter' && mentionIndex.value >= 0) { e.preventDefault(); selectUser(mentionResults.value[mentionIndex.value]) }
  else if (e.key === 'Escape') { showMentionList.value = false }
}

function selectUser(user: any) {
  const textarea = textareaRef.value
  if (!textarea) return
  const cursorPos = textarea.selectionStart
  const { text, pos } = insertMention(props.modelValue, cursorPos, user)
  emit('update:modelValue', text)
  showMentionList.value = false
  setTimeout(() => { textarea.selectionStart = textarea.selectionEnd = pos }, 10)
}

defineExpose({ textareaRef })
</script>
