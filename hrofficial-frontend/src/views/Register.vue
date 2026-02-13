<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { User, Lock, Key, ArrowLeft, Loading, Plus, Close, UserFilled, Medal } from '@element-plus/icons-vue'

const router = useRouter()
const loading = ref(false)
const isExiting = ref(false)

const registerForm = ref({
  username: '',
  password: '',
  name: '',
  activationCode: '',
  confirmPassword: ''
})

// 身份选择相关
const currentYear = new Date().getFullYear()
const gradeOptions = [
  { label: `${currentYear}级`, value: `${currentYear}级` },
  { label: `${currentYear - 1}级`, value: `${currentYear - 1}级` },
  { label: `${currentYear - 2}级`, value: `${currentYear - 2}级` },
  { label: `${currentYear - 3}级`, value: `${currentYear - 3}级` }
]
const roleOptions = [
  { label: '部员', value: '部员' },
  { label: '副部长', value: '副部长' },
  { label: '部长', value: '部长' }
]

// 第一个身份
const role1Grade = ref('')
const role1Position = ref('')

// 第二个身份（可选）
const role2Grade = ref('')
const role2Position = ref('')
const showSecondRole = ref(false)

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  activationCode: [
    { required: true, message: '请输入激活码', trigger: 'blur' }
  ]
}

const formRef = ref()

