<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage, ElTable, ElMessageBox } from 'element-plus'
import Layout from '@/components/Layout.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 表单数据
const form = reactive({
  expireDays: 30
})

// 生成的激活码列表
const codeList = ref<any[]>([])
const loading = ref(false)
const tableRef = ref<InstanceType<typeof ElTable>>()
const pageLoading = ref(false)

// 页面挂载时加载已生成的激活码
import { onMounted } from 'vue'

onMounted(async () => {
  await loadActivationCodes()
})

// 加载已生成的激活码
const loadActivationCodes = async () => {
  pageLoading.value = true
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      return
    }

    const response = await fetch('/api/users/activation-codes', {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    const result = await response.json()

    if (result.code === 200 && result.data) {
      // 格式化数据
      codeList.value = result.data.map((code: any) => ({
        codeId: code.codeId,
        code: code.code,
        status: code.status === '未使用' ? '未使用' : '已使用',
        createTime: new Date(code.createTime).toLocaleString(),
        expireTime: new Date(code.expireTime).toLocaleString()
      }))
    }
  } catch (error) {
    console.error('加载激活码失败:', error)
  } finally {
    pageLoading.value = false
  }
}

// 生成单个激活码
const generateCode = async () => {
  // 验证用户权限
  const roleHistory = userStore.userInfo?.roleHistory || ''
  if (!roleHistory.includes('部长')) {
    ElMessage.error('仅部长及以上权限可生成激活码')
    return
  }

  loading.value = true
  try {
    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('未登录，请先登录')
      return
    }

    const response = await fetch('/api/auth/generate-code', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        expireDays: form.expireDays
      })
    })

    const result = await response.json()

    if (result.code === 200) {
      const newCode = {
        code: result.data.code,
        expireTime: result.data.expireTime,
        createTime: new Date().toLocaleString(),
        status: '未使用'
      }
      codeList.value.unshift(newCode)
      ElMessage.success('激活码生成成功！')
    } else {
      ElMessage.error(result.message || '生成失败')
    }
  } catch (error) {
    console.error('生成激活码失败:', error)
    ElMessage.error('生成激活码失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 复制激活码
const copyCode = (code: string) => {
  navigator.clipboard.writeText(code).then(() => {
    ElMessage.success('已复制到剩贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 删除激活码
const deleteCode = async (codeId: number, code: string) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除激活码 ${code} 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const token = localStorage.getItem('token')
    if (!token) {
      ElMessage.error('未登录，请先登录')
      return
    }

    const response = await fetch(`/api/users/activation-codes/${codeId}/delete`, {
      method: 'PUT',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    })

    const result = await response.json()

    if (result.code === 200) {
      // 从列表中删除
      const index = codeList.value.findIndex((item: any) => item.codeId === codeId)
      if (index > -1) {
        codeList.value.splice(index, 1)
      }
      ElMessage.success('激活码删除成功')
    } else {
      ElMessage.error(result.message || '删除失败')
    }
  } catch (error: any) {
    if (error.message === 'Cancel') {
      // 用户取消
      return
    }
    console.error('删除激活码失败:', error)
    ElMessage.error('删除激活码失败，请检查网络连接')
  }
}

// 批量生成激活码
const generateBatch = async (count: number) => {
  if (count <= 0 || count > 100) {
    ElMessage.warning('请输入1-100之间的数字')
    return
  }

  loading.value = true
  let successCount = 0

  try {
    for (let i = 0; i < count; i++) {
      const token = localStorage.getItem('token')
      if (!token) {
        ElMessage.error('未登录，请先登录')
        break
      }

      const response = await fetch('/api/auth/generate-code', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          expireDays: form.expireDays
        })
      })

      const result = await response.json()

      if (result.code === 200) {
        const newCode = {
          code: result.data.code,
          expireTime: result.data.expireTime,
          createTime: new Date().toLocaleString(),
          status: '未使用'
        }
        codeList.value.unshift(newCode)
        successCount++
      }
    }

    ElMessage.success(`成功生成 ${successCount} 个激活码`)
  } catch (error) {
    console.error('批量生成激活码失败:', error)
    ElMessage.error('批量生成部分失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <Layout>
    <div class="activation-code-manager">
      <el-card class="manager-card">
        <template #header>
          <div class="card-header">
            <span class="title">激活码管理</span>
            <span class="subtitle">生成和管理用户注册激活码</span>
          </div>
        </template>

        <!-- 生成表单 -->
        <div class="generate-section">
          <el-form :model="form" label-width="100px" class="generate-form">
            <el-form-item label="有效期（天）">
              <el-input-number
                v-model="form.expireDays"
                :min="1"
                :max="365"
                placeholder="激活码有效期天数"
              />
            </el-form-item>

            <div class="button-group">
              <el-button type="primary" @click="generateCode" :loading="loading">
                生成单个激活码
              </el-button>
              <el-popconfirm
                title="确认批量生成激活码吗？"
                description="一次最多生成100个"
                @confirm="generateBatch(10)"
              >
                <template #reference>
                  <el-button type="success" :loading="loading">
                    批量生成（10个）
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </el-form>
        </div>

        <!-- 激活码列表 -->
        <div class="list-section">
          <h3>已生成的激活码</h3>
          <el-table
            ref="tableRef"
            :data="codeList"
            stripe
            style="width: 100%; margin-top: 20px"
            :default-sort="{ prop: 'createTime', order: 'descending' }"
          >
            <el-table-column
              prop="code"
              label="激活码"
              width="200"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                <span class="code-text">{{ row.code }}</span>
              </template>
            </el-table-column>
            <el-table-column
              prop="status"
              label="状态"
              width="100"
              :filters="[
                { text: '未使用', value: '未使用' },
                { text: '已使用', value: '已使用' }
              ]"
              :filter-method="
                (value: string, row: any) => row.status === value
              "
            >
              <template #default="{ row }">
                <el-tag :type="row.status === '未使用' ? 'success' : 'info'">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="createTime"
              label="生成时间"
              width="180"
              sortable
            />
            <el-table-column
              prop="expireTime"
              label="过期时间"
              width="180"
            />
            <el-table-column
              label="操作"
              width="150"
              fixed="right"
            >
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  size="small"
                  @click="copyCode(row.code)"
                >
                  复制
                </el-button>
                <el-button
                  link
                  type="danger"
                  size="small"
                  @click="deleteCode(row.codeId, row.code)"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 使用说明 -->
        <div class="instruction-section">
          <h3>使用说明</h3>
          <ul>
            <li>仅<strong>部长及以上权限</strong>的用户可以生成激活码</li>
            <li>激活码默认有效期为30天，可在生成前修改</li>
            <li>用户注册时需要提供有效的激活码</li>
            <li>激活码使用后状态会自动更新为"已使用"</li>
            <li>可以通过复制按钮快速复制激活码分发给用户</li>
          </ul>
        </div>
      </el-card>
    </div>
  </Layout>
</template>

<style scoped>
.activation-code-manager {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.manager-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.title {
  font-size: 20px;
  font-weight: bold;
}

.subtitle {
  font-size: 14px;
  color: var(--color-text-light);
}

.generate-section {
  margin-bottom: 30px;
  padding: 20px;
  background-color: var(--color-bg-secondary);
  border-radius: 8px;
}

.generate-form {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}

.generate-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.button-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.list-section {
  margin-bottom: 30px;
}

.list-section h3 {
  margin-bottom: 15px;
  color: var(--color-text-dark);
  font-size: 16px;
}

.code-text {
  font-family: monospace;
  font-size: 13px;
  padding: 4px 8px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.instruction-section {
  padding: 20px;
  background-color: var(--color-bg-secondary);
  border-radius: 8px;
  border-left: 4px solid var(--color-primary);
}

.instruction-section h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: var(--color-text-dark);
  font-size: 16px;
}

.instruction-section ul {
  margin: 0;
  padding-left: 20px;
}

.instruction-section li {
  margin-bottom: 10px;
  line-height: 1.6;
  color: var(--color-text);
}

.instruction-section strong {
  color: var(--color-primary);
  font-weight: bold;
}
</style>
