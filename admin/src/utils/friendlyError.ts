/**
 * 统一错误消息翻译器（管理后台版）
 *
 * 目标：把后端 / 网络 / HTTP 抛出的"技术性"错误信息，
 *      转成"普通管理员一眼能看懂"的中文提示。
 *
 * 翻译维度：
 *  1) 业务码（业务层 code 含义）
 *  2) HTTP 状态码（401/403/404/500...）
 *  3) 网络层错误（timeout/离线/CORS...）
 *  4) 校验错误（必填、长度、格式、唯一性...）
 *  5) 兜底：原始 message 中出现技术关键词时替换成友好提示
 *
 * 设计原则：
 *  - 1~2 句话内能讲清楚"为什么 + 怎么办"
 *  - 移动端不溢出：每条消息控制在 60 字内
 *  - 完全未匹配时返回原 message，不强行翻译
 */

export interface FriendlyErrorOptions {
  raw?: any
  code?: number | string
  status?: number
  message?: string
  fatal?: boolean
  fallback?: string
}

export interface FriendlyError {
  title: string
  detail?: string
  code?: number | string
  status?: number
  raw?: any
}

// 管理后台业务码语义表（与后端约定，可按需扩充）
const BUSINESS_CODE_MAP: Record<string, string> = {
  '0': '操作成功',
  '200': '操作成功',
  '400': '请求参数有误，请检查后重试',
  '401': '登录已过期，请重新登录',
  '403': '您没有权限执行此操作',
  '404': '请求的资源不存在',
  '408': '请求超时，请稍后重试',
  '409': '操作冲突，请刷新后重试',
  '422': '提交的内容不合法',
  '429': '操作太频繁，请稍后再试',
  '500': '服务器繁忙，请稍后重试',
  '502': '服务暂时不可用，请稍后重试',
  '503': '服务维护中，请稍后重试',
  '504': '服务响应超时，请稍后重试',
  // 业务
  '1001': '账号或密码错误',
  '1003': '账号已被封禁',
  '1004': '验证码错误或已过期',
  '1005': '该数据已存在，请勿重复添加',
  '1010': '两次密码输入不一致',
  '2001': '内容不存在或已删除',
  '2010': '含有敏感词，请修改后再试',
  '2011': '内容超出最大长度限制',
  '4001': '上传文件过大',
  '4002': '不支持的文件格式',
  '4003': '上传失败，请重试',
  '5001': '积分不足',
  '9999': '服务异常，请稍后重试',
}

// 技术关键词 → 友好提示（命中即替换）
const TECH_KEYWORD_MAP: Array<{ pattern: RegExp; replace: string }> = [
  { pattern: /ERR_NETWORK|network\s*error|fetch failed/i, replace: '网络连接失败，请检查网络后重试' },
  { pattern: /ERR_CONNECTION_REFUSED|ECONNREFUSED|connection refused/i, replace: '服务连接失败，请稍后重试' },
  { pattern: /ERR_CANCELED|aborted/i, replace: '请求已取消' },
  { pattern: /timeout|ECONNABORTED|request timeout|timed out/i, replace: '请求超时，请检查网络后重试' },
  { pattern: /CORS|Access-Control-Allow-Origin/i, replace: '访问受限，请联系管理员' },
  { pattern: /status\s*[:=]\s*5\d\d/i, replace: '服务繁忙，请稍后重试' },
  { pattern: /status\s*[:=]\s*404/i, replace: '请求的资源不存在' },
  { pattern: /status\s*[:=]\s*403/i, replace: '您没有权限执行此操作' },
  { pattern: /status\s*[:=]\s*401/i, replace: '登录已过期，请重新登录' },
  { pattern: /Failed to fetch dynamically imported module/i, replace: '模块加载失败，请刷新重试' },
  { pattern: /QuotaExceeded|exceeded the quota/i, replace: '本地存储已满，请清理浏览器数据' },
]

const VALIDATION_PATTERNS: Array<{ pattern: RegExp; replace: string }> = [
  { pattern: /必填|required|cannot be empty/i, replace: '请填写必填项' },
  { pattern: /长度|length.*(between|min|max)|too\s*long|too\s*short/i, replace: '内容长度不合法' },
  { pattern: /格式|format|invalid\s*email|邮箱.*格式/i, replace: '内容格式不正确' },
  { pattern: /手机号.*格式|phone.*invalid/i, replace: '手机号格式不正确' },
  { pattern: /密码.*强度|password.*strong/i, replace: '密码强度不够' },
  { pattern: /两次.*密码|password.*match/i, replace: '两次密码输入不一致' },
  { pattern: /验证码.*错误|captcha.*invalid/i, replace: '验证码错误或已过期' },
  { pattern: /已存在|already\s*exists|duplicate/i, replace: '内容已存在' },
  { pattern: /File too large|file size exceeds/i, replace: '文件过大，请选择较小的文件' },
  { pattern: /invalid\s*file\s*type|unsupported.*format/i, replace: '文件格式不支持' },
]

