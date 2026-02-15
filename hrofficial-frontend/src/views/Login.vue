<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login as loginApi } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, OfficeBuilding, ArrowRight, Loading, View, Hide } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginForm = ref({
  username: '',
  password: ''
})
const loading = ref(false)
const showPassword = ref(false)
const focusedField = ref('')
const isExiting = ref(false)

// 检查是否因为token过期跳转而来
onMounted(() => {
  if (route.query.expired === '1') {
    ElMessage.warning('登录已过期，请重新登录')
    userStore.logout()
  }
})

const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    const res = await loginApi(loginForm.value)
    console.log('登录响应:', res)

    if (res.code === 200 && res.data) {
      if (!res.data.token) {
        ElMessage.error('登录响应数据格式错误')
        return
      }

      userStore.login(res.data)
      ElMessage.success('登录成功')
      router.push('/home')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (error: any) {
    console.error('登录错误:', error)
    ElMessage.error(error.message || '登录失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 跳转到注册页面 - 带退出动画
const goToRegister = () => {
  isExiting.value = true
  setTimeout(() => {
    router.push('/register')
  }, 400)
}
</script>

<template>
  <div class="auth-page">
    <!-- 有机曲线背景装饰 -->
    <div class="bg-decoration">
      <div class="blob blob-1"></div>
      <div class="blob blob-2"></div>
      <div class="blob blob-3"></div>
      <div class="blob blob-4"></div>
      <!-- 浮动装饰元素 -->
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
    </div>

    <!-- 主容器 - 双栏布局 -->
    <div class="auth-container" :class="{ exiting: isExiting }">
      <!-- 左侧 - 欢迎区域 -->
      <div class="welcome-section">
        <div class="welcome-content">
          <div class="logo-large">
            <div class="logo-icon-large">
              <el-icon :size="48" color="white"><OfficeBuilding /></el-icon>
            </div>
          </div>
          <h2 class="welcome-title">欢迎回来！</h2>
          <p class="welcome-desc">
            还没有账号？<br>
            加入我们，开启人力资源中心之旅
          </p>
          <button class="switch-btn" @click="goToRegister">
            <span>去注册</span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>
        <!-- 装饰圆圈 -->
        <div class="deco-circle circle-1"></div>
        <div class="deco-circle circle-2"></div>
      </div>

      <!-- 右侧 - 登录表单 -->
      <div class="form-section">
        <div class="form-card">
          <!-- 移动端欢迎标题 -->
          <div class="mobile-welcome">
            <h1 class="form-title">登录</h1>
            <p class="form-subtitle">Human Resource Management System</p>
          </div>

          <!-- 登录表单 -->
          <form class="login-form" @submit.prevent="handleLogin">
            <div class="form-group" :class="{ focused: focusedField === 'username' }">
              <label class="form-label">
                <span class="label-icon">
                  <el-icon><User /></el-icon>
                </span>
                用户名
              </label>
              <div class="input-wrapper">
                <input
                  v-model="loginForm.username"
                  type="text"
                  class="form-input"
                  placeholder="请输入用户名"
                  :disabled="loading"
                  @focus="focusedField = 'username'"
                  @blur="focusedField = ''"
                />
                <div class="input-glow"></div>
              </div>
            </div>

            <div class="form-group" :class="{ focused: focusedField === 'password' }">
              <label class="form-label">
                <span class="label-icon">
                  <el-icon><Lock /></el-icon>
                </span>
                密码
              </label>
              <div class="input-wrapper">
                <input
                  v-model="loginForm.password"
                  :type="showPassword ? 'text' : 'password'"
                  class="form-input"
                  placeholder="请输入密码"
                  :disabled="loading"
                  @focus="focusedField = 'password'"
                  @blur="focusedField = ''"
                  @keyup.enter="handleLogin"
                />
                <div class="input-glow"></div>
                <button
                  type="button"
                  class="password-toggle"
                  @click="showPassword = !showPassword"
                >
                  <el-icon :size="18">
                    <component :is="showPassword ? View : Hide" />
                  </el-icon>
                </button>
              </div>
            </div>

            <div class="form-options">
              <label class="remember-me">
                <input type="checkbox" />
                <span class="checkmark"></span>
                <span>记住我</span>
              </label>
              <a href="#" class="forgot-link">忘记密码？</a>
            </div>

            <button
              type="submit"
              class="submit-btn"
              :disabled="loading"
              :class="{ loading: loading }"
            >
              <span class="btn-content">
                <el-icon v-if="loading" class="animate-spin"><Loading /></el-icon>
                <span>{{ loading ? '登录中...' : '登录' }}</span>
              </span>
              <div class="btn-glow"></div>
            </button>
          </form>

          <!-- 移动端切换链接 -->
          <div class="mobile-switch">
            <span>还没有账号？</span>
            <router-link to="/register" class="mobile-link">立即注册</router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部信息 -->
    <div class="auth-footer">
      <p>© 2024 人力资源中心 版权所有</p>
    </div>
  </div>
</template>

<style scoped>
/* ============================================
   Auth Page - 登录注册统一风格
   双栏布局 + 流畅切换动画
   ============================================ */

.auth-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--bg-warm) 0%, #FFFBF5 25%, var(--coral-50) 50%, #FDF2F0 75%, #FFF0ED 100%);
  position: relative;
  overflow: hidden;
  padding: var(--space-6);
}

/* 有机曲线背景装饰 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  pointer-events: none;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: blobFloat 20s ease-in-out infinite;
}

.blob-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #FFB09E, #FF8A70);
  top: -200px;
  right: -200px;
  animation-delay: 0s;
}

.blob-2 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #FCD34D, #FBBF24);
  bottom: -150px;
  left: -150px;
  animation-delay: -5s;
}

.blob-3 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #FDA4AF, #FB7185);
  top: 40%;
  left: 20%;
  opacity: 0.35;
  animation-delay: -10s;
}

.blob-4 {
  width: 350px;
  height: 350px;
  background: linear-gradient(135deg, #A7F3D0, #6EE7B7);
  bottom: 20%;
  right: 15%;
  opacity: 0.25;
  animation-delay: -15s;
}

@keyframes blobFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1) rotate(0deg);
    border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
  }
  25% {
    transform: translate(30px, -30px) scale(1.05) rotate(5deg);
    border-radius: 30% 60% 70% 40% / 50% 60% 30% 60%;
  }
  50% {
    transform: translate(-20px, 20px) scale(0.95) rotate(-5deg);
    border-radius: 50% 60% 30% 60% / 30% 60% 70% 40%;
  }
  75% {
    transform: translate(20px, 10px) scale(1.02) rotate(3deg);
    border-radius: 60% 40% 60% 30% / 70% 30% 50% 60%;
  }
}

/* 浮动装饰元素 */
.floating-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.6;
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 20px;
  height: 20px;
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 15px;
  height: 15px;
  background: linear-gradient(135deg, #F59E0B, #10B981);
  top: 60%;
  right: 10%;
  animation-delay: -2s;
}

.shape-3 {
  width: 12px;
  height: 12px;
  background: linear-gradient(135deg, #FB7185, #FF6B4A);
  bottom: 30%;
  left: 15%;
  animation-delay: -4s;
}

.shape-4 {
  width: 18px;
  height: 18px;
  background: linear-gradient(135deg, #60A5FA, #A78BFA);
  top: 30%;
  right: 20%;
  animation-delay: -1s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-20px) rotate(180deg);
  }
}

/* ============================================
   主容器 - 双栏卡片布局
   ============================================ */
.auth-container {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  max-width: 900px;
  min-height: 560px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 32px;
  box-shadow:
    0 25px 80px rgba(234, 88, 12, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  overflow: hidden;
  animation: cardEnter 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid rgba(255, 255, 255, 0.5);
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.auth-container.exiting {
  opacity: 0;
  transform: translateX(-30px) scale(0.98);
}

@keyframes cardEnter {
  from {
    opacity: 0;
    transform: translateY(40px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* ============================================
   左侧欢迎区域
   ============================================ */
.welcome-section {
  position: relative;
  width: 45%;
  background: var(--gradient-warm);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
  overflow: hidden;
}

.welcome-content {
  position: relative;
  z-index: 2;
  text-align: center;
  color: white;
}

.logo-large {
  margin-bottom: var(--space-6);
}

.logo-icon-large {
  width: 100px;
  height: 100px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto;
  border: 2px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
  animation: logoPulse 2s ease-in-out infinite;
}

@keyframes logoPulse {
  0%, 100% {
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
    transform: scale(1);
  }
  50% {
    box-shadow: 0 25px 70px rgba(0, 0, 0, 0.3);
    transform: scale(1.02);
  }
}

.welcome-title {
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  margin: 0 0 var(--space-3) 0;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.welcome-desc {
  font-size: var(--text-base);
  line-height: 1.8;
  margin: 0 0 var(--space-6) 0;
  opacity: 0.95;
}

.switch-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-8);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: #FF6B4A;
  background: white;
  border: none;
  border-radius: 100px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.switch-btn:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.2);
}

.switch-btn:hover .el-icon {
  transform: translateX(4px);
}

.switch-btn .el-icon {
  transition: transform 0.3s ease;
}

/* 装饰圆圈 */
.deco-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  border: 2px solid rgba(255, 255, 255, 0.15);
}

.circle-1 {
  width: 300px;
  height: 300px;
  top: -100px;
  right: -100px;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -60px;
  left: -60px;
}

/* ============================================
   右侧表单区域
   ============================================ */
.form-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-10);
}

.form-card {
  width: 100%;
  max-width: 360px;
}

.mobile-welcome {
  text-align: center;
  margin-bottom: var(--space-8);
}

.form-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-2) 0;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.form-subtitle {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin: 0;
  letter-spacing: 0.5px;
}

