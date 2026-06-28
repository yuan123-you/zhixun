/**
 * 统一错误消息翻译器
 *
 * 目标：把后端 / 网络 / 框架 / 浏览器抛出的"技术性"错误信息，
 *      转成"普通用户一眼能看懂"的中文提示。
 *
 * 覆盖范围：
 *  1) 业务码（业务层 code 含义）
 *  2) HTTP 状态码（401/403/404/500...）
 *  3) 网络层错误（timeout/离线/CORS...）
 *  4) 浏览器 / 框架错误（render/hydration/vite hmr...）
 *  5) 表单 / 校验错误（字段必填、长度、格式...）
 *  6) 兜底：原始 message 中出现技术关键词时替换成友好提示
 *
 * 设计原则：
 *  - 翻译结果必须 1~2 句话内能讲清楚"为什么 + 怎么办"
 *  - 移动端不溢出：每条消息控制在 60 字内
 *  - 完全未匹配时返回原 message，不强行翻译
 */

export interface FriendlyErrorOptions {
  /** 原始错误对象 */
  raw?: any
  /** 业务 code（可与 http status 并存） */
  code?: number | string
  /** http 状态码 */
  status?: number
  /** 业务消息（来自后端 data.message） */
  message?: string
  /** 是否致命（影响是否弹全屏遮罩） */
  fatal?: boolean
  /** 自定义兜底：上述都没匹配时使用 */
  fallback?: string
}

export interface FriendlyError {
  title: string
  detail?: string
  code?: number | string
  status?: number
  raw?: any
}

// ====== 业务码语义表（与后端约定，可按需扩充） ======
const BUSINESS_CODE_MAP: Record<string, string> = {
  // 通用
  '0': '操作成功',
  '200': '操作成功',
  '400': '请求有误，请检查后重试',
  '401': '登录已过期，请重新登录',
  '403': '您没有权限执行此操作',
  '404': '内容不存在或已被删除',
  '408': '请求超时，请稍后重试',
  '409': '操作冲突，请刷新后重试',
  '422': '提交的内容不合法',
  '429': '操作太频繁，请稍后再试',
  '500': '服务繁忙，请稍后重试',
  '502': '服务暂时不可用，请稍后重试',
  '503': '服务维护中，请稍后重试',
  '504': '服务响应超时，请稍后重试',
  // 业务
  '1001': '账号或密码错误',
  '1002': '账号未激活，请先验证邮箱/手机',
  '1003': '账号已被封禁',
  '1004': '验证码错误或已过期',
  '1005': '该手机号/邮箱已被注册',
  '1010': '两次密码输入不一致',
  '2001': '文章不存在或已删除',
  '2002': '文章正在审核中',
  '2003': '文章已发布，无法重复操作',
  '2010': '含有敏感词，请修改后再试',
  '2011': '内容超出最大长度限制',
  '3001': '关注失败，请稍后重试',
  '3002': '不能关注自己',
  '3010': '评论失败，请稍后重试',
  '3011': '评论包含不当内容',
  '4001': '上传文件过大',
  '4002': '不支持的文件格式',
  '4003': '上传失败，请重试',
  '5001': '积分不足',
  '5002': '签到失败，请稍后重试',
  '6001': '活动已结束',
  '6002': '活动尚未开始',
  '9999': '服务异常，请稍后重试',
}

// ====== 技术关键词 → 友好提示（命中即替换） ======
// 按"更具体"排在前面的顺序匹配
const TECH_KEYWORD_MAP: Array<{ pattern: RegExp; replace: string }> = [
  // 网络
  { pattern: /ERR_NETWORK|network\s*error|fetch failed/i, replace: '网络连接失败，请检查 Wi-Fi 或移动数据' },
  { pattern: /ERR_CONNECTION_REFUSED|ECONNREFUSED|connection refused/i, replace: '服务连接失败，请稍后重试' },
  { pattern: /ERR_CANCELED|aborted/i, replace: '请求已取消' },
  { pattern: /timeout|ECONNABORTED|request timeout|timed out/i, replace: '请求超时，请检查网络后重试' },
  { pattern: /CORS|Access-Control-Allow-Origin/i, replace: '访问受限，请联系管理员' },
  { pattern: /status\s*[:=]\s*5\d\d/i, replace: '服务繁忙，请稍后重试' },
  { pattern: /status\s*[:=]\s*404/i, replace: '内容不存在或已被删除' },
  { pattern: /status\s*[:=]\s*403/i, replace: '您没有权限执行此操作' },
  { pattern: /status\s*[:=]\s*401/i, replace: '登录已过期，请重新登录' },
  // 框架 / Vue
  { pattern: /hydration|hydrating|hydrate/i, replace: '页面初始化失败，请刷新重试' },
  { pattern: /instance\s*unavailable/i, replace: '页面加载异常，请刷新后重试' },
  { pattern: /Cannot\s*(read|access).*of\s*(null|undefined)/i, replace: '页面数据异常，请刷新后重试' },
  { pattern: /render\s*error|component\s*error|vnode/i, replace: '页面组件异常，请刷新后重试' },
  { pattern: /Failed to fetch dynamically imported module/i, replace: '模块加载失败，请刷新重试' },
  // HMR / 开发环境
  { pattern: /hmr|hot.?reload|server connection lost|websocket|Polling for restart/i, replace: '服务正在更新，请稍候' },
  // 路由
  { pattern: /NavigationDuplicated|redirect.*navigation/i, replace: '页面切换中...' },
  // 存储
  { pattern: /QuotaExceeded|exceeded the quota/i, replace: '本地存储已满，请清理浏览器数据' },
  { pattern: /localStorage|IndexedDB/i, replace: '浏览器存储不可用，请检查浏览器设置' },
  // 权限
  { pattern: /permission denied|NotAllowed|denied\s*permission/i, replace: '浏览器拒绝了此操作，请在设置中允许后重试' },
  // 上传
  { pattern: /File too large|file size exceeds/i, replace: '文件过大，请选择较小的文件' },
  { pattern: /invalid\s*file\s*type|unsupported.*format/i, replace: '文件格式不支持' },
]

