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
    return roleHistory.includes('部长') || roleHistory.includes('副部长')
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