const handleRegister = async () => {
  // 验证表单
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  // 验证密码一致
  if (registerForm.value.password !== registerForm.value.confirmPassword) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  // 验证身份选择
  if (!role1Grade.value || !role1Position.value) {
    ElMessage.warning('请至少选择一个身份（年级 + 职位）')
    return
  }

  // 如果显示第二个身份，必须两个都选
  if (showSecondRole.value && (!role2Grade.value || !role2Position.value)) {
    ElMessage.warning('请完整选择第二个身份，或取消添加第二个身份')
    return
  }

  // 构建 roleHistory
  let roleHistory = `${role1Grade.value}${role1Position.value}`
  if (showSecondRole.value && role2Grade.value && role2Position.value) {
    roleHistory += `&${role2Grade.value}${role2Position.value}`
  }

  loading.value = true
  try {
    const requestData = {
      username: registerForm.value.username,
      password: registerForm.value.password,
      confirmPassword: registerForm.value.confirmPassword,
      name: registerForm.value.name,
      activationCode: registerForm.value.activationCode,
      roleHistory: roleHistory
    }

    const res = await register(requestData as any)
    if (res.code === 200) {
      ElMessage.success('注册成功，即将跳转到登录页...')
      setTimeout(() => {
        goToLogin()
      }, 1500)
    } else {
      ElMessage.error(res.message || '注册失败')
    }
  } catch (error: any) {
    console.error('注册失败:', error)
    ElMessage.error(error.message || '注册失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 跳转到登录页面 - 带退出动画
const goToLogin = () => {
  isExiting.value = true
  setTimeout(() => {
    router.push('/login')
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
      <!-- 左侧 - 表单区域 -->
      <div class="form-section">
        <div class="form-card">
          <!-- 移动端欢迎标题 -->
          <div class="mobile-welcome">
            <h1 class="form-title">注册</h1>
            <p class="form-subtitle">Human Resource Management System</p>
          </div>

          <!-- 注册表单 -->
          <el-form
            ref="formRef"
            :model="registerForm"
            :rules="rules"
            class="register-form"
            label-position="top"
            @submit.prevent="handleRegister"
          >
            <!-- 基本信息 -->
            <div class="form-section-block">
              <div class="section-header">
                <div class="section-icon">
                  <el-icon><UserFilled /></el-icon>
                </div>
                <span class="section-title">基本信息</span>
              </div>

              <div class="form-grid">
                <el-form-item label="用户名" prop="username" class="custom-form-item">
                  <el-input
                    v-model="registerForm.username"
                    placeholder="请输入用户名"
                    :prefix-icon="User"
                    size="large"
                  />
                </el-form-item>

                <el-form-item label="姓名" prop="name" class="custom-form-item">
                  <el-input
                    v-model="registerForm.name"
                    placeholder="请输入真实姓名"
                    :prefix-icon="User"
                    size="large"
                  />
                </el-form-item>

                <el-form-item label="密码" prop="password" class="custom-form-item">
                  <el-input
                    v-model="registerForm.password"
                    type="password"
                    placeholder="请输入密码"
                    :prefix-icon="Lock"
                    show-password
                    size="large"
                  />
                </el-form-item>

                <el-form-item label="确认密码" required class="custom-form-item">
                  <el-input
                    v-model="registerForm.confirmPassword"
                    type="password"
                    placeholder="请再次输入密码"
                    :prefix-icon="Lock"
                    show-password
                    size="large"
                  />
                </el-form-item>
              </div>
            </div>

            <!-- 身份信息 -->
            <div class="form-section-block">
              <div class="section-header">
                <div class="section-icon">
                  <el-icon><Medal /></el-icon>
                </div>
                <span class="section-title">身份信息（至少选择一个）</span>
              </div>

              <div class="identity-cards">
                <!-- 第一个身份 -->
                <div class="identity-card primary">
                  <div class="card-badge">主要身份</div>
                  <div class="identity-grid">
                    <el-form-item label="年级" required class="custom-form-item">
                      <el-select
                        v-model="role1Grade"
                        placeholder="请选择年级"
                        size="large"
                        class="custom-select"
                      >
                        <el-option
                          v-for="item in gradeOptions"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="职位" required class="custom-form-item">
                      <el-select
                        v-model="role1Position"
                        placeholder="请选择职位"
                        size="large"
                        class="custom-select"
                      >
                        <el-option
                          v-for="item in roleOptions"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </el-form-item>
                  </div>
                </div>

                <!-- 添加第二个身份按钮 -->
                <div v-if="!showSecondRole" class="add-identity-wrapper">
                  <button
                    type="button"
                    class="add-identity-btn"
                    @click="showSecondRole = true"
                  >
                    <el-icon><Plus /></el-icon>
                    <span>添加第二个身份（可选）</span>
                  </button>
                </div>

                <!-- 第二个身份 -->
                <div v-if="showSecondRole" class="identity-card secondary">
                  <div class="card-badge secondary">次要身份</div>
                  <button
                    type="button"
                    class="remove-identity-btn"
                    @click="showSecondRole = false; role2Grade = ''; role2Position = ''"
                  >
                    <el-icon><Close /></el-icon>
                  </button>
                  <div class="identity-grid">
                    <el-form-item label="年级" class="custom-form-item">
                      <el-select
                        v-model="role2Grade"
                        placeholder="请选择年级"
                        size="large"
                        class="custom-select"
                      >
                        <el-option
                          v-for="item in gradeOptions"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="职位" class="custom-form-item">
                      <el-select
                        v-model="role2Position"
                        placeholder="请选择职位"
                        size="large"
                        class="custom-select"
                      >
                        <el-option
                          v-for="item in roleOptions"
                          :key="item.value"
                          :label="item.label"
                          :value="item.value"
                        />
                      </el-select>
                    </el-form-item>
                  </div>
                </div>
              </div>
            </div>

            <!-- 激活码 -->
            <div class="form-section-block">
              <div class="section-header">
                <div class="section-icon">
                  <el-icon><Key /></el-icon>
                </div>
                <span class="section-title">激活验证</span>
              </div>

              <el-form-item label="激活码" prop="activationCode" class="custom-form-item">
                <el-input
                  v-model="registerForm.activationCode"
                  placeholder="请输入激活码（请向管理员获取）"
                  :prefix-icon="Key"
                  size="large"
                />
              </el-form-item>
            </div>

            <!-- 提交按钮 -->
            <div class="form-actions">
              <button
                type="submit"
                class="submit-btn"
                :disabled="loading"
                :class="{ loading: loading }"
              >
                <span class="btn-content">
                  <el-icon v-if="loading" class="animate-spin"><Loading /></el-icon>
                  <span>{{ loading ? '注册中...' : '立即注册' }}</span>
                </span>
                <div class="btn-glow"></div>
              </button>
            </div>
          </el-form>

          <!-- 移动端切换链接 -->
          <div class="mobile-switch">
            <span>已有账号？</span>
            <router-link to="/login" class="mobile-link">立即登录</router-link>
          </div>
        </div>
      </div>

      <!-- 右侧 - 欢迎区域 -->
      <div class="welcome-section">
        <div class="welcome-content">
          <div class="logo-large">
            <div class="logo-icon-large">
              <el-icon :size="48" color="white"><User /></el-icon>
            </div>
          </div>
          <h2 class="welcome-title">你好，新伙伴！</h2>
          <p class="welcome-desc">
            已经有账号了？<br>
            欢迎回来，继续你的旅程
          </p>
          <button class="switch-btn" @click="goToLogin">
            <el-icon><ArrowLeft /></el-icon>
            <span>去登录</span>
          </button>
        </div>
        <!-- 装饰圆圈 -->
        <div class="deco-circle circle-1"></div>
        <div class="deco-circle circle-2"></div>
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
  background: linear-gradient(135deg, #FFF8F5 0%, #FFFBF5 25%, #FFF5F0 50%, #FDF2F0 75%, #FFF0ED 100%);
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
  left: -200px;
  animation-delay: 0s;
}

.blob-2 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #FCD34D, #FBBF24);
  bottom: -150px;
  right: -150px;
  animation-delay: -5s;
}

.blob-3 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #FDA4AF, #FB7185);
  top: 40%;
  right: 20%;
  opacity: 0.35;
  animation-delay: -10s;
}

.blob-4 {
  width: 350px;
  height: 350px;
  background: linear-gradient(135deg, #A7F3D0, #6EE7B7);
  bottom: 20%;
  left: 15%;
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
  right: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 15px;
  height: 15px;
  background: linear-gradient(135deg, #F59E0B, #10B981);
  top: 60%;
  left: 10%;
  animation-delay: -2s;
}

.shape-3 {
  width: 12px;
  height: 12px;
  background: linear-gradient(135deg, #FB7185, #FF6B4A);
  bottom: 30%;
  right: 15%;
  animation-delay: -4s;
}

.shape-4 {
  width: 18px;
  height: 18px;
  background: linear-gradient(135deg, #60A5FA, #A78BFA);
  top: 30%;
  left: 20%;
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
  max-width: 1000px;
  min-height: 640px;
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
  transform: translateX(30px) scale(0.98);
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
   左侧表单区域
   ============================================ */
.form-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
  overflow-y: auto;
  max-height: 90vh;
}

.form-card {
  width: 100%;
  max-width: 480px;
}

.mobile-welcome {
  text-align: center;
  margin-bottom: var(--space-6);
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

/* 表单区块 */
.form-section-block {
  background: rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  padding: var(--space-5);
  border: 1px solid rgba(255, 255, 255, 0.8);
  margin-bottom: var(--space-5);
  transition: all 0.3s ease;
}

.form-section-block:hover {
  background: rgba(255, 255, 255, 0.8);
  box-shadow: 0 4px 20px rgba(255, 107, 74, 0.1);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.section-icon {
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
}

.section-title {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--text-primary);
}

/* 表单网格 */
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

/* 自定义表单项 */
:deep(.custom-form-item) {
  margin-bottom: 0;
}

:deep(.custom-form-item .el-form-item__label) {
  font-weight: var(--font-medium);
  color: var(--text-secondary);
  padding-bottom: var(--space-1);
}

:deep(.custom-form-item .el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(229, 231, 235, 0.8) inset;
  transition: all 0.3s ease;
  padding: 4px 16px;
}

:deep(.custom-form-item .el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #FF8A70 inset;
}

:deep(.custom-form-item .el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px #FF8A70 inset, 0 0 0 4px rgba(255, 138, 112, 0.15);
}

:deep(.custom-select .el-input__wrapper) {
  border-radius: 12px;
}

/* 身份卡片 */
.identity-cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.identity-card {
  background: white;
  border-radius: 16px;
  padding: var(--space-4);
  position: relative;
  border: 2px solid transparent;
  transition: all 0.3s ease;
}

.identity-card.primary {
  border-color: rgba(255, 107, 74, 0.3);
  box-shadow: 0 4px 20px rgba(255, 107, 74, 0.1);
}

.identity-card.secondary {
  border-color: rgba(245, 158, 11, 0.3);
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.1);
}

.identity-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 30px rgba(255, 107, 74, 0.15);
}

.card-badge {
  position: absolute;
  top: -10px;
  left: 16px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: var(--font-semibold);
  color: white;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
}

.card-badge.secondary {
  background: linear-gradient(135deg, #F59E0B, #D97706);
}

.identity-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
  padding-top: var(--space-2);
}

@media (max-width: 640px) {
  .identity-grid {
    grid-template-columns: 1fr;
  }
}

/* 添加身份按钮 */
.add-identity-wrapper {
  display: flex;
  justify-content: center;
}

.add-identity-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-6);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: #E35532;
  background: rgba(255, 107, 74, 0.08);
  border: 2px dashed rgba(255, 107, 74, 0.3);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.add-identity-btn:hover {
  background: rgba(255, 107, 74, 0.15);
  border-color: rgba(255, 107, 74, 0.5);
  transform: translateY(-2px);
}

/* 移除身份按钮 */
.remove-identity-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(239, 68, 68, 0.1);
  border: none;
  border-radius: 8px;
  color: #EF4444;
  cursor: pointer;
  transition: all 0.3s ease;
}

.remove-identity-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  transform: scale(1.1);
}

/* 表单操作 */
.form-actions {
  margin-top: var(--space-2);
}

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

/* 动画 */
.animate-spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ============================================
   右侧欢迎区域
   ============================================ */
.welcome-section {
  position: relative;
  width: 40%;
  background: linear-gradient(135deg, #FF6B4A 0%, #E35532 50%, #F59E0B 100%);
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
  transform: translateX(-4px);
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
  left: -100px;
}

.circle-2 {
  width: 200px;
  height: 200px;
  bottom: -60px;
  right: -60px;
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
@media (max-width: 900px) {
  .auth-container {
    flex-direction: column;
    max-width: 500px;
    min-height: auto;
  }

  .welcome-section {
    display: none;
  }

  .form-section {
    padding: var(--space-6);
    max-height: none;
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

  .form-section-block {
    background: rgba(255, 255, 255, 0.05);
  }

  .identity-card {
    background: rgba(255, 255, 255, 0.05);
  }
}
</style>
