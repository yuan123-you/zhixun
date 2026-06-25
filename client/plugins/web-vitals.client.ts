/** Web Vitals 性能监控插件 — 采集 FCP/LCP/CLS 指标，仅客户端运行 */
export default defineNuxtPlugin(() => {
  // SSR 环境下跳过
  if (import.meta.server) return

  import('web-vitals').then(({ onLCP, onFID, onCLS }) => {
    const sendMetric = (metric: { name: string; value: number; rating: string; delta: number; id: string }) => {
      const body = {
        name: metric.name,
        value: metric.value,
        rating: metric.rating,
        delta: metric.delta,
        id: metric.id,
        page: window.location.pathname,
        timestamp: Date.now(),
      }

      if (process.env.NODE_ENV === 'production') {
        navigator.sendBeacon('/api/v1/metrics', JSON.stringify(body))
      } else {
        console.log('[WebVitals]', metric.name, metric.value, metric.rating)
      }
    }

    onLCP(sendMetric)
    onFID(sendMetric)
    onCLS(sendMetric)
  }).catch(() => {
    // web-vitals 包未安装时静默跳过
  })
})
