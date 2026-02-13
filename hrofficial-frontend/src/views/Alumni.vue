<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import AnimatedCounter from '@/components/AnimatedCounter.vue'
import { ref, computed, onMounted } from 'vue'
import { getAlumni } from '@/api/user'
import { ElMessage } from 'element-plus'
import { User, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'

interface Member {
  id: number
  name: string
  role: string
  leaveYear: number
}

const alumni = ref<Member[]>([])
const loading = ref(false)
const currentYearIndex = ref(0)
const isTransitioning = ref(false)

const fetchAlumni = async () => {
  loading.value = true
  try {
    const response = await getAlumni()
    if (response.code === 200) {
      const alumniResponses = response.data || []
      if (Array.isArray(alumniResponses)) {
        let idCounter = 0
        alumni.value = alumniResponses.flatMap((yearGroup: any) => 
          yearGroup.members.map((member: any) => {
            const cleanName = (member.name || '').replace(/[[\]"']/g, '').trim()
            const cleanRole = (member.role || '部员').replace(/[[\]"']/g, '').trim()
            return {
              id: ++idCounter,
              name: cleanName,
              role: cleanRole,
              leaveYear: yearGroup.year
            }
          })
        ) as Member[]
      }
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取成员列表失败')
  } finally {
    loading.value = false
  }
}

const availableYears = computed(() => {
  return [...new Set(alumni.value.map(a => a.leaveYear))].sort((a, b) => b - a)
})

const currentYear = computed(() => {
  return availableYears.value[currentYearIndex.value] || availableYears.value[0]
})

const currentMembers = computed(() => {
  return alumni.value.filter(a => a.leaveYear === currentYear.value)
})

// 按身份分类成员
const ministers = computed(() => currentMembers.value.filter(m => m.role === '部长'))
const viceMinisters = computed(() => currentMembers.value.filter(m => m.role === '副部长'))
const members = computed(() => currentMembers.value.filter(m => m.role === '部员'))

// 切换年份
const switchYear = (index: number) => {
  if (index === currentYearIndex.value || isTransitioning.value) return
  if (index >= 0 && index < availableYears.value.length) {
    isTransitioning.value = true
    currentYearIndex.value = index
    setTimeout(() => {
      isTransitioning.value = false
    }, 400)
  }
}

const nextYear = () => {
  const next = (currentYearIndex.value + 1) % availableYears.value.length
  switchYear(next)
}

const prevYear = () => {
  const prev = (currentYearIndex.value - 1 + availableYears.value.length) % availableYears.value.length
  switchYear(prev)
}

// 计算圆形布局位置
const getCircularPosition = (index: number, total: number, radius: number) => {
  if (total === 0) return { x: 0, y: 0 }
  const angle = (index / total) * 2 * Math.PI - Math.PI / 2
  return {
    x: Math.cos(angle) * radius,
    y: Math.sin(angle) * radius
  }
}

const getAvatarColor = (name: string, role: string) => {
  if (role === '部长') {
    const colors = ['#FF6B4A', '#E85A3C', '#FF8A70']
    return colors[name.charCodeAt(0) % colors.length]
  }
  if (role === '副部长') {
    const colors = ['#F59E0B', '#D97706', '#FBBF24']
    return colors[name.charCodeAt(0) % colors.length]
  }
  const colors = ['#3B82F6', '#6366F1', '#8B5CF6', '#EC4899', '#14B8A6']
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return colors[Math.abs(hash) % colors.length]
}

const stats = computed(() => ({
  total: alumni.value.length,
  years: availableYears.value.length,
  currentYearMembers: currentMembers.value.length
}))

onMounted(() => {
  fetchAlumni()
})
</script>

<template>
  <Layout>
    <div class="alumni-page">
      <!-- 顶部标题区域 -->
      <header class="page-header">
        <div class="header-content">
          <div class="header-left">
            <div class="header-badge">
              <el-icon><User /></el-icon>
              <span>往届成员</span>
            </div>
            <h1 class="header-title">成员星河</h1>
            <p class="header-subtitle">致敬每一位为人力资源中心付出过的成员</p>
          </div>
          
          <div class="header-right">
            <div class="stats-grid">
              <div class="stat-card">
                <span class="stat-value"><AnimatedCounter :value="stats.total" /></span>
                <span class="stat-label">位成员</span>
              </div>
              <div class="stat-card">
                <span class="stat-value"><AnimatedCounter :value="stats.years" /></span>
                <span class="stat-label">届传承</span>
              </div>
              <div class="stat-card highlight">
                <span class="stat-value"><AnimatedCounter :value="stats.currentYearMembers" /></span>
                <span class="stat-label">本届成员</span>
              </div>
            </div>
          </div>
        </div>
      </header>

      <!-- 主内容区域 -->
      <main class="main-content">
        <!-- 左侧控制面板 -->
        <aside class="control-panel">
          <!-- 年份选择器 -->
          <div class="year-selector">
            <h3 class="panel-title">选择届数</h3>
            <div class="year-list">
              <button
                v-for="(year, index) in availableYears"
                :key="year"
                class="year-btn"
                :class="{ active: currentYearIndex === index }"
                @click="switchYear(index)"
              >
                <span class="year-num">{{ year }}</span>
                <span class="year-suffix">届</span>
                <div v-if="currentYearIndex === index" class="active-indicator"></div>
              </button>
            </div>
          </div>

          <!-- 图例 -->
          <div class="legend-panel">
            <h3 class="panel-title">成员类型</h3>
            <div class="legend-list">
              <div class="legend-item">
                <div class="legend-dot minister"></div>
                <span class="legend-text">部长</span>
                <span class="legend-count">{{ ministers.length }}人</span>
              </div>
              <div class="legend-item">
                <div class="legend-dot vice"></div>
                <span class="legend-text">副部长</span>
                <span class="legend-count">{{ viceMinisters.length }}人</span>
              </div>
              <div class="legend-item">
                <div class="legend-dot member"></div>
                <span class="legend-text">部员</span>
                <span class="legend-count">{{ members.length }}人</span>
              </div>
            </div>
          </div>
        </aside>

        <!-- 右侧展示区域 -->
        <section class="display-area">
          <!-- 当前届数标题 -->
          <div class="current-year-header">
            <button class="nav-arrow" @click="prevYear" :disabled="availableYears.length <= 1">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <div class="year-display">
              <span class="year-number">{{ currentYear }}</span>
              <span class="year-text">届成员</span>
            </div>
            <button class="nav-arrow" @click="nextYear" :disabled="availableYears.length <= 1">
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>

          <!-- 成员星河图 -->
          <div class="galaxy-container" :class="{ 'is-transitioning': isTransitioning }">
            <!-- 轨道环 -->
            <div class="orbit-ring ring-1"></div>
            <div class="orbit-ring ring-2"></div>
            <div class="orbit-ring ring-3"></div>

            <!-- 中心 -->
            <div class="galaxy-center">
              <div class="center-core">
                <span class="core-text">HR</span>
              </div>
            </div>

            <!-- 部长 - 内圈 -->
            <div
              v-for="(member, index) in ministers"
              :key="'m-' + member.id"
              class="member-node minister-node"
              :style="{
                '--delay': `${index * 0.1}s`,
                transform: `translate(${getCircularPosition(index, ministers.length || 1, 90).x}px, ${getCircularPosition(index, ministers.length || 1, 90).y}px)`
              }"
            >
              <div class="node-glow" :style="{ background: getAvatarColor(member.name, member.role) }"></div>
              <div class="node-content" :style="{ background: getAvatarColor(member.name, member.role) }">
                <span class="node-name">{{ member.name }}</span>
                <span class="node-role">{{ member.role }}</span>
              </div>
            </div>

            <!-- 副部长 - 中圈 -->
            <div
              v-for="(member, index) in viceMinisters"
              :key="'v-' + member.id"
              class="member-node vice-node"
              :style="{
                '--delay': `${index * 0.08}s`,
                transform: `translate(${getCircularPosition(index, viceMinisters.length || 1, 170).x}px, ${getCircularPosition(index, viceMinisters.length || 1, 170).y}px)`
              }"
            >
              <div class="node-glow" :style="{ background: getAvatarColor(member.name, member.role) }"></div>
              <div class="node-content" :style="{ background: getAvatarColor(member.name, member.role) }">
                <span class="node-name">{{ member.name }}</span>
                <span class="node-role">{{ member.role }}</span>
              </div>
            </div>

            <!-- 部员 - 外圈 -->
            <div
              v-for="(member, index) in members"
              :key="'mem-' + member.id"
              class="member-node member-circle"
              :style="{
                '--delay': `${index * 0.05}s`,
                transform: `translate(${getCircularPosition(index, members.length || 1, 260).x}px, ${getCircularPosition(index, members.length || 1, 260).y}px)`
              }"
            >
              <div class="node-glow" :style="{ background: getAvatarColor(member.name, member.role) }"></div>
              <div class="node-content" :style="{ background: getAvatarColor(member.name, member.role) }">
                <span class="node-name">{{ member.name }}</span>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="currentMembers.length === 0" class="empty-state">
              <el-icon :size="48" color="#D6D3D1"><User /></el-icon>
              <p>该届暂无成员数据</p>
            </div>
          </div>
        </section>
      </main>
    </div>
  </Layout>
</template>

<style scoped>
.alumni-page {
  min-height: calc(100vh - 64px);
  background: linear-gradient(135deg, #FFFBF7 0%, #FFF5F0 50%, #FFEDE5 100%);
  display: flex;
  flex-direction: column;
}

/* 顶部标题区域 */
.page-header {
  background: white;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 24px 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.header-content {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 32px;
}

.header-left {
  flex: 1;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: rgba(255, 107, 74, 0.1);
  border-radius: 20px;
  color: #FF6B4A;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 12px;
}

.header-title {
  font-size: 32px;
  font-weight: 800;
  color: #1C1917;
  margin-bottom: 6px;
  letter-spacing: -0.02em;
}

.header-subtitle {
  font-size: 14px;
  color: #78716C;
  font-weight: 400;
}

.header-right {
  flex-shrink: 0;
}

.stats-grid {
  display: flex;
  gap: 16px;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px 24px;
  background: #F5F5F4;
  border-radius: 12px;
  min-width: 80px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  background: #E7E5E4;
  transform: translateY(-2px);
}

.stat-card.highlight {
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  color: white;
}

.stat-card.highlight .stat-label {
  color: rgba(255, 255, 255, 0.85);
}

.stat-value {
  font-size: 28px;
  font-weight: 800;
  color: #FF6B4A;
  line-height: 1;
}

.stat-card.highlight .stat-value {
  color: white;
}

.stat-label {
  font-size: 12px;
  color: #78716C;
  margin-top: 4px;
  font-weight: 500;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  padding: 24px 32px;
  gap: 24px;
}

/* 左侧控制面板 */
.control-panel {
  width: 240px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #44403C;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid #FF6B4A;
  display: inline-block;
}

/* 年份选择器 */
.year-selector {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.year-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.year-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  border: 2px solid transparent;
  background: #F5F5F4;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.25s ease;
  position: relative;
  overflow: hidden;
}

.year-btn:hover {
  background: #E7E5E4;
  transform: translateX(4px);
}

.year-btn.active {
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border-color: transparent;
}

.year-num {
  font-size: 18px;
  font-weight: 700;
  color: #44403C;
}

.year-btn.active .year-num {
  color: white;
}

.year-suffix {
  font-size: 13px;
  color: #A8A29E;
  font-weight: 500;
}

.year-btn.active .year-suffix {
  color: rgba(255, 255, 255, 0.85);
}

.active-indicator {
  position: absolute;
  right: 12px;
  width: 8px;
  height: 8px;
  background: white;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.3);
}

/* 图例面板 */
.legend-panel {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.legend-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #FAFAF9;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.legend-item:hover {
  background: #F5F5F4;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-dot.minister {
  background: #FF6B4A;
  box-shadow: 0 0 0 3px rgba(255, 107, 74, 0.2);
}

.legend-dot.vice {
  background: #F59E0B;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.2);
}

.legend-dot.member {
  background: #3B82F6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.legend-text {
  font-size: 14px;
  color: #44403C;
  font-weight: 500;
  flex: 1;
}

.legend-count {
  font-size: 12px;
  color: #A8A29E;
  font-weight: 600;
  background: white;
  padding: 2px 8px;
  border-radius: 10px;
}

/* 右侧展示区域 */
.display-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

/* 当前届数标题 */
.current-year-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.nav-arrow {
  width: 40px;
  height: 40px;
  border: 2px solid #E7E5E4;
  background: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #78716C;
  transition: all 0.2s ease;
  font-size: 18px;
}

.nav-arrow:hover:not(:disabled) {
  border-color: #FF6B4A;
  color: #FF6B4A;
  transform: scale(1.1);
}

.nav-arrow:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.year-display {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 12px 32px;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border-radius: 30px;
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.3);
}

.year-number {
  font-size: 32px;
  font-weight: 800;
  color: white;
  line-height: 1;
}

.year-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.9);
  font-weight: 500;
}

/* 成员星河图 */
.galaxy-container {
  position: relative;
  width: 600px;
  height: 600px;
  flex-shrink: 0;
}

.orbit-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 1px dashed rgba(0, 0, 0, 0.08);
}

