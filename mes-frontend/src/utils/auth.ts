const TOKEN_KEY = 'mes_token'

/**
 * Get authentication token: 优先 sessionStorage（未勾选"记住我"），其次 localStorage
 */
export function getToken(): string | null {
  return sessionStorage.getItem(TOKEN_KEY) || localStorage.getItem(TOKEN_KEY)
}

/**
 * Store authentication token.
 * @param remember true 时持久化到 localStorage（"记住我"），否则仅当前会话（sessionStorage）
 */
export function setToken(token: string, remember = true) {
  if (remember) {
    localStorage.setItem(TOKEN_KEY, token)
    sessionStorage.removeItem(TOKEN_KEY)
  } else {
    sessionStorage.setItem(TOKEN_KEY, token)
    localStorage.removeItem(TOKEN_KEY)
  }
}

/**
 * Remove authentication token from both storages
 */
export function removeToken() {
  localStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(TOKEN_KEY)
}
