﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿﻿<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import { ref, computed, onMounted } from 'vue'
import { getMyTasks, markTaskDone } from '@/api/tasks'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Clock, Check, Bell, List, Timer, Flag, Trophy, Loading } from '@element-plus/icons-vue'
import type { Assignment } from '@/types'

const loading = ref(false)
const assignments = ref<Assignment[]>([])
const activeTab = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filteredAssignments = computed(() => {
  if (activeTab.value === 'pending') return assignments.value.filter(a => a.status === 'PENDING')
  if (activeTab.value === 'done') return assignments.value.filter(a => a.status === 'DONE')
  return assignments.value
})

const pendingCount = computed(() => assignments.value.filter(a => a.status === 'PENDING').length)
const doneCount = computed(() => assignments.value.filter(a => a.status === 'DONE').length)
const completionRate = computed(() => {
  if (assignments.value.length === 0) return 0
  return Math.round((doneCount.value / assignments.value.length) * 100)
})

const priorityMap: Record<number, { label: string; color: string }> = {
  0: { label: '低', color: '#909399' },
  1: { label: '中', color: '#E6A23C' },
  2: { label: '高', color: '#F56C6C' }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getMyTasks({ page: currentPage.value, pageSize: pageSize.value })
    if ((res.code === 0 || res.code === 200) && res.data) {
      assignments.value = res.data.records
      total.value = res.data.total
    }
  } catch (e: any) {
    ElMessage.error(e.message || '获取任务失败')
  } finally {
    loading.value = false
  }
}

async function handleDone(assignmentId: number) {
  try {
    await ElMessageBox.confirm('确认标记此任务为已完成？', '完成确认', { type: 'success' })
    const res = await markTaskDone(assignmentId)
    if (res.code === 0 || res.code === 200) {
      ElMessage.success('已标记完成')
      fetchData()
    }
  } catch { /* cancel */ }
}

