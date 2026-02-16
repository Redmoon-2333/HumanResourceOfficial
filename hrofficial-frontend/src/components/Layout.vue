<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  OfficeBuilding,
  Expand,
  Fold,
  Menu,
  ArrowDown,
  User,
  SwitchButton
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapsed = ref(false)
const isMobile = ref(false)
const mobileMenuOpen = ref(false)

// 检测是否为移动端
const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  if (!isMobile.value) {
    mobileMenuOpen.value = false
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})

const activeMenu = computed(() => route.path)

/**
 * 权限判断
 * - 游客：只能看公开页面
 * - 登录用户（未激活）：可以看公开页面 + 个人中心
 * - 部员（已激活）：可以使用全部功能
 * - 部长/副部长：可以使用全部功能 + 管理功能
 */

const isMinisterOrViceMinister = computed(() => {
  return userStore.isMinister
})

const isMember = computed(() => {
  return userStore.isMember
})

const isLoggedIn = computed(() => {
  return userStore.isLoggedIn
})

// 公开菜单（所有人可见）
const publicMenuItems = [
  { path: '/home', title: '首页', icon: 'HomeFilled', description: '系统概览' },
  { path: '/activities', title: '活动介绍', icon: 'Calendar', description: '了解我们的特色活动' },
  { path: '/past-activities', title: '往届活动', icon: 'PictureFilled', description: '回顾精彩瞬间' },
  { path: '/alumni', title: '往届成员', icon: 'UserFilled', description: '成员展示' }
]

// 部员专属菜单（需要激活）
const memberMenuItems = [
  { path: '/materials', title: '资料管理', icon: 'FolderOpened', description: '内部资料共享' },
  { path: '/ai-chat', title: 'AI对话', icon: 'ChatDotRound', description: '智能助手' },
  { path: '/plan-generator', title: '策划案生成', icon: 'DocumentAdd', description: 'AI辅助策划' }
]

// 管理菜单（仅部长/副部长可见）
const adminMenuItems = [
  { path: '/activation-code-manager', title: '激活码管理', icon: 'Key', description: '成员账号管理' },
  { path: '/rag-management', title: '知识库管理', icon: 'Collection', description: 'AI知识库维护' }
]

// 根据角色动态计算显示的菜单
const menuItems = computed(() => {
  let items = [...publicMenuItems]

  // 部员或部长可以看到部员专属菜单
  if (isMember.value) {
    items = [...items, ...memberMenuItems]
  }

  return items
})

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      })

      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {
      // 用户取消
    }
  }
}

const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}
</script>

