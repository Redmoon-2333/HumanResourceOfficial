<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import { ref, computed, onMounted } from 'vue'
import { getUsers, updateRole, appointMinister, appointDeputy } from '@/api/roles'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, User, Promotion, Avatar, UserFilled, Medal } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import type { User as UserType } from '@/types'

const userStore = useUserStore()
const loading = ref(false)
const users = ref<UserType[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showEditDialog = ref(false)
const editUser = ref<UserType | null>(null)
const updatingRole = ref(false)
const editForm = ref({ year: new Date().getFullYear(), newRole: '部员', reason: '' })

/**
 * 从 role_history 解析最新届别与角色
 * 格式统一为: 2024级部员, 2024级副部长, 2024级部长, 2024级部员&2025级部长
 * 兼容旧格式带空格: 2024 级部长, ["2024 级部长"]
 * 返回 null 表示尚未激活
 */
function parseLatestRole(roleHistory?: string | null): { year: number; role: string } | null {
  if (!roleHistory) return null
  // 清理 JSON 数组格式和空格
  const cleaned = roleHistory
    .replace(/^\[/, '')
    .replace(/\]$/, '')
    .replace(/"/g, '')
    .replace(/'/g, '')
    .replace(/ /g, '')
  const segments = cleaned.split('&').map(s => s.trim()).filter(Boolean)
  if (segments.length === 0) return null
  const last = segments[segments.length - 1]!
  let role: string | null = null
  if (last.includes('副部长')) role = '副部长'
  else if (last.includes('部长')) role = '部长'
  else if (last.includes('部员')) role = '部员'
  if (!role) return null
  const yearMatch = last.match(/(\d{4})/)
  if (!yearMatch) return null
  const result = { year: parseInt(yearMatch[1]!, 10), role }
  console.log('[parseLatestRole] input:', roleHistory, '| last:', last, '| parsed:', result)
  return result
}

/** 角色权重：部长 > 副部长 > 部员 > 无 */
function roleWeight(role?: string | null): number {
  if (role === '部长') return 3
  if (role === '副部长') return 2
  if (role === '部员') return 1
  return 0
}

// 当前登录用户
const currentUserId = computed<number | undefined>(() =>
  (userStore.userInfo as any)?.userId ?? userStore.userInfo?.id
)
const currentUserRole = computed(() => parseLatestRole(userStore.userInfo?.roleHistory))
const currentUserWeight = computed(() => roleWeight(currentUserRole.value?.role))

// 仪表板统计
const activeCount = computed(() => users.value.filter(u => u.roleHistory).length)

/**
 * 可用角色选项（根据当前用户身份智能过滤）
 * 部长：可分配 部长 / 副部长 / 部员
 * 副部长：只可分配 副部长 / 部员（不能任命部长）
 */
const availableRoles = computed(() => {
  const isMinister = currentUserRole.value?.role === '部长'
  return isMinister
    ? [{ label: '部长', value: '部长' }, { label: '副部长', value: '副部长' }, { label: '部员', value: '部员' }]
    : [{ label: '副部长', value: '副部长' }, { label: '部员', value: '部员' }]
})

/**
 * 权限判定：操作者能否编辑目标
 * 规则：
 *  1. 不能编辑自己
 *  2. 操作者角色权重必须严格大于或等于目标（严格同级以下 = <=，即目标权重 <= 操作者）
 *     注：按产品要求"同级以下才能编辑入口"，即同级不可互相编辑，只能编辑更低权重的用户
 *  3. 未激活用户（权重 0）可被任何部长/副部长编辑
 */
function canEdit(row: UserType): boolean {
  if (!currentUserId.value) return false
  const rowId = (row as any).userId ?? row.id
  if (rowId === currentUserId.value) return false
  const target = parseLatestRole(row.roleHistory)
  if (!target) return false
  if (target.year !== currentUserRole.value?.year) return false
  const targetWeight = roleWeight(target?.role)
  return currentUserWeight.value > targetWeight
}

/** 能否任命为部长：必须当前为部长才可操作 */
function canAppointMinister(row: UserType): boolean {
  return canEdit(row) && currentUserRole.value?.role === '部长'
}

/** 能否任命为副部长：部长或副部长都可，且遵守 canEdit 规则 */
function canAppointDeputy(row: UserType): boolean {
  return canEdit(row)
}

/** 修改身份：遵守 canEdit 规则 */
function canUpdateRole(row: UserType): boolean {
  return canEdit(row)
}

function getRowId(row: UserType): number | undefined {
  return (row as any).userId ?? row.id
}

function roleTagType(role?: string | null): 'danger' | 'warning' | 'success' | 'info' {
  if (role === '部长') return 'danger'
  if (role === '副部长') return 'warning'
  if (role === '部员') return 'success'
  return 'info'
}

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getUsers({ page: currentPage.value, pageSize: pageSize.value })
    if ((res.code === 0 || res.code === 200) && res.data) {
      users.value = res.data.records
      total.value = res.data.total
    }
  } catch (e: any) {
    ElMessage.error(e.message || '获取用户列表失败')
  } finally {
    loading.value = false
  }
}

function openEdit(user: UserType) {
  if (!canUpdateRole(user)) {
    ElMessage.warning('你没有权限修改该用户的身份')
    return
  }
  editUser.value = user
  const latest = parseLatestRole(user.roleHistory)
  editForm.value = {
    year: latest?.year ?? new Date().getFullYear(),
    newRole: '部员',
    reason: ''
  }
  // 副部长不允许将人任命为部长，前端 radio 层面限制
  showEditDialog.value = true
}

async function handleUpdateRole() {
  if (!editUser.value || updatingRole.value) return
  const target = parseLatestRole(editUser.value.roleHistory)
  if (!target || target.year !== editForm.value.year) {
    ElMessage.error('无法跨届任命')
    return
  }
  if (editForm.value.newRole === '部长' && currentUserRole.value?.role !== '部长') {
    ElMessage.error('只有部长可以将用户任命为部长')
    return
  }
  const id = getRowId(editUser.value)
  if (!id) {
    ElMessage.error('目标用户 ID 缺失，无法操作')
    return
  }
  updatingRole.value = true
  try {
    const res = await updateRole(id, editForm.value)
    if (res.code === 0 || res.code === 200) {
      ElMessage.success('角色更新成功')
      showEditDialog.value = false
      fetchUsers()
    } else {
      ElMessage.error(res.message || '更新失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '更新角色失败')
  } finally {
    updatingRole.value = false
  }
}

async function handleAppointMinister(user: UserType) {
  if (!canAppointMinister(user)) {
    ElMessage.warning('无权任命为部长')
    return
  }
  const id = getRowId(user)
  if (!id) { ElMessage.error('目标用户 ID 缺失'); return }
  try {
    await ElMessageBox.confirm(`确认任命 ${user.name} 为 ${new Date().getFullYear()} 届部长？`, '任命确认', { type: 'warning' })
    const res = await appointMinister(id, { year: new Date().getFullYear(), reason: '手动任命' })
    if (res.code === 0 || res.code === 200) { ElMessage.success('任命成功'); fetchUsers() }
  } catch { /* cancel */ }
}

async function handleAppointDeputy(user: UserType) {
  if (!canAppointDeputy(user)) {
    ElMessage.warning('无权任命为副部长')
    return
  }
  const id = getRowId(user)
  if (!id) { ElMessage.error('目标用户 ID 缺失'); return }
  try {
    await ElMessageBox.confirm(`确认任命 ${user.name} 为 ${new Date().getFullYear()} 届副部长？`, '任命确认', { type: 'warning' })
    const res = await appointDeputy(id, { year: new Date().getFullYear(), reason: '手动任命' })
    if (res.code === 0 || res.code === 200) { ElMessage.success('任命成功'); fetchUsers() }
  } catch { /* cancel */ }
}

onMounted(fetchUsers)
</script>

<template>
  <Layout>
    <div class="people-page">
      <div class="page-hero">
        <div class="page-hero-bg">
          <div class="gradient-orb orb-1"></div>
          <div class="gradient-orb orb-2"></div>
          <div class="network-pattern">
            <svg viewBox="0 0 400 400" class="network-svg">
              <defs>
                <linearGradient id="peopleLineGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#C35E7F;stop-opacity:0.3" />
                  <stop offset="100%" style="stop-color:#D98099;stop-opacity:0.3" />
                </linearGradient>
              </defs>
              <circle cx="120" cy="100" r="4" fill="#C35E7F" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" repeatCount="indefinite" />
              </circle>
              <circle cx="280" cy="130" r="4" fill="#D98099" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" begin="0.5s" repeatCount="indefinite" />
              </circle>
              <circle cx="200" cy="290" r="4" fill="#A94768" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" begin="1s" repeatCount="indefinite" />
              </circle>
              <circle cx="320" cy="280" r="3" fill="#D98099" opacity="0.5">
                <animate attributeName="opacity" values="0.5;0.9;0.5" dur="2.5s" begin="1.5s" repeatCount="indefinite" />
              </circle>
              <line x1="120" y1="100" x2="280" y2="130" stroke="url(#peopleLineGradient)" stroke-width="1" opacity="0.5" />
              <line x1="280" y1="130" x2="200" y2="290" stroke="url(#peopleLineGradient)" stroke-width="1" opacity="0.5" />
              <line x1="200" y1="290" x2="120" y2="100" stroke="url(#peopleLineGradient)" stroke-width="1" opacity="0.5" />
              <line x1="280" y1="130" x2="320" y2="280" stroke="url(#peopleLineGradient)" stroke-width="1" opacity="0.4" />
            </svg>
          </div>
        </div>

        <div class="page-hero-content">
          <div class="page-hero-badge">
            <el-icon><Avatar /></el-icon>
            <span>组织架构中心</span>
          </div>
          <h1 class="page-hero-title">
            人员<span class="gradient-text">身份管理</span>
          </h1>
          <p class="page-hero-sub">
            编辑部员身份、任命部长与副部长，维护组织的传承与结构
          </p>
        </div>
      </div>

      <div class="dashboard-section">
        <div class="dashboard-grid">
          <div class="dashboard-card">
            <div class="card-header">
              <el-icon :size="20" color="#C35E7F"><UserFilled /></el-icon>
              <span>在册成员</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">{{ total }}</span>
              <span class="stat-label">累计注册用户</span>
            </div>
          </div>
          <div class="dashboard-card">
            <div class="card-header">
              <el-icon :size="20" color="#10B981"><Medal /></el-icon>
              <span>已激活</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">{{ activeCount }}</span>
              <span class="stat-label">有身份记录</span>
            </div>
          </div>
        </div>
      </div>

      <div class="content-section">
        <el-table
          :data="users"
          v-loading="loading"
          stripe
          style="width: 100%"
          class="people-table"
          :header-cell-style="{ background: 'rgba(195, 94, 127, 0.06)', color: '#6B2F42', fontWeight: 600, fontSize: '13px' }"
          :cell-style="{ padding: '14px 0', fontSize: '13px' }"
          :default-sort="{ prop: 'name', order: 'ascending' }"
        >
          <el-table-column prop="name" label="姓名" width="120" sortable />
          <el-table-column prop="username" label="用户名" width="150" sortable />
          <el-table-column prop="studentId" label="学号" width="150">
            <template #default="{ row }">{{ row.studentId || '—' }}</template>
          </el-table-column>
          <el-table-column label="当前身份" min-width="150" sortable :sort-by="(row: any) => parseLatestRole(row.roleHistory)?.role ?? ''">
            <template #default="{ row }">
              <template v-if="parseLatestRole(row.roleHistory)">
                <el-tag :type="roleTagType(parseLatestRole(row.roleHistory)?.role)" effect="light">
                  {{ parseLatestRole(row.roleHistory)!.year }} 届{{ parseLatestRole(row.roleHistory)!.role }}
                </el-tag>
              </template>
              <el-tag v-else type="info">未激活</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="历史身份" min-width="240" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="history-text">{{ row.roleHistory || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="320" align="center" fixed="right">
            <template #default="{ row }">
              <template v-if="getRowId(row) === currentUserId">
                <el-tag size="small" type="info">当前登录</el-tag>
              </template>
              <template v-else-if="!canEdit(row)">
                <el-tooltip content="无权操作：需要高于目标用户的身份" placement="top">
                  <el-tag size="small" type="info">无权限</el-tag>
                </el-tooltip>
              </template>
              <template v-else>
                <el-button size="small" :icon="Edit" @click="openEdit(row)">修改身份</el-button>
                <el-button
                  v-if="canAppointMinister(row)"
                  size="small"
                  type="danger"
                  :icon="Promotion"
                  @click="handleAppointMinister(row)"
                >任命部长</el-button>
                <el-button
                  v-if="canAppointDeputy(row)"
                  size="small"
                  type="warning"
                  :icon="User"
                  @click="handleAppointDeputy(row)"
                >任命副部长</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchUsers"
          class="pagination"
        />
      </div>

      <el-dialog v-model="showEditDialog" title="修改身份" width="520px" class="edit-dialog">
        <el-form :model="editForm" label-width="80px">
          <el-form-item label="用户">
            <strong>{{ editUser?.name }}</strong>
            <span class="meta-text"> ({{ editUser?.username }})</span>
          </el-form-item>
          <el-form-item label="届别">
            <el-input-number v-model="editForm.year" :min="2020" :max="2030" />
          </el-form-item>
          <el-form-item label="新身份">
            <el-radio-group v-model="editForm.newRole">
              <el-radio
                v-for="role in availableRoles"
                :key="role.value"
                :value="role.value"
              >
                {{ role.label }}
              </el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="原因">
            <el-input v-model="editForm.reason" type="textarea" :rows="2" placeholder="请输入变更原因" maxlength="255" show-word-limit />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" class="primary-btn" @click="handleUpdateRole">确认</el-button>
        </template>
      </el-dialog>
    </div>
  </Layout>
</template>

<style scoped>
/* ===== 主题：尘粉（nav 第 12 位，位于 知识库管理#4F46E5 与 日常图片#F59E0B 之间 2/3 处，优雅收尾回归暖色系） ===== */
.people-page {
  --theme-primary: #C35E7F;
  --theme-accent:  #D98099;
  --theme-deep:    #A94768;
  --theme-soft:    rgba(195, 94, 127, 0.06);
  --theme-border:  rgba(195, 94, 127, 0.15);
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-6) var(--space-12);
}

.page-hero {
  position: relative;
  padding: var(--space-10) var(--space-8);
  margin: var(--space-6);
  margin-bottom: var(--space-8);
  border-radius: 32px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(195, 94, 127, 0.06) 0%, rgba(255, 255, 255, 0.95) 100%);
  border: 1px solid var(--theme-border);
}
.page-hero-bg { position: absolute; inset: 0; overflow: hidden; }
.gradient-orb { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.4; }
.orb-1 {
  width: 400px; height: 400px;
  background: radial-gradient(ellipse 60% 50% at 40% 60%, #C35E7F 0%, rgba(195, 94, 127, 0.4) 45%, transparent 70%);
  top: -100px; right: -100px;
  animation: float-orb 8s ease-in-out infinite;
  border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
  filter: blur(60px); opacity: 0.35;
}
.orb-2 {
  width: 300px; height: 300px;
  background: radial-gradient(ellipse 55% 65% at 55% 35%, #D98099 0%, rgba(217, 128, 153, 0.35) 50%, transparent 65%);
  bottom: -50px; left: -50px;
  animation: float-orb 10s ease-in-out infinite -3s;
  border-radius: 40% 60% 70% 30% / 40% 50% 50% 60%;
  filter: blur(50px); opacity: 0.25;
}
@keyframes float-orb {
  0%, 100% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(20px, -20px) scale(1.05); }
}
@keyframes fadeInDown {
  from { opacity: 0; transform: translateY(-20px); filter: blur(4px); }
  to { opacity: 1; transform: translateY(0); filter: blur(0); }
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); filter: blur(4px); }
  to { opacity: 1; transform: translateY(0); filter: blur(0); }
}
@keyframes badgeShine {
  0%, 100% { left: -100%; }
  50% { left: 100%; }
}
.network-pattern {
  position: absolute; inset: 0;
  display: flex; align-items: center; justify-content: center;
  opacity: 0.6;
}
.network-svg { width: 100%; height: 100%; max-width: 600px; }
.page-hero-content { position: relative; z-index: 1; max-width: 1200px; margin: 0 auto; text-align: center; }
.page-hero-badge {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-accent));
  border: none; border-radius: 20px;
  color: white; font-size: 14px; font-weight: 500;
  margin-bottom: 16px;
  box-shadow: 0 4px 12px rgba(195, 94, 127, 0.3);
  animation: fadeInDown 0.6s ease both;
  position: relative; overflow: hidden;
}
.page-hero-badge::before {
  content: ''; position: absolute; top: 0; left: -100%;
  width: 100%; height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: badgeShine 3s ease-in-out infinite;
}
.page-hero-title {
  font-size: 38px; font-weight: 700; color: #1C1917;
  margin-bottom: 10px; letter-spacing: -0.02em;
  animation: fadeInUp 0.6s ease 0.1s both;
}
.gradient-text {
  background: linear-gradient(135deg, var(--theme-primary) 0%, var(--theme-accent) 100%);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.page-hero-sub {
  font-size: 15px; color: #78716C;
  animation: fadeInUp 0.6s ease 0.2s both;
}

.dashboard-section {
  position: relative;
  margin: 0 var(--space-6);
  padding: 24px;
  background: #FFFFFF; border-radius: 24px;
  border: 1px solid var(--theme-border);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.dashboard-section::before {
  content: ''; position: absolute;
  top: 0; left: 6px; right: 6px; height: 3px;
  background: linear-gradient(90deg, var(--theme-primary), var(--theme-accent));
  border-radius: 18px 18px 0 0;
}
.dashboard-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.dashboard-card {
  background: #FAFAFA; border-radius: 16px; padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}
.dashboard-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(195, 94, 127, 0.1);
  border-color: var(--theme-border);
}
.card-header {
  display: flex; align-items: center; gap: 10px;
  font-size: 14px; font-weight: 600; color: #78716C;
  margin-bottom: 16px;
}
.stat-content { display: flex; flex-direction: column; justify-content: center; gap: 4px; }
.stat-number { font-size: 32px; font-weight: 700; color: #1C1917; line-height: 1; }
.stat-label { font-size: 13px; color: #A8A29E; }

.content-section {
  margin: 0 var(--space-6);
  padding: 20px;
  background: #FFFFFF;
  border-radius: 24px;
  border: 1px solid var(--theme-border);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow-x: auto;
}
.people-table { background: transparent; border-radius: 12px; overflow: hidden; }
:deep(.people-table) { --el-table-border-color: rgba(195, 94, 127, 0.12); }
:deep(.people-table tr:hover > td) { background: var(--theme-soft) !important; }
:deep(.el-table__fixed-right) { height: calc(100% - 1px) !important; }
.role-tag {
  color: var(--theme-deep) !important;
  border-color: var(--theme-border) !important;
  background: var(--theme-soft) !important;
}
.history-text { color: #6b7280; font-size: 13px; }
.stat-sep { color: #D1D5DB; font-weight: 400; margin: 0 4px; }
.disabled-hint { color: #A8A29E; font-size: 11px; margin-left: 4px; }
.pagination { margin-top: 16px; display: flex; justify-content: center; }
.meta-text { color: #9ca3af; font-size: 13px; }

.edit-dialog :deep(.primary-btn) {
  background: linear-gradient(135deg, #C35E7F, #D98099) !important;
  border: none !important;
  color: white !important;
}

@media (max-width: 768px) {
  .dashboard-grid { grid-template-columns: 1fr; }
  .page-hero-title { font-size: 28px; }
  .page-hero { padding: var(--space-8) var(--space-5); }
}
</style>

<style>
/* 非scoped：el-dialog append-to-body 渲染在 body 下 */
.edit-dialog .el-dialog__footer .el-button--primary.primary-btn {
  background: linear-gradient(135deg, #C35E7F, #D98099) !important;
  border-color: #C35E7F !important;
  color: white !important;
}
.edit-dialog .el-dialog__footer .el-button--primary.primary-btn:hover {
  background: linear-gradient(135deg, #B54D6E, #C97088) !important;
  border-color: #B54D6E !important;
  color: white !important;
}
</style>
