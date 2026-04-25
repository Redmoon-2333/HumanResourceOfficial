import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

/**
 * 权限检查封装
 */
export function usePermission() {
  const userStore = useUserStore()

  const isLoggedIn = computed(() => userStore.isLoggedIn)
  const userInfo = computed(() => userStore.userInfo)

  const isMinister = computed(() => {
    if (!userStore.userInfo) return false
    const roleHistory = userStore.userInfo.roleHistory || ''
    const roles = roleHistory.split('&').map(r => r.trim())
    // 先匹配副部长，再匹配部长（避免"副部长"被误判为"部长"）
    return roles.some(r => r.includes('副部长') || r.endsWith('部长'))
  })

  const isMember = computed(() => {
    return !!userStore.userInfo?.roleHistory
  })

  const hasRole = (role: string) => {
    if (!userStore.userInfo) return false
    return userStore.userInfo.roleHistory?.includes(role) || false
  }

  const checkMinisterPermission = () => {
    if (!isLoggedIn.value) {
      return { allowed: false, reason: '请先登录' }
    }
    if (!isMinister.value) {
      return { allowed: false, reason: '权限不足，需要部长或副部长权限' }
    }
    return { allowed: true, reason: '' }
  }

  const checkMemberPermission = () => {
    if (!isLoggedIn.value) {
      return { allowed: false, reason: '请先登录' }
    }
    if (!isMember.value) {
      return { allowed: false, reason: '权限不足，需要成员权限' }
    }
    return { allowed: true, reason: '' }
  }

  return {
    isLoggedIn,
    userInfo,
    isMinister,
    isMember,
    hasRole,
    checkMinisterPermission,
    checkMemberPermission
  }
}
