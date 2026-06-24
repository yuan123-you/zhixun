/**
 * 资源 URL 解析组合式函数
 * 将后端返回的内部 MinIO 地址替换为公网可访问的代理路径
 */

export const useResourceUrl = () => {
  const config = useRuntimeConfig()

  /**
   * 解析资源 URL：将内部 MinIO 地址替换为公网可访问的地址
   * 后端存储的 URL 格式可能是:
   *   - http://minio:9000/bucket/path (Docker 内部地址)
   *   - http://localhost:9000/bucket/path (本地开发地址)
   *   → /minio/bucket/path (通过 Nginx 代理访问)
   */
  const resolveUrl = (url: string | null | undefined): string | null => {
    if (!url) return null
    // 已经是完整URL（https:或data:）直接返回（https 的公网地址无需替换）
    if (/^(https:|data:)/.test(url)) return url
    // 将内部 MinIO 地址替换为公网代理路径
    const minioInternal = config.public.minioInternalUrl as string
    const minioPublic = config.public.minioPublicUrl as string
    if (minioInternal && url.startsWith(minioInternal)) {
      return minioPublic + url.slice(minioInternal.length)
    }
    // 兼容后端可能存储的 localhost MinIO 地址（本地开发环境）
    // 匹配 http://localhost:9000/zhixun/... 格式
    const localhostMinioMatch = url.match(/^http:\/\/localhost:\d+\/(zhixun\/.*)$/)
    if (localhostMinioMatch) {
      return minioPublic + '/' + localhostMinioMatch[1]
    }
    // 兼容其他 MinIO 内部主机名（如 http://minio:9000/zhixun/... 但不在配置中）
    const minioHostMatch = url.match(/^http:\/\/[a-zA-Z0-9._-]+:\d+\/(zhixun\/.*)$/)
    if (minioHostMatch) {
      return minioPublic + '/' + minioHostMatch[1]
    }
    // 以/开头的相对路径，直接使用
    if (url.startsWith('/')) return url
    // 其他相对路径，拼接公共API基础地址
    const base = config.public.apiBase as string
    return `${base}/${url}`
  }

  return { resolveUrl }
}
