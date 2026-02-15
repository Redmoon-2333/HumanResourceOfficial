<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import AnimatedCounter from '@/components/AnimatedCounter.vue'
import OrganicBlob from '@/components/OrganicBlob.vue'
import SparkleEffect from '@/components/SparkleEffect.vue'
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
  
  // 根据屏幕宽度动态调整轨道半径
  const screenWidth = window.innerWidth
  let scaleFactor = 1
  if (screenWidth <= 480) {
    scaleFactor = 0.35 // 移动端缩小到 35%
  } else if (screenWidth <= 768) {
    scaleFactor = 0.6 // 平板端缩小到 60%
  } else if (screenWidth <= 1200) {
    scaleFactor = 0.8 // 中等屏幕缩小到 80%
  }
  
  ministerList.forEach((member, index) => {
    const total = ministerList.length || 1
    const startAngle = (index / total) * Math.PI * 2
    orbitNodes.value.push({
      member,
      angle: startAngle,
      speed: 0.0003,
      orbitA: 200 * scaleFactor,
      orbitB: 70 * scaleFactor,
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
      orbitA: 310 * scaleFactor,
      orbitB: 105 * scaleFactor,
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
      orbitA: 420 * scaleFactor,
      orbitB: 140 * scaleFactor,
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
    transform: `translate(-50%, -50%) translate(${pos.x}px, ${pos.y}px)`
  }
}

// 高对比度配色方案 - 突破主色系限制，增强视觉层次
// 设计理念：部长/副部长使用暖金/橙色系，部员使用全色谱增加活力
const ROLE_COLORS = {
  // 部长 - 尊贵金色系（高饱和度）
  minister: [
    '#F59E0B', // 琥珀金
    '#D97706', // 深琥珀
    '#B45309', // 棕金
    '#FBBF24', // 亮金黄
  ],
  // 副部长 - 活力橙红色系
  viceMinister: [
    '#F97316', // 活力橙
    '#EA580C', // 深橙
    '#DC2626', // 红橙
    '#FB923C', // 浅橙
  ],
  // 部员 - 全色谱高对比（突破暖色限制，增加视觉活力）
  member: [
    // 暖色系
    '#F59E0B', // 琥珀金
    '#F97316', // 活力橙
    '#EF4444', // 鲜红
    '#EC4899', // 粉红
    '#D946EF', // 紫红
    // 冷色系（对比色，增加层次）
    '#8B5CF6', // 紫色
    '#6366F1', // 靛蓝
    '#3B82F6', // 蓝色
    '#06B6D4', // 青色
    '#14B8A6', // 青绿
    // 中性色
    '#10B981', // 翠绿
    '#84CC16', // 黄绿
  ]
}

const getAvatarColor = (name: string, role: string) => {
  // 使用名字生成稳定的哈希值
  let hash = 0
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  const index = Math.abs(hash)

  if (role === '部长') {
    return ROLE_COLORS.minister[index % ROLE_COLORS.minister.length]
  }
  if (role === '副部长') {
    return ROLE_COLORS.viceMinister[index % ROLE_COLORS.viceMinister.length]
  }
  // 部员使用全色谱高对比颜色
  return ROLE_COLORS.member[index % ROLE_COLORS.member.length]
}

const stats = computed(() => ({
  total: totalUniqueCount.value,
  years: availableYears.value.length,
  currentYearMembers: yearUniqueCounts.value.get(currentYear.value || 0) || currentMembers.value.length
}))

watch(currentYear, () => {
  nextTick(() => {
    initOrbitNodes()
  })
})

onMounted(() => {
  fetchAlumni()
  // 监听窗口大小变化，重新初始化轨道
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
  }
  window.removeEventListener('resize', handleResize)
})

// 防抖处理窗口大小变化
let resizeTimeout: number | null = null
const handleResize = () => {
  if (resizeTimeout) {
    clearTimeout(resizeTimeout)
  }
  resizeTimeout = window.setTimeout(() => {
    initOrbitNodes()
  }, 250)
}

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
      <!-- Hero Section - 往届成员页 (暖金黄 #FBBF24) -->
      <section class="hero-section hero-organic page-alumni">
        <!-- 有机Blob装饰层 - 分散环绕布局设计，每个Blob有独特的seed，加深颜色 -->
        <div class="hero-organic__blobs">
          <!-- Blob 1: 右侧中央大型，深暖金黄，seed=101 -->
          <OrganicBlob
            size="large"
            color="#D97706"
            color-light="#F59E0B"
            :position="{ top: '10%', right: '-100px' }"
            :delay="0"
            :opacity="0.6"
            :seed="101"
            glow
          />
          <!-- Blob 2: 左上中型，深琥珀金，seed=202 -->
          <OrganicBlob
            size="medium"
            color="#B45309"
            color-light="#D97706"
            :position="{ top: '-40px', left: '20%' }"
            :delay="-2"
            :opacity="0.5"
            :seed="202"
            float
          />
          <!-- Blob 3: 底部中央小型，深活力橙，seed=303 -->
          <OrganicBlob
            size="small"
            color="#EA580C"
            color-light="#F97316"
            :position="{ bottom: '-20px', left: '50%' }"
            :delay="-4"
            :opacity="0.45"
            :seed="303"
          />
          <!-- 网格纹理 -->
          <div class="grid-pattern"></div>
        </div>

        <!-- Sparkle粒子层 -->
        <SparkleEffect :count="12" color="rgba(255, 255, 255, 0.95)" />

        <!-- 浮动装饰元素 -->
        <div class="floating-elements">
          <div class="float-item float-1">
            <el-icon :size="20" color="rgba(251, 191, 36, 0.5)"><StarFilled /></el-icon>
          </div>
          <div class="float-item float-2">
            <el-icon :size="18" color="rgba(245, 158, 11, 0.5)"><User /></el-icon>
          </div>
          <div class="float-item float-3">
            <el-icon :size="16" color="rgba(249, 115, 22, 0.5)"><Medal /></el-icon>
          </div>
        </div>

        <!-- 内容层 -->
        <div class="hero-organic__content hero-organic__content--left">
          <!-- 徽章 -->
          <div class="hero-badge">
            <el-icon><User /></el-icon>
            <span>往届成员</span>
          </div>

          <!-- 主标题 -->
          <h1 class="hero-title">
            成员<div class="title-highlight">星河</div>
          </h1>

          <!-- 副标题 -->
          <p class="hero-subtitle">
            致敬每一位为人力资源中心付出过的成员
          </p>
        </div>
      </section>

      <!-- 主内容区域 - 新布局：左右两列等高 -->
      <main class="main-content">
        <div class="two-column-layout">
          <!-- 左侧列：年份选择器 + 成员类型 -->
          <aside class="left-column">
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

          <!-- 右侧列：统计卡片 + 星河图 -->
          <div class="right-column">
            <!-- 统计数据卡片区域 -->
            <section class="stats-section">
              <div class="stats-card">
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
            </section>

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
              <div
                class="node-content"
                :style="{ background: `linear-gradient(135deg, ${getAvatarColor(member.name, member.role)} 0%, ${getAvatarColor(member.name, member.role)}dd 100%)` }"
              >
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
              <div
                class="node-content"
                :style="{ background: `linear-gradient(135deg, ${getAvatarColor(member.name, member.role)} 0%, ${getAvatarColor(member.name, member.role)}dd 100%)` }"
              >
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
              <div
                class="node-content"
                :style="{ background: `linear-gradient(135deg, ${getAvatarColor(member.name, member.role)} 0%, ${getAvatarColor(member.name, member.role)}dd 100%)` }"
              >
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
          </div>
        </div>
      </main>
    </div>
  </Layout>
</template>

<style scoped>
/* ============================================
   成员星河页面 - 暖金黄主题 (#FBBF24)
   Warm Golden Galaxy Design
   ============================================
   
   设计理念：
   - 暖金黄主色调，温暖明亮
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
  background: #FFFFFF;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

/* ============================================
   Hero Section - 暖金黄主题标题区
   ============================================
   
   参考首页Hero设计，融入暖金黄元素：
   - 有机曲线Blob背景
   - 网格点状装饰
   - 浮动装饰元素
   - 左对齐内容布局
   ============================================ */

.hero-section {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  text-align: left;
  padding: var(--space-8) var(--space-8) var(--space-12);
  margin: var(--space-6);
  margin-bottom: 0;
  background:
    radial-gradient(ellipse 80% 60% at 90% 10%, rgba(251, 191, 36, 0.12) 0%, transparent 40%),
    radial-gradient(ellipse 60% 50% at 10% 90%, rgba(245, 158, 11, 0.1) 0%, transparent 35%),
    linear-gradient(135deg, #FFFBF0 0%, #FFF8E0 50%, #FFF5D5 100%);
  border-radius: 32px;
  overflow: hidden;
}

.hero-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 50% 35% at 75% 15%, rgba(251, 191, 36, 0.12) 0%, transparent 50%),
    radial-gradient(ellipse 40% 25% at 25% 85%, rgba(245, 158, 11, 0.08) 0%, transparent 45%);
  pointer-events: none;
  z-index: 0;
}

/* 有机Blob容器 */
.hero-organic__blobs {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 1;
}

.hero-organic__blobs .grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(251, 191, 36, 0.1) 1px, transparent 0);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 20%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 20%, transparent 70%);
}

