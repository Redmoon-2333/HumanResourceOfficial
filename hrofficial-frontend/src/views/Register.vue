<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import type { RegisterRequest } from '@/types'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const registerForm = ref<RegisterRequest>({
  username: '',
  password: '',
  name: '',
  phone: '',
  email: '',
  studentId: '',
  grade: '',
  major: '',
  activationCode: ''
})

const confirmPassword = ref('')

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
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  studentId: [
    { required: true, message: '请输入学号', trigger: 'blur' }
  ],
  grade: [
    { required: true, message: '请输入年级', trigger: 'blur' }
  ],
  major: [
    { required: true, message: '请输入专业', trigger: 'blur' }
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
  if (registerForm.value.password !== confirmPassword.value) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }

  loading.value = true
  try {
    const res = await register(registerForm.value)
    if (res.code === 200) {
      ElMessage.success('注册成功，即将跳转到登录页...')
      setTimeout(() => {
        router.push('/login')
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
</script>

<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h1 class="gradient-text">用户注册</h1>
        <p>Human Resource Management System</p>
      </div>

      <el-form
        ref="formRef"
        :model="registerForm"
        :rules="rules"
        class="register-form"
        label-width="80px"
        @submit.prevent="handleRegister"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名"
                prefix-icon="User"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input
                v-model="registerForm.name"
                placeholder="请输入真实姓名"
                prefix-icon="User"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="请输入密码"
                prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" required>
              <el-input
                v-model="confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="学号" prop="studentId">
              <el-input
                v-model="registerForm.studentId"
                placeholder="请输入学号"
                prefix-icon="Postcard"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年级" prop="grade">
              <el-input
                v-model="registerForm.grade"
                placeholder="例如：2024"
                prefix-icon="Calendar"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="专业" prop="major">
          <el-input
            v-model="registerForm.major"
            placeholder="请输入专业"
            prefix-icon="School"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input
                v-model="registerForm.phone"
                placeholder="请输入手机号"
                prefix-icon="Phone"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="请输入邮箱"
                prefix-icon="Message"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="激活码" prop="activationCode">
          <el-input
            v-model="registerForm.activationCode"
            placeholder="请输入激活码（请向管理员获取）"
            prefix-icon="Key"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            @click="handleRegister"
            :loading="loading"
            class="register-button"
            size="large"
          >
            {{ loading ? '注册中...' : '注册' }}
          </el-button>
        </el-form-item>

        <div class="form-footer">
          <router-link to="/login" class="login-link">
            已有账号？立即登录
          </router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #FF6B35 0%, #F7931E 50%, #FFC312 100%);
  padding: var(--spacing-lg);
}

.register-box {
  width: 100%;
  max-width: 800px;
  background: white;
  border-radius: var(--radius-xl);
  padding: var(--spacing-2xl);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.register-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
}

.register-header h1 {
  font-size: 32px;
  font-weight: 700;
  margin-bottom: var(--spacing-sm);
}

.register-header p {
  color: var(--color-text-secondary);
  font-size: 14px;
}

.register-form {
  margin-top: var(--spacing-lg);
}

.register-button {
  width: 100%;
}

.form-footer {
  text-align: center;
  margin-top: var(--spacing-md);
}

.login-link {
  color: var(--color-primary);
  text-decoration: none;
  font-size: 14px;
  transition: color 0.3s;
}

.login-link:hover {
  color: var(--color-primary-dark);
  text-decoration: underline;
}
</style>
