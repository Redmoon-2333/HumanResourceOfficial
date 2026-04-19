import { defineStore } from 'pinia'
import { ref, computed, watch } from 'vue'
import type { User } from '@/types'
import { getCurrentUser } from '@/api/user'

// JWT payload解码（不解签名）
function decodeJwtToken(token: string): { exp?: number; [key: string]: any } | null {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return null

    const payload = parts[1]!
      .replace(/-/g, '+')
      .replace(/_/g, '/')
    const padded = payload.padEnd(payload.length + (4 - (payload.length % 4)) % 4, '=')
    const decoded = decodeURIComponent(escape(atob(padded)))
    return JSON.parse(decoded)
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<User | null>(null)
  const loading = ref(false)

  // 从localStorage恢复时检查是否过期
  const initialToken = token.value
  if (initialToken) {
    const decoded = decodeJwtToken(initialToken)
    if (decoded && decoded.exp) {
      const nowSeconds = Date.now() / 1000
      if (nowSeconds >= decoded.exp) {
        // token已过期，清除
        localStorage.removeItem('token')
        token.value = null
      }
    }
  }

  const isLoggedIn = computed(() => {
    if (!token.value) return false
    if (!userInfo.value) return false

    const decoded = decodeJwtToken(token.value)
    if (decoded && decoded.exp) {
      const nowSeconds = Date.now() / 1000
      if (nowSeconds >= decoded.exp) {
        return false
      }
    }
    return true
  })

  const isMinister = computed(() => {
    if (!userInfo.value) return false
    const role = userInfo.value.roleHistory
    return role.includes('部长') || role.includes('副部长')
  })
  const isMember = computed(() => {
    if (!userInfo.value) return false
    return !!userInfo.value.roleHistory
  })

  // Token过期倒计时定时器
  let expirationTimer: ReturnType<typeof setTimeout> | null = null

  // 设置Token过期倒计时
  function setExpirationTimer(expTimestamp: number) {
    // 清除旧定时器
    if (expirationTimer) {
      clearTimeout(expirationTimer)
    }

    const nowSeconds = Date.now() / 1000
    const secondsUntilExpiration = expTimestamp - nowSeconds

    if (secondsUntilExpiration > 0) {
      // 在过期前5秒触发登出，给UI更新留出时间
      const timeoutMs = Math.max(0, (secondsUntilExpiration - 5) * 1000)
      expirationTimer = setTimeout(() => {
        logout()
        window.location.href = '/login?expired=1'
      }, timeoutMs)
    }
  }

  // 设置Token
  function setToken(newToken: string | null) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('token', newToken)

      // 解析过期时间并设置倒计时
      const decoded = decodeJwtToken(newToken)
      if (decoded && decoded.exp) {
        setExpirationTimer(decoded.exp)
      }
    } else {
      localStorage.removeItem('token')
      // 清除倒计时定时器
      if (expirationTimer) {
        clearTimeout(expirationTimer)
        expirationTimer = null
      }
    }
  }

  // 获取用户信息
  async function fetchUserInfo() {
    if (!token.value) return

    loading.value = true
    try {
      const res = await getCurrentUser()
      if (res.code === 200) {
        userInfo.value = res.data as User
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      // Token可能已过期，由HTTP拦截器统一处理
      throw error
    } finally {
      loading.value = false
    }
  }

  // 登录
  function login(loginData: any) {
    // 从登录数据中提取token和用户信息
    const { token: newToken, user } = loginData
    setToken(newToken)
    if (user) {
      userInfo.value = user
    }
  }

  // 退出登录
  function logout() {
    // 清除倒计时定时器
    if (expirationTimer) {
      clearTimeout(expirationTimer)
      expirationTimer = null
    }
    setToken(null)
    userInfo.value = null
  }

  return {
    token,
    userInfo,
    loading,
    isLoggedIn,
    isMinister,
    isMember,
    setToken,
    fetchUserInfo,
    login,
    logout
  }
})