.ring-1 {
  width: 180px;
  height: 180px;
}

.ring-2 {
  width: 340px;
  height: 340px;
}

.ring-3 {
  width: 520px;
  height: 520px;
}

.galaxy-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 50;
}

.center-core {
  width: 70px;
  height: 70px;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 
    0 0 0 4px rgba(255, 107, 74, 0.15),
    0 0 0 8px rgba(255, 107, 74, 0.08),
    0 12px 40px rgba(255, 107, 74, 0.35);
}

.core-text {
  font-size: 20px;
  font-weight: 900;
  color: white;
  letter-spacing: 1px;
}

/* 成员节点 */
.member-node {
  position: absolute;
  top: 50%;
  left: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  animation: nodeEnter 0.5s ease backwards;
  animation-delay: var(--delay, 0s);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.galaxy-container.is-transitioning .member-node {
  opacity: 0;
  transform: scale(0.5) !important;
}

@keyframes nodeEnter {
  from {
    opacity: 0;
    transform: translate(0, 0) scale(0.5);
  }
}

.node-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(12px);
  opacity: 0.4;
  transition: all 0.3s ease;
}

.node-content {
  position: relative;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.18);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 2;
}

.member-node:hover .node-content {
  transform: scale(1.15);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.25);
}

.member-node:hover .node-glow {
  opacity: 0.7;
  filter: blur(20px);
}

