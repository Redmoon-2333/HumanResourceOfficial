<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import { useUserStore } from '@/stores/user'
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Sunny,
  StarFilled,
  Medal,
  Trophy,
  OfficeBuilding,
  Coffee,
  ChatRound,
  PictureRounded,
  ArrowLeft,
  ArrowRight,
  Setting,
  Edit
} from '@element-plus/icons-vue'
import { getActiveImages, type DailyImage } from '@/api/dailyImage'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()

// 3D轮播图相关状态
const carouselRef = ref<HTMLElement | null>(null)
const currentIndex = ref(0)
const isAutoPlaying = ref(true)
const isTransitioning = ref(false)
let autoPlayTimer: ReturnType<typeof setInterval> | null = null

// 图片加载状态
const imagesLoading = ref(false)
const imagesError = ref(false)

// 从后端获取的图片数据
const carouselImages = ref<DailyImage[]>([])

// 计算总图片数量
const totalImages = computed(() => carouselImages.value.length)

// 部门简介内容
const departmentIntro = {
  title: '部门简介',
  content: '人力资源部门作为院学生会部门之一，负责学院举行的各种评奖评优选举活动，其中包括通知信息，名单确认，场务引导，维护秩序等任务。此外，我们还负责团校，团务和人力调动的相关事务。总的来讲，我们做的是一些学院平凡但重要的工作。'
}

// 我们的日常内容
const dailyLife = {
  title: '我们的日常',
  content: '每周一次的例会都可能有一份惊喜——奶茶，而每位新成员的生日都会得到特别准备的蛋糕，这些已成为我们的常规活动。此外，每周我们还设有"树洞"环节，无论是关于生活的吐槽还是学习上的疑惑，新成员们都能获得学长学姐们的耐心指导和解答。我们的团建活动同样丰富多彩，从桌游游玩到美食聚餐，再到小屋聚会，你总能找到属于自己的快乐时光。'
}

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '夜深了'
  if (hour < 12) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

// 从后端加载图片数据
const loadCarouselImages = async () => {
  // 如果已经有图片数据，不重复加载
  if (carouselImages.value.length > 0) return

  imagesLoading.value = true
  imagesError.value = false

  try {
    const res = await getActiveImages()
    if (res.code === 200 && Array.isArray(res.data) && res.data.length > 0) {
      carouselImages.value = res.data.sort((a, b) => a.sortOrder - b.sortOrder)
      // 启动自动播放
      startAutoPlay()
    } else {
      // 如果后端没有数据，使用默认数据
      useDefaultImages()
    }
  } catch (error) {
    console.error('加载轮播图图片失败:', error)
    imagesError.value = true
    // 使用默认数据作为降级方案
    useDefaultImages()
  } finally {
    imagesLoading.value = false
  }
}

// 使用默认图片数据（降级方案）
const useDefaultImages = () => {
  carouselImages.value = [
    { imageId: 1, imageUrl: '/images/daily/life1.jpg', title: '团建聚餐', description: '美食与欢笑', sortOrder: 1, isActive: true, createTime: '', updateTime: '' },
    { imageId: 2, imageUrl: '/images/daily/life2.jpg', title: '生日庆祝', description: '温馨的祝福', sortOrder: 2, isActive: true, createTime: '', updateTime: '' },
    { imageId: 3, imageUrl: '/images/daily/life3.jpg', title: '桌游时光', description: '欢乐互动', sortOrder: 3, isActive: true, createTime: '', updateTime: '' },
    { imageId: 4, imageUrl: '/images/daily/life4.jpg', title: '例会惊喜', description: '奶茶相伴', sortOrder: 4, isActive: true, createTime: '', updateTime: '' },
    { imageId: 5, imageUrl: '/images/daily/life5.jpg', title: '树洞交流', description: '倾听与分享', sortOrder: 5, isActive: true, createTime: '', updateTime: '' },
    { imageId: 6, imageUrl: '/images/daily/life6.jpg', title: '小屋聚会', description: '温馨时光', sortOrder: 6, isActive: true, createTime: '', updateTime: '' }
  ]
  startAutoPlay()
}

// 3D轮播图控制函数
const rotateTo = (index: number) => {
  if (isTransitioning.value || totalImages.value === 0) return
  isTransitioning.value = true
  currentIndex.value = index
  setTimeout(() => {
    isTransitioning.value = false
  }, 800)
}

