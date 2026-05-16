/**
 * Format datetime string to "yyyy-MM-dd HH:mm:ss".
 */
export function formatDateTime(dt: string | null | undefined): string {
  if (!dt) return '--'
  return normalizeDateTime(dt)
}

/** 当前本地时区时间，格式 "yyyy-MM-dd HH:mm:ss"。 */
export function nowLocalDateTime(): string {
  return formatLocalDateTime(new Date())
}

/** 兼容旧调用名：返回当前本地时区时间，格式 "yyyy-MM-dd HH:mm:ss"。 */
export function nowLocalIso(): string {
  return nowLocalDateTime()
}

export function formatLocalDateTime(d: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

export function normalizeDateTime(value: string): string {
  const trimmed = value.trim()
  if (!trimmed) return '--'
  const matched = trimmed.match(/^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2}:\d{2})/)
  if (matched) return `${matched[1]} ${matched[2]}`
  if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return `${trimmed} 00:00:00`
  const parsed = new Date(trimmed)
  if (!Number.isNaN(parsed.getTime())) return formatLocalDateTime(parsed)
  return trimmed.replace('T', ' ').substring(0, 19)
}

/** 当前本地日期，格式 yyyy-MM-dd — 用于文件名等场景。 */
export function todayLocalDate(): string {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}
