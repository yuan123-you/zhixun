import { defineStore } from 'pinia'
import { ref } from 'vue'
import { groupApi } from '~/api/group'
import type { GroupInfo, GroupMessage } from '~/api/group'

export const useGroupStore = defineStore('group', () => {
  const groups = ref<GroupInfo[]>([])
  const currentGroup = ref<GroupInfo | null>(null)
  const messages = ref<GroupMessage[]>([])
  const loading = ref(false)

  async function fetchMyGroups() {
    try {
      const res = await groupApi.getMyGroups()
      groups.value = res.data.data.list
    } catch (e) { /* ignore */ }
  }

  async function fetchGroupDetail(id: number) {
    try {
      const res = await groupApi.getGroupDetail(id)
      currentGroup.value = res.data.data
      return res.data.data
    } catch (e) { throw e }
  }

  async function fetchMessages(groupId: number, offset = 0) {
    try {
      const res = await groupApi.getMessages(groupId, offset)
      const data = res.data.data
      if (offset === 0) messages.value = data
      else messages.value = [...data, ...messages.value]
    } catch (e) { /* ignore */ }
  }

  function addMessage(msg: GroupMessage) {
    messages.value.push(msg)
  }

  async function createGroup(name: string, description?: string, avatar?: string) {
    const res = await groupApi.createGroup({ name, description, avatar })
    await fetchMyGroups()
    return res.data.data
  }

  return {
    groups, currentGroup, messages, loading,
    fetchMyGroups, fetchGroupDetail, fetchMessages,
    addMessage, createGroup,
  }
})