/* 表单样式 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  transition: all 0.3s ease;
}

.form-group.focused {
  transform: translateX(4px);
}

.form-label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-secondary);
  transition: color 0.3s ease;
}

.form-group.focused .form-label {
  color: #FF6B4A;
}

.label-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: rgba(255, 107, 74, 0.1);
  color: #FF6B4A;
  transition: all 0.3s ease;
}

.form-group.focused .label-icon {
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  color: white;
  transform: scale(1.1);
}

.input-wrapper {
  position: relative;
}

.form-input {
  width: 100%;
  padding: var(--space-3) var(--space-4);
  padding-right: 48px;
  font-size: var(--text-base);
  border: 2px solid rgba(229, 231, 235, 0.8);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  outline: none;
}

.form-input:focus {
  border-color: #FF8A70;
  background: white;
  box-shadow: 0 0 0 4px rgba(255, 138, 112, 0.15);
}

.form-input::placeholder {
  color: var(--text-tertiary);
  transition: all 0.3s ease;
}

.form-input:focus::placeholder {
  opacity: 0.5;
  transform: translateX(8px);
}

.input-glow {
  position: absolute;
  inset: -2px;
  border-radius: 16px;
  background: linear-gradient(135deg, #FF6B4A, #F59E0B, #FB7185);
  opacity: 0;
  z-index: -1;
  transition: opacity 0.3s ease;
  filter: blur(8px);
}

.form-group.focused .input-glow {
  opacity: 0.3;
}

.password-toggle {
  position: absolute;
  right: var(--space-3);
  top: 50%;
  transform: translateY(-50%);
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: var(--space-1);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.password-toggle:hover {
  color: #FF6B4A;
  background: rgba(255, 107, 74, 0.1);
}

/* 表单选项 */
.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--text-sm);
  margin-top: var(--space-1);
}