/* 浮动装饰元素 */
.floating-elements {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 1;
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
  animation: float 6s ease-in-out infinite;
}

.float-1 {
  top: 15%;
  right: 15%;
  animation-delay: 0s;
}

.float-2 {
  top: 50%;
  right: 8%;
  animation-delay: -2s;
}

.float-3 {
  bottom: 20%;
  right: 20%;
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

/* Hero 内容 - 左对齐 */
.hero-organic__content {
  position: relative;
  z-index: 2;
  padding: 32px 32px 48px;
}

.hero-organic__content--left {
  text-align: left;
  padding-left: 48px;
  padding-top: 24px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  background: linear-gradient(135deg, #FBBF24, #F59E0B);
  color: white;
  border-radius: var(--radius-full, 100px);
  font-size: var(--text-sm);
  font-weight: var(--font-medium, 500);
  margin-bottom: var(--space-6);
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.35), 0 0 20px rgba(251, 191, 36, 0.15);
  animation: fadeInDown 0.6s ease;
  position: relative;
  overflow: hidden;
}

.hero-badge::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: badgeShine 3s ease-in-out infinite;
}

@keyframes badgeShine {
  0%, 100% { left: -100%; }
  50% { left: 100%; }
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
  font-size: clamp(1.875rem, 4vw, 2.5rem);
  font-weight: 800;
  line-height: 1.2;
  color: #1F2937;
  margin: 0 0 var(--space-4, 16px) 0;
  animation: fadeInUp 0.6s ease 0.1s both;
}

