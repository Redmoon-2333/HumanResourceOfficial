<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/home')
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="not-found-page">
    <div class="bg-decoration">
      <div class="bg-circle circle-1"></div>
      <div class="bg-circle circle-2"></div>
    </div>

    <div class="content">
      <div class="error-code">404</div>
      <h1 class="error-title">页面不存在</h1>
      <p class="error-desc">抱歉，您访问的页面不存在或已被移除</p>

      <div class="actions">
        <button class="btn btn-primary" @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </button>
        <button class="btn btn-outline" @click="goBack">
          <el-icon><Back /></el-icon>
          返回上一页
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #FFF5F0 0%, #FFFBEB 50%, #FFF1F2 100%);
  position: relative;
  overflow: hidden;
}

.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
}

.circle-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #FFB09E, #FF8A70);
  top: -100px;
  right: -100px;
}

.circle-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #FCD34D, #FBBF24);
  bottom: -50px;
  left: -50px;
}

.content {
  text-align: center;
  position: relative;
  z-index: 1;
}

.error-code {
  font-size: 160px;
  font-weight: 900;
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
  margin-bottom: var(--space-4);
}

.error-title {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-4) 0;
}

.error-desc {
  font-size: var(--text-lg);
  color: var(--text-secondary);
  margin: 0 0 var(--space-8) 0;
}

.actions {
  display: flex;
  gap: var(--space-4);
  justify-content: center;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-6);
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  color: white;
  box-shadow: 0 4px 16px rgba(255, 107, 74, 0.35);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 74, 0.45);
}

.btn-outline {
  background: transparent;
  border: 2px solid var(--border-medium);
  color: var(--text-primary);
}

.btn-outline:hover {
  border-color: #FF6B4A;
  color: #FF6B4A;
}

@media (max-width: 640px) {
  .error-code {
    font-size: 100px;
  }

  .error-title {
    font-size: var(--text-2xl);
  }

  .actions {
    flex-direction: column;
  }
}
</style>
