/**
 * Friendly error fallback messages keyed by:
 *   1. Backend ApiResponse.code (preferred when present)
 *   2. HTTP status (used when backend gave nothing useful)
 *
 * Resolution order in `resolveErrorMessage(error)`:
 *   - Backend `message` if present and not a generic placeholder
 *   - CODE_MAP[backend code] if known
 *   - HTTP_MAP[http status] if known
 *   - Network category (Axios timeouts / network errors)
 *   - Generic last-resort
 */

const CODE_MAP: Record<number, string> = {
  40001: '请求参数有误，请检查后重试',
  40002: '操作不符合业务规则',
  40100: '登录已失效，正在为您跳转登录',
  40101: '登录已过期，正在为您跳转登录',
  40300: '当前账号无该操作权限',
  40301: '跨列表对比，请清空对比后重试',
  40302: '不能访问其他组织的数据',
  40303: '只能操作自己创建的资源',
  40400: '资源不存在或已被删除，请刷新后重试',
  40900: '资源冲突，可能是重复提交',
  41300: '文件过大，请压缩或分段上传',
  42900: '请求过于频繁，请稍后再试',
  50000: '服务器开了个小差，请稍后再试',
  50001: '服务暂时无法处理该请求',
  50002: 'AI 服务暂时无法响应，请稍后再试',
  50003: '数据库异常，请联系管理员',
  50004: '第三方服务暂时不可用，请稍后再试',
}

const HTTP_MAP: Record<number, string> = {
  400: '请求格式有误',
  401: '登录已失效，正在为您跳转登录',
  403: '当前账号无该操作权限',
  404: '资源不存在或已被删除',
  409: '资源冲突，可能是重复提交',
  413: '文件过大',
  429: '请求过于频繁，请稍后再试',
  500: '服务器开了个小差，请稍后再试',
  502: '后端网关无响应，请稍后再试',
  503: '服务暂时不可用，请稍后再试',
  504: '后端处理超时，请稍后再试',
}

// Backend messages we treat as too generic and prefer to override with a friendlier one
const GENERIC_BACKEND_MSGS = new Set([
  '内部错误', '业务内部错误', '系统错误', '操作失败', '请求失败', '未知错误', '服务器错误', '处理失败',
])

export function resolveErrorMessage(error: any): string {
  if (!error) return '操作未能完成，请稍后再试'

  // Axios error shape
  const status: number | undefined = error?.response?.status
  const body = error?.response?.data
  const backendCode: number | undefined = body?.code
  const backendMsg: string | undefined = body?.message

  if (backendMsg && !GENERIC_BACKEND_MSGS.has(backendMsg.trim())) {
    return backendMsg
  }
  if (typeof backendCode === 'number' && CODE_MAP[backendCode]) return CODE_MAP[backendCode]
  if (typeof status === 'number' && HTTP_MAP[status]) return HTTP_MAP[status]

  // Network layer
  const code: string | undefined = error?.code
  if (code === 'ECONNABORTED' || /timeout/i.test(error?.message || '')) return '请求超时，请检查网络后重试'
  if (code === 'ERR_NETWORK' || code === 'ENOTFOUND' || /Network Error/i.test(error?.message || '')) {
    return '网络不通，请检查网络连接'
  }

  return error?.message || '操作未能完成，请稍后再试'
}