.title-highlight {
  display: inline;
  color: #1F2937;
  font-weight: 800;
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
  font-size: var(--text-lg);
  color: var(--text-secondary, #6B7280);
  line-height: 1.7;
  max-width: 600px;
  margin: 0;
  animation: fadeInUp 0.6s ease 0.2s both;
}

/* 主内容区域 - 减少留白 */
.main-content {
  position: relative;
  z-index: 5;
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 1400px;
  margin: 0 auto;
  width: 100%;
  padding: 16px 24px;
}

/* 两列等高布局 */
.two-column-layout {
  display: flex;
  gap: 20px;
  align-items: stretch;
  flex: 1;
}

/* 左侧列 */
.left-column {
  width: 220px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* 右侧列 */
.right-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

/* 统计数据卡片区域 - 白色卡片风格，拉满右侧列 */
.stats-section {
  width: 100%;
  display: flex;
}

.stats-card {
  display: flex;
  align-items: center;
  justify-content: space-around;
  gap: 16px;
  padding: 16px 24px;
  background: white;
  border: 1px solid rgba(251, 191, 36, 0.12);
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  animation: fadeInUp 0.6s ease 0.3s both;
  position: relative;
  width: 100%;
}

/* 统计卡片顶部装饰线 */
.stats-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #FBBF24, #F59E0B, #F97316);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 20px;
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.08), rgba(245, 158, 11, 0.04));
  border: 1px solid rgba(251, 191, 36, 0.15);
  border-radius: 12px;
  min-width: 80px;
  flex: 1;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.stat-item:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(251, 191, 36, 0.2);
  border-color: rgba(251, 191, 36, 0.3);
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.12), rgba(245, 158, 11, 0.08));
}