/** 友好化处理：把任意错误收敛成 { title, detail } */
export function toFriendlyError(options: FriendlyErrorOptions = {}): FriendlyError {
  const { raw, code, status, message, fallback } = options

  const out: FriendlyError = {
    title: '',
    detail: undefined,
    code,
    status,
    raw,
  }

  // 1) 业务码优先（最具体）
  if (code !== undefined && code !== null && BUSINESS_CODE_MAP[String(code)]) {
    out.title = BUSINESS_CODE_MAP[String(code)]
    if (message && message !== out.title) out.detail = trimDetail(message)
    return finalize(out, fallback)
  }

  // 2) HTTP 状态码
  if (status !== undefined && BUSINESS_CODE_MAP[String(status)]) {
    out.title = BUSINESS_CODE_MAP[String(status)]
    if (message && message !== out.title) out.detail = trimDetail(message)
    return finalize(out, fallback)
  }

  // 3) 原始 message / raw.message 中的技术关键词
  const rawText = (typeof message === 'string' && message)
    || (typeof raw === 'string' && raw)
    || raw?.message
    || raw?.toString?.()
    || ''

  if (rawText) {
    for (const { pattern, replace } of TECH_KEYWORD_MAP) {
      if (pattern.test(rawText)) {
        out.title = replace
        out.detail = rawText !== replace ? trimDetail(rawText) : undefined
        return finalize(out, fallback)
      }
    }
    for (const { pattern, replace } of VALIDATION_PATTERNS) {
      if (pattern.test(rawText)) {
        out.title = replace
        out.detail = rawText !== replace ? trimDetail(rawText) : undefined
        return finalize(out, fallback)
      }
    }
  }

  // 4) 命中具体后端业务消息
  if (rawText) {
    if (looksTechnical(rawText)) {
      out.title = fallback || '操作失败，请稍后重试'
      out.detail = trimDetail(rawText)
      return out
    }
    out.title = trimTitle(rawText)
    return finalize(out, fallback)
  }

  // 5) 终极兜底
  out.title = fallback || '操作失败，请稍后重试'
  return out
}

function finalize(out: FriendlyError, fallback?: string): FriendlyError {
  if (!out.title) out.title = fallback || '操作失败，请稍后重试'
  if (out.detail && out.detail === out.title) out.detail = undefined
  return out
}

function trimTitle(s: string): string {
  const trimmed = String(s).replace(/\s+/g, ' ').trim()
  if (trimmed.length <= 60) return trimmed
  return trimmed.slice(0, 58) + '…'
}

function trimDetail(s: string): string | undefined {
  if (!s) return undefined
  const trimmed = String(s).replace(/\s+/g, ' ').trim()
  if (trimmed.length <= 120) return trimmed
  return trimmed.slice(0, 118) + '…'
}

function looksTechnical(text: string): boolean {
  if (/[A-Z]{2,}_[A-Z_]+/.test(text)) return true
  if (/[a-z][A-Z]/.test(text)) return true
  if (/^\s*at\s+[A-Za-z_$]+\s*\(/.test(text)) return true
  if (/Exception|Error:|Failed to|undefined|null is not/i.test(text)) return true
  if (!/[\u4e00-\u9fa5]/.test(text) && text.length > 8) return true
  return false
}

/**
 * 直接从一个任意值里提取友好消息（仅 title 字符串）。
 * 适用于 ElMessage.error(msg) 这类只接收一个字符串的 API。
 */
export function friendlyMessage(input: any, fallback = '操作失败，请稍后重试'): string {
  if (input === null || input === undefined) return fallback
  if (typeof input === 'string') {
    const trimmed = input.trim()
    if (!trimmed) return fallback
    const fe = toFriendlyError({ message: trimmed })
    return fe.title || fallback
  }
  const fe = toFriendlyError({
    raw: input,
    code: input?.code,
    status: input?.status ?? input?.response?.status,
    message: input?.response?.data?.message ?? input?.message,
  })
  return fe.title || fallback
}

export default toFriendlyError