// ====== 表单 / 校验错误正则 ======
const VALIDATION_PATTERNS: Array<{ pattern: RegExp; replace: string }> = [
  { pattern: /必填|required|cannot be empty/i, replace: '请填写必填项' },
  { pattern: /长度|length.*(between|min|max)|too\s*long|too\s*short/i, replace: '内容长度不合法' },
  { pattern: /格式|format|invalid\s*email|邮箱.*格式/i, replace: '内容格式不正确' },
  { pattern: /手机号.*格式|phone.*invalid/i, replace: '手机号格式不正确' },
  { pattern: /密码.*强度|password.*strong/i, replace: '密码强度不够' },
  { pattern: /两次.*密码|password.*match/i, replace: '两次密码输入不一致' },
  { pattern: /验证码.*错误|captcha.*invalid/i, replace: '验证码错误或已过期' },
  { pattern: /已存在|already\s*exists|duplicate/i, replace: '内容已存在' },
]

/**
 * 友好化处理：把任意错误收敛成 { title, detail }
 */
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
    // 先尝试命中技术关键词（命中即替换为友好提示）
    for (const { pattern, replace } of TECH_KEYWORD_MAP) {
      if (pattern.test(rawText)) {
        out.title = replace
        out.detail = rawText !== replace ? trimDetail(rawText) : undefined
        return finalize(out, fallback)
      }
    }
    // 再尝试命中校验关键词
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
    // 如果是英文 / 含下划线 / 含大写代码 → 视为技术消息，强制用兜底
    if (looksTechnical(rawText)) {
      out.title = fallback || '操作失败，请稍后重试'
      out.detail = trimDetail(rawText)
      return out
    }
    // 否则原样返回（可能是后端已经写好的友好提示）
    out.title = trimTitle(rawText)
    return finalize(out, fallback)
  }

  // 5) 终极兜底
  out.title = fallback || '操作失败，请稍后重试'
  return out
}

function finalize(out: FriendlyError, fallback?: string): FriendlyError {
  if (!out.title) out.title = fallback || '操作失败，请稍后重试'
  // 避免 detail 和 title 完全一致
  if (out.detail && out.detail === out.title) out.detail = undefined
  return out
}

/** 限制 title 长度，移动端一行内可读完 */
function trimTitle(s: string): string {
  const trimmed = String(s).replace(/\s+/g, ' ').trim()
  if (trimmed.length <= 60) return trimmed
  return trimmed.slice(0, 58) + '…'
}

/** detail 限长，超出截断 */
function trimDetail(s: string): string | undefined {
  if (!s) return undefined
  const trimmed = String(s).replace(/\s+/g, ' ').trim()
  if (trimmed.length <= 120) return trimmed
  return trimmed.slice(0, 118) + '…'
}

/** 简单启发式：是否像是技术/未翻译的英文消息 */
function looksTechnical(text: string): boolean {
  // 包含下划线、驼峰英文、HTTP 关键字、堆栈特征等
  if (/[A-Z]{2,}_[A-Z_]+/.test(text)) return true
  if (/[a-z][A-Z]/.test(text)) return true
  if (/^\s*at\s+[A-Za-z_$]+\s*\(/.test(text)) return true // 堆栈
  if (/Exception|Error:|Failed to|undefined|null is not/i.test(text)) return true
  // 整体英文（无可识别中文字符）超过 8 个字符也视为技术
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