const nextSlide = () => {
  if (totalImages.value === 0) return
  const next = (currentIndex.value + 1) % totalImages.value
  rotateTo(next)
}

const prevSlide = () => {
  if (totalImages.value === 0) return
  const prev = (currentIndex.value - 1 + totalImages.value) % totalImages.value
  rotateTo(prev)
}

// 自动播放控制
const startAutoPlay = () => {
  if (autoPlayTimer) clearInterval(autoPlayTimer)
  autoPlayTimer = setInterval(() => {
    if (isAutoPlaying.value && totalImages.value > 0) {
      nextSlide()
    }
  }, 4000)
}

const stopAutoPlay = () => {
  isAutoPlaying.value = false
}

const resumeAutoPlay = () => {
  isAutoPlaying.value = true
}

// 计算每个图片的3D变换
const getItemTransform = (index: number) => {
  if (totalImages.value === 0) return 'rotateY(0deg) translateZ(350px)'
  const angle = (360 / totalImages.value) * index
  const radius = 350 // 旋转半径
  const rotateY = angle - currentIndex.value * (360 / totalImages.value)
  return `rotateY(${rotateY}deg) translateZ(${radius}px)`
}

// 计算每个图片的透明度（只显示前方的图片）
const getItemOpacity = (index: number) => {
  if (totalImages.value === 0) return 0
  const angle = ((360 / totalImages.value) * index - currentIndex.value * (360 / totalImages.value)) % 360
  const normalizedAngle = angle < 0 ? angle + 360 : angle
  // 只显示正面90度范围内的图片
  if (normalizedAngle <= 60 || normalizedAngle >= 300) return 1
  if (normalizedAngle <= 120 || normalizedAngle >= 240) return 0.3
  return 0.1
}

// 计算每个图片的缩放
const getItemScale = (index: number) => {
  if (totalImages.value === 0) return 1
  const angle = ((360 / totalImages.value) * index - currentIndex.value * (360 / totalImages.value)) % 360
  const normalizedAngle = angle < 0 ? angle + 360 : angle
  if (normalizedAngle <= 30 || normalizedAngle >= 330) return 1.1
  if (normalizedAngle <= 60 || normalizedAngle >= 300) return 0.9
  return 0.7
}

onMounted(() => {
  loadCarouselImages()
})

onUnmounted(() => {
  if (autoPlayTimer) clearInterval(autoPlayTimer)
})

const goToImageManagement = () => {
  router.push('/daily-image-management')
}
</script>