function formatDate(dateStr: string | null) {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(fetchData)
</script>

<template>
  <Layout>
    <div class="task-board-page">
      <div class="page-hero">
        <div class="page-hero-bg">
          <div class="gradient-orb orb-1"></div>
          <div class="gradient-orb orb-2"></div>
          <div class="network-pattern">
            <svg viewBox="0 0 400 400" class="network-svg">
              <defs>
                <linearGradient id="taskLineGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#CE4AAA;stop-opacity:0.3" />
                  <stop offset="100%" style="stop-color:#E85A8E;stop-opacity:0.3" />
                </linearGradient>
              </defs>
              <circle cx="80" cy="120" r="4" fill="#CE4AAA" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" repeatCount="indefinite" />
              </circle>
              <circle cx="320" cy="90" r="4" fill="#E85A8E" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" begin="0.5s" repeatCount="indefinite" />
              </circle>
              <circle cx="220" cy="280" r="4" fill="#F472B6" opacity="0.6">
                <animate attributeName="opacity" values="0.6;1;0.6" dur="2s" begin="1s" repeatCount="indefinite" />
              </circle>
              <circle cx="130" cy="320" r="3" fill="#CE4AAA" opacity="0.5">
                <animate attributeName="opacity" values="0.5;0.9;0.5" dur="2.5s" begin="1.5s" repeatCount="indefinite" />
              </circle>
              <line x1="80" y1="120" x2="320" y2="90" stroke="url(#taskLineGradient)" stroke-width="1" opacity="0.5" />
              <line x1="320" y1="90" x2="220" y2="280" stroke="url(#taskLineGradient)" stroke-width="1" opacity="0.5" />
              <line x1="220" y1="280" x2="80" y2="120" stroke="url(#taskLineGradient)" stroke-width="1" opacity="0.5" />
              <line x1="220" y1="280" x2="130" y2="320" stroke="url(#taskLineGradient)" stroke-width="1" opacity="0.4" />
            </svg>
          </div>
        </div>

        <div class="page-hero-content">
          <div class="page-hero-badge">
            <el-icon><List /></el-icon>
            <span>个人任务中心</span>
          </div>
          <h1 class="page-hero-title">
            我的<span class="gradient-text">任务清单</span>
          </h1>
          <p class="page-hero-sub">
            查看部长指派给你的任务，按时响应与完成，推动组织运转
          </p>
        </div>
      </div>

      <div class="dashboard-section">
        <div class="dashboard-grid">
          <div class="dashboard-card stat-card">
            <div class="card-header">
              <el-icon :size="20" color="#CE4AAA"><Loading /></el-icon>
              <span>待完成</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">{{ pendingCount }}</span>
              <span class="stat-label">项任务等待处理</span>
            </div>
          </div>

          <div class="dashboard-card stat-card">
            <div class="card-header">
              <el-icon :size="20" color="#10B981"><Check /></el-icon>
              <span>已完成</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">{{ doneCount }}</span>
              <span class="stat-label">项任务已交付</span>
            </div>
          </div>

          <div class="dashboard-card stat-card">
            <div class="card-header">
              <el-icon :size="20" color="#E85A8E"><Trophy /></el-icon>
              <span>完成率</span>
            </div>
            <div class="stat-content">
              <span class="stat-number">{{ completionRate }}<span class="unit">%</span></span>
              <span class="stat-label">累计 {{ total }} 项</span>
            </div>
          </div>
        </div>
      </div>

      <div class="content-section">
        <el-tabs v-model="activeTab" class="task-tabs">
          <el-tab-pane label="全部" name="all" />
          <el-tab-pane label="待完成" name="pending" />
          <el-tab-pane label="已完成" name="done" />
        </el-tabs>

        <div v-loading="loading" class="task-list">
          <el-empty v-if="!loading && filteredAssignments.length === 0" description="暂无任务，等待部长分配" />

          <div
            v-for="a in filteredAssignments"
            :key="a.assignmentId"
            class="task-card"
            :class="{ 'task-card-done': a.status === 'DONE' }"
          >
            <div class="task-card-bar" />
            <div class="task-card-main">
              <div class="task-card-header">
                <span class="task-title">{{ a.taskTitle || `任务 #${a.assignmentId}` }}</span>
                <el-tag :type="a.status === 'DONE' ? 'success' : 'warning'" size="small" round effect="light">
                  {{ a.status === 'DONE' ? '已完成' : '待完成' }}
                </el-tag>
              </div>
              <p v-if="a.taskDescription" class="task-desc">{{ a.taskDescription }}</p>
              <div class="task-meta">
                <span v-if="a.creatorName">
                  <el-icon><Flag /></el-icon> 分配人：{{ a.creatorName }}
                </span>
                <span v-if="a.taskDueTime">
                  <el-icon><Timer /></el-icon> 截止：{{ formatDate(a.taskDueTime) }}
                </span>
                <span v-if="a.taskPriority !== undefined && a.taskPriority !== null">
                  <el-icon :color="priorityMap[a.taskPriority]?.color"><Flag /></el-icon>
                  优先级：{{ priorityMap[a.taskPriority]?.label }}
                </span>
                <span v-if="a.doneTime">
                  <el-icon><Clock /></el-icon> 完成：{{ formatDate(a.doneTime) }}
                </span>
                <span v-if="a.remindCount > 0" class="remind-warn">
                  <el-icon><Bell /></el-icon> 已催促 {{ a.remindCount }} 次
                </span>
              </div>
              <div v-if="a.doneRemark" class="task-remark">
                <el-icon><Check /></el-icon> {{ a.doneRemark }}
              </div>
            </div>
            <div class="task-card-actions">
              <el-button
                v-if="a.status === 'PENDING'"
                type="primary"
                :icon="Check"
                class="done-btn"
                @click="handleDone(a.assignmentId)"
              >
                标记完成
              </el-button>
            </div>
          </div>
        </div>

        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchData"
          class="pagination"
        />
      </div>
    </div>
  </Layout>
</template>

<style scoped>
/* ===== 主题：品红（nav 第 8 位，位于 策划案生成#A855F7 与 激活码管理#F43F5E 之间中点） ===== */
.task-board-page {
  --theme-primary: #CE4AAA;
  --theme-accent:  #E85A8E;
  --theme-soft:    rgba(206, 74, 170, 0.06);
  --theme-border:  rgba(206, 74, 170, 0.15);
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-6) var(--space-12);
}

