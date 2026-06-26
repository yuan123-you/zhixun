export interface AIResponse {
  content: string
  usage: string
}

export const aiApi = {
  generateText: (prompt: string, context?: string, mode = 'expand') => {
    const { post } = useApi()
    return post<AIResponse>('/ai/write', { prompt, context, mode })
  },
  summarizeText: (content: string) => {
    const { post } = useApi()
    const instance = (useApi() as any).instance
    // Use raw axios instance for text/plain content type
    return instance.post('/ai/summarize', content, { headers: { 'Content-Type': 'text/plain' } })
  },
  reviewContent: (content: string) => {
    const { post } = useApi()
    return post<AIResponse>('/ai/review', { content })
  },
}