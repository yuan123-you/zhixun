import { defineStore } from 'pinia'
import { groupApi } from '@/api/group'
import type { GroupInfo, GroupMessage, GroupMember } from '@/api/group'
import { useChatLocalDb } from '@/composables/useChatLocalDb'

export const useGroupStore = defineStore('group', () => {
  const localDb = useChatLocalDb()
  const groups = ref<GroupInfo[]>([])
  const currentGroup = ref<GroupInfo | null>(null)
  const messages = ref<GroupMessage[]>([])
  const members = ref<GroupMember[]>([])
  const loading = ref(false)

  async function fetchMyGroups() {
    try {
      const res = await groupApi.getMyGroups()
      const data = res.data.data
      groups.value = data?.list || (Array.isArray(data) ? data : [])
    } catch { /* ignore */ }
  }

  async function fetchGroupDetail(id: number) {
    try {
      const res = await groupApi.getGroupDetail(id)
      currentGroup.value = res.data.data
      return res.data.data
    } catch (e) { throw e }
  }

  async function fetchMessages(groupId: number, offset = 0) {
    if (offset === 0) {
      try {
        const local = await localDb.getGroupMessages(groupId)
        if (local.length > 0) {
          messages.value = local
        }
      } catch { /* ignore */ }
    }
    try {
      const res = await groupApi.getMessages(groupId, offset)
      const data = res.data.data
      const rawList = Array.isArray(data) ? data : ((data as any)?.list || [])
      const serverMsgs = [...rawList].reverse()
      if (offset === 0) {
        messages.value = localDb.mergeGroupMessages(messages.value, serverMsgs)
        localDb.saveGroupMessages(serverMsgs, groupId).catch(() => {})
      } else {
        messages.value = [...serverMsgs, ...messages.value]
        localDb.saveGroupMessages(serverMsgs, groupId).catch(() => {})
      }
    } catch { /* ignore */ }
  }

  async function fetchMembers(groupId: number) {
    try {
      const res = await groupApi.getMembers(groupId)
      members.value = res.data.data || []
    } catch { /* ignore */ }
  }

  function addMessage(msg: GroupMessage) {
    if (!messages.value.some(m => m.id === msg.id)) {
      messages.value.push(msg)
      if (msg.groupId) {
        localDb.saveGroupMessage(msg, msg.groupId).catch(() => {})
      }
    }
  }

  async function createGroup(name: string, description?: string, avatar?: string) {
    const res = await groupApi.createGroup({ name, description, avatar })
    await fetchMyGroups()
    return res.data.data
  }

  return {
    groups, currentGroup, messages, members, loading,
    fetchMyGroups, fetchGroupDetail, fetchMessages, fetchMembers,
    addMessage, createGroup,
  }
})