.stat-item.highlight {
  background: linear-gradient(135deg, #FBBF24, #F59E0B);
  border: 1px solid transparent;
  color: white;
  box-shadow: 0 8px 24px rgba(251, 191, 36, 0.3);
}

.stat-item .stat-value {
  font-size: 32px;
  font-weight: 800;
  background: linear-gradient(135deg, #FBBF24, #F59E0B);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  line-height: 1.2;
}

.stat-item.highlight .stat-value {
  background: white;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stat-item .stat-label {
  font-size: 13px;
  color: #6B7280;
  margin-top: 8px;
  font-weight: 500;
  white-space: nowrap;
}

.stat-item.highlight .stat-label {
  color: rgba(255, 255, 255, 0.95);
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: linear-gradient(180deg, transparent, rgba(251, 191, 36, 0.3), transparent);
}

/* ============================================
   面板标题 - 暖金黄毛玻璃标签风格
   ============================================ */
.panel-title {
  font-size: 12px;
  font-weight: 700;
  color: #1F2937;
  margin-bottom: 12px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(251, 191, 36, 0.2);
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 2px 8px rgba(251, 191, 36, 0.1);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.panel-title::before {
  content: '';
  width: 3px;
  height: 14px;
  background: linear-gradient(180deg, #FBBF24, #F59E0B);
  border-radius: 2px;
}

/* 年份选择器 - 白色卡片风格 */
.year-selector {
  background: white;
  border: 1px solid rgba(251, 191, 36, 0.12);
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  flex: 1;
  display: flex;
  flex-direction: column;
}

/* 年份选择器顶部装饰线 */
.year-selector::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #FBBF24, #F59E0B);
}

.year-selector:hover {
  box-shadow: 0 8px 24px rgba(251, 191, 36, 0.1);
}

.year-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 年份按钮 */
.year-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;
}

/* 年份按钮之间的分隔线 */
.year-btn:not(:last-child) {
  border-bottom: 1px solid rgba(251, 191, 36, 0.1);
  border-radius: 10px 10px 0 0;
  margin-bottom: 4px;
}

.year-btn::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(251, 191, 36, 0.08), rgba(245, 158, 11, 0.05));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.year-btn:hover {
  background: rgba(255, 255, 255, 0.9);
  transform: translateX(6px);
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.12);
}

.year-btn:hover::before {
  opacity: 1;
}

.year-btn.active {
  background: linear-gradient(135deg, #FBBF24, #F59E0B);
  border-color: transparent;
  box-shadow: 0 6px 24px rgba(251, 191, 36, 0.35);
}

.year-num {
  font-size: 18px;
  font-weight: 700;
  color: #374151;
  position: relative;
  z-index: 1;
}

.year-btn.active .year-num {
  color: white;
}

.year-suffix {
  font-size: 13px;
  color: #6B7280;
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
  box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.3), 0 0 12px rgba(255, 255, 255, 0.5);
  animation: indicatorPulse 2s ease-in-out infinite;
}

@keyframes indicatorPulse {
  0%, 100% {
    box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.3), 0 0 12px rgba(255, 255, 255, 0.5);
  }
  50% {
    box-shadow: 0 0 0 5px rgba(255, 255, 255, 0.2), 0 0 20px rgba(255, 255, 255, 0.6);
  }
}