<template>
  <div class="layout-wrapper">
    <!-- 移动端遮罩 -->
    <div
      v-if="mobileMenuOpen"
      class="mobile-overlay"
      @click="mobileMenuOpen = false"
    />

    <!-- 侧边栏 -->
    <aside
      class="sidebar"
      :class="{
        'sidebar-collapsed': isCollapsed && !isMobile,
        'sidebar-mobile': isMobile,
        'sidebar-mobile-open': mobileMenuOpen && isMobile
      }"
    >
      <!-- Logo区域 -->
      <div class="sidebar-header">
        <div class="logo">
          <div class="logo-icon">
            <el-icon :size="28" color="white"><OfficeBuilding /></el-icon>
          </div>
          <div v-show="!isCollapsed || isMobile" class="logo-text">
            <h1>人力资源中心</h1>
            <span>Human Resources</span>
          </div>
        </div>
        <button
          v-if="!isMobile"
          class="collapse-btn"
          @click="isCollapsed = !isCollapsed"
        >
          <el-icon>
            <component :is="isCollapsed ? 'Expand' : 'Fold'" />
          </el-icon>
        </button>
      </div>

      <!-- 导航菜单 -->
      <nav class="sidebar-nav">
        <!-- 公开菜单 -->
        <div class="nav-section">
          <p v-show="!isCollapsed || isMobile" class="nav-section-title">主菜单</p>
          <router-link
            v-for="item in publicMenuItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: activeMenu === item.path }"
            @click="isMobile && (mobileMenuOpen = false)"
          >
            <div class="nav-icon">
              <el-icon :size="20">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div v-show="!isCollapsed || isMobile" class="nav-content">
              <span class="nav-title">{{ item.title }}</span>
              <span class="nav-desc">{{ item.description }}</span>
            </div>
          </router-link>
        </div>

        <!-- 部员专属菜单 -->
        <div v-if="isMember" class="nav-section">
          <p v-show="!isCollapsed || isMobile" class="nav-section-title">部员功能</p>
          <router-link
            v-for="item in memberMenuItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: activeMenu === item.path }"
            @click="isMobile && (mobileMenuOpen = false)"
          >
            <div class="nav-icon">
              <el-icon :size="20">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div v-show="!isCollapsed || isMobile" class="nav-content">
              <span class="nav-title">{{ item.title }}</span>
              <span class="nav-desc">{{ item.description }}</span>
            </div>
          </router-link>
        </div>

        <!-- 管理菜单（仅部长/副部长可见） -->
        <div v-if="isMinisterOrViceMinister" class="nav-section">
          <p v-show="!isCollapsed || isMobile" class="nav-section-title">管理</p>
          <router-link
            v-for="item in adminMenuItems"
            :key="item.path"
            :to="item.path"
            class="nav-item"
            :class="{ active: activeMenu === item.path }"
            @click="isMobile && (mobileMenuOpen = false)"
          >
            <div class="nav-icon">
              <el-icon :size="20">
                <component :is="item.icon" />
              </el-icon>
            </div>
            <div v-show="!isCollapsed || isMobile" class="nav-content">
              <span class="nav-title">{{ item.title }}</span>
              <span class="nav-desc">{{ item.description }}</span>
            </div>
          </router-link>
        </div>
      </nav>

      <!-- 侧边栏底部 -->
      <div class="sidebar-footer">
        <div v-if="userStore.isLoggedIn" class="user-mini">
          <el-avatar :size="36" class="user-avatar">
            {{ userStore.userInfo?.name?.charAt(0) || 'U' }}
          </el-avatar>
          <div v-show="!isCollapsed || isMobile" class="user-info">
            <span class="user-name">{{ userStore.userInfo?.name || '用户' }}</span>
            <span class="user-role">{{ userStore.userInfo?.roleHistory || '成员' }}</span>
          </div>
        </div>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部导航栏 -->
      <header class="top-header">
        <div class="header-left">
          <!-- 移动端菜单按钮 -->
          <button
            v-if="isMobile"
            class="mobile-menu-btn"
            @click="toggleMobileMenu"
          >
            <el-icon :size="24"><Menu /></el-icon>
          </button>

          <!-- 面包屑/页面标题 -->
          <div class="page-info">
            <h2 class="page-title">{{ route.meta.title || '首页' }}</h2>
            <p class="page-subtitle">{{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}</p>
          </div>
        </div>

        <div class="header-right">
          <!-- 未登录状态 -->
          <div v-if="!userStore.isLoggedIn" class="auth-actions">
            <router-link to="/login" class="btn btn-ghost">
              登录
            </router-link>
            <router-link to="/register" class="btn btn-primary">
              注册
            </router-link>
          </div>

          <!-- 已登录状态 -->
          <el-dropdown v-else @command="handleCommand" trigger="click">
            <div class="user-dropdown">
              <el-avatar :size="40" class="user-avatar-gradient">
                {{ userStore.userInfo?.name?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-meta">
                <span class="user-name">{{ userStore.userInfo?.name || '用户' }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu class="user-dropdown-menu">
                <el-dropdown-item @click="router.push('/profile')">
                  <el-icon><User /></el-icon>
                  <span>个人中心</span>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  <span class="text-danger">退出登录</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <div class="page-content">
        <slot />
      </div>
    </main>
  </div>
</template>

<style scoped>
.layout-wrapper {
  display: flex;
  min-height: 100vh;
  background: var(--bg-primary);
}

/* ============================================
   侧边栏样式 - 暖色调 + 流动导航
   ============================================ */
.sidebar {
  width: 280px;
  background: var(--bg-elevated);
  border-right: 1px solid var(--border-light);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  height: 100vh;
  z-index: var(--z-fixed);
  transition: transform var(--transition-normal) var(--ease-out), width var(--transition-normal) var(--ease-out);
  overflow: hidden;
  will-change: transform, width;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    180deg,
    rgba(255, 107, 74, 0.03) 0%,
    transparent 30%,
    transparent 70%,
    rgba(245, 158, 11, 0.03) 100%
  );
  pointer-events: none;
}

.sidebar::after {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    ellipse at 30% 20%,
    rgba(255, 107, 74, 0.05) 0%,
    transparent 50%
  );
  animation: sidebarGlow 15s ease-in-out infinite;
  pointer-events: none;
}

@keyframes sidebarGlow {
  0%, 100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(5%, 5%);
  }
}

.sidebar-collapsed {
  width: 80px;
}

.sidebar-mobile {
  transform: translateX(-100%);
  width: 280px;
}

.sidebar-mobile-open {
  transform: translateX(0);
}

.mobile-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--bg-overlay);
  z-index: calc(var(--z-fixed) - 1);
  backdrop-filter: blur(var(--glass-blur-sm));
  -webkit-backdrop-filter: blur(var(--glass-blur-sm));
  transition: opacity var(--transition-normal) var(--ease-out);
  animation: fadeIn 0.2s ease-out;
  padding-top: var(--mobile-safe-area-top);
  padding-bottom: var(--mobile-safe-area-bottom);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 侧边栏头部 */
.sidebar-header {
  padding: var(--space-6);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: calc(var(--space-6) + var(--mobile-safe-area-top));
}

