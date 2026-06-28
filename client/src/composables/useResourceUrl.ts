/**
 * 资源 URL 解析组合式函数
 * 将后端返回的内部 MinIO 地址替换为服务器代理路径
 */

export const useResourceUrl = () => {
  /**
   * 解析资源 URL：将内部 MinIO 地址替换为服务器代理访问地址
   * 后端存储的 URL 格式可能是:
   *   - http://minio:9000/bucket/path (Docker 内部地址)
   *   - http://localhost:9000/bucket/path (本地开发地址)
   *   - /minio/bucket/path (旧的 Vite 代理格式)
   *   → /api/v1/files/view/bucket/path （通过后端代理访问，避免 MinIO 重定向循环和 ORB）
   */
  const resolveUrl = (url: string | null | undefined): string | null => {
    if (!url) return null
    // 已经是完整URL（https:或data:）直接返回（https 的公网地址无需替换）
    if (/^(https:|data:)/.test(url)) return url
    // 拦截 MinIO Console 端口（9001）的 URL：控制台只返回 HTML，被浏览器以图片资源请求会触发 ORB
    // 将其视为无效 URL，返回 null 以便调用方走占位符兜底
    if (/^https?:\/\/[^/]*:9001(\/|$)/i.test(url)) {
      return null
    }

    // 将 MinIO 直链转换为后端代理路径，避免 MinIO 307 重定向触发 ERR_TOO_MANY_REDIRECTS
    // 匹配 http://host:port/bucket/path 格式
    const minioUrlMatch = url.match(/^https?:\/\/[^/]+\/([^/]+)\/(.+)$/)
    if (minioUrlMatch) {
      const bucket = minioUrlMatch[1]
      const objectPath = minioUrlMatch[2]
      // 仅当 bucket 看起来像 MinIO 存储桶（不含路径分隔符以外的特殊字符）时转换
      if (/^[a-zA-Z0-9._-]+$/.test(bucket)) {
        return `/api/v1/files/view/${bucket}/${objectPath}`
      }
    }

    // 兼容老的 Vite 代理 /minio/... 格式（数据库中可能存在旧数据）
    if (url.startsWith('/minio/')) {
      const pathAfter = url.slice('/minio/'.length)
      if (!pathAfter || pathAfter === '/') return null
      const firstSlash = pathAfter.indexOf('/')
      if (firstSlash > 0) {
        const bucket = pathAfter.slice(0, firstSlash)
        const objectPath = pathAfter.slice(firstSlash + 1)
        if (/^[a-zA-Z0-9._-]+$/.test(bucket) && objectPath) {
          return `/api/v1/files/view/${bucket}/${objectPath}`
        }
      }
      return null
    }
    if (url === '/minio' || url === '/minio/') return null

    // 兼容老的内部 MinIO 地址配置（保留以防 VITE_MINIO_INTERNAL_URL 设置不一致）
    const minioInternal = import.meta.env.VITE_MINIO_INTERNAL_URL as string
    const minioPublic = import.meta.env.VITE_MINIO_PUBLIC_URL || '/minio'
    if (minioInternal && url.startsWith(minioInternal)) {
      // 防止仅替换出 /minio 基础路径（无对象路径），
      // 这种请求会让 Vite 代理反复重写 Location 头造成 ERR_TOO_MANY_REDIRECTS
      const pathAfter = url.slice(minioInternal.length)
      if (!pathAfter || pathAfter === '/' || pathAfter === '?') return null
      return minioPublic + pathAfter
    }
    // 以/开头的相对路径
    if (url.startsWith('/')) {
      return url
    }
    // 其他相对路径，拼接公共API基础地址
    const base = "/api/v1" as string
    return `${base}/${url}`
  }

  return { resolveUrl }
}