/* 图例面板 - 白色卡片风格 */
.legend-panel {
  background: white;
  border: 1px solid rgba(251, 191, 36, 0.12);
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

/* 图例面板顶部装饰线 */
.legend-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, #F59E0B, #F97316);
}

.legend-panel:hover {
  box-shadow: 0 8px 24px rgba(251, 191, 36, 0.1);
}

.legend-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

/* 图例项 */
.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 8px;
  transition: all 0.25s ease;
  border: 1px solid transparent;
}

/* 图例项之间的分隔线 */
.legend-item:not(:last-child) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 8px 8px 0 0;
  margin-bottom: 2px;
}

.legend-item:hover {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(251, 191, 36, 0.15);
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

/* 图例颜色与成员节点颜色统一 */
.legend-dot.minister {
  background: linear-gradient(135deg, #F59E0B, #D97706);
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.4);
}

.legend-dot.minister::after {
  background: radial-gradient(circle, rgba(245, 158, 11, 0.4), transparent);
}

.legend-dot.vice {
  background: linear-gradient(135deg, #F97316, #EA580C);
  box-shadow: 0 2px 8px rgba(249, 115, 22, 0.4);
}

.legend-dot.vice::after {
  background: radial-gradient(circle, rgba(249, 115, 22, 0.4), transparent);
}

/* 部员图例使用彩虹渐变表示多样性 */
.legend-dot.member {
  background: conic-gradient(from 0deg, #8B5CF6, #6366F1, #3B82F6, #06B6D4, #10B981, #84CC16, #F59E0B, #F97316, #EF4444, #EC4899, #8B5CF6);
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.4);
}

.legend-dot.member::after {
  background: radial-gradient(circle, rgba(99, 102, 241, 0.3), transparent);
}

.legend-text {
  font-size: 14px;
  color: #374151;
  font-weight: 500;
  flex: 1;
}

.legend-count {
  font-size: 12px;
  color: #6B7280;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.8);
  padding: 4px 10px;
  border-radius: 12px;
  border: 1px solid rgba(251, 191, 36, 0.1);
}

/* 右侧展示区域 - 白色卡片风格，与左侧对齐 */
.display-area {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: white;
  border: 1px solid rgba(251, 191, 36, 0.12);
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
  overflow: hidden;
  min-height: 320px;
}

/* 展示区域顶部渐变装饰线 */
.display-area::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #FBBF24, #F59E0B, #F97316, #FBBF24);
  background-size: 200% 100%;
  animation: gradientFlow 3s linear infinite;
}

@keyframes gradientFlow {
  0% { background-position: 0% 50%; }
  100% { background-position: 200% 50%; }
}

/* 展示区域内部横线分隔 */
.display-divider {
  width: 100%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(251, 191, 36, 0.2), transparent);
  margin: 20px 0;
}

/* 当前届数标题 */
.current-year-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.nav-arrow {
  width: 44px;
  height: 44px;
  border: 1px solid rgba(251, 191, 36, 0.25);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #6B7280;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(251, 191, 36, 0.08);
}

.nav-arrow:hover:not(:disabled) {
  border-color: rgba(251, 191, 36, 0.5);
  background: rgba(255, 255, 255, 0.95);
  color: #FBBF24;
  transform: scale(1.1);
  box-shadow: 0 6px 20px rgba(251, 191, 36, 0.2);
}

.nav-arrow:disabled {
  opacity: 0.35;
  cursor: not-allowed;
  background: rgba(255, 255, 255, 0.5);
}

.year-display {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 8px 20px;
  background: linear-gradient(135deg, #FBBF24, #F59E0B);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.3);
  position: relative;
  overflow: hidden;
}

.year-display::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.15), transparent 50%);
  pointer-events: none;
}

.year-number {
  font-size: 20px;
  font-weight: 800;
  color: white;
  line-height: 1;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.year-text {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.95);
  font-weight: 500;
}

