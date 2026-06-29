export interface AIResponse {
  content: string
  usage: string
}

/** AI 请求超时时间（130秒），需覆盖后端 RestTemplate 120s read + 重试延迟 */
const AI_TIMEOUT = 130_000

export const aiApi = {
  generateText: (prompt: string, context?: string, mode = 'expand') => {
    const { post } = useApi()
    return post<AIResponse>('/ai/write', { prompt, context, mode }, { timeout: AI_TIMEOUT })
  },
  summarizeText: (content: string) => {
    const { instance } = useApi() as any
    // Use raw axios instance for text/plain content type
    return instance.post('/ai/summarize', content, {
      headers: { 'Content-Type': 'text/plain' },
      timeout: AI_TIMEOUT,
    })
  },
  reviewContent: (content: string) => {
    const { instance } = useApi() as any
    // 后端 @RequestBody String content 期望 raw string，非 JSON
    return instance.post('/ai/review', content, {
      headers: { 'Content-Type': 'text/plain' },
      timeout: AI_TIMEOUT,
    })
  },
}