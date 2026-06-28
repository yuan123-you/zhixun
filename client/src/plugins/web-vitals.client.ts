/** Web Vitals 性能监控 —— 采集 FCP/LCP/CLS 指标，仅客户端运行 */
/** 暂不可用：web-vitals 包未安装 */
// import('web-vitals').then(({ onLCP, onFID, onCLS }) => {
//   const sendMetric = (metric: { name: string; value: number; rating: string; delta: number; id: string }) => {
//     const body = {
//       name: metric.name,
//       value: metric.value,
//       rating: metric.rating,
//       delta: metric.delta,
//       id: metric.id,
//       page: window.location.pathname,
//       timestamp: Date.now(),
//     }
//
//     // 仅在开发环境打印，生产环境暂不上报
//     if (import.meta.env.DEV) {
//       console.log('[WebVitals]', metric.name, metric.value, metric.rating)
//     }
//   }
//
//   onLCP(sendMetric)
//   onFID(sendMetric)
//   onCLS(sendMetric)
// }).catch(() => {
//   // web-vitals 包未安装时静默跳过
// })