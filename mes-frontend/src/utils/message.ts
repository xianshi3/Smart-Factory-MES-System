import { ElMessage, ElMessageBox } from 'element-plus'

const theme = {
  primary: '#0f3460',
  secondary: '#1a1a2e',
  accent: '#e94560',
  success: '#52c41a',
  warning: '#faad14',
  danger: '#f5222d',
  text: '#ffffff',
  bg: '#1a1a2e'
}

export function showError(message: string, title: string = '错误') {
  console.error(`${title}:`, message)
  ElMessageBox.confirm(message, title, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'error',
    roundButton: true,
    center: true,
    customClass: 'mes-message-box'
  })
}

export function showSuccess(message: string, title: string = '成功') {
  ElMessage({
    message,
    type: 'success',
    duration: 3000,
    plain: false
  })
}

export function showWarning(message: string, title: string = '警告') {
  ElMessage({
    message,
    type: 'warning',
    duration: 3000,
    plain: false
  })
}

export function showInfo(message: string, title: string = '提示') {
  ElMessage({
    message,
    type: 'info',
    duration: 3000,
    plain: false
  })
}

export function showConfirm(message: string, title: string = '确认'): Promise<void> {
  return ElMessageBox.confirm(message, title, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
    roundButton: true,
    center: true,
    customClass: 'mes-message-box'
  })
}