/* ============================================
   成员星河图 - 暖金黄椭圆轨道设计
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
  height: 100%;
  min-height: 300px;
  flex: 1;
}

/* 星河背景光晕 - 淡雅暖色 */
.galaxy-container::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 500px;
  height: 280px;
  background: radial-gradient(ellipse at center, 
    rgba(251, 191, 36, 0.06) 0%, 
    rgba(245, 158, 11, 0.03) 40%, 
    transparent 70%
  );
  border-radius: 50%;
  filter: blur(50px);
  pointer-events: none;
  animation: galaxyGlow 8s ease-in-out infinite;
}

/* 星点粒子背景 - 淡雅暖色 */
.galaxy-container::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: 
    radial-gradient(2px 2px at 20% 30%, rgba(251, 191, 36, 0.25), transparent),
    radial-gradient(2px 2px at 40% 70%, rgba(245, 158, 11, 0.2), transparent),
    radial-gradient(1px 1px at 60% 20%, rgba(200, 200, 200, 0.4), transparent),
    radial-gradient(2px 2px at 80% 50%, rgba(251, 191, 36, 0.2), transparent),
    radial-gradient(1px 1px at 10% 80%, rgba(245, 158, 11, 0.25), transparent),
    radial-gradient(1px 1px at 90% 10%, rgba(200, 200, 200, 0.3), transparent);
  background-size: 200px 200px;
  border-radius: 50%;
  animation: starTwinkle 4s ease-in-out infinite;
  pointer-events: none;
  opacity: 0.6;
}

@keyframes galaxyGlow {
  0%, 100% { opacity: 0.8; transform: translate(-50%, -50%) scale(1); }
  50% { opacity: 1; transform: translate(-50%, -50%) scale(1.05); }
}

@keyframes starTwinkle {
  0%, 100% { opacity: 0.6; }
  50% { opacity: 1; }
}

/* 轨道环 - 静止不动 - 暖金黄，增加视觉层次，缩小尺寸 */
.orbit-ring {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  pointer-events: none;
}

/* 内圈轨道 - 部长（长方形） */
.ring-1 {
  width: 400px;
  height: 140px;
  border: 1.5px dashed rgba(245, 158, 11, 0.35);
  background: radial-gradient(ellipse at center, transparent 60%, rgba(245, 158, 11, 0.03) 100%);
  border-radius: 70px;
}

/* 中圈轨道 - 副部长（长方形） */
.ring-2 {
  width: 620px;
  height: 210px;
  border: 1px dashed rgba(249, 115, 22, 0.28);
  background: radial-gradient(ellipse at center, transparent 55%, rgba(249, 115, 22, 0.02) 100%);
  border-radius: 105px;
}

/* 外圈轨道 - 部员（长方形） */
.ring-3 {
  width: 840px;
  height: 280px;
  border: 1px dashed rgba(99, 102, 241, 0.2);
  background: radial-gradient(ellipse at center, transparent 50%, rgba(99, 102, 241, 0.02) 100%);
  border-radius: 140px;
}

/* 中心核心 - 暖金黄 */
.galaxy-center {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 50;
}

.center-core {
  width: 50px;
  height: 50px;
  background: linear-gradient(135deg, #FBBF24, #F59E0B);
  border: 2px solid rgba(255, 255, 255, 0.5);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 
    0 0 0 4px rgba(251, 191, 36, 0.15),
    0 0 0 8px rgba(251, 191, 36, 0.08),
    0 0 30px rgba(251, 191, 36, 0.4),
    0 8px 24px rgba(251, 191, 36, 0.3);
  animation: corePulse 3s ease-in-out infinite;
}

@keyframes corePulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.03); }
}

.core-text {
  font-size: 16px;
  font-weight: 900;
  color: white;
  letter-spacing: 1px;
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
    0 0 30px rgba(244, 63, 94, 0.4),
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
}

/* 不同角色的节点尺寸与样式 - 动态暖色主题 */
/* 部长 - 最大尺寸 */
.minister-node .node-content {
  width: 44px;
  height: 44px;
  box-shadow: 
    0 4px 16px rgba(251, 191, 36, 0.4),
    0 0 0 2px rgba(255, 220, 150, 0.3),
    0 0 15px rgba(251, 191, 36, 0.3),
    inset 0 1px 3px rgba(255, 255, 255, 0.4);
}

