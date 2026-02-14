<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import AnimatedCounter from '@/components/AnimatedCounter.vue'
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { getAlumni } from '@/api/user'
import { ElMessage } from 'element-plus'
import { User, ArrowLeft, ArrowRight, StarFilled, Medal } from '@element-plus/icons-vue'

interface Member {
  id: number
  name: string
  role: string
  leaveYear: number
}

interface OrbitNode {
  member: Member
  angle: number
  speed: number
  orbitA: number
  orbitB: number
  reverse: boolean
}

const alumni = ref<Member[]>([])
const loading = ref(false)
const currentYearIndex = ref(0)
const isTransitioning = ref(false)
const yearUniqueCounts = ref<Map<number, number>>(new Map())
const totalUniqueCount = ref(0)

const fetchAlumni = async () => {
  loading.value = true
  try {
    const response = await getAlumni()
    if (response.code === 200) {
      const alumniResponses = response.data || []
      if (Array.isArray(alumniResponses)) {
        let idCounter = 0
        const newYearUniqueCounts = new Map<number, number>()
        const allUniqueNames = new Set<string>()
        
        const allMembers = alumniResponses.flatMap((yearGroup: any) => {
          const year = yearGroup.year
          const uniqueCount = yearGroup.uniqueMemberCount || yearGroup.members?.length || 0
          newYearUniqueCounts.set(year, uniqueCount)
          
          return (yearGroup.members || []).map((member: any) => {
            const cleanName = (member.name || '').replace(/[[\]"']/g, '').trim()
            const cleanRole = (member.role || '部员').replace(/[[\]"']/g, '').trim()
            allUniqueNames.add(cleanName)
            return {
              id: ++idCounter,
              name: cleanName,
              role: cleanRole,
              leaveYear: year
            }
          })
        }) as Member[]
        
        alumni.value = allMembers
        yearUniqueCounts.value = newYearUniqueCounts
        totalUniqueCount.value = allUniqueNames.size
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

const ministers = computed(() => currentMembers.value.filter(m => m.role === '部长'))
const viceMinisters = computed(() => currentMembers.value.filter(m => m.role === '副部长'))
const members = computed(() => currentMembers.value.filter(m => m.role === '部员'))

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

const orbitNodes = ref<OrbitNode[]>([])
let animationFrameId: number | null = null

const initOrbitNodes = () => {
  orbitNodes.value = []
  
  const ministerList = ministers.value
  const viceList = viceMinisters.value
  const memberList = members.value
  
  ministerList.forEach((member, index) => {
    const total = ministerList.length || 1
    const startAngle = (index / total) * Math.PI * 2
    orbitNodes.value.push({
      member,
      angle: startAngle,
      speed: 0.0003,
      orbitA: 240,
      orbitB: 130,
      reverse: false
    })
  })
  
  viceList.forEach((member, index) => {
    const total = viceList.length || 1
    const startAngle = (index / total) * Math.PI * 2
    orbitNodes.value.push({
      member,
      angle: startAngle,
      speed: 0.0002,
      orbitA: 350,
      orbitB: 190,
      reverse: true
    })
  })
  
  memberList.forEach((member, index) => {
    const total = memberList.length || 1
    const startAngle = (index / total) * Math.PI * 2
    orbitNodes.value.push({
      member,
      angle: startAngle,
      speed: 0.0001,
      orbitA: 480,
      orbitB: 260,
      reverse: false
    })
  })
}

const nodePositions = ref<Record<number, { x: number; y: number }>>({})

const animate = () => {
  const newPositions: Record<number, { x: number; y: number }> = {}
  
  orbitNodes.value.forEach(node => {
    if (node.reverse) {
      node.angle -= node.speed * 16.67
    } else {
      node.angle += node.speed * 16.67
    }
    
    const x = node.orbitA * Math.cos(node.angle)
    const y = node.orbitB * Math.sin(node.angle)
    
    newPositions[node.member.id] = { x, y }
  })
  
  nodePositions.value = newPositions
  animationFrameId = requestAnimationFrame(animate)
}

const getNodeStyle = (memberId: number) => {
  const pos = nodePositions.value[memberId]
  if (!pos) return {}
  return {
    transform: `translate(calc(-50% + ${pos.x}px), calc(-50% + ${pos.y}px))`
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
  total: totalUniqueCount.value,
  years: availableYears.value.length,
  currentYearMembers: yearUniqueCounts.value.get(currentYear.value) || currentMembers.value.length
}))

watch(currentYear, () => {
  nextTick(() => {
    initOrbitNodes()
  })
})

onMounted(() => {
  fetchAlumni()
})

onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
})

watch([ministers, viceMinisters, members], () => {
  initOrbitNodes()
  if (!animationFrameId && orbitNodes.value.length > 0) {
    animationFrameId = requestAnimationFrame(animate)
  }
}, { immediate: true })
</script>

<template>
  <Layout>
    <div class="alumni-page">
      <!-- Hero 标题区域 - 星云风格 -->
      <section class="hero-section">
        <!-- 有机曲线背景 -->
        <div class="hero-background">
          <div class="blob blob-1"></div>
          <div class="blob blob-2"></div>
          <div class="blob blob-3"></div>
          <div class="grid-pattern"></div>
        </div>

        <!-- 浮动装饰元素 -->
        <div class="floating-elements">
          <div class="float-item float-1">
            <el-icon :size="20"><StarFilled /></el-icon>
          </div>
          <div class="float-item float-2">
            <el-icon :size="18"><User /></el-icon>
          </div>
          <div class="float-item float-3">
            <el-icon :size="16"><Medal /></el-icon>
          </div>
        </div>

        <div class="hero-content">
          <!-- 徽章 -->
          <div class="hero-badge">
            <el-icon><User /></el-icon>
            <span>往届成员</span>
          </div>

          <!-- 主标题 -->
          <h1 class="hero-title">
            <span class="title-highlight">成员星河</span>
          </h1>

          <!-- 副标题 -->
          <p class="hero-subtitle">
            致敬每一位为人力资源中心付出过的成员
          </p>

          <!-- 统计数据 -->
          <div class="hero-stats">
            <div class="stat-item">
              <span class="stat-value"><AnimatedCounter :value="stats.total" /></span>
              <span class="stat-label">位成员</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <span class="stat-value"><AnimatedCounter :value="stats.years" /></span>
              <span class="stat-label">届传承</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item highlight">
              <span class="stat-value"><AnimatedCounter :value="stats.currentYearMembers" /></span>
              <span class="stat-label">本届成员</span>
            </div>
          </div>
        </div>
      </section>

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
            <!-- 轨道环 - 静止不动 -->
            <div class="orbit-ring ring-1"></div>
            <div class="orbit-ring ring-2"></div>
            <div class="orbit-ring ring-3"></div>

            <!-- 中心 -->
            <div class="galaxy-center">
              <div class="center-core">
                <span class="core-text">HR</span>
              </div>
            </div>

            <!-- 部长 - 内圈轨道 -->
            <div
              v-for="member in ministers"
              :key="'m-' + member.id"
              class="member-node minister-node"
              :style="getNodeStyle(member.id)"
            >
              <div class="node-content">
                <span class="node-name">{{ member.name }}</span>
              </div>
            </div>

            <!-- 副部长 - 中圈轨道 -->
            <div
              v-for="member in viceMinisters"
              :key="'v-' + member.id"
              class="member-node vice-node"
              :style="getNodeStyle(member.id)"
            >
              <div class="node-content">
                <span class="node-name">{{ member.name }}</span>
              </div>
            </div>

            <!-- 部员 - 外圈轨道 -->
            <div
              v-for="member in members"
              :key="'mem-' + member.id"
              class="member-node member-circle"
              :style="getNodeStyle(member.id)"
            >
              <div class="node-content">
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
/* ============================================
   成员星河页面 - 星云毛玻璃风格
   Galaxy Glassmorphism Design
   ============================================
   
   设计理念：
   - 深空星云氛围，暖色调光晕
   - 毛玻璃层次感，保持通透性
   - 有机曲线装饰，流动感背景
   - 与整体暖色系统协调统一
   ============================================ */

.alumni-page {
  min-height: calc(100vh - 64px);
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(135deg, #FFFBF7 0%, #FFF5F0 50%, #FFEDE6 100%);
}

/* ============================================
   Hero Section - 星云风格标题区
   ============================================
   
   参考首页Hero设计，融入星云元素：
   - 有机曲线Blob背景
   - 网格点状装饰
   - 浮动装饰元素
   - 居中内容布局
   ============================================ */

.hero-section {
  position: relative;
  padding: var(--space-12, 48px) var(--space-8, 32px);
  margin: var(--space-6, 24px);
  margin-bottom: 0;
  border-radius: 32px;
  overflow: hidden;
  background: linear-gradient(135deg,
    rgba(255, 107, 74, 0.08) 0%,
    rgba(245, 158, 11, 0.08) 50%,
    rgba(227, 85, 50, 0.08) 100%
  );
}

/* 有机曲线背景 */
.hero-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.6;
  animation: blobFloat 20s ease-in-out infinite;
}

.blob-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #FF6B4A, #F59E0B);
  top: -100px;
  right: -100px;
  animation-delay: 0s;
}

.blob-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #F59E0B, #E35532);
  bottom: -50px;
  left: -50px;
  animation-delay: -7s;
}

.blob-3 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, #FB7185, #FF6B4A);
  top: 50%;
  left: 30%;
  animation-delay: -14s;
  opacity: 0.4;
}

/* 网格点状背景 */
.grid-pattern {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(circle at 1px 1px, rgba(255, 107, 74, 0.15) 1px, transparent 0);
  background-size: 40px 40px;
}

/* 浮动装饰元素 */
.floating-elements {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.float-item {
  position: absolute;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border-radius: 16px;
  font-size: 32px;
  color: var(--coral-500, #FF6B4A);
  animation: float 6s ease-in-out infinite;
}

.float-1 {
  top: 15%;
  left: 10%;
  animation-delay: 0s;
}

.float-2 {
  top: 25%;
  right: 15%;
  animation-delay: -2s;
}

.float-3 {
  bottom: 20%;
  left: 15%;
  animation-delay: -4s;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-15px) rotate(5deg);
  }
}

@keyframes blobFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -20px) scale(1.05);
  }
  66% {
    transform: translate(-20px, 15px) scale(0.95);
  }
}

