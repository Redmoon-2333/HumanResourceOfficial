<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)

const isMinisterOrViceMinister = computed(() => {
  const roleHistory = userStore.userInfo?.roleHistory || ''
  return roleHistory.includes('部长')
})

const menuItems = [
  { path: '/home', title: '首页', icon: 'HomeFilled' },
  { path: '/activities', title: '活动介绍', icon: 'Calendar' },
  { path: '/materials', title: '资料管理', icon: 'FolderOpened' },
  { path: '/past-activities', title: '往届活动', icon: 'PictureFilled' },
  { path: '/alumni', title: '往届成员', icon: 'UserFilled' },
  { path: '/ai-chat', title: 'AI对话', icon: 'ChatDotRound' },
  { path: '/plan-generator', title: '策划案生成', icon: 'DocumentAdd' }
]

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {
      // 用户取消
    }
  }
}
</script>

<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="200px" class="layout-aside">
      <div class="logo gradient-bg">
        <h2>HR系统</h2>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        router
        class="layout-menu"
      >
        <el-menu-item
          v-for="item in menuItems"
          :key="item.path"
          :index="item.path"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部栏 -->
      <el-header class="layout-header">
        <div class="header-left">
          <span class="page-title">{{ route.meta.title || '首页' }}</span>
        </div>
        
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.userInfo?.name?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.name || '用户' }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click.stop="router.push('/profile')">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item 
                  v-if="isMinisterOrViceMinister"
                  divided
                  @click.stop="router.push('/activation-code-manager')"
                >
                  <el-icon><DocumentAdd /></el-icon>
                  激活码管理
                </el-dropdown-item>
                <el-dropdown-item 
                  divided
                  command="logout"
                >
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout-main">
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.layout-aside {
  background: var(--color-bg-white);
  box-shadow: var(--shadow-md);
}

.logo {
  height: 60px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: white;
  font-weight: bold;
  font-size: 20px;
}

.layout-menu {
  border-right: none;
  padding: var(--spacing-md) 0;
}

.layout-header {
  background: var(--color-bg-white);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 var(--spacing-lg);
  box-shadow: var(--shadow-sm);
}

.header-left .page-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-primary);
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  cursor: pointer;
  padding: var(--spacing-sm);
  border-radius: var(--radius-md);
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: var(--color-bg-secondary);
}

.user-avatar {
  background-color: var(--color-primary);
  color: white;
}

.user-name {
  color: var(--color-text-primary);
  font-weight: 500;
}

.layout-main {
  background: var(--color-bg-primary);
  padding: var(--spacing-lg);
}
</style>