/* Hero */
.page-hero {
  position: relative;
  padding: var(--space-10) var(--space-8);
  margin: var(--space-6);
  margin-bottom: 0;
  border-radius: 32px;
  overflow: hidden;
  background: linear-gradient(135deg, rgba(206, 74, 170, 0.06) 0%, rgba(255, 255, 255, 0.95) 100%);
  border: 1px solid var(--theme-border);
}
.page-hero-bg { position: absolute; inset: 0; overflow: hidden; }
.gradient-orb { position: absolute; border-radius: 50%; filter: blur(80px); opacity: 0.4; }
.orb-1 {
  width: 400px; height: 400px;
  background: radial-gradient(ellipse 60% 50% at 40% 60%, #CE4AAA 0%, rgba(206, 74, 170, 0.4) 45%, transparent 70%);
  top: -100px; right: -100px;
  animation: float-orb 8s ease-in-out infinite;
  border-radius: 60% 40% 30% 70% / 60% 30% 70% 40%;
  filter: blur(60px); opacity: 0.35;
}
.orb-2 {
  width: 300px; height: 300px;
  background: radial-gradient(ellipse 55% 65% at 55% 35%, #E85A8E 0%, rgba(232, 90, 142, 0.35) 50%, transparent 65%);
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
  box-shadow: 0 4px 12px rgba(206, 74, 170, 0.3);
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

/* Dashboard */
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
  box-shadow: 0 4px 16px rgba(206, 74, 170, 0.1);
  border-color: var(--theme-border);
}
.card-header {
  display: flex; align-items: center; gap: 10px;
  font-size: 14px; font-weight: 600; color: #78716C;
  margin-bottom: 16px;
}
.stat-card { display: flex; flex-direction: column; }
.stat-content { flex: 1; display: flex; flex-direction: column; justify-content: center; gap: 4px; }
.stat-number { font-size: 32px; font-weight: 700; color: #1C1917; line-height: 1; }
.stat-number .unit { font-size: 18px; color: #78716C; margin-left: 2px; }
.stat-label { font-size: 13px; color: #A8A29E; }

/* Content */
.content-section {
  margin: 0 var(--space-6);
  padding: 24px;
  background: #FFFFFF;
  border-radius: 24px;
  border: 1px solid var(--theme-border);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}
.task-tabs { margin-bottom: 12px; }
:deep(.task-tabs .el-tabs__active-bar) {
  background: linear-gradient(90deg, var(--theme-primary), var(--theme-accent));
}
:deep(.task-tabs .el-tabs__item.is-active) { color: var(--theme-primary); }
:deep(.task-tabs .el-tabs__item:hover) { color: var(--theme-accent); }

.task-list { display: flex; flex-direction: column; gap: 12px; min-height: 200px; }
.task-card {
  display: flex; gap: 16px; align-items: stretch;
  background: #FAFAFA; border-radius: 14px;
  border: 1px solid rgba(0, 0, 0, 0.04);
  padding: 16px 18px;
  transition: all 0.25s ease;
}
.task-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(206, 74, 170, 0.12);
  border-color: var(--theme-border);
}
.task-card-done { opacity: 0.75; }
.task-card-bar {
  width: 4px; border-radius: 4px; flex-shrink: 0;
  background: linear-gradient(180deg, var(--theme-primary), var(--theme-accent));
}
.task-card-done .task-card-bar { background: linear-gradient(180deg, #10B981, #34D399); }
.task-card-main { flex: 1; min-width: 0; }
.task-card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; gap: 12px; }
.task-title { font-weight: 700; font-size: 16px; color: #1f2937; }
.task-desc { color: #4b5563; font-size: 13px; line-height: 1.6; margin: 4px 0 10px; }
.task-meta { display: flex; flex-wrap: wrap; gap: 16px; color: #6b7280; font-size: 13px; }
.task-meta span { display: inline-flex; align-items: center; gap: 4px; }
.remind-warn { color: #E6A23C !important; }
.task-remark {
  margin-top: 8px; padding: 8px 12px; border-radius: 8px;
  background: rgba(16, 185, 129, 0.08); color: #059669; font-size: 13px;
  display: inline-flex; align-items: center; gap: 6px;
}
.task-card-actions { display: flex; align-items: center; flex-shrink: 0; }
.done-btn {
  background: linear-gradient(135deg, var(--theme-primary), var(--theme-accent)) !important;
  border: none !important;
  box-shadow: 0 4px 12px rgba(206, 74, 170, 0.25);
}
.done-btn:hover { box-shadow: 0 6px 18px rgba(206, 74, 170, 0.35); }
.pagination { margin-top: 16px; display: flex; justify-content: center; }

@media (max-width: 768px) {
  .dashboard-grid { grid-template-columns: 1fr; }
  .page-hero-title { font-size: 28px; }
  .page-hero { padding: var(--space-8) var(--space-5); }
}
</style>
