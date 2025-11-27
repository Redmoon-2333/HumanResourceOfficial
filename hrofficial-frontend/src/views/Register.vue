<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const registerForm = ref({
  username: '',
  password: '',
  name: '',
  activationCode: '',
  confirmPassword: ''
})

// 身份选择相关
const selectedRoles = ref<string[]>([])
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
                v-model="registerForm.confirmPassword"
                type="password"
                placeholder="请再次输入密码"
                prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第一个身份（必选） -->
        <el-divider content-position="left">
          <el-icon><UserFilled /></el-icon>
          身份信息（至少选择一个）
        </el-divider>
        
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="年级" required>
              <el-select 
                v-model="role1Grade" 
                placeholder="请选择年级"
                style="width: 100%"
              >
                <el-option
                  v-for="item in gradeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位" required>
              <el-select 
                v-model="role1Position" 
                placeholder="请选择职位"
                style="width: 100%"
              >
                <el-option
                  v-for="item in roleOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 第二个身份（可选） -->
        <div v-if="!showSecondRole" style="margin-bottom: 18px">
          <el-button 
            type="primary" 
            plain 
            size="small"
            @click="showSecondRole = true"
          >
            <el-icon><Plus /></el-icon>
            添加第二个身份（可选）
          </el-button>
        </div>

        <div v-if="showSecondRole">
          <el-divider content-position="left">
            <el-icon><UserFilled /></el-icon>
            第二个身份（可选）
            <el-button 
              type="danger" 
              text 
              size="small"
              @click="showSecondRole = false; role2Grade = ''; role2Position = ''"
              style="margin-left: 10px"
            >
              <el-icon><Close /></el-icon>
              取消
            </el-button>
          </el-divider>
          
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="年级">
                <el-select 
                  v-model="role2Grade" 
                  placeholder="请选择年级"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in gradeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="职位">
                <el-select 
                  v-model="role2Position" 
                  placeholder="请选择职位"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in roleOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

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
