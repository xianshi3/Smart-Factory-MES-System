import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

export function useEchartsTheme() {
  const themeStore = useThemeStore()

  return computed(() => {
    const isDark = themeStore.isDark
    return {
      isDark,
      textColor: isDark ? '#fff' : '#333',
      lineColor: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
      labelColor: isDark ? 'rgba(255,255,255,0.6)' : 'rgba(0,0,0,0.6)',
      bgColor: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)',
      borderColor: isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)',
      splitLineColor: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)'
    }
  })
}