.remember-me {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-secondary);
  cursor: pointer;
  position: relative;
}

.remember-me input[type="checkbox"] {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  width: 0;
  height: 0;
}

.checkmark {
  width: 20px;
  height: 20px;
  border: 2px solid var(--border-medium);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  background: white;
}

.checkmark::after {
  content: '';
  width: 10px;
  height: 10px;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border-radius: 3px;
  transform: scale(0);
  transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.remember-me input[type="checkbox"]:checked + .checkmark {
  border-color: #FF6B4A;
}

.remember-me input[type="checkbox"]:checked + .checkmark::after {
  transform: scale(1);
}

.forgot-link {
  color: #E35532;
  text-decoration: none;
  font-weight: var(--font-medium);
  transition: all 0.3s ease;
  position: relative;
}

.forgot-link::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: linear-gradient(90deg, #FF6B4A, #E35532);
  transition: width 0.3s ease;
  border-radius: 1px;
}

.forgot-link:hover {
  color: #D13D1A;
}

.forgot-link:hover::after {
  width: 100%;
}

/* 提交按钮 */
.submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: var(--space-4) var(--space-4);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: white;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border: none;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.35);
  position: relative;
  overflow: hidden;
  margin-top: var(--space-2);
}

.btn-content {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  position: relative;
  z-index: 2;
}

.btn-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #FF8A70, #FF6B4A);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.01);
  box-shadow: 0 12px 32px rgba(255, 107, 74, 0.5);
}

.submit-btn:hover:not(:disabled) .btn-glow {
  opacity: 1;
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.99);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.submit-btn.loading {
  cursor: wait;
}

/* 移动端切换链接 */
.mobile-switch {
  display: none;
  text-align: center;
  margin-top: var(--space-6);
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.mobile-link {
  color: #E35532;
  font-weight: var(--font-semibold);
  text-decoration: none;
  margin-left: var(--space-1);
  transition: all 0.3s ease;
}

.mobile-link:hover {
  color: #D13D1A;
  text-decoration: underline;
}

/* 动画 */
.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 底部信息 */
.auth-footer {
  position: relative;
  z-index: 1;
  margin-top: var(--space-8);
  text-align: center;
}

.auth-footer p {
  font-size: var(--text-xs);
  color: var(--text-tertiary);
  margin: 0;
  opacity: 0.8;
}

/* ============================================
   响应式设计
   ============================================ */
@media (max-width: 768px) {
  .auth-page {
    padding: var(--space-4);
  }

  .auth-container {
    flex-direction: column;
    max-width: 400px;
    min-height: auto;
  }

  .welcome-section {
    display: none;
  }

  .form-section {
    padding: var(--space-8) var(--space-6);
  }

  .mobile-switch {
    display: block;
  }

  .blob {
    opacity: 0.3;
    filter: blur(60px);
  }

  .blob-1 { width: 300px; height: 300px; }
  .blob-2 { width: 250px; height: 250px; }
  .blob-3 { width: 200px; height: 200px; }
  .blob-4 { width: 180px; height: 180px; }

  .floating-shape {
    display: none;
  }
}

/* 暗色模式适配 */
@media (prefers-color-scheme: dark) {
  .auth-page {
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #1a1a2e 100%);
  }

  .auth-container {
    background: rgba(30, 30, 46, 0.9);
    border-color: rgba(255, 255, 255, 0.1);
  }

  .form-input {
    background: rgba(255, 255, 255, 0.05);
    border-color: rgba(255, 255, 255, 0.1);
    color: white;
  }

  .form-input:focus {
    background: rgba(255, 255, 255, 0.1);
  }
}
</style>