.minister-node .node-name {
  font-size: 11px;
}

.minister-node:hover .node-content {
  box-shadow: 
    0 8px 24px rgba(251, 191, 36, 0.5),
    0 0 0 3px rgba(255, 220, 150, 0.4),
    0 0 30px rgba(251, 191, 36, 0.5),
    inset 0 1px 3px rgba(255, 255, 255, 0.4);
}

/* 副部长 - 中等尺寸 */
.vice-node .node-content {
  width: 36px;
  height: 36px;
  box-shadow: 
    0 4px 12px rgba(252, 211, 77, 0.35),
    0 0 0 2px rgba(255, 235, 150, 0.25),
    0 0 10px rgba(252, 211, 77, 0.25),
    inset 0 1px 3px rgba(255, 255, 255, 0.35);
}

.vice-node .node-name {
  font-size: 10px;
}

.vice-node:hover .node-content {
  box-shadow: 
    0 6px 20px rgba(251, 191, 36, 0.45),
    0 0 0 2px rgba(255, 235, 150, 0.35),
    0 0 20px rgba(252, 211, 77, 0.4),
    inset 0 1px 3px rgba(255, 255, 255, 0.35);
}

/* 部员 - 最小尺寸 */
.member-circle .node-content {
  width: 28px;
  height: 28px;
  box-shadow: 
    0 3px 10px rgba(249, 115, 22, 0.3),
    0 0 0 1px rgba(255, 200, 150, 0.2),
    0 0 8px rgba(249, 115, 22, 0.2),
    inset 0 1px 2px rgba(255, 255, 255, 0.3);
}

.member-circle .node-name {
  font-size: 9px;
}

.member-circle:hover .node-content {
  box-shadow: 
    0 4px 16px rgba(249, 115, 22, 0.4),
    0 0 0 2px rgba(255, 200, 150, 0.3),
    0 0 18px rgba(249, 115, 22, 0.35),
    inset 0 1px 2px rgba(255, 255, 255, 0.3);
}

.galaxy-container.is-transitioning .member-node {
  opacity: 0;
}

/* 空状态 - 暖金黄 */
.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  color: #6B7280;
  padding: 40px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border-radius: 20px;
  border: 1px solid rgba(251, 191, 36, 0.15);
}

.empty-state p {
  margin-top: 12px;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .content-wrapper {
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

  .stats-card {
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

  .content-wrapper {
    flex-direction: column;
  }

  .control-panel {
    flex-direction: row;
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
    height: 110px;
  }

  .orbit-ring.ring-2 {
    width: 480px;
    height: 165px;
  }

  .orbit-ring.ring-3 {
    width: 640px;
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
    width: 48px;
    height: 48px;
  }

  .minister-node .node-name {
    font-size: 11px;
  }

  .vice-node .node-content {
    width: 38px;
    height: 38px;
  }

  .vice-node .node-name {
    font-size: 10px;
  }

  .member-circle .node-content {
    width: 30px;
    height: 30px;
  }

  .member-circle .node-name {
    font-size: 9px;
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
    height: 280px;
    overflow: hidden;
  }

  .galaxy-container::before {
    width: 200px;
    height: 110px;
  }

  .galaxy-container::after {
    background-size: 100px 100px;
  }

  .orbit-ring.ring-1 {
    width: 200px;
    height: 70px;
  }

  .orbit-ring.ring-2 {
    width: 300px;
    height: 100px;
  }

  .orbit-ring.ring-3 {
    width: 400px;
    height: 130px;
  }

  .center-core {
    width: 40px;
    height: 40px;
  }

  .core-text {
    font-size: 14px;
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

  .content-wrapper {
    flex-direction: column;
  }

  .control-panel {
    flex-direction: column;
  }
  
  /* 移动端节点内容溢出修复 */
  .node-content {
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
  }
  
  .node-name {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding: 1px;
  }
}

/* 减少动画偏好支持 */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
