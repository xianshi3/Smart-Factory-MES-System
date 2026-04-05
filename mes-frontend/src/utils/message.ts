import { ElMessage } from 'element-plus'

export function showError(message: string) {
  ElMessage.error(message)
}

export function showSuccess(message: string) {
  ElMessage.success(message)
}

export function showWarning(message: string) {
  ElMessage.warning(message)
}

export function showInfo(message: string) {
  ElMessage.info(message)
}