.node-name {
  font-weight: 700;
  text-align: center;
  line-height: 1.2;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
}

.node-role {
  font-size: 10px;
  opacity: 0.95;
  margin-top: 2px;
  font-weight: 500;
}

/* 不同角色的节点尺寸 */
.minister-node {
  margin-left: -45px;
  margin-top: -45px;
}

.minister-node .node-content {
  width: 90px;
  height: 90px;
}

.minister-node .node-glow {
  width: 90px;
  height: 90px;
}

.minister-node .node-name {
  font-size: 16px;
}

.vice-node {
  margin-left: -36px;
  margin-top: -36px;
}

.vice-node .node-content {
  width: 72px;
  height: 72px;
}

.vice-node .node-glow {
  width: 72px;
  height: 72px;
}

.vice-node .node-name {
  font-size: 13px;
}

.member-circle {
  margin-left: -30px;
  margin-top: -30px;
}

.member-circle .node-content {
  width: 60px;
  height: 60px;
}

.member-circle .node-glow {
  width: 60px;
  height: 60px;
}

.member-circle .node-name {
  font-size: 12px;
}

/* 空状态 */
.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #A8A29E;
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }

  .control-panel {
    width: 100%;
    flex-direction: row;
    gap: 16px;
  }

  .year-selector,
  .legend-panel {
    flex: 1;
  }

  .year-list {
    flex-direction: row;
    flex-wrap: wrap;
  }

  .year-btn {
    flex: 1;
    min-width: 100px;
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 20px 24px;
  }

  .header-content {
    flex-direction: column;
    gap: 20px;
    text-align: center;
  }

  .header-title {
    font-size: 26px;
  }

  .stats-grid {
    justify-content: center;
  }

  .stat-card {
    padding: 12px 18px;
    min-width: 70px;
  }

  .stat-value {
    font-size: 22px;
  }

  .main-content {
    padding: 16px 20px;
  }

  .control-panel {
    flex-direction: column;
  }

  .galaxy-container {
    width: 400px;
    height: 400px;
  }

  .orbit-ring.ring-1 {
    width: 120px;
    height: 120px;
  }

  .orbit-ring.ring-2 {
    width: 220px;
    height: 220px;
  }

  .orbit-ring.ring-3 {
    width: 340px;
    height: 340px;
  }

  .minister-node {
    margin-left: -32px;
    margin-top: -32px;
  }

  .minister-node .node-content {
    width: 64px;
    height: 64px;
  }

  .minister-node .node-glow {
    width: 64px;
    height: 64px;
  }

  .minister-node .node-name {
    font-size: 13px;
  }

  .vice-node {
    margin-left: -26px;
    margin-top: -26px;
  }

  .vice-node .node-content {
    width: 52px;
    height: 52px;
  }

  .vice-node .node-glow {
    width: 52px;
    height: 52px;
  }

  .vice-node .node-name {
    font-size: 11px;
  }

  .member-circle {
    margin-left: -22px;
    margin-top: -22px;
  }

  .member-circle .node-content {
    width: 44px;
    height: 44px;
  }

  .member-circle .node-glow {
    width: 44px;
    height: 44px;
  }

  .member-circle .node-name {
    font-size: 10px;
  }

  .center-core {
    width: 50px;
    height: 50px;
  }

  .core-text {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .galaxy-container {
    width: 320px;
    height: 320px;
  }

  .orbit-ring.ring-1 {
    width: 100px;
    height: 100px;
  }

  .orbit-ring.ring-2 {
    width: 180px;
    height: 180px;
  }

  .orbit-ring.ring-3 {
    width: 280px;
    height: 280px;
  }

  .year-display {
    padding: 10px 24px;
  }

  .year-number {
    font-size: 24px;
  }

  .nav-arrow {
    width: 36px;
    height: 36px;
  }
}
</style>