<template>
  <Layout>
    <div class="home-page">
      <!-- Hero Section - 有机曲线风格 -->
      <section class="hero-section">
        <div class="hero-background">
          <div class="blob blob-1"></div>
          <div class="blob blob-2"></div>
          <div class="blob blob-3"></div>
          <div class="grid-pattern"></div>
        </div>

        <div class="hero-content">
          <div class="welcome-badge">
            <el-icon><Sunny /></el-icon>
            <span>{{ greeting }}</span>
          </div>
          <h1 class="hero-title" style="color: #1C1917;">
            <span class="name-highlight">{{ userStore.userInfo?.name || '访客' }}</span>
            <span class="welcome-text" style="color: #1C1917;">，欢迎回来！</span>
          </h1>
          <p class="hero-subtitle" style="color: #57534E;">
            这里是人力资源中心管理系统，为您提供活动管理、资料共享、AI助手等服务
          </p>

          <div class="floating-elements">
            <div class="float-item item-1">
              <el-icon><StarFilled /></el-icon>
            </div>
            <div class="float-item item-2">
              <el-icon><Trophy /></el-icon>
            </div>
            <div class="float-item item-3">
              <el-icon><Medal /></el-icon>
            </div>
          </div>
        </div>
      </section>

      <!-- 部门简介板块 -->
      <section class="intro-section">
        <div class="section-header">
          <div class="section-icon intro-icon">
            <el-icon><OfficeBuilding /></el-icon>
          </div>
          <div class="section-title-group">
            <h2 class="section-title">{{ departmentIntro.title }}</h2>
            <p class="section-subtitle">About Our Department</p>
          </div>
        </div>

        <div class="intro-card">
          <div class="intro-decoration">
            <div class="deco-circle circle-1"></div>
            <div class="deco-circle circle-2"></div>
            <div class="deco-line"></div>
          </div>
          <div class="intro-content">
            <p class="intro-text">{{ departmentIntro.content }}</p>
          </div>
          <div class="intro-features">
            <div class="feature-tag">
              <el-icon><StarFilled /></el-icon>
              <span>评奖评优</span>
            </div>
            <div class="feature-tag">
              <el-icon><Medal /></el-icon>
              <span>团校团务</span>
            </div>
            <div class="feature-tag">
              <el-icon><Trophy /></el-icon>
              <span>人力调动</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 我们的日常板块 -->
      <section class="daily-section">
        <div class="section-header">
          <div class="section-icon daily-icon">
            <el-icon><Coffee /></el-icon>
          </div>
          <div class="section-title-group">
            <h2 class="section-title">{{ dailyLife.title }}</h2>
            <p class="section-subtitle">Our Daily Life</p>
          </div>
        </div>

        <!-- 文字内容区域 -->
        <div class="daily-text-card">
          <div class="daily-text-content">
            <p>{{ dailyLife.content }}</p>
          </div>
          <div class="daily-highlights">
            <div class="highlight-item">
              <div class="highlight-icon">
                <el-icon><Coffee /></el-icon>
              </div>
              <span>例会奶茶</span>
            </div>
            <div class="highlight-item">
              <div class="highlight-icon">
                <el-icon><PictureRounded /></el-icon>
              </div>
              <span>生日蛋糕</span>
            </div>
            <div class="highlight-item">
              <div class="highlight-icon">
                <el-icon><ChatRound /></el-icon>
              </div>
              <span>树洞交流</span>
            </div>
            <div class="highlight-item">
              <div class="highlight-icon">
                <el-icon><StarFilled /></el-icon>
              </div>
              <span>团建活动</span>
            </div>
          </div>
        </div>

        <!-- 3D轮播图区域 -->
        <div
          class="carousel-container"
          @mouseenter="stopAutoPlay"
          @mouseleave="resumeAutoPlay"
        >
          <!-- 管理入口 - 仅部长/副部长可见 -->
          <transition name="management-fade">
            <button 
              v-if="userStore.isMinister"
              class="management-entry"
              @click.stop="goToImageManagement"
              title="管理轮播图片"
            >
              <div class="entry-glow"></div>
              <el-icon class="entry-icon"><Setting /></el-icon>
              <span class="entry-text">管理图片</span>
              <div class="entry-particles">
                <span class="particle"></span>
                <span class="particle"></span>
                <span class="particle"></span>
              </div>
            </button>
          </transition>

          <!-- 加载状态 -->
          <div v-if="imagesLoading" class="carousel-loading">
            <el-icon class="loading-icon" :size="40"><Loading /></el-icon>
            <span>加载中...</span>
          </div>

          <!-- 3D轮播 -->
          <template v-else-if="carouselImages.length > 0">
            <div class="carousel-3d" ref="carouselRef">
              <div
                class="carousel-track"
                :style="{ transform: `rotateY(${-currentIndex * (360 / totalImages)}deg)` }"
              >
                <div
                  v-for="(image, index) in carouselImages"
                  :key="image.imageId"
                  class="carousel-item"
                  :style="{
                    transform: getItemTransform(index),
                    opacity: getItemOpacity(index),
                    '--scale': getItemScale(index)
                  }"
                >
                  <div class="carousel-image-wrapper">
                    <img
                      :src="image.imageUrl"
                      :alt="image.title"
                      loading="lazy"
                      class="carousel-image"
                      @error="$event.target.src = '/images/placeholder.jpg'"
                    />
                    <div class="carousel-image-overlay">
                      <h4>{{ image.title }}</h4>
                      <p>{{ image.description }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 轮播控制按钮 -->
            <button class="carousel-btn prev-btn" @click="prevSlide">
              <el-icon><ArrowLeft /></el-icon>
            </button>
            <button class="carousel-btn next-btn" @click="nextSlide">
              <el-icon><ArrowRight /></el-icon>
            </button>

            <!-- 指示器 -->
            <div class="carousel-indicators">
              <button
                v-for="(_, index) in carouselImages"
                :key="index"
                class="indicator-dot"
                :class="{ active: currentIndex === index }"
                @click="rotateTo(index)"
              />
            </div>
          </template>

          <!-- 空状态 -->
          <div v-else class="carousel-empty">
            <el-icon :size="48"><PictureRounded /></el-icon>
            <span>暂无图片</span>
          </div>
        </div>
      </section>
    </div>
  </Layout>
</template>

<style scoped>
/* ============================================
   Home Page - Organic Curves Design
   ============================================ */

.home-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 var(--space-6) var(--space-12);
}

