<script setup lang="ts">
import { onMounted, ref, watch, computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isReady = ref(false)
const route = useRoute()
const prevRoute = ref('')

watch(() => route.path, (newPath, oldPath) => {
  prevRoute.value = oldPath || ''
})

const transitionName = computed(() => {
  const currentDepth = route.meta?.depth || 0
  const isModal = route.meta?.modal
  
  if (isModal) {
    return 'scale'
  }
  
  return 'page-slide'
})

onMounted(() => {
  if (userStore.isLoggedIn) {
    userStore.fetchUserInfo()
  }
  
  requestAnimationFrame(() => {
    isReady.value = true
  })
})
</script>

<template>
  <div id="app" :class="{ 'app-ready': isReady }">
    <RouterView v-slot="{ Component, route }">
      <transition 
        :name="transitionName" 
        mode="out-in"
      >
        <component :is="Component" :key="route.path" />
      </transition>
    </RouterView>
  </div>
</template>

<style>
#app.app-ready {
  opacity: 1 !important;
}

.page-slide-enter-active,
.page-slide-leave-active {
  transition: all 400ms cubic-bezier(0.4, 0, 0.2, 1);
}

.page-slide-enter-from {
  opacity: 0;
  transform: translateY(24px);
}

.page-slide-leave-to {
  opacity: 0;
  transform: translateY(-16px);
}

.scale-enter-active,
.scale-leave-active {
  transition: all 300ms cubic-bezier(0.34, 1.56, 0.64, 1);
}

.scale-enter-from,
.scale-leave-to {
  opacity: 0;
  transform: scale(0.92);
}
</style>
