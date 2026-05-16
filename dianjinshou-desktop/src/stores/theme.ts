import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type ThemeMode = 'light' | 'dark'

export const useThemeStore = defineStore('theme', () => {
  const theme = ref<ThemeMode>(
    (localStorage.getItem('theme') as ThemeMode) || 'light'
  )

  function setTheme(mode: ThemeMode) {
    theme.value = mode
    document.documentElement.setAttribute('data-theme', mode)
    localStorage.setItem('theme', mode)

    // Toggle Arco Design dark mode class
    if (mode === 'dark') {
      document.body.setAttribute('arco-theme', 'dark')
    } else {
      document.body.removeAttribute('arco-theme')
    }
  }

  function toggleTheme() {
    setTheme(theme.value === 'light' ? 'dark' : 'light')
  }

  function initTheme() {
    setTheme(theme.value)
  }

  watch(theme, (val) => {
    document.documentElement.setAttribute('data-theme', val)
  })

  return {
    theme,
    setTheme,
    toggleTheme,
    initTheme
  }
})