/* ============================================
   Hero Section - 有机曲线风格
   ============================================ */
.hero-section {
  position: relative;
  padding: var(--space-12) var(--space-8);
  margin-bottom: var(--space-8);
  border-radius: 32px;
  overflow: hidden;
  background: linear-gradient(135deg,
    rgba(255, 107, 74, 0.08) 0%,
    rgba(245, 158, 11, 0.08) 50%,
    rgba(227, 85, 50, 0.08) 100%);
}

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

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(255, 107, 74, 0.15) 1px, transparent 0);
  background-size: 40px 40px;
}

.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  background: linear-gradient(135deg, var(--primary-500), var(--terracotta-500));
  color: white;
  border-radius: 100px;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  margin-bottom: var(--space-6);
  box-shadow: 0 4px 16px rgba(255, 107, 74, 0.35);
  animation: fadeInDown 0.6s ease;
}

.hero-title {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  line-height: 1.2;
  color: var(--text-primary);
  margin: 0 0 var(--space-4) 0;
  animation: fadeInUp 0.6s ease 0.1s both;
}

.name-highlight {
  background: linear-gradient(135deg, var(--primary-500), var(--terracotta-500));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.welcome-text {
  color: var(--text-primary);
}

.hero-subtitle {
  font-size: var(--text-lg);
  color: var(--text-secondary);
  line-height: 1.8;
  max-width: 600px;
  margin: 0 auto;
  animation: fadeInUp 0.6s ease 0.2s both;
}

/* Floating Elements */
.floating-elements {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
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
  animation: float 6s ease-in-out infinite;
}

.item-1 {
  top: 20%;
  left: 10%;
  animation-delay: 0s;
  color: #FF6B4A;
  filter: drop-shadow(0 4px 12px rgba(255, 107, 74, 0.4));
}

.item-2 {
  top: 60%;
  right: 15%;
  animation-delay: -2s;
  color: #F59E0B;
  filter: drop-shadow(0 4px 12px rgba(245, 158, 11, 0.4));
}

.item-3 {
  bottom: 20%;
  left: 20%;
  animation-delay: -4s;
  color: #E35532;
  filter: drop-shadow(0 4px 12px rgba(227, 85, 50, 0.4));
}

/* ============================================
   Section Common Styles
   ============================================ */
section {
  margin-bottom: var(--space-10);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.section-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  font-size: var(--text-xl);
}

.intro-icon {
  background: linear-gradient(135deg, var(--primary-100), var(--primary-50));
  color: var(--primary-600);
}

.daily-icon {
  background: linear-gradient(135deg, var(--secondary-100), var(--secondary-50));
  color: var(--secondary-600);
}

.section-title-group {
  flex: 1;
}

.section-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-1) 0;
}

.section-subtitle {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin: 0;
}

/* ============================================
   部门简介板块
   ============================================ */
.intro-section {
  animation: fadeInUp 0.6s ease forwards;
}

.intro-card {
  position: relative;
  padding: var(--space-8);
  background: white;
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: all 0.3s ease;
}

.intro-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.1);
}

.intro-decoration {
  position: absolute;
  top: 0;
  right: 0;
  width: 200px;
  height: 200px;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
}

.circle-1 {
  width: 150px;
  height: 150px;
  background: linear-gradient(135deg, var(--primary-500), var(--terracotta-500));
  top: -50px;
  right: -50px;
}

.circle-2 {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, var(--secondary-500), var(--accent-500));
  top: 30px;
  right: 60px;
}

.deco-line {
  position: absolute;
  width: 2px;
  height: 80px;
  background: linear-gradient(to bottom, var(--primary-300), transparent);
  top: 20px;
  right: 100px;
  transform: rotate(15deg);
}

.intro-content {
  position: relative;
  z-index: 1;
  max-width: 800px;
}

