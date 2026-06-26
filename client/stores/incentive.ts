import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { incentiveApi } from '~/api/incentive'
import type { CheckInStatus, Badge } from '~/api/incentive'

export const useIncentiveStore = defineStore('incentive', () => {
  const checkInStatus = ref<CheckInStatus | null>(null)
  const badges = ref<Badge[]>([])
  const myBadges = ref<Badge[]>([])
  const loading = ref(false)

  const isCheckedIn = computed(() => checkInStatus.value?.hasCheckedIn ?? false)
  const userLevel = computed(() => checkInStatus.value?.level ?? 1)
  const levelName = computed(() => checkInStatus.value?.levelName ?? '初级用户')
  const consecutiveDays = computed(() => checkInStatus.value?.consecutiveDays ?? 0)

  async function fetchCheckInStatus() {
    try {
      const res = await incentiveApi.getCheckInStatus()
      checkInStatus.value = res.data.data
    } catch (e) { /* ignore */ }
  }

  async function doCheckIn() {
    loading.value = true
    try {
      const res = await incentiveApi.checkIn()
      checkInStatus.value = res.data.data
      return res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchAllBadges() {
    try {
      const res = await incentiveApi.getAllBadges()
      badges.value = res.data.data
    } catch (e) { /* ignore */ }
  }

  async function fetchMyBadges() {
    try {
      const res = await incentiveApi.getMyBadges()
      myBadges.value = res.data.data
    } catch (e) { /* ignore */ }
  }

  return {
    checkInStatus, badges, myBadges, loading,
    isCheckedIn, userLevel, levelName, consecutiveDays,
    fetchCheckInStatus, doCheckIn, fetchAllBadges, fetchMyBadges,
  }
})