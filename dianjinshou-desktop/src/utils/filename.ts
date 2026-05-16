/**
 * Validate filename: max 15 codePoints, no illegal characters
 */
export function validateFilename(name: string): { valid: boolean; message?: string } {
  if (!name || name.trim().length === 0) {
    return { valid: false, message: '文件名不能为空' }
  }

  const trimmed = name.trim()
  const codePointCount = [...trimmed].length

  if (codePointCount > 15) {
    return { valid: false, message: `文件名不能超过15个字符（当前${codePointCount}个）` }
  }

  const illegalChars = /[\\/:*?"<>|]/
  if (illegalChars.test(trimmed)) {
    return { valid: false, message: '文件名不能包含 \\ / : * ? " < > | 字符' }
  }

  return { valid: true }
}