/* Hero 内容 */
.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 8px);
  padding: var(--space-2, 8px) var(--space-4, 16px);
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  color: white;
  border-radius: 100px;
  font-size: var(--text-sm, 14px);
  font-weight: 500;
  margin-bottom: var(--space-6, 24px);
  box-shadow: 
    0 4px 16px rgba(255, 107, 74, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  animation: fadeInDown 0.6s ease;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-title {
  font-size: var(--text-5xl, 48px);
  font-weight: 800;
  line-height: 1.2;
  margin: 0 0 var(--space-4, 16px) 0;
  animation: fadeInUp 0.6s ease 0.1s both;
}

.title-highlight {
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.hero-subtitle {
  font-size: var(--text-lg, 18px);
  color: var(--text-secondary, #78716C);
  line-height: 1.8;
  max-width: 500px;
  margin: 0 auto var(--space-8, 32px);
  animation: fadeInUp 0.6s ease 0.2s both;
}

/* 统计数据 */
.hero-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-6, 24px);
  animation: fadeInUp 0.6s ease 0.3s both;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--space-4, 16px) var(--space-6, 24px);
  background: rgba(255, 255, 255, 0.80);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  min-width: 100px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.stat-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.12);
}

.stat-item.highlight {
  background: linear-gradient(135deg, 
    rgba(255, 107, 74, 0.95) 0%, 
    rgba(227, 85, 50, 0.95) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  box-shadow: 
    0 8px 32px rgba(255, 107, 74, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.stat-item .stat-value {
  font-size: 28px;
  font-weight: 800;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1;
}

.stat-item.highlight .stat-value {
  background: white;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-item .stat-label {
  font-size: 12px;
  color: var(--text-tertiary, #A8A29E);
  margin-top: 6px;
  font-weight: 500;
}

.stat-item.highlight .stat-label {
  color: rgba(255, 255, 255, 0.90);
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: linear-gradient(180deg, transparent, rgba(255, 107, 74, 0.3), transparent);
}

/* 主内容区域 */
.main-content {
  position: relative;
  z-index: 5;
  flex: 1;
  display: flex;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  padding: var(--space-6, 24px) var(--space-8, 32px);
  gap: var(--space-6, 24px);
}

/* 左侧控制面板 - 毛玻璃效果 */
.control-panel {
  width: 260px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ============================================
   面板标题 - 毛玻璃标签风格
   ============================================ */
.panel-title {
  font-size: 13px;
  font-weight: 700;
  color: #1C1917;
  margin-bottom: 16px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 107, 74, 0.2);
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 2px 8px rgba(255, 107, 74, 0.1);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.panel-title::before {
  content: '';
  width: 4px;
  height: 16px;
  background: linear-gradient(180deg, #FF6B4A, #F59E0B);
  border-radius: 2px;
}

/* 年份选择器 - 毛玻璃卡片 */
.year-selector {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 
    0 8px 32px rgba(255, 107, 74, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.year-selector:hover {
  box-shadow: 
    0 12px 40px rgba(255, 107, 74, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.year-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.year-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 14px 18px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;
}

.year-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, 
    rgba(255, 107, 74, 0.08) 0%, 
    rgba(245, 158, 11, 0.05) 100%
  );
  opacity: 0;
  transition: opacity 0.3s ease;
}

.year-btn:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateX(6px);
  box-shadow: 0 4px 16px rgba(255, 107, 74, 0.1);
}

.year-btn:hover::before {
  opacity: 1;
}

.year-btn.active {
  background: linear-gradient(135deg, 
    rgba(255, 107, 74, 0.95) 0%, 
    rgba(227, 85, 50, 0.95) 100%
  );
  border-color: transparent;
  box-shadow: 
    0 6px 24px rgba(255, 107, 74, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
}

.year-num {
  font-size: 18px;
  font-weight: 700;
  color: #44403C;
  position: relative;
  z-index: 1;
}

.year-btn.active .year-num {
  color: white;
}

.year-suffix {
  font-size: 13px;
  color: #A8A29E;
  font-weight: 500;
  position: relative;
  z-index: 1;
}

.year-btn.active .year-suffix {
  color: rgba(255, 255, 255, 0.90);
}

.active-indicator {
  position: absolute;
  right: 14px;
  width: 8px;
  height: 8px;
  background: white;
  border-radius: 50%;
  box-shadow: 
    0 0 0 3px rgba(255, 255, 255, 0.3),
    0 0 12px rgba(255, 255, 255, 0.5);
  animation: indicatorPulse 2s ease-in-out infinite;
}

@keyframes indicatorPulse {
  0%, 100% {
    box-shadow: 
      0 0 0 3px rgba(255, 255, 255, 0.3),
      0 0 12px rgba(255, 255, 255, 0.5);
  }
  50% {
    box-shadow: 
      0 0 0 5px rgba(255, 255, 255, 0.2),
      0 0 20px rgba(255, 255, 255, 0.6);
  }
}

/* 图例面板 - 毛玻璃卡片 */
.legend-panel {
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 20px;
  padding: 20px;
  box-shadow: 
    0 8px 32px rgba(255, 107, 74, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  transition: all 0.3s ease;
}

.legend-panel:hover {
  box-shadow: 
    0 12px 40px rgba(255, 107, 74, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.legend-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 12px;
  transition: all 0.25s ease;
  border: 1px solid transparent;
}

.legend-item:hover {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(255, 107, 74, 0.1);
  transform: translateX(4px);
}

.legend-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  flex-shrink: 0;
  position: relative;
}

.legend-dot::after {
  content: '';
  position: absolute;
  inset: -3px;
  border-radius: 50%;
  opacity: 0.3;
}

.legend-dot.minister {
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  box-shadow: 0 2px 8px rgba(255, 107, 74, 0.4);
}

.legend-dot.minister::after {
  background: radial-gradient(circle, rgba(255, 107, 74, 0.4), transparent);
}

.legend-dot.vice {
  background: linear-gradient(135deg, #F59E0B, #D97706);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
}

.legend-dot.vice::after {
  background: radial-gradient(circle, rgba(245, 158, 11, 0.4), transparent);
}

.legend-dot.member {
  background: linear-gradient(135deg, #3B82F6, #2563EB);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
}

.legend-dot.member::after {
  background: radial-gradient(circle, rgba(59, 130, 246, 0.4), transparent);
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
  background: rgba(255, 255, 255, 0.8);
  padding: 4px 10px;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

/* 右侧展示区域 - 毛玻璃容器 */
.display-area {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: rgba(255, 255, 255, 0.70);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  border-radius: 28px;
  padding: 28px;
  box-shadow: 
    0 12px 48px rgba(255, 107, 74, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

/* 当前届数标题 */
.current-year-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 24px;
}

.nav-arrow {
  width: 44px;
  height: 44px;
  border: 1px solid rgba(255, 107, 74, 0.2);
  background: rgba(255, 255, 255, 0.80);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #78716C;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
}

.nav-arrow:hover:not(:disabled) {
  border-color: rgba(255, 107, 74, 0.4);
  background: rgba(255, 255, 255, 0.95);
  color: #FF6B4A;
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(255, 107, 74, 0.15);
}

.nav-arrow:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  background: rgba(255, 255, 255, 0.5);
}

.year-display {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 14px 36px;
  background: linear-gradient(135deg, 
    rgba(255, 107, 74, 0.95) 0%, 
    rgba(227, 85, 50, 0.95) 100%
  );
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 32px;
  box-shadow: 
    0 8px 32px rgba(255, 107, 74, 0.35),
    inset 0 1px 0 rgba(255, 255, 255, 0.2);
  position: relative;
  overflow: hidden;
}

.year-display::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, 
    rgba(255, 255, 255, 0.1) 0%, 
    transparent 50%
  );
  pointer-events: none;
}

.year-number {
  font-size: 32px;
  font-weight: 800;
  color: white;
  line-height: 1;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.year-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.90);
  font-weight: 500;
}

/* ============================================
   成员星河图 - 全新椭圆轨道设计
   ============================================
   
   设计原理：
   1. 轨道环保持静止不动
   2. 每个成员节点独立沿椭圆轨道运动
   3. 节点内部反向旋转，保持文字始终水平
   ============================================ */

.galaxy-container {
  position: relative;
  width: 100%;
  max-width: 1000px;
  height: 560px;
  flex-shrink: 0;
}

/* 星河背景光晕 */
.galaxy-container::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 600px;
  height: 320px;
  background: radial-gradient(ellipse at center, 
    rgba(255, 107, 74, 0.1) 0%, 
    rgba(245, 158, 11, 0.05) 40%, 
    transparent 70%
  );
  border-radius: 50%;
  filter: blur(40px);
  pointer-events: none;
  animation: galaxyGlow 8s ease-in-out infinite;
}

/* 星点粒子背景 */
.galaxy-container::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: 
    radial-gradient(2px 2px at 20% 30%, rgba(255, 107, 74, 0.4), transparent),
    radial-gradient(2px 2px at 40% 70%, rgba(245, 158, 11, 0.3), transparent),
    radial-gradient(1px 1px at 60% 20%, rgba(255, 255, 255, 0.6), transparent),
    radial-gradient(2px 2px at 80% 50%, rgba(255, 107, 74, 0.3), transparent),
    radial-gradient(1px 1px at 10% 80%, rgba(245, 158, 11, 0.4), transparent),
    radial-gradient(1px 1px at 90% 10%, rgba(255, 255, 255, 0.5), transparent);
  background-size: 200px 200px;
  border-radius: 50%;
  animation: starTwinkle 4s ease-in-out infinite;
  pointer-events: none;
  opacity: 0.8;
}

@keyframes galaxyGlow {
  0%, 100% { opacity: 0.8; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 1; transform: translate(-50%, -50%) scale(1.05); }
}

@keyframes starTwinkle {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* 轨道环 - 静止不动 */
.orbit-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  border: 1px dashed;
  pointer-events: none;
}

.ring-1 {
  width: 480px;
  height: 260px;
  border-color: rgba(255, 107, 74, 0.25);
}

.ring-2 {
  width: 700px;
  height: 380px;
  border-color: rgba(245, 158, 11, 0.2);
}

.ring-3 {
  width: 960px;
  height: 520px;
  border-color: rgba(255, 107, 74, 0.12);
}

/* 中心核心 */
.galaxy-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 50;
}

.center-core {
  width: 120px;
  height: 120px;
  background: linear-gradient(135deg, #FF6B4A, #E35532);
  border: 4px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 
    0 0 0 8px rgba(255, 107, 74, 0.15),
    0 0 0 16px rgba(255, 107, 74, 0.08),
    0 0 60px rgba(255, 107, 74, 0.4),
    0 16px 48px rgba(255, 107, 74, 0.3);
  animation: corePulse 3s ease-in-out infinite;
}

@keyframes corePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.03); }
}

.core-text {
  font-size: 32px;
  font-weight: 900;
  color: white;
  letter-spacing: 3px;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

/* ============================================
   成员节点 - 椭圆轨道运动
   ============================================
   
   使用JavaScript + requestAnimationFrame实现精确椭圆轨道：
   - 位置由JS实时计算并应用transform
   - 节点始终保持水平方向
   - 平滑动画，性能优化
   ============================================ */

.member-node {
  position: absolute;
  top: 50%;
  left: 50%;
  cursor: pointer;
  will-change: transform;
  transition: opacity 0.3s ease;
}

/* 节点内容 */
/* ============================================
   成员节点样式 - 美化姓名标签
   ============================================ */

.node-content {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: white;
  box-shadow: 
    0 4px 20px rgba(0, 0, 0, 0.25),
    0 0 0 2px rgba(255, 255, 255, 0.15),
    inset 0 2px 4px rgba(255, 255, 255, 0.4),
    inset 0 -2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: none;
  position: relative;
  overflow: hidden;
}

/* 节点内部光泽效果 */
.node-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(180deg, 
    rgba(255, 255, 255, 0.3) 0%, 
    rgba(255, 255, 255, 0) 100%
  );
  border-radius: 50% 50% 0 0;
  pointer-events: none;
}

.member-node:hover .node-content {
  transform: scale(1.25);
  box-shadow: 
    0 8px 32px rgba(0, 0, 0, 0.3),
    0 0 0 3px rgba(255, 255, 255, 0.25),
    0 0 30px rgba(255, 107, 74, 0.4),
    inset 0 2px 4px rgba(255, 255, 255, 0.4);
}

/* 姓名标签样式 */
.node-name {
  font-weight: 700;
  text-align: center;
  line-height: 1.15;
  text-shadow: 
    0 1px 2px rgba(0, 0, 0, 0.3),
    0 0 8px rgba(0, 0, 0, 0.15);
  letter-spacing: 0.02em;
  position: relative;
  z-index: 1;
  padding: 2px;
}

/* 不同角色的节点尺寸与样式 */
/* 部长 - 最大，金色光晕 */
.minister-node .node-content {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #FF6B4A 0%, #E35532 50%, #C44224 100%);
  box-shadow: 
    0 6px 24px rgba(255, 107, 74, 0.4),
    0 0 0 3px rgba(255, 200, 150, 0.3),
    0 0 20px rgba(255, 107, 74, 0.3),
    inset 0 2px 4px rgba(255, 255, 255, 0.4);
}

.minister-node .node-name {
  font-size: 12px;
}

.minister-node:hover .node-content {
  box-shadow: 
    0 10px 40px rgba(255, 107, 74, 0.5),
    0 0 0 4px rgba(255, 200, 150, 0.4),
    0 0 40px rgba(255, 107, 74, 0.5),
    inset 0 2px 4px rgba(255, 255, 255, 0.4);
}

/* 副部长 - 中等，琥珀色光晕 */
.vice-node .node-content {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #F59E0B 0%, #D97706 50%, #B45309 100%);
  box-shadow: 
    0 5px 20px rgba(245, 158, 11, 0.35),
    0 0 0 2px rgba(255, 220, 150, 0.25),
    0 0 15px rgba(245, 158, 11, 0.25),
    inset 0 2px 4px rgba(255, 255, 255, 0.35);
}

.vice-node .node-name {
  font-size: 10px;
}

.vice-node:hover .node-content {
  box-shadow: 
    0 8px 32px rgba(245, 158, 11, 0.45),
    0 0 0 3px rgba(255, 220, 150, 0.35),
    0 0 30px rgba(245, 158, 11, 0.4),
    inset 0 2px 4px rgba(255, 255, 255, 0.35);
}

/* 部员 - 最小，蓝色光晕 */
.member-circle .node-content {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #3B82F6 0%, #2563EB 50%, #1D4ED8 100%);
  box-shadow: 
    0 4px 16px rgba(59, 130, 246, 0.3),
    0 0 0 2px rgba(150, 180, 255, 0.2),
    0 0 12px rgba(59, 130, 246, 0.2),
    inset 0 1px 3px rgba(255, 255, 255, 0.3);
}

.member-circle .node-name {
  font-size: 9px;
}

.member-circle:hover .node-content {
  box-shadow: 
    0 6px 24px rgba(59, 130, 246, 0.4),
    0 0 0 3px rgba(150, 180, 255, 0.3),
    0 0 25px rgba(59, 130, 246, 0.35),
    inset 0 1px 3px rgba(255, 255, 255, 0.3);
}

.galaxy-container.is-transitioning .member-node {
  opacity: 0;
}

/* 空状态 */
.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #A8A29E;
  padding: 40px;
  background: rgba(255, 255, 255, 0.60);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.5);
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
  /* Hero Section 响应式 */
  .hero-section {
    padding: var(--space-8, 32px) var(--space-4, 16px);
    margin: var(--space-4, 16px);
    border-radius: 24px;
  }

  .blob-1 {
    width: 250px;
    height: 250px;
  }

  .blob-2 {
    width: 180px;
    height: 180px;
  }

  .blob-3 {
    width: 150px;
    height: 150px;
  }

  .float-item {
    width: 36px;
    height: 36px;
  }

  .hero-title {
    font-size: 36px;
  }

  .hero-subtitle {
    font-size: 15px;
    max-width: 300px;
  }

  .hero-stats {
    flex-wrap: wrap;
    gap: var(--space-4, 16px);
  }

  .stat-item {
    padding: var(--space-3, 12px) var(--space-4, 16px);
    min-width: 80px;
  }

  .stat-item .stat-value {
    font-size: 22px;
  }

  .stat-divider {
    display: none;
  }

  /* 主内容响应式 */
  .main-content {
    padding: 16px 20px;
  }

  .control-panel {
    flex-direction: column;
  }

  /* 星河图响应式 */
  .galaxy-container {
    height: 420px;
  }

  .galaxy-container::before {
    width: 400px;
    height: 220px;
  }

  .orbit-ring.ring-1 {
    width: 320px;
    height: 175px;
  }

  .orbit-ring.ring-2 {
    width: 480px;
    height: 260px;
  }

  .orbit-ring.ring-3 {
    width: 640px;
    height: 350px;
  }

  .center-core {
    width: 90px;
    height: 90px;
  }

  .core-text {
    font-size: 24px;
  }

  .minister-node .node-content {
    width: 40px;
    height: 40px;
  }

  .minister-node .node-name {
    font-size: 10px;
  }

  .vice-node .node-content {
    width: 32px;
    height: 32px;
  }

  .vice-node .node-name {
    font-size: 9px;
  }

  .member-circle .node-content {
    width: 26px;
    height: 26px;
  }

  .member-circle .node-name {
    font-size: 8px;
  }
}

@media (max-width: 480px) {
  .hero-section {
    padding: var(--space-6, 24px) var(--space-3, 12px);
    margin: var(--space-3, 12px);
    border-radius: 20px;
  }

  .floating-elements {
    display: none;
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-subtitle {
    font-size: 14px;
  }

  .stat-item {
    min-width: 70px;
  }

  .stat-item .stat-value {
    font-size: 18px;
  }

  .display-area {
    padding: 16px;
    border-radius: 20px;
  }

  .galaxy-container {
    height: 320px;
  }

  .galaxy-container::before {
    width: 260px;
    height: 145px;
  }

  .galaxy-container::after {
    background-size: 150px 150px;
  }

  .orbit-ring.ring-1 {
    width: 200px;
    height: 110px;
  }

  .orbit-ring.ring-2 {
    width: 300px;
    height: 165px;
  }

  .orbit-ring.ring-3 {
    width: 400px;
    height: 220px;
  }

  .center-core {
    width: 60px;
    height: 60px;
  }

  .core-text {
    font-size: 18px;
  }

  .minister-node .node-content {
    width: 32px;
    height: 32px;
  }

  .minister-node .node-name {
    font-size: 9px;
  }

  .vice-node .node-content {
    width: 26px;
    height: 26px;
  }

  .vice-node .node-name {
    font-size: 8px;
  }

  .member-circle .node-content {
    width: 22px;
    height: 22px;
  }

  .member-circle .node-name {
    font-size: 7px;
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