.logo {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.logo-icon {
  width: 48px;
  height: 48px;
  background: var(--gradient-warm);
  border-radius: var(--radius-xl);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: var(--shadow-coral);
}

.logo-text {
  display: flex;
  flex-direction: column;
}

.logo-text h1 {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin: 0;
  line-height: 1.2;
}

.logo-text span {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.collapse-btn {
  width: var(--touch-target-min);
  height: var(--touch-target-min);
  border: none;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.collapse-btn:hover {
  background: var(--coral-100);
  color: var(--coral-600);
}

/* 导航菜单 */
.sidebar-nav {
  flex: 1;
  padding: var(--space-4);
  overflow-y: auto;
  padding-bottom: calc(var(--space-4) + var(--mobile-safe-area-bottom));
}

.nav-section {
  margin-bottom: var(--space-6);
}

.nav-section-title {
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: var(--space-3);
  padding-left: var(--space-3);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  min-height: var(--touch-target-min);
  border-radius: var(--radius-lg);
  color: var(--text-secondary);
  text-decoration: none;
  transition: all var(--transition-fast);
  margin-bottom: var(--space-1);
  position: relative;
  overflow: hidden;
}

.nav-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 0;
  background: linear-gradient(180deg, var(--coral-500), var(--amber-500));
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  transition: height 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.nav-item::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(255, 107, 74, 0.08), transparent);
  transform: translateX(-100%);
  transition: transform 0.4s ease;
}

.nav-item:hover {
  background: var(--coral-50);
  color: var(--coral-600);
}

.nav-item:hover::before {
  height: 60%;
}

.nav-item:hover::after {
  transform: translateX(0);
}

.nav-item.active {
  background: var(--gradient-warm);
  color: white;
  box-shadow: var(--shadow-coral);
}

.nav-item.active::before {
  height: 0;
}

.nav-item.active::after {
  display: none;
}

.nav-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.nav-content {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.nav-title {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  white-space: nowrap;
}

.nav-desc {
  font-size: var(--text-xs);
  opacity: 0.7;
  white-space: nowrap;
}

/* 侧边栏底部 */
.sidebar-footer {
  padding: var(--space-4);
  border-top: 1px solid var(--border-light);
  padding-bottom: calc(var(--space-4) + var(--mobile-safe-area-bottom));
}

.user-mini {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2);
  border-radius: var(--radius-lg);
  background: var(--bg-secondary);
  min-height: var(--touch-target-min);
}

.user-avatar {
  background: var(--gradient-warm);
  color: white;
  font-weight: var(--font-bold);
}

.user-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.user-name {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-primary);
  white-space: nowrap;
}

.user-role {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  white-space: nowrap;
}

/* ============================================
   主内容区样式
   ============================================ */
.main-content {
  flex: 1;
  margin-left: 280px;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  min-width: 0;
  width: calc(100% - 280px);
  transition: margin-left var(--transition-normal) var(--ease-out), width var(--transition-normal) var(--ease-out);
}

.sidebar-collapsed + .main-content {
  margin-left: 80px;
  width: calc(100% - 80px);
}

@media (max-width: 768px) {
  .main-content {
    margin-left: 0;
    width: 100%;
  }
}

/* 顶部导航栏 */
.top-header {
  height: 72px;
  background: var(--bg-elevated);
  border-bottom: 1px solid var(--border-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  padding-top: calc(var(--space-0) + var(--mobile-safe-area-top));
  height: calc(72px + var(--mobile-safe-area-top));
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.mobile-menu-btn {
  width: var(--touch-target-min);
  height: var(--touch-target-min);
  border: none;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-primary);
  transition: all var(--transition-fast);
}

.mobile-menu-btn:hover {
  background: var(--coral-100);
  color: var(--coral-600);
}

.page-info {
  display: flex;
  flex-direction: column;
}

.page-title {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin: 0;
}

.page-subtitle {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.auth-actions {
  display: flex;
  gap: var(--space-2);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  min-height: var(--touch-target-min);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.user-dropdown:hover {
  background: var(--bg-secondary);
}

.user-avatar-gradient {
  background: var(--gradient-warm);
  color: white;
  font-weight: var(--font-bold);
}

.user-meta {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.user-meta .user-name {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-primary);
}

/* 页面内容 */
.page-content {
  flex: 1;
  padding: var(--page-padding);
  overflow-x: hidden;
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
}

@media (max-width: 768px) {
  .page-content {
    padding: var(--mobile-container-padding);
  }

  .top-header {
    padding: 0 var(--mobile-container-padding);
    padding-top: calc(var(--space-0) + var(--mobile-safe-area-top));
  }

  .user-meta {
    display: none;
  }

  .page-subtitle {
    display: none;
  }

  .page-title {
    font-size: var(--text-lg);
  }
}

@media (max-width: 480px) {
  .page-content {
    padding: var(--space-3);
  }

  .top-header {
    padding: 0 var(--space-3);
    padding-top: calc(var(--space-0) + var(--mobile-safe-area-top));
  }
}

/* 下拉菜单样式 */
:deep(.user-dropdown-menu) {
  padding: var(--space-2);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border-light);
}

:deep(.user-dropdown-menu .el-dropdown-menu__item) {
  padding: var(--space-3) var(--space-4);
  min-height: var(--touch-target-min);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

:deep(.user-dropdown-menu .el-dropdown-menu__item:hover) {
  background: var(--coral-50);
  color: var(--coral-600);
}

.text-danger {
  color: var(--error-500);
}
</style>
