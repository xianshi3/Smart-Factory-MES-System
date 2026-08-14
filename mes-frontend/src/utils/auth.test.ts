// @vitest-environment happy-dom
import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { getToken, setToken, removeToken } from '@/utils/auth'

const TOKEN = 'test-token-abc'

describe('token 存储（记住我逻辑）', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
    sessionStorage.clear()
  })

  it('setToken(remember=true) 写入 localStorage', () => {
    setToken(TOKEN, true)
    expect(localStorage.getItem('mes_token')).toBe(TOKEN)
    expect(sessionStorage.getItem('mes_token')).toBeNull()
    expect(getToken()).toBe(TOKEN)
  })

  it('setToken(remember=false) 写入 sessionStorage', () => {
    setToken(TOKEN, false)
    expect(sessionStorage.getItem('mes_token')).toBe(TOKEN)
    expect(localStorage.getItem('mes_token')).toBeNull()
    expect(getToken()).toBe(TOKEN)
  })

  it('removeToken 清理两个存储', () => {
    setToken(TOKEN, true)
    removeToken()
    expect(getToken()).toBeNull()
  })

  it('切换存储方式时互斥清除', () => {
    setToken(TOKEN, true)
    setToken('new-token', false)
    expect(localStorage.getItem('mes_token')).toBeNull()
    expect(sessionStorage.getItem('mes_token')).toBe('new-token')
  })
})