.intro-text {
  font-size: var(--text-lg);
  color: var(--text-secondary);
  line-height: 2;
  margin: 0;
  text-align: justify;
}

.intro-features {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-6);
}

.feature-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  background: linear-gradient(135deg, var(--primary-50), var(--terracotta-50));
  border: 1px solid var(--primary-100);
  border-radius: 100px;
  font-size: var(--text-sm);
  color: var(--primary-700);
  font-weight: var(--font-medium);
  transition: all 0.3s ease;
}

.feature-tag:hover {
  background: linear-gradient(135deg, var(--primary-100), var(--terracotta-100));
  transform: translateY(-2px);
}

.feature-tag .el-icon {
  font-size: 16px;
}

/* ============================================
   我们的日常板块
   ============================================ */
.daily-section {
  animation: fadeInUp 0.6s ease 0.2s forwards;
  opacity: 0;
}

/* 文字卡片 - 占满宽度 */
.daily-text-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: var(--space-8);
  background: linear-gradient(135deg,
    rgba(255, 107, 74, 0.03) 0%,
    rgba(245, 158, 11, 0.03) 100%);
  border-radius: 24px;
  border: 1px solid rgba(255, 107, 74, 0.1);
  transition: all 0.3s ease;
}

.daily-text-card:hover {
  border-color: rgba(255, 107, 74, 0.2);
  box-shadow: 0 8px 32px rgba(255, 107, 74, 0.08);
}

.daily-text-content {
  flex: 1;
}

.daily-text-content p {
  font-size: var(--text-base);
  color: var(--text-secondary);
  line-height: 2;
  margin: 0;
  text-align: justify;
}

.daily-highlights {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
  margin-top: var(--space-6);
}

.highlight-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: white;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.highlight-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.highlight-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--primary-500), var(--terracotta-500));
  border-radius: 12px;
  color: white;
  font-size: 20px;
}

.highlight-item span {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-primary);
}

/* ============================================
   3D轮播图样式
   ============================================ */
.carousel-container {
  position: relative;
  height: 400px;
  perspective: 1200px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: var(--space-6);
  background: linear-gradient(135deg,
    rgba(255, 107, 74, 0.03) 0%,
    rgba(245, 158, 11, 0.03) 100%);
  border-radius: 24px;
  border: 1px solid rgba(255, 107, 74, 0.1);
}

/* 管理入口按钮 - 精致悬浮风格 */
.management-entry {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(255, 107, 74, 0.2);
  border-radius: 12px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 4px 16px rgba(255, 107, 74, 0.1);
}

.management-entry:hover {
  transform: translateY(-2px) scale(1.02);
  border-color: rgba(255, 107, 74, 0.4);
  box-shadow: 0 8px 28px rgba(255, 107, 74, 0.2);
}

.management-entry:active {
  transform: translateY(0) scale(0.98);
}

.entry-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, 
    rgba(255, 107, 74, 0.1) 0%, 
    rgba(245, 158, 11, 0.1) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.management-entry:hover .entry-glow {
  opacity: 1;
}

.entry-icon {
  font-size: 18px;
  color: var(--primary-500);
  transition: transform 0.4s ease;
}

.management-entry:hover .entry-icon {
  transform: rotate(90deg);
}

.entry-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
}

.entry-particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.particle {
  position: absolute;
  width: 4px;
  height: 4px;
  background: var(--primary-400);
  border-radius: 50%;
  opacity: 0;
}

.management-entry:hover .particle {
  animation: particleFloat 1.5s ease-out infinite;
}

.particle:nth-child(1) {
  top: 20%;
  left: 10%;
  animation-delay: 0s;
}

.particle:nth-child(2) {
  top: 60%;
  right: 15%;
  animation-delay: 0.3s;
}

.particle:nth-child(3) {
  bottom: 25%;
  left: 30%;
  animation-delay: 0.6s;
}

@keyframes particleFloat {
  0% {
    opacity: 0;
    transform: translateY(0) scale(0);
  }
  50% {
    opacity: 0.6;
    transform: translateY(-10px) scale(1);
  }
  100% {
    opacity: 0;
    transform: translateY(-20px) scale(0);
  }
}

