import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUnreadCount } from '@/api/messages'

export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)
  const unreadByType = ref<Record<string, number>>({})
  let pollingTimer: ReturnType<typeof setInterval> | null = null

  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      if (res.code === 200 && res.data) {
        unreadCount.value = res.data.unreadCount || 0
        const { unreadCount: _, ...rest } = res.data as any
        unreadByType.value = rest
      }
    } catch {
      // 静默失败，不影响用户体验
    }
  }

  function startPolling(intervalMs = 30000) {
    if (pollingTimer) return
    fetchUnreadCount()
    pollingTimer = setInterval(fetchUnreadCount, intervalMs)
  }

  function stopPolling() {
    if (pollingTimer) {
      clearInterval(pollingTimer)
      pollingTimer = null
    }
  }

  return { unreadCount, unreadByType, fetchUnreadCount, startPolling, stopPolling }
})
