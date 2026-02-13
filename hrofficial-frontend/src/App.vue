<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterView } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isReady = ref(false)

onMounted(() => {
  // 初始化时加载用户信息
  if (userStore.isLoggedIn) {
    userStore.fetchUserInfo()
  }
  
  // 标记应用已准备好，触发显示动画
  // Why: 防止CSS变量加载前的内容闪烁(FOUC)
  requestAnimationFrame(() => {
    isReady.value = true
  })
})
</script>

<template>
  <div id="app" :class="{ 'app-ready': isReady }">
    <RouterView />
  </div>
</template>

<style>
/* 全局样式已在 main.css 中定义 */

/* 应用准备就绪后的样式 */
#app.app-ready {
  opacity: 1 !important;
}
</style>
