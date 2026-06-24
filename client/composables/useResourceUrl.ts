/**
 * 资源 URL 解析组合式函数
 * 将后端返回的内部 MinIO 地址替换为公网可访问的代理路径
 */

export const useResourceUrl = () => {
  const config = useRuntimeConfig()

  /**
   * 解析资源 URL：将内部 MinIO 地址替换为公网可访问的地址
   * 后端存储的 URL 格式: http://minio:9000/bucket/path → /minio/bucket/path
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
    // 以/开头的相对路径，直接使用
    if (url.startsWith('/')) return url
    // http:// 开头但不是 MinIO 内部地址的，也直接返回（可能是其他外部资源）
    if (/^http:/.test(url)) return url
    // 其他相对路径，拼接公共API基础地址
    const base = config.public.apiBase as string
    return `${base}/${url}`
  }

  return { resolveUrl }
}
