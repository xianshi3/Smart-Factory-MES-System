import request from './index'

/**
 * User login API
 */
export function login(data: { username: string; password: string }) {
  return request({ url: '/auth/login', method: 'post', data })
}

/**
 * User registration API
 */
export function register(data: any) {
  return request({ url: '/auth/register', method: 'post', data })
}

/**
 * Get current user information API
 */
export function getUserInfo() {
  return request({ url: '/auth/info', method: 'get' })
}

/**
 * Get current user permission codes API
 */
export function getUserPermissions() {
  return request({ url: '/auth/user/permissions', method: 'get' })
}

/**
 * Update user profile API
 */
export function updateProfile(data: {
  realName?: string
  nickname?: string
  phone?: string
  email?: string
  avatar?: string
  department?: string
  position?: string
}) {
  return request({ url: '/auth/profile', method: 'put', data })
}

/**
 * Change password API
 */
export function changePassword(data: {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}) {
  return request({ url: '/auth/password', method: 'put', data })
}