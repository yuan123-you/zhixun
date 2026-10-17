import type { Message } from '@/types'
import type { GroupMessage } from '@/api/group'

/**
 * 本地聊天数据持久化与缓存存储（IndexedDB）
 * 支持私信消息与群聊消息本地存储、即时加载与防重复请求
 */

const DB_NAME = 'zhixun_chat'
const DB_VERSION = 2
const STORE_NAME = 'messages'
const GROUP_STORE_NAME = 'group_messages'

let dbInstance: IDBDatabase | null = null

/** 生成会话 key：双方 userId 排序后拼接，确保双向一致 */
function convKey(uid1: number, uid2: number): string {
  return uid1 < uid2 ? `${uid1}_${uid2}` : `${uid2}_${uid1}`
}

/** 打开/初始化 IndexedDB */
function openDb(): Promise<IDBDatabase> {
  if (dbInstance) return Promise.resolve(dbInstance)

  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION)

    request.onupgradeneeded = () => {
      const db = request.result
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        const store = db.createObjectStore(STORE_NAME, { keyPath: 'id' })
        store.createIndex('convKey', 'convKey', { unique: false })
        store.createIndex('createdAt', 'createdAt', { unique: false })
        store.createIndex('conv_created', ['convKey', 'createdAt'], { unique: false })
      }
      if (!db.objectStoreNames.contains(GROUP_STORE_NAME)) {
        const groupStore = db.createObjectStore(GROUP_STORE_NAME, { keyPath: 'id' })
        groupStore.createIndex('groupId', 'groupId', { unique: false })
        groupStore.createIndex('createdAt', 'createdAt', { unique: false })
        groupStore.createIndex('group_created', ['groupId', 'createdAt'], { unique: false })
      }
    }

    request.onsuccess = () => {
      dbInstance = request.result
      dbInstance.onclose = () => { dbInstance = null }
      resolve(dbInstance)
    }

    request.onerror = () => {
      console.error('[ChatLocalDb] 打开 IndexedDB 失败:', request.error)
      reject(request.error)
    }
  })
}

// ==================== 私信相关 ====================

/** 为消息附加 convKey 字段后写入 */
function toStoredMsg(msg: Message, myUserId: number, otherUserId: number): Message & { convKey: string } {
  return { ...msg, convKey: convKey(myUserId, otherUserId) }
}

/** 批量写入私信消息（put = upsert），需显式传入对话另一方 userId */
async function saveMessages(msgs: Message[], myUserId: number, otherUserId: number): Promise<void> {
  if (!msgs.length) return
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    for (const msg of msgs) {
      store.put(toStoredMsg(msg, myUserId, otherUserId))
    }
    tx.oncomplete = () => resolve()
    tx.onerror = () => {
      console.error('[ChatLocalDb] 写入失败:', tx.error)
      reject(tx.error)
    }
  })
}

/** 按会话查询私信消息，按时间正序返回 */
async function getMessages(myUserId: number, otherUserId: number): Promise<Message[]> {
  const db = await openDb()
  const key = convKey(myUserId, otherUserId)

  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly')
    const store = tx.objectStore(STORE_NAME)
    const index = store.index('convKey')
    const request = index.getAll(key)

    request.onsuccess = () => {
      const results = (request.result || []) as Message[]
      results.sort((a, b) => a.createdAt.localeCompare(b.createdAt))
      resolve(results)
    }

    request.onerror = () => {
      console.error('[ChatLocalDb] 查询失败:', request.error)
      reject(request.error)
    }
  })
}

/** 获取本地某个会话中最早的消息时间 */
async function getEarliestTime(myUserId: number, otherUserId: number): Promise<string | null> {
  const msgs = await getMessages(myUserId, otherUserId)
  return msgs.length > 0 ? msgs[0].createdAt : null
}

/** 删除指定会话的所有本地私信消息 */
async function deleteByConv(myUserId: number, otherUserId: number): Promise<number> {
  const db = await openDb()
  const key = convKey(myUserId, otherUserId)

  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite')
    const store = tx.objectStore(STORE_NAME)
    const index = store.index('convKey')
    const request = index.openCursor(key)
    let count = 0

    request.onsuccess = () => {
      const cursor = request.result
      if (cursor) {
        cursor.delete()
        count++
        cursor.continue()
      }
    }

    tx.oncomplete = () => resolve(count)
    tx.onerror = () => {
      console.error('[ChatLocalDb] 删除会话消息失败:', tx.error)
      reject(tx.error)
    }
  })
}

/** 合并服务端私信消息与本地消息，去重后返回完整列表（时间正序） */
function mergeMessages(local: Message[], server: Message[]): Message[] {
  const map = new Map<number, Message>()
  for (const m of local) map.set(m.id, m)
  for (const m of server) map.set(m.id, m)
  return Array.from(map.values()).sort((a, b) => a.createdAt.localeCompare(b.createdAt))
}

