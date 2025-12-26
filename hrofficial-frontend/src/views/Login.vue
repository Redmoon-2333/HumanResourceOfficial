<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login as loginApi } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginForm = ref({
  username: '',
  password: ''
})
const loading = ref(false)

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
    console.log('登录响应:', res) // 调试日志
    
    if (res.code === 200 && res.data) {
      // 确保数据存在
      if (!res.data.token) {
        ElMessage.error('登录响应数据格式错误')
        return
      }
      
      // 传递完整的登录数据给store
      userStore.login(res.data)
      ElMessage.success('登录成功')
      router.push('/home')
    } else {
      ElMessage.error(res.message || '登录失败')
    }
  } catch (error: any) {
    console.error('登录错误:', error) // 调试日志
    ElMessage.error(error.message || '登录失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1 class="gradient-text">人力资源管理系统</h1>
        <p>Human Resource Management System</p>
      </div>

      <el-form :model="loginForm" class="login-form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="loginForm.username"
            size="large"
            placeholder="用户名"
            prefix-icon="User"
          />
        </el-form-item>

        <el-form-item>
          <el-input
            v-model="loginForm.password"
            size="large"
            type="password"
            placeholder="密码"
            prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-button"
            native-type="submit"
          >
            登录
          </el-button>
        </el-form-item>

        <div class="login-footer">
          <router-link to="/register" class="register-link">
            还没有账号？立即注册
          </router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #FF6B35 0%, #F7931E 50%, #FFC312 100%);
  padding: var(--spacing-lg);
}

.login-box {
  width: 100%;
  max-width: 420px;
  background: white;
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.login-header h1 {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: var(--spacing-sm);
}

.login-header p {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.login-form {
  margin-top: var(--spacing-lg);
}

.login-button {
  width: 100%;
}

.login-footer {
  text-align: center;
  margin-top: var(--spacing-md);
}

.register-link {
  color: var(--color-primary);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.register-link:hover {
  color: var(--color-primary-dark);
  text-decoration: underline;
}
</style>
