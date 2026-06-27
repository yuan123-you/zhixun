<template>
  <div class="bg-white dark:bg-gray-800 rounded-2xl p-5 shadow-sm">
    <div v-if="status">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-2">
          <span class="text-xl">&#x1F525;</span>
          <span class="font-semibold text-gray-900 dark:text-white">每日签到</span>
        </div>
        <span class="text-xs text-gray-400">Lv.{{ status.level }} {{ status.levelName }}</span>
      </div>
      <div v-if="status.hasCheckedIn" class="text-center py-3">
        <div class="text-3xl mb-2">&#x2705;</div>
        <p class="text-green-600 font-medium">今日已签到</p>
        <p class="text-sm text-gray-400 mt-1">连续 {{ status.consecutiveDays }} 天 | +{{ status.todayPoints }} 积分</p>
      </div>
      <div v-else class="text-center py-3">
        <button @click="$emit('checkin')" :disabled="loading"
          class="px-8 py-2.5 bg-gradient-to-r from-blue-500 to-purple-500 text-white rounded-full font-medium hover:from-blue-600 hover:to-purple-600 disabled:opacity-50 transition shadow-lg shadow-blue-500/25">
          {{ loading ? '签到中...' : '签到领积分' }}
        </button>
        <p v-if="status.consecutiveDays > 0" class="text-xs text-gray-400 mt-2">已连续签到 {{ status.consecutiveDays }} 天</p>
      </div>
      <div class="flex justify-between mt-3 pt-3 border-t border-gray-100 dark:border-gray-700">
        <div class="text-center flex-1">
          <div class="text-lg font-bold text-gray-900 dark:text-white">{{ status.level }}</div>
          <div class="text-xs text-gray-400">等级</div>
        </div>
        <div class="text-center flex-1">
          <div class="text-lg font-bold text-gray-900 dark:text-white">{{ status.totalExp }}</div>
          <div class="text-xs text-gray-400">经验值</div>
        </div>
        <div class="text-center flex-1">
          <div class="text-lg font-bold text-gray-900 dark:text-white">{{ status.consecutiveDays }}</div>
          <div class="text-xs text-gray-400">连签天数</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  status: any
  loading: boolean
}>()

defineEmits(['checkin'])
</script>
