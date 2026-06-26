<template>
  <div class="relative inline-block">
    <button @click="open = !open" class="p-1.5 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700 transition" type="button">
      <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.828 14.828a4 4 0 01-5.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
    </button>
    <div v-if="open" class="absolute bottom-full left-0 mb-2 bg-white dark:bg-gray-800 rounded-xl shadow-xl border border-gray-200 dark:border-gray-700 p-3 w-72 z-50">
      <div class="flex flex-wrap gap-1 max-h-48 overflow-y-auto">
        <button v-for="emoji in emojis" :key="emoji" @click="selectEmoji(emoji)"
          class="w-8 h-8 flex items-center justify-center text-lg hover:bg-gray-100 dark:hover:bg-gray-700 rounded transition">
          {{ emoji }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits(['select'])

const open = ref(false)

const emojis = [
  '\u{1F600}','\u{1F603}','\u{1F604}','\u{1F601}','\u{1F605}','\u{1F602}','\u{1F923}','\u{1F60A}','\u{1F607}','\u{1F642}','\u{1F609}','\u{1F60C}',
  '\u{1F60D}','\u{1F970}','\u{1F618}','\u{1F617}','\u{1F60B}','\u{1F61B}','\u{1F61C}','\u{1F92A}',
  '\u{1F60E}','\u{1F929}','\u{1F973}','\u{1F60F}','\u{1F612}','\u{1F61E}','\u{1F614}','\u{1F61F}','\u{1F615}','\u{1F641}',
  '\u{1F623}','\u{1F616}','\u{1F62B}','\u{1F629}','\u{1F97A}','\u{1F622}','\u{1F62D}','\u{1F624}','\u{1F620}','\u{1F621}',
  '\u{1F914}','\u{1F917}','\u{1F92D}','\u{1F44D}','\u{1F44E}','\u{1F44F}','\u{1F64C}','\u{1F91D}','\u{1F4AA}','\u{270C}\u{FE0F}',
  '\u{1F91E}','\u{1F91F}','\u{2764}\u{FE0F}','\u{1F9E1}','\u{1F49B}','\u{1F49A}','\u{1F499}','\u{1F49C}','\u{1F5A4}','\u{1F90D}',
  '\u{1F525}','\u{2B50}','\u{1F389}','\u{1F38A}','\u{2728}','\u{1F4A1}','\u{1F4CC}','\u{1F4CD}','\u{1F3E0}','\u{1F30D}',
  '\u{1F338}','\u{1F33A}','\u{1F308}','\u{2600}\u{FE0F}','\u{1F319}','\u{26A1}','\u{1F4A7}','\u{1F3B5}','\u{1F3B6}','\u{1F4AF}'
]

function selectEmoji(emoji: string) {
  emit('select', emoji)
  open.value = false
}
</script>