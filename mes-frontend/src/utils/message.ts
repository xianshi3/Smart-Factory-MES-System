import { ElMessage, ElMessageBox } from 'element-plus'

export function showError(message: string, title: string = '错误') {
  console.error(`${title}:`, message)
  ElMessageBox.alert(message, title, {
    confirmButtonText: '确定',
    type: 'error',
    roundButton: true,
    center: true
  })
}

export function showSuccess(message: string, title: string = '成功') {
  ElMessage.success(message)
}

export function showWarning(message: string, title: string = '警告') {
  ElMessage.warning(message)
}

export function showConfirm(message: string, title: string = '确认'): Promise<void> {
  return ElMessageBox.confirm(message, title, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    roundButton: true,
    center: true
  })
}
