import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '../theme'

describe('theme store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    document.documentElement.removeAttribute('data-theme')
    document.body.removeAttribute('arco-theme')
  })

  it('defaults to light theme', () => {
    const store = useThemeStore()
    expect(store.theme).toBe('light')
  })

  it('sets dark theme', () => {
    const store = useThemeStore()
    store.setTheme('dark')
    expect(store.theme).toBe('dark')
    expect(localStorage.getItem('theme')).toBe('dark')
    expect(document.body.getAttribute('arco-theme')).toBe('dark')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })

  it('sets light theme removes arco-theme', () => {
    const store = useThemeStore()
    store.setTheme('dark')
    store.setTheme('light')
    expect(store.theme).toBe('light')
    expect(document.body.getAttribute('arco-theme')).toBeNull()
  })

  it('toggleTheme switches between light and dark', () => {
    const store = useThemeStore()
    expect(store.theme).toBe('light')
    store.toggleTheme()
    expect(store.theme).toBe('dark')
    store.toggleTheme()
    expect(store.theme).toBe('light')
  })

  it('initTheme applies stored theme', () => {
    localStorage.setItem('theme', 'dark')
    const store = useThemeStore()
    store.initTheme()
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
  })
})
