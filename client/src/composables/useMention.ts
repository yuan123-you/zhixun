import { ref } from 'vue'

interface MentionUser {
  id: number
  nickname: string
  avatar: string
  uid: string
}

export function useMention() {
  const mentionSearch = ref('')
  const mentionResults = ref<MentionUser[]>([])
  const showMentionList = ref(false)
  const mentionIndex = ref(-1)

  async function searchUsers(query: string) {
    if (!query || query.length < 1) {
      mentionResults.value = []
      showMentionList.value = false
      return
    }
    try {
      const { get } = useApi()
      const res = await get<{ list: MentionUser[] }>('/search', {
        keyword: query,
        tab: 'user',
        page: 1,
        pageSize: 5,
      })
      const data = res.data.data
      mentionResults.value = (data as any)?.list || []
      showMentionList.value = mentionResults.value.length > 0
      mentionIndex.value = -1
    } catch (e) {
      mentionResults.value = []
      showMentionList.value = false
    }
  }

  function insertMention(text: string, cursorPos: number, user: MentionUser): { text: string; pos: number } {
    const before = text.substring(0, cursorPos)
    const after = text.substring(cursorPos)
    const atIndex = before.lastIndexOf('@')
    if (atIndex === -1) return { text, pos: cursorPos }
    const newBefore = before.substring(0, atIndex) + `@${user.nickname} `
    const newText = newBefore + after
    return { text: newText, pos: newBefore.length }
  }

  function detectMentionTrigger(text: string, cursorPos: number): { triggered: boolean; query: string } {
    const beforeCursor = text.substring(0, cursorPos)
    const atMatch = beforeCursor.match(/@([^\s@]*)$/)
    if (atMatch) return { triggered: true, query: atMatch[1] }
    return { triggered: false, query: '' }
  }

  return {
    mentionSearch, mentionResults, showMentionList, mentionIndex,
    searchUsers, insertMention, detectMentionTrigger,
  }
}