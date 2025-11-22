import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types'
import { getCurrentUser } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userInfo = ref<User | null>(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isMinister = computed(() => {
    if (!userInfo.value) return false
    const role = userInfo.value.roleHistory
    return role.includes('部长') || role.includes('副部长')
  })
  const isMember = computed(() => {
    if (!userInfo.value) return false
    return !!userInfo.value.roleHistory
  })

  // 设置Token
  function setToken(newToken: string | null) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('token', newToken)
    } else {
      localStorage.removeItem('token')
    }
  }

  // 获取用户信息
  async function fetchUserInfo() {
    if (!token.value) return

    loading.value = true
    try {
      const res = await getCurrentUser()
      if (res.code === 200) {
        userInfo.value = res.data
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      // Token可能已过期
      logout()
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
