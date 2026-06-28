export interface Collaborator {
  id: number
  articleId: number
  userId: number
  userName: string
  userAvatar: string
  permission: string
  status: number
  createdAt: string
}

export const collaborationApi = {
  inviteCollaborator: (articleId: number, userId: number, permission = 'edit') => {
    const { post } = useApi()
    return post<void>('/collaborations/invite', { articleId, userId, permission })
  },
  respondInvitation: (id: number, status: number) => {
    const { put } = useApi()
    return put<void>(`/collaborations/${id}/respond`, { status })
  },
  getArticleCollaborators: (articleId: number) => {
    const { get } = useApi()
    return get<Collaborator[]>(`/collaborations/article/${articleId}`)
  },
  getMyInvitations: () => {
    const { get } = useApi()
    return get<Collaborator[]>('/collaborations/invitations')
  },
  removeCollaborator: (articleId: number, userId: number) => {
    const { delete: del } = useApi()
    return del(`/collaborations/${articleId}/user/${userId}`)
  },
  canEdit: (articleId: number) => {
    const { get } = useApi()
    return get<boolean>(`/collaborations/can-edit/${articleId}`)
  },
}