import { get, del } from './request'
import type { ConversationInfo, MessageInfo, MessageQuery, PageResult } from '@/types'

/** 获取会话列表 */
export function getConversationList(params: MessageQuery) {
  return get<PageResult<ConversationInfo>>('/admin/messages/conversations', params as unknown as Record<string, unknown>)
}

/** 获取会话消息 */
export function getConversationMessages(conversationId: number, page = 1, pageSize = 20) {
  return get<PageResult<MessageInfo>>(`/admin/messages/conversations/${conversationId}`, { page, pageSize } as unknown as Record<string, unknown>)
}

/** 删除消息 */
export function deleteMessage(id: number) {
  return del(`/admin/messages/${id}`)
}