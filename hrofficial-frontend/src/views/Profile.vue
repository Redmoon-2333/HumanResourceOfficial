<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { ref, computed } from 'vue'

const userStore = useUserStore()

const editDialogVisible = ref(false)
const editForm = ref({
  name: ''
})
const loading = ref(false)

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

const displayRoleHistory = computed(() => {
  if (!userStore.userInfo?.roleHistory) return ''
  return userStore.userInfo.roleHistory
})

// 解析roleHistory字符串
const parseRoleHistory = (roleHistory: string) => {
  if (!roleHistory) return
  
  const roles = roleHistory.split('&')
  
  // 解析第一个身份
  if (roles[0]) {
    const match1 = roles[0].match(/^(\d{4}级)(.+)$/)
    if (match1 && match1[1] && match1[2]) {
      role1Grade.value = match1[1]
      role1Position.value = match1[2]
    }
  }
  
  // 解析第二个身份
  if (roles[1]) {
    const match2 = roles[1].match(/^(\d{4}级)(.+)$/)
    if (match2 && match2[1] && match2[2]) {
      role2Grade.value = match2[1]
      role2Position.value = match2[2]
      showSecondRole.value = true
    }
  }
}

const handleEdit = () => {
  if (!userStore.userInfo) return
  
  editForm.value.name = userStore.userInfo.name
  parseRoleHistory(userStore.userInfo.roleHistory)
  
  editDialogVisible.value = true
}

const handleSave = async () => {
  if (!editForm.value.name.trim()) {
    ElMessage.warning('请输入姓名')
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
  
  // 构建roleHistory
  let roleHistory = `${role1Grade.value}${role1Position.value}`
  if (showSecondRole.value && role2Grade.value && role2Position.value) {
    roleHistory += `&${role2Grade.value}${role2Position.value}`
  }
  
  loading.value = true
  try {
    const res = await updateUserInfo({
      name: editForm.value.name,
      roleHistory: roleHistory
    })
    
    if (res.code === 200) {
      ElMessage.success('修改成功')
      // 更新本地用户信息
      if (userStore.userInfo) {
        userStore.userInfo.name = editForm.value.name
        userStore.userInfo.roleHistory = roleHistory
        localStorage.setItem('userInfo', JSON.stringify(userStore.userInfo))
      }
      editDialogVisible.value = false
    } else {
      ElMessage.error(res.message || '修改失败')
    }
  } catch (error: any) {
    ElMessage.error(error.message || '修改失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Layout>
    <div class="container">
      <div class="page-header">
        <h1 class="page-title">个人中心</h1>
        <p class="page-subtitle">查看和管理个人信息</p>
      </div>
      
      <el-card v-if="userStore.userInfo">
        <template #header>
          <div class="card-header">
            <span>个人信息</span>
            <el-button type="primary" @click="handleEdit">
              <el-icon><Edit /></el-icon>
              编辑资料
            </el-button>
          </div>
        </template>
        
        <el-descriptions :column="2" border>
          <el-descriptions-item label="用户名">{{ userStore.userInfo.username }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ userStore.userInfo.name }}</el-descriptions-item>
          <el-descriptions-item label="往届身份" :span="2">{{ displayRoleHistory }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
      
      <el-empty v-else description="加载中..." />
      
      <!-- 编辑对话框 -->
      <el-dialog
        v-model="editDialogVisible"
        title="编辑个人资料"
        width="600px"
      >
        <el-form :model="editForm" label-width="100px">
          <el-form-item label="姓名" required>
            <el-input v-model="editForm.name" placeholder="请输入姓名" />
          </el-form-item>
          
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
        </el-form>
        <template #footer>
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="loading">
            保存
          </el-button>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<style scoped>
.container {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.page-title {
  margin: 0 0 10px 0;
  font-size: 28px;
  font-weight: bold;
  color: var(--color-text-dark);
}

.page-subtitle {
  margin: 0;
  font-size: 14px;
  color: var(--color-text-light);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-tip {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}
</style>