// ==================== 群聊相关 ====================

/** 批量写入群组消息（upsert） */
async function saveGroupMessages(msgs: GroupMessage[], groupId: number): Promise<void> {
  if (!msgs.length) return
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(GROUP_STORE_NAME, 'readwrite')
    const store = tx.objectStore(GROUP_STORE_NAME)
    for (const msg of msgs) {
      store.put({ ...msg, groupId })
    }
    tx.oncomplete = () => resolve()
    tx.onerror = () => {
      console.error('[ChatLocalDb] 群消息写入失败:', tx.error)
      reject(tx.error)
    }
  })
}

/** 写入单条群组消息 */
async function saveGroupMessage(msg: GroupMessage, groupId: number): Promise<void> {
  return saveGroupMessages([msg], groupId)
}

/** 按群组 ID 获取本地缓存的群聊消息，按时间正序返回 */
async function getGroupMessages(groupId: number): Promise<GroupMessage[]> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(GROUP_STORE_NAME, 'readonly')
    const store = tx.objectStore(GROUP_STORE_NAME)
    const index = store.index('groupId')
    const request = index.getAll(groupId)

    request.onsuccess = () => {
      const results = (request.result || []) as GroupMessage[]
      results.sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''))
      resolve(results)
    }

    request.onerror = () => {
      console.error('[ChatLocalDb] 群消息查询失败:', request.error)
      reject(request.error)
    }
  })
}

/** 合并群组本地消息与服务端消息（服务端权威覆盖，时间正序） */
function mergeGroupMessages(local: GroupMessage[], server: GroupMessage[]): GroupMessage[] {
  const map = new Map<number, GroupMessage>()
  for (const m of local) map.set(m.id, m)
  for (const m of server) map.set(m.id, m)
  return Array.from(map.values()).sort((a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''))
}

/** 删除指定群组的所有本地消息 */
async function deleteGroupMessages(groupId: number): Promise<number> {
  const db = await openDb()
  return new Promise((resolve, reject) => {
    const tx = db.transaction(GROUP_STORE_NAME, 'readwrite')
    const store = tx.objectStore(GROUP_STORE_NAME)
    const index = store.index('groupId')
    const request = index.openCursor(groupId)
    let count = 0

    request.onsuccess = () => {
      const cursor = request.result
      if (cursor) {
        cursor.delete()
        count++
        cursor.continue()
      }
    }

    tx.oncomplete = () => resolve(count)
    tx.onerror = () => reject(tx.error)
  })
}

/** 退出登录时清空所有本地消息 */
async function deleteAll(): Promise<void> {
  if (!dbInstance) return
  return new Promise((resolve, reject) => {
    const tx = dbInstance!.transaction([STORE_NAME, GROUP_STORE_NAME], 'readwrite')
    tx.objectStore(STORE_NAME).clear()
    tx.objectStore(GROUP_STORE_NAME).clear()
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
}

interface MessageFilters {
  startDate?: string
  endDate?: string
  type?: string
  keyword?: string
}

async function getFilteredMessages(
  myUserId: number,
  otherUserId: number,
  filters: MessageFilters = {},
): Promise<Message[]> {
  const msgs = await getMessages(myUserId, otherUserId)
  let result = msgs

  if (filters.startDate) {
    const start = filters.startDate + 'T00:00:00'
    result = result.filter(m => m.createdAt >= start)
  }
  if (filters.endDate) {
    const end = filters.endDate + 'T23:59:59'
    result = result.filter(m => m.createdAt <= end)
  }
  if (filters.type) {
    result = result.filter(m => m.type === filters.type)
  }
  if (filters.keyword) {
    const kw = filters.keyword.toLowerCase()
    result = result.filter(m => m.content?.toLowerCase().includes(kw))
  }

  return result.reverse()
}

interface ConversationStats {
  totalCount: number
  imageCount: number
  textCount: number
  earliestTime: string | null
  latestTime: string | null
}

async function getStats(myUserId: number, otherUserId: number): Promise<ConversationStats> {
  const msgs = await getMessages(myUserId, otherUserId)
  return {
    totalCount: msgs.length,
    imageCount: msgs.filter(m => m.type === 'image').length,
    textCount: msgs.filter(m => m.type !== 'image').length,
    earliestTime: msgs.length > 0 ? msgs[0].createdAt : null,
    latestTime: msgs.length > 0 ? msgs[msgs.length - 1].createdAt : null,
  }
}

export const useChatLocalDb = () => ({
  openDb,
  // 私信
  saveMessages,
  getMessages,
  getEarliestTime,
  deleteAll,
  deleteByConv,
  getFilteredMessages,
  getStats,
  mergeMessages,
  // 群聊
  saveGroupMessages,
  saveGroupMessage,
  getGroupMessages,
  mergeGroupMessages,
  deleteGroupMessages,
})
