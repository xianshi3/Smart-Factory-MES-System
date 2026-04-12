import { defineStore } from 'pinia'
import { ref } from 'vue'

const THEME_KEY = 'mes-theme'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(true)
  
  const toggleTheme = () => {
    isDark.value = !isDark.value
    applyTheme()
  }
  
  const applyTheme = () => {
    const html = document.documentElement
    if (isDark.value) {
      html.classList.remove('light')
      html.classList.add('dark')
    } else {
      html.classList.remove('dark')
      html.classList.add('light')
    }
    localStorage.setItem(THEME_KEY, isDark.value ? 'dark' : 'light')
  }
  
  const initTheme = () => {
    const saved = localStorage.getItem(THEME_KEY)
    if (saved) {
      isDark.value = saved === 'dark'
    }
    applyTheme()
  }
  
  return { isDark, toggleTheme, applyTheme, initTheme }
})