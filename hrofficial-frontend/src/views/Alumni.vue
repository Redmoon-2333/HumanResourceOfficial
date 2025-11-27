<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import Layout from '@/components/Layout.vue'
import { getAlumni } from '@/api/user'
import type { AlumniMember } from '@/types'
import { ElMessage } from 'element-plus'

// 年份分组数据结构
interface AlumniYearGroup {
  year: number
  members: AlumniMember[]
}

const loading = ref(false)
const alumniByYear = ref<AlumniYearGroup[]>([])
const keyword = ref('')

// 统计信息
const totalMembers = computed(() => {
  // 使用Set去重，统计独立的人数
  const uniqueNames = new Set<string>()
  alumniByYear.value.forEach(group => {
    group.members.forEach(member => {
      uniqueNames.add(member.name)
    })
  })
  return uniqueNames.size
})

// 记录总数（包含同一人在不同年份的多个身份）
const totalRecords = computed(() => {
  return alumniByYear.value.reduce((sum, group) => sum + group.members.length, 0)
})

const totalYears = computed(() => alumniByYear.value.length)

const allRoles = computed(() => {
  const rolesSet = new Set<string>()
  alumniByYear.value.forEach(group => {
    group.members.forEach(member => {
      if (member.role) {
        rolesSet.add(member.role)
      }
    })
  })
  return Array.from(rolesSet)
})

// 加载成员列表
const loadAlumni = async () => {
  loading.value = true
  try {
    const res = await getAlumni()
    if (res.code === 200 && res.data) {
      // 后端返回的是按年份分组的数据
      alumniByYear.value = res.data
    } else {
      ElMessage.error(res.message || '加载成员失败')
    }
  } catch (error: any) {
    console.error('加载往届成员失败:', error)
    ElMessage.error(error.message || '加载成员失败')
  } finally {
    loading.value = false
  }
}

// 搜索过滤
const filteredAlumni = computed(() => {
  if (!keyword.value.trim()) {
    return alumniByYear.value
  }
  
  const kw = keyword.value.toLowerCase()
  return alumniByYear.value.map(group => ({
    year: group.year,
    members: group.members.filter(member => 
      member.name.toLowerCase().includes(kw) ||
      member.role.toLowerCase().includes(kw)
    )
  })).filter(group => group.members.length > 0)
})

// 重置
const handleReset = () => {
  keyword.value = ''
}

// 格式化日期
const formatDate = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleDateString('zh-CN')
}

// 获取角色标签类型
const getRoleType = (role: string) => {
  if (role.includes('部长')) return 'danger'
  if (role.includes('副部长')) return 'warning'
  return ''
}

onMounted(() => {
  loadAlumni()
})
</script>

<template>
  <Layout>
    <div class="alumni-container">
      <!-- 头部 -->
      <div class="page-header">
        <h1 class="page-title">往届成员</h1>
        <p class="page-subtitle">按年份查看历届成员信息</p>
      </div>

      <!-- 搜索栏 -->
      <el-card class="search-card">
        <el-form :inline="true">
          <el-form-item label="搜索">
            <el-input
              v-model="keyword"
              placeholder="姓名/职位"
              clearable
              style="width: 300px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 按年份分组展示 -->
      <div v-loading="loading">
        <el-empty v-if="filteredAlumni.length === 0" description="暂无数据" />
        
        <div v-for="yearGroup in filteredAlumni" :key="yearGroup.year" class="year-section">
          <el-card>
            <template #header>
              <div class="year-header">
                <h2 class="year-title">
                  <el-icon><Calendar /></el-icon>
                  {{ yearGroup.year }} 年
                </h2>
                <el-tag size="large" type="info">
                  {{ yearGroup.members.length }} 人
                </el-tag>
              </div>
            </template>

            <el-table
              :data="yearGroup.members"
              stripe
              style="width: 100%"
            >
              <el-table-column type="index" label="#" width="60" />
              <el-table-column prop="name" label="姓名" min-width="150">
                <template #default="{ row }">
                  <div class="name-cell">
                    <el-avatar :size="32">
                      {{ row.name.charAt(0) }}
                    </el-avatar>
                    <span>{{ row.name }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="role" label="当年职位" min-width="150">
                <template #default="{ row }">
                  <el-tag
                    size="large"
                    :type="getRoleType(row.role)"
                  >
                    {{ row.role }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </div>

      <!-- 统计信息 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :xs="24" :sm="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <el-icon :size="32" color="var(--color-primary)">
                <UserFilled />
              </el-icon>
              <div>
                <p class="stat-value">{{ totalMembers }}</p>
                <p class="stat-label">独立人数</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <el-icon :size="32" color="var(--color-secondary)">
                <Document />
              </el-icon>
              <div>
                <p class="stat-value">{{ totalRecords }}</p>
                <p class="stat-label">记录总数</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <el-icon :size="32" color="var(--color-accent)">
                <Calendar />
              </el-icon>
              <div>
                <p class="stat-value">{{ totalYears }}</p>
                <p class="stat-label">年份数</p>
              </div>
            </div>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="6">
          <el-card class="stat-card">
            <div class="stat-content">
              <el-icon :size="32" color="var(--color-warning)">
                <Star />
              </el-icon>
              <div>
                <p class="stat-value">{{ allRoles.length }}</p>
                <p class="stat-label">职位种类</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </Layout>
</template>

<style scoped>
.alumni-container {
  padding: var(--spacing-lg);
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: var(--spacing-xl);
}

.page-title {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
  margin-bottom: var(--spacing-sm);
}

.page-subtitle {
  color: var(--color-text-secondary);
  font-size: 16px;
}

.search-card {
  margin-bottom: var(--spacing-lg);
}

.year-section {
  margin-bottom: var(--spacing-xl);
}

.year-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.year-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-primary);
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.name-cell {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
}

.stats-row {
  margin-top: var(--spacing-xl);
}

.stat-card {
  text-align: center;
}

.stat-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-lg);
  padding: var(--spacing-md);
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: var(--spacing-xs);
}

.stat-label {
  font-size: 14px;
  color: var(--color-text-secondary);
}
</style>
