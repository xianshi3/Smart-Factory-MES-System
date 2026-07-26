import type { Directive, DirectiveBinding } from 'vue'
import { usePermissionStore } from '@/stores/permission'

export const vPermission: Directive = {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const permissionCode = binding.value
    
    // Get permission store
    const permissionStore = usePermissionStore()
    
    // Check if has permission
    if (!permissionStore.hasPermission(permissionCode)) {
      el.style.display = 'none'
    }
  },
  updated(el: HTMLElement, binding: DirectiveBinding) {
    const permissionCode = binding.value
    const permissionStore = usePermissionStore()
    
    if (!permissionStore.hasPermission(permissionCode)) {
      el.style.display = 'none'
    } else {
      el.style.display = ''
    }
  }
}

export function hasPermission(code: string): boolean {
  const permissionStore = usePermissionStore()
  return permissionStore.hasPermission(code)
}

export function hasMenu(code: string): boolean {
  const permissionStore = usePermissionStore()
  return permissionStore.hasMenu(code)
}