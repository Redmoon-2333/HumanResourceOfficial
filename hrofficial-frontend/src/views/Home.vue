<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import { useUserStore } from '@/stores/user'
import { computed } from 'vue'

const userStore = useUserStore()

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const quickLinks = [
  { title: '创建活动', path: '/activities', icon: 'Calendar', color: '#FF6B35' },
  { title: '上传资料', path: '/materials', icon: 'FolderOpened', color: '#F7931E' },
  { title: 'AI对话', path: '/ai-chat', icon: 'ChatDotRound', color: '#FFC312' },
  { title: '生成策划案', path: '/plan-generator', icon: 'DocumentAdd', color: '#FF4757' }
]
</script>

<template>
  <Layout>
    <div class="home-container">
      <!-- 欢迎卡片 -->
      <el-card class="welcome-card gradient-bg">
        <div class="welcome-content">
          <h1 class="welcome-title">{{ greeting }}，{{ userStore.userInfo?.name }}！</h1>
          <p class="welcome-subtitle">欢迎使用人力资源管理系统</p>
        </div>
      </el-card>

      <!-- 快速入口 -->
      <div class="quick-links">
        <h2 class="section-title">快速入口</h2>
        <el-row :gutter="20">
          <el-col
            v-for="link in quickLinks"
            :key="link.path"
            :xs="12"
            :sm="6"
          >
            <router-link :to="link.path">
              <el-card class="quick-link-card hover-card">
                <div class="quick-link-content" :style="{ color: link.color }">
                  <el-icon :size="48"><component :is="link.icon" /></el-icon>
                  <p class="quick-link-title">{{ link.title }}</p>
                </div>
              </el-card>
            </router-link>
          </el-col>
        </el-row>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-section">
        <h2 class="section-title">数据概览</h2>
        <el-row :gutter="20">
          <el-col :xs="12" :sm="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <el-icon :size="32" color="#FF6B35"><Calendar /></el-icon>
                <div class="stat-info">
                  <p class="stat-value">--</p>
                  <p class="stat-label">活动总数</p>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="12" :sm="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <el-icon :size="32" color="#F7931E"><FolderOpened /></el-icon>
                <div class="stat-info">
                  <p class="stat-value">--</p>
                  <p class="stat-label">资料总数</p>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="12" :sm="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <el-icon :size="32" color="#FFC312"><UserFilled /></el-icon>
                <div class="stat-info">
                  <p class="stat-value">--</p>
                  <p class="stat-label">成员总数</p>
                </div>
              </div>
            </el-card>
          </el-col>

          <el-col :xs="12" :sm="6">
            <el-card class="stat-card">
              <div class="stat-content">
                <el-icon :size="32" color="#FF4757"><PictureFilled /></el-icon>
                <div class="stat-info">
                  <p class="stat-value">--</p>
                  <p class="stat-label">往届活动</p>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
    </div>
  </Layout>
</template>

<style scoped>
.home-container {
  max-width: 1200px;
  margin: 0 auto;
}

.welcome-card {
  margin-bottom: var(--spacing-xl);
  border: none;
}

.welcome-content {
  padding: var(--spacing-xl);
  color: white;
  text-align: center;
}

.welcome-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: var(--spacing-sm);
}

.welcome-subtitle {
  font-size: 18px;
  opacity: 0.9;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-primary);
  margin-bottom: var(--spacing-lg);
  padding-bottom: var(--spacing-sm);
  border-bottom: 2px solid var(--color-primary);
}

.quick-links {
  margin-bottom: var(--spacing-xl);
}

.quick-link-card {
  text-align: center;
  cursor: pointer;
}

.quick-link-content {
  padding: var(--spacing-lg) 0;
}

.quick-link-title {
  margin-top: var(--spacing-md);
  font-size: 16px;
  font-weight: 500;
}

a {
  text-decoration: none;
  color: inherit;
}

.stats-section {
  margin-bottom: var(--spacing-xl);
}

.stat-card {
  text-align: center;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
  padding: var(--spacing-md);
}

.stat-info {
  flex: 1;
  text-align: left;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
}

.stat-label {
  font-size: 14px;
  color: var(--color-text-secondary);
}
</style>
