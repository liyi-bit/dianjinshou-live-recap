import { describe, it, expect } from 'vitest'
import { validateFilename } from '../filename'

describe('validateFilename', () => {
  it('accepts valid short name', () => {
    expect(validateFilename('直播录制片段')).toEqual({ valid: true })
  })

  it('accepts exactly 15 codePoints', () => {
    const name = '一二三四五六七八九十壹贰叁肆伍'
    expect([...name].length).toBe(15)
    expect(validateFilename(name)).toEqual({ valid: true })
  })

  it('rejects 16 codePoints', () => {
    const name = '一二三四五六七八九十壹贰叁肆伍陆'
    expect([...name].length).toBe(16)
    const result = validateFilename(name)
    expect(result.valid).toBe(false)
    expect(result.message).toContain('15')
  })

  it('counts emoji as single codePoint', () => {
    // Emoji like 🎉 is a single codePoint
    const name = '直播🎉分析结果'
    expect([...name].length).toBe(7)
    expect(validateFilename(name)).toEqual({ valid: true })
  })

  it('rejects empty string', () => {
    expect(validateFilename('')).toEqual({ valid: false, message: '文件名不能为空' })
  })

  it('rejects whitespace only', () => {
    expect(validateFilename('   ')).toEqual({ valid: false, message: '文件名不能为空' })
  })

  it('rejects illegal characters', () => {
    expect(validateFilename('test/file')).toEqual({
      valid: false,
      message: expect.stringContaining('\\')
    })
    expect(validateFilename('test:file')).toEqual({
      valid: false,
      message: expect.stringContaining('\\')
    })
    expect(validateFilename('test*file')).toEqual({
      valid: false,
      message: expect.stringContaining('\\')
    })
  })

  it('trims whitespace before validation', () => {
    expect(validateFilename('  valid  ')).toEqual({ valid: true })
  })

  it('handles mixed ASCII and CJK within limit', () => {
    const name = 'ABC直播录制XYZ' // 10 codePoints
    expect([...name].length).toBe(10)
    expect(validateFilename(name)).toEqual({ valid: true })
  })
})
