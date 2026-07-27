import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

export function useChartTheme() {
  const themeStore = useThemeStore()

  return computed(() => ({
    isDark: themeStore.isDark,
    textColor: themeStore.isDark ? '#fff' : '#333',
    lineColor: themeStore.isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
    labelColor: themeStore.isDark ? 'rgba(255,255,255,0.6)' : 'rgba(0,0,0,0.6)',
    bgColor: themeStore.isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)',
    splitLineColor: themeStore.isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)'
  }))
}
