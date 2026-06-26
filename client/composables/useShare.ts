/**
 * 分享功能 composable
 * 支持 QQ、微信、微博 跳转分享，以及链接复制到剪贴板
 */

/** 分享平台 */
export type SharePlatform = 'qq' | 'wechat' | 'weibo' | 'link'

/** 分享参数 */
export interface ShareParams {
  url: string
  title: string
  summary?: string
  image?: string
}

/** 分享结果 */
export interface ShareResult {
  success: boolean
  message?: string
  /** 是否需要降级处理（如微信桌面端无法直接唤起） */
  fallback?: boolean
}

/** 平台名称映射 */
const PLATFORM_NAMES: Record<SharePlatform, string> = {
  qq: 'QQ',
  wechat: '微信',
  weibo: '微博',
  link: '',
}

/** 获取平台中文名 */
export function getPlatformName(platform: SharePlatform): string {
  return PLATFORM_NAMES[platform]
}

/** 检测是否为移动端 */
export function isMobile(): boolean {
  if (!import.meta.client) return false
  return /Android|iPhone|iPad|iPod|webOS/i.test(navigator.userAgent)
}

/** 复制文本到剪贴板 */
export async function copyToClipboard(text: string): Promise<ShareResult> {
  try {
    // 优先使用现代 Clipboard API
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
      return { success: true }
    }
    // 降级方案：使用 execCommand
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.left = '-9999px'
    textarea.style.top = '-9999px'
    document.body.appendChild(textarea)
    textarea.focus()
    textarea.select()
    const successful = document.execCommand('copy')
    document.body.removeChild(textarea)
    if (successful) {
      return { success: true }
    }
    return { success: false, message: '复制失败，请手动复制' }
  } catch {
    return { success: false, message: '复制失败，请手动复制' }
  }
}

/** 构建 QQ 分享 URL */
export function buildQQShareUrl(params: ShareParams): string {
  const { url, title, summary = '' } = params
  return `https://connect.qq.com/widget/shareqq/index.html?url=${encodeURIComponent(url)}&title=${encodeURIComponent(title)}&summary=${encodeURIComponent(summary || title)}&source=知讯&desc=${encodeURIComponent(summary || title)}`
}

/** 构建微博分享 URL */
export function buildWeiboShareUrl(params: ShareParams): string {
  const { url, title, image = '' } = params
  let shareUrl = `https://service.weibo.com/share/share.php?url=${encodeURIComponent(url)}&title=${encodeURIComponent(title)}`
  if (image) {
    shareUrl += `&pic=${encodeURIComponent(image)}`
  }
  return shareUrl
}

/** 构建微信二维码分享链接（桌面端降级方案） */
export function buildWechatQRUrl(url: string): string {
  // 使用微信开放平台生成二维码的API
  return url
}

/** 尝试唤起微信 App（移动端） */
export function tryOpenWechatApp(url: string): ShareResult {
  if (!isMobile()) {
    return { success: false, fallback: true, message: '桌面端请使用微信扫码分享' }
  }
  // 尝试通过 scheme 唤起微信（仅移动端尝试）
  const wechatUrl = `weixin://dl/business/?t=${encodeURIComponent(url)}`
  try {
    window.open(wechatUrl, '_self')
    return { success: true }
  } catch {
    return { success: false, fallback: true, message: '未安装微信或唤起失败' }
  }
}

/** 跳转到外部分享页面 */
export function openShareUrl(shareUrl: string): ShareResult {
  try {
    const win = window.open(shareUrl, '_blank', 'noopener,noreferrer')
    if (!win) {
      // 可能被浏览器拦截
      window.location.href = shareUrl
    }
    return { success: true }
  } catch {
    return { success: false, message: '跳转失败，请检查浏览器设置' }
  }
}

/** 分享到指定平台（不含确认逻辑） */
export function executeShare(platform: SharePlatform, params: ShareParams): ShareResult {
  switch (platform) {
    case 'qq': {
      const qqUrl = buildQQShareUrl(params)
      return openShareUrl(qqUrl)
    }
    case 'weibo': {
      const weiboUrl = buildWeiboShareUrl(params)
      return openShareUrl(weiboUrl)
    }
    case 'wechat': {
      return tryOpenWechatApp(params.url)
    }
    case 'link':
    default: {
      // link 分享应使用 copyToClipboard（异步），此处返回占位结果
      return { success: false, message: '' }
    }
  }
}

/**
 * 应用卸载检测（移动端尝试唤起后检测是否成功）
 * 原理：唤起外部应用后，浏览器会进入后台；如果一定时间后仍在当前页面，说明唤起失败
 */
export function detectAppOpenFailure(timeout = 1500): Promise<boolean> {
  return new Promise((resolve) => {
    if (!isMobile()) {
      resolve(false)
      return
    }
    // 监听页面可见性变化
    let resolved = false
    const handleVisibility = () => {
      if (resolved) return
      // 页面隐藏说明成功跳转了
      if (document.hidden) {
        resolved = true
        cleanup()
        resolve(false) // false 表示成功（没有失败）
      }
    }
    document.addEventListener('visibilitychange', handleVisibility)
    const cleanup = () => {
      document.removeEventListener('visibilitychange', handleVisibility)
      clearTimeout(timer)
    }
    const timer = setTimeout(() => {
      if (!resolved) {
        resolved = true
        cleanup()
        resolve(true) // true 表示失败
      }
    }, timeout)
  })
}
