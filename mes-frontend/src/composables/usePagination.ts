import { ref, reactive } from 'vue'

export function usePagination(fetchFn: () => Promise<void>, defaultSize = 12) {
  const pagination = reactive({ page: 1, size: defaultSize, total: 0 })
  const loading = ref(false)

  const loadData = async () => {
    loading.value = true
    try {
      await fetchFn()
    } catch (e) {
      console.error(e)
    } finally {
      loading.value = false
    }
  }

  const reset = () => {
    pagination.page = 1
    loadData()
  }

  return { pagination, loading, loadData, reset }
}
