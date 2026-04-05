const TOKEN_KEY = 'mes_token'

/**
 * Get authentication token from localStorage
 */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * Store authentication token to localStorage
 */
export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * Remove authentication token from localStorage
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
}