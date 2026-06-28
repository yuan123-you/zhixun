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
    const { instance } = useApi() as any
    // 后端 @RequestBody String content 期望 raw string，非 JSON
    return instance.post('/ai/review', content, { headers: { 'Content-Type': 'text/plain' } })
  },
}