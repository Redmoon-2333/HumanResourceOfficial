﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import { ref, computed, onMounted } from 'vue'
import { getCreatedTasks, getTaskDetail, createTask, deleteTask, getCandidates, remindTask } from '@/api/tasks'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Bell, View, Management, DataAnalysis, CircleCheck, Clock } from '@element-plus/icons-vue'
import type { TaskDetail, Candidate, TaskCreateRequest } from '@/types'

const loading = ref(false)
const tasks = ref<TaskDetail[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const showCreateDialog = ref(false)
const showDetailDrawer = ref(false)
const currentTask = ref<TaskDetail | null>(null)
const candidates = ref<Candidate[]>([])

const createForm = ref<TaskCreateRequest>({
  title: '',
  description: '',
  dueTime: '',
  priority: 1,
  assigneeIds: []
  // remindCooldownMinutes 已由后端固定为24小时
})

const priorityMap: Record<number, { label: string; type: string }> = {
  0: { label: '低', type: 'info' },
  1: { label: '中', type: 'warning' },
  2: { label: '高', type: 'danger' }
}

const openCount = computed(() => tasks.value.filter(t => t.status === 'OPEN').length)
const closedCount = computed(() => tasks.value.filter(t => t.status === 'CLOSED').length)

async function fetchTasks() {
  loading.value = true
  try {
    const res = await getCreatedTasks({ page: currentPage.value, pageSize: pageSize.value })
    if ((res.code === 0 || res.code === 200) && res.data) {
      tasks.value = res.data.records
      total.value = res.data.total
    }
  } catch (e: any) {
    ElMessage.error(e.message || '获取任务列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchCandidates() {
  try {
    const res = await getCandidates()
    if ((res.code === 0 || res.code === 200) && res.data) candidates.value = res.data
  } catch (e: any) {
    ElMessage.error(e.message || '获取候选部员失败')
  }
}

async function handleCreate() {
  if (!createForm.value.title.trim()) { ElMessage.warning('请输入任务标题'); return }
  if (createForm.value.assigneeIds.length === 0) { ElMessage.warning('请至少选择一名指派人'); return }
  try {
    const res = await createTask(createForm.value)
    if (res.code === 0 || res.code === 200) {
      ElMessage.success('任务创建成功')
      showCreateDialog.value = false
      createForm.value = { title: '', description: '', dueTime: '', priority: 1, assigneeIds: [] }
      fetchTasks()
    }
  } catch (e: any) {
    ElMessage.error(e.message || '创建任务失败')
  }
}

async function handleDelete(taskId: number) {
  try {
    await ElMessageBox.confirm('确认删除此任务？', '删除确认', { type: 'warning' })
    const res = await deleteTask(taskId)
    if (res.code === 0 || res.code === 200) { ElMessage.success('任务已删除'); fetchTasks() }
  } catch { /* cancel */ }
}

async function handleRemind(assignmentId: number) {
  try {
    const res = await remindTask(assignmentId)
    if (res.code === 0 || res.code === 200) {
      ElMessage.success('催促已发送')
      if (currentTask.value) {
        const detail = await getTaskDetail(currentTask.value.taskId)
        if (detail.code === 0 || detail.code === 200) currentTask.value = detail.data
      }
    }
  } catch (e: any) {
    ElMessage.error(e.message || '催促失败')
  }
}

async function openDetail(taskId: number) {
  try {
    const res = await getTaskDetail(taskId)
    if ((res.code === 0 || res.code === 200) && res.data) {
      currentTask.value = res.data
      showDetailDrawer.value = true
    } else {
      ElMessage.error(res.message || '获取任务详情失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '获取任务详情失败')
  }
}

function openCreate() { fetchCandidates(); showCreateDialog.value = true }
function formatDate(dateStr: string | null) { return dateStr ? new Date(dateStr).toLocaleString('zh-CN') : '-' }
function progressText(row: TaskDetail): string {
  const done = row.assignments?.filter(a => a.status === 'DONE').length || 0
  const t = row.assignments?.length || 0
  return `${done}/${t}`
}
function canRemind(a: { status: string }): boolean { return a.status !== 'DONE' }

onMounted(fetchTasks)
</script>

<template>
  <Layout>
    <div class="task-mgmt-page">
      <div class="page-hero">
        <div class="page-hero-bg">
          <div class="gradient-orb orb-1"></div>
          <div class="gradient-orb orb-2"></div>
          <div class="network-pattern">
            <svg viewBox="0 0 400 400" class="network-svg">
              <defs>
                <linearGradient id="tmLineGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#8952B2;stop-opacity:0.3" />
                  <stop offset="100%" style="stop-color:#A672C9;stop-opacity:0.3" />
                </linearGradient>
              </defs>
              <circle cx="110" cy="90" r="4" fill="#8952B2" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" repeatCount="indefinite" />
              </circle>
              <circle cx="290" cy="140" r="4" fill="#A672C9" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" begin="0.5s" repeatCount="indefinite" />
              </circle>
              <circle cx="180" cy="310" r="4" fill="#6B3F9B" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" begin="1s" repeatCount="indefinite" />
              </circle>
              <line x1="110" y1="90" x2="290" y2="140" stroke="url(#tmLineGradient)" stroke-width="1" opacity="0.5" />
              <line x1="290" y1="140" x2="180" y2="310" stroke="url(#tmLineGradient)" stroke-width="1" opacity="0.5" />
              <line x1="180" y1="310" x2="110" y2="90" stroke="url(#tmLineGradient)" stroke-width="1" opacity="0.5" />
            </svg>
          </div>
        </div>

        <div class="page-hero-content">
          <div class="page-hero-badge">
            <el-icon><Management /></el-icon>
            <span>部长工作台</span>
          </div>
          <h1 class="page-hero-title">
            任务<span class="gradient-text">分配与跟进</span>
          </h1>
          <p class="page-hero-sub">
            创建任务、指派部员、跟踪进度，通过邮件与站内信双通道手动催促
          </p>
          <el-button type="primary" size="large" :icon="Plus" class="page-hero-btn" @click="openCreate">
            新建任务
          </el-button>
        </div>
      </div>

      <div class="dashboard-section">
        <div class="dashboard-grid">
          <div class="dashboard-card">
            <div class="card-header">
              <el-icon :size="20" color="#8952B2"><DataAnalysis /></el-icon>
              <span>任务总数</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">{{ total }}</span>
              <span class="stat-label">我创建的任务</span>
            </div>
          </div>
          <div class="dashboard-card">
            <div class="card-header">
              <el-icon :size="20" color="#E6A23C"><Clock /></el-icon>
              <span>进行中</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">{{ openCount }}</span>
              <span class="stat-label">尚未关闭</span>
            </div>
          </div>
          <div class="dashboard-card">
            <div class="card-header">
              <el-icon :size="20" color="#10B981"><CircleCheck /></el-icon>
              <span>已关闭</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">{{ closedCount }}</span>
              <span class="stat-label">全员已完成</span>
            </div>
          </div>
        </div>
      </div>

      <div class="content-section">
        <el-table
          :data="tasks"
          v-loading="loading"
          stripe
          style="width: 100%"
          class="task-table"
          :header-cell-style="{ background: 'rgba(137, 82, 178, 0.06)', color: '#4A2F6B', fontWeight: 600 }"
        >
          <el-table-column prop="title" label="任务标题" min-width="220" show-overflow-tooltip />
          <el-table-column label="指派进度" width="120" align="center">
            <template #default="{ row }">
              <el-tag round size="small">{{ progressText(row) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="priorityMap[row.priority]?.type" size="small">{{ priorityMap[row.priority]?.label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="截止时间" width="180">
            <template #default="{ row }">{{ formatDate(row.dueTime) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="row.status === 'OPEN' ? 'warning' : 'success'" size="small">
                {{ row.status === 'OPEN' ? '进行中' : '已关闭' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" align="center">
            <template #default="{ row }">
              <el-button size="small" :icon="View" @click="openDetail(row.taskId)">详情</el-button>
              <el-button size="small" type="danger" :icon="Delete" @click="handleDelete(row.taskId)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchTasks"
          class="pagination"
        />
      </div>

      <el-dialog v-model="showCreateDialog" title="新建任务" width="640px" class="create-dialog" append-to-body>
        <el-form :model="createForm" label-width="100px">
          <el-form-item label="任务标题" required>
            <el-input v-model="createForm.title" placeholder="请输入任务标题" maxlength="200" show-word-limit />
          </el-form-item>
          <el-form-item label="任务描述">
            <el-input v-model="createForm.description" type="textarea" :rows="3" placeholder="请输入任务描述" maxlength="5000" show-word-limit />
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker v-model="createForm.dueTime" type="datetime" placeholder="选择截止时间" style="width: 100%" />
          </el-form-item>
          <el-form-item label="优先级">
            <el-radio-group v-model="createForm.priority">
              <el-radio :value="0">低</el-radio>
              <el-radio :value="1">中</el-radio>
              <el-radio :value="2">高</el-radio>
            </el-radio-group>
          </el-form-item>
          <!-- 冷却时间已由后端固定为24小时，不再由前端配置 -->
          <el-form-item label="指派人" required>
            <el-select v-model="createForm.assigneeIds" multiple filterable placeholder="选择部员" style="width: 100%">
              <el-option v-for="c in candidates" :key="c.userId" :label="`${c.name} (${c.studentId})`" :value="c.userId" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" class="primary-btn" @click="handleCreate">创建</el-button>
        </template>
      </el-dialog>

      <el-drawer v-model="showDetailDrawer" title="任务详情" size="520px" append-to-body>
        <template v-if="currentTask">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="标题">{{ currentTask.title }}</el-descriptions-item>
            <el-descriptions-item label="描述">{{ currentTask.description || '-' }}</el-descriptions-item>
            <el-descriptions-item label="截止时间">{{ formatDate(currentTask.dueTime) }}</el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag :type="priorityMap[currentTask.priority]?.type" size="small">{{ priorityMap[currentTask.priority]?.label }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="currentTask.status === 'OPEN' ? 'warning' : 'success'">
                {{ currentTask.status === 'OPEN' ? '进行中' : '已关闭' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="催促间隔">24 小时</el-descriptions-item>
          </el-descriptions>

          <h3 class="drawer-section-title">指派人列表</h3>
          <div v-for="a in currentTask.assignments" :key="a.assignmentId" class="assignee-item">
            <div class="assignee-info">
              <span class="assignee-name">{{ a.assigneeName }}</span>
              <span class="assignee-sid">{{ a.assigneeStudentId }}</span>
              <el-tag :type="a.status === 'DONE' ? 'success' : 'warning'" size="small">
                {{ a.status === 'DONE' ? '已完成' : '待完成' }}
              </el-tag>
            </div>
            <div class="assignee-actions">
              <span v-if="a.remindCount > 0" class="remind-info">已催促 {{ a.remindCount }} 次</span>
              <el-button
                v-if="canRemind(a)"
                size="small"
                type="warning"
                :icon="Bell"
                @click="handleRemind(a.assignmentId)"
              >催促</el-button>
            </div>
          </div>
        </template>
      </el-drawer>
    </div>
  </Layout>
</template>

<style scoped>
/* ===== 主题：宝石紫（nav 第 11 位，位于 知识库管理#4F46E5 与 日常图片#F59E0B 之间 1/3 处） ===== */
.task-mgmt-page {
  --theme-primary: #8952B2;
  --theme-accent:  #A672C9;
  --theme-deep:    #6B3F9B;
  --theme-soft:    rgba(137, 82, 178, 0.06);
  --theme-border:  rgba(137, 82, 178, 0.15);
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-6) var(--space-12);
}

.page-hero {
  position: relative;
  padding: var(--space-10) var(--space-8);
  margin: var(--space-6);
  margin-bottom: 0;
  border-radius: 32px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(137, 82, 178, 0.06) 0%, rgba(255, 255, 255, 0.95) 100%);
  border: 1px solid var(--theme-border);
}
.page-hero-bg { position: absolute; inset: 0; overflow: hidden; }
.gradient-orb { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.4; }
.orb-1 {
  width: 400px; height: 400px;
  background: radial-gradient(ellipse 60% 50% at 40% 60%, #8952B2 0%, rgba(137, 82, 178, 0.4) 45%, transparent 70%);
  top: -100px; right: -100px;
  animation: float-orb 8s ease-in-out infinite;
  border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
  filter: blur(60px); opacity: 0.35;
}
.orb-2 {
  width: 300px; height: 300px;
  background: radial-gradient(ellipse 55% 65% at 55% 35%, #A672C9 0%, rgba(166, 114, 201, 0.35) 50%, transparent 65%);
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
  box-shadow: 0 4px 12px rgba(137, 82, 178, 0.3);
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
  margin-bottom: 24px;
}
.page-hero-btn {
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-accent)) !important;
  border: none !important;
  box-shadow: 0 6px 20px rgba(137, 82, 178, 0.35);
  animation: fadeInUp 0.6s ease 0.3s both;
}
.page-hero-btn:hover { box-shadow: 0 8px 28px rgba(137, 82, 178, 0.45); transform: translateY(-1px); }

.dashboard-section {
  position: relative;
  margin: var(--space-8) var(--space-6);
  margin-top: var(--space-6);
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
.dashboard-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.dashboard-card {
  background: #FAFAFA; border-radius: 16px; padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}
.dashboard-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(137, 82, 178, 0.1);
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
  overflow: visible;
}
.task-table { background: transparent; border-radius: 12px; overflow: hidden; }
:deep(.task-table) { --el-table-border-color: rgba(137, 82, 178, 0.12); }
:deep(.task-table tr:hover > td) { background: var(--theme-soft) !important; }
.pagination { margin-top: 16px; display: flex; justify-content: center; }

.create-dialog :deep(.el-dialog__footer) {
  padding: 16px 20px;
  border-top: 1px solid var(--theme-border);
}

.create-dialog :deep(.el-button--primary) {
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-accent)) !important;
  border: none !important;
  color: white !important;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(195, 94, 127, 0.35);
}

.create-dialog :deep(.el-button--primary:hover) {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(195, 94, 127, 0.45);
}
.unit-hint { margin-left: 10px; color: #9ca3af; font-size: 12px; }

.drawer-section-title {
  margin: 20px 0 10px; font-size: 15px; font-weight: 700;
  color: var(--theme-primary);
}
.assignee-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 14px; border-radius: 10px;
  background: var(--theme-soft);
  margin-bottom: 8px;
  border: 1px solid var(--theme-border);
}
.assignee-info { display: flex; align-items: center; gap: 10px; }
.assignee-name { font-weight: 600; color: #1f2937; }
.assignee-sid { color: #6b7280; font-size: 12px; }
.assignee-actions { display: flex; align-items: center; gap: 8px; }
.remind-info { color: #E6A23C; font-size: 12px; }

@media (max-width: 768px) {
  .dashboard-grid { grid-template-columns: 1fr; }
  .page-hero-title { font-size: 28px; }
  .page-hero { padding: var(--space-8) var(--space-5); }
}
</style>