/* 管理入口过渡动画 */
.management-fade-enter-active {
  animation: managementSlideIn 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.management-fade-leave-active {
  animation: managementSlideIn 0.3s ease reverse;
}

@keyframes managementSlideIn {
  from {
    opacity: 0;
    transform: translateY(-10px) scale(0.9);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.carousel-3d {
  position: relative;
  width: 300px;
  height: 300px;
  transform-style: preserve-3d;
}

.carousel-track {
  position: relative;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d;
  transition: transform 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.carousel-item {
  position: absolute;
  width: 280px;
  height: 280px;
  left: 50%;
  top: 50%;
  margin-left: -140px;
  margin-top: -140px;
  transform-style: preserve-3d;
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
  scale: var(--scale, 1);
}

.carousel-image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  background: linear-gradient(135deg, var(--primary-100), var(--secondary-100));
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.carousel-item:hover .carousel-image {
  transform: scale(1.05);
}

.carousel-image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--space-4);
  background: linear-gradient(to top, rgba(0, 0, 0, 0.7), transparent);
  color: white;
  transform: translateY(100%);
  transition: transform 0.3s ease;
}

.carousel-item:hover .carousel-image-overlay {
  transform: translateY(0);
}

.carousel-image-overlay h4 {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  margin: 0 0 var(--space-1) 0;
}

.carousel-image-overlay p {
  font-size: var(--text-sm);
  margin: 0;
  opacity: 0.9;
}

/* 轮播控制按钮 */
.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: white;
  border: none;
  border-radius: 50%;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  cursor: pointer;
  color: var(--text-primary);
  font-size: 20px;
  transition: all 0.3s ease;
  z-index: 10;
}

.carousel-btn:hover {
  background: linear-gradient(135deg, var(--primary-500), var(--terracotta-500));
  color: white;
  transform: translateY(-50%) scale(1.1);
  box-shadow: 0 6px 24px rgba(255, 107, 74, 0.4);
}

.prev-btn {
  left: 0;
}

.next-btn {
  right: 0;
}

/* 指示器 */
.carousel-indicators {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: var(--space-2);
}

.indicator-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: none;
  background: var(--border-color);
  cursor: pointer;
  transition: all 0.3s ease;
}

.indicator-dot:hover {
  background: var(--primary-300);
  transform: scale(1.2);
}

.indicator-dot.active {
  background: linear-gradient(135deg, var(--primary-500), var(--terracotta-500));
  width: 28px;
  border-radius: 5px;
}

/* 加载状态 */
.carousel-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  color: var(--text-secondary);
}

.loading-icon {
  animation: spin 1s linear infinite;
  color: var(--primary-500);
}

/* 空状态 */
.carousel-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  color: var(--text-tertiary);
}

/* ============================================
   Animations
   ============================================ */
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

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  50% {
    transform: translateY(-12px) rotate(5deg);
  }
}

@keyframes blobFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.1);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.9);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* ============================================
   Responsive Design
   ============================================ */
@media (max-width: 1024px) {
  .carousel-container {
    height: 350px;
  }

  .carousel-3d {
    width: 250px;
    height: 250px;
  }

  .carousel-item {
    width: 230px;
    height: 230px;
    margin-left: -115px;
    margin-top: -115px;
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 0 var(--space-4) var(--space-8);
  }

  .hero-section {
    padding: var(--space-8) var(--space-4);
    border-radius: 24px;
  }

  .hero-title {
    font-size: var(--text-2xl);
  }

  .intro-card {
    padding: var(--space-6);
  }

  .intro-text {
    font-size: var(--text-base);
  }

  .daily-text-card {
    padding: var(--space-6);
  }

  .daily-highlights {
    grid-template-columns: 1fr;
  }

  .carousel-container {
    height: 300px;
  }

  .management-entry {
    padding: 8px 14px;
    top: 12px;
    right: 12px;
  }

  .entry-text {
    display: none;
  }

  .entry-icon {
    font-size: 16px;
  }

  .carousel-3d {
    width: 200px;
    height: 200px;
  }

  .carousel-item {
    width: 180px;
    height: 180px;
    margin-left: -90px;
    margin-top: -90px;
  }

  .carousel-btn {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }

  .floating-elements {
    display: none;
  }
}

@media (max-width: 480px) {
  .intro-features {
    flex-direction: column;
  }

  .feature-tag {
    justify-content: center;
  }
}
</style>
