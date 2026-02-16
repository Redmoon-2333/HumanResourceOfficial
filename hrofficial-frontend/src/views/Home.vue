<script setup lang="ts">
import Layout from '@/components/Layout.vue'
import OrganicBlob from '@/components/OrganicBlob.vue'
import SparkleEffect from '@/components/SparkleEffect.vue'
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
  Edit,
  Loading
} from '@element-plus/icons-vue'
import { getActiveImages, type DailyImage } from '@/api/dailyImage'
import { ElMessage } from 'element-plus'
import { getFullImageUrl } from '@/utils/image'

const userStore = useUserStore()
const router = useRouter()
const heroVisible = ref(false)

// 3D轮播图相关状态
const carouselRef = ref<HTMLElement | null>(null)
const currentIndex = ref(0)
const isAutoPlaying = ref(true)
const isTransitioning = ref(false)
let autoPlayTimer: ReturnType<typeof setInterval> | null = null

// 触摸滑动状态
const touchStartX = ref(0)
const touchEndX = ref(0)

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

onMounted(() => {
  setTimeout(() => {
    heroVisible.value = true
  }, 100)
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
      // 处理图片URL，使用工具函数确保完整URL
      carouselImages.value = res.data
        .map((img: any) => ({
          ...img,
          imageUrl: getFullImageUrl(img.imageUrl)
        }))
        .sort((a: any, b: any) => a.sortOrder - b.sortOrder)
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
  // 使用工具函数处理默认图片URL
  carouselImages.value = [
    { imageId: 1, imageUrl: getFullImageUrl('/images/daily/life1.jpg'), title: '团建聚餐', description: '美食与欢笑', sortOrder: 1, isActive: true, createTime: '', updateTime: '' },
    { imageId: 2, imageUrl: getFullImageUrl('/images/daily/life2.jpg'), title: '生日庆祝', description: '温馨的祝福', sortOrder: 2, isActive: true, createTime: '', updateTime: '' },
    { imageId: 3, imageUrl: getFullImageUrl('/images/daily/life3.jpg'), title: '桌游时光', description: '欢乐互动', sortOrder: 3, isActive: true, createTime: '', updateTime: '' },
    { imageId: 4, imageUrl: getFullImageUrl('/images/daily/life4.jpg'), title: '例会惊喜', description: '奶茶相伴', sortOrder: 4, isActive: true, createTime: '', updateTime: '' },
    { imageId: 5, imageUrl: getFullImageUrl('/images/daily/life5.jpg'), title: '树洞交流', description: '倾听与分享', sortOrder: 5, isActive: true, createTime: '', updateTime: '' },
    { imageId: 6, imageUrl: getFullImageUrl('/images/daily/life6.jpg'), title: '小屋聚会', description: '温馨时光', sortOrder: 6, isActive: true, createTime: '', updateTime: '' }
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

// 计算每个图片的3D变换（位置固定，由 track 整体旋转）
const getItemTransform = (index: number) => {
  if (totalImages.value === 0) return 'rotateY(0deg) translateZ(280px)'
  const angle = (360 / totalImages.value) * index
  const radius = 280
  const scale = getItemScale(index)
  // 缩放放在最后，避免影响旋转和位移
  return `rotateY(${angle}deg) translateZ(${radius}px) scale(${scale})`
}

// 计算每个图片相对于正面的角度（考虑 track 整体旋转）
const getNormalizedAngle = (index: number): number => {
  if (totalImages.value === 0) return 0
  // 图片原始位置 - track 旋转角度 = 图片当前实际位置
  const angle = ((360 / totalImages.value) * index - currentIndex.value * (360 / totalImages.value)) % 360
  return angle < 0 ? angle + 360 : angle
}

// 计算每个图片的透明度
const getItemOpacity = (index: number) => {
  if (totalImages.value === 0) return 0
  const normalizedAngle = getNormalizedAngle(index)
  // 主图片（正面±30度）完全不透明，其他图片统一半透明
  if (normalizedAngle <= 30 || normalizedAngle >= 330) return 1
  return 0.35
}

// 计算每个图片的缩放
const getItemScale = (index: number) => {
  if (totalImages.value === 0) return 1
  const normalizedAngle = getNormalizedAngle(index)
  // 主图片明显放大，其他图片统一较小尺寸
  if (normalizedAngle <= 30 || normalizedAngle >= 330) return 1.2
  return 0.7
}

// 触摸滑动处理
const handleTouchStart = (e: TouchEvent) => {
  if (e.touches[0]) {
    touchStartX.value = e.touches[0].clientX
  }
  stopAutoPlay()
}

const handleTouchMove = (e: TouchEvent) => {
  if (e.touches[0]) {
    touchEndX.value = e.touches[0].clientX
  }
}

const handleTouchEnd = () => {
  const touchDiff = touchStartX.value - touchEndX.value
  // 滑动距离阈值（像素）
  const swipeThreshold = 50
  
  if (Math.abs(touchDiff) > swipeThreshold) {
    if (touchDiff > 0) {
      // 向左滑动 -> 下一张
      nextSlide()
    } else {
      // 向右滑动 -> 上一张
      prevSlide()
    }
  }
  resumeAutoPlay()
}

// 处理图片加载错误 - 防止无限循环
const handleImageError = (event: Event) => {
  const img = event.target as HTMLImageElement
  // 如果已经尝试过占位图，则隐藏图片
  if (img.src.includes('data:image')) {
    img.style.display = 'none'
    return
  }
  // 第一次失败，使用 base64 占位图
  // Why: 避免 404 请求，使用内联 SVG 作为占位图
  img.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNDAwIiBoZWlnaHQ9IjMwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KICA8cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSIjZjVmNWY1Ii8+CiAgPHRleHQgeD0iNTAlIiB5PSI1MCUiIGZvbnQtZmFtaWx5PSJBcmlhbCIgZm9udC1zaXplPSIxOCIgZmlsbD0iIzk5OSIgdGV4dC1hbmNob3I9Im1pZGRsZSIgZHk9Ii4zZW0iPuaXoOWbvjwvdGV4dD4KPC9zdmc+'
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
      <!-- Hero Section - 温暖拥抱主题 (首页 - 暖橙红 #E85A3C) -->
      <section class="hero-section hero-organic page-home" :class="{ 'hero-visible': heroVisible }">
        <!-- 有机Blob装饰层 -->
        <div class="hero-organic__blobs">
          <!-- Blob 1: 右上角，主色暖橙红 - 加深色彩 -->
          <OrganicBlob
            size="large"
            color="#E85A3C"
            color-light="#FF6B4A"
            :position="{ top: '-100px', right: '-60px' }"
            :delay="0"
            :opacity="0.55"
            glow
          />
          <!-- Blob 2: 右下角，次色活力橙 - 加深色彩 -->
          <OrganicBlob
            size="medium"
            color="#F97316"
            color-light="#FF8A70"
            :position="{ bottom: '-60px', right: '100px' }"
            :delay="-3"
            :opacity="0.5"
            float
          />
          <!-- Blob 3: 左下角，点缀色琥珀金 - 加深色彩 -->
          <OrganicBlob
            size="small"
            color="#F59E0B"
            color-light="#FDBA74"
            :position="{ bottom: '-20px', left: '-20px' }"
            :delay="-6"
            :opacity="0.45"
          />
          <!-- 网格纹理 -->
          <div class="grid-pattern"></div>
        </div>

        <!-- Sparkle粒子层 -->
        <SparkleEffect :count="12" color="rgba(255, 255, 255, 0.95)" />

        <!-- 内容层 - 左对齐布局 -->
        <div class="hero-organic__content hero-organic__content--left">
          <div class="welcome-badge animate-hero-scale stagger-delay-1">
            <el-icon><Sunny /></el-icon>
            <span>{{ greeting }}</span>
          </div>
          <h1 class="hero-title hero-title-universal animate-hero-scale stagger-delay-2">
            <span class="name-highlight">{{ userStore.userInfo?.name || '访客' }}</span>
            <span class="welcome-text">，欢迎回来！</span>
          </h1>
          <p class="hero-subtitle animate-hero-scale stagger-delay-3">
            这里是人力资源中心管理系统，为您提供活动管理、资料共享、AI助手等服务
          </p>

          <div class="floating-elements">
            <div class="float-item item-1 animate-breathe">
              <el-icon><StarFilled /></el-icon>
            </div>
            <div class="float-item item-2 animate-breathe">
              <el-icon><Trophy /></el-icon>
            </div>
            <div class="float-item item-3 animate-breathe">
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
          @touchstart="handleTouchStart"
          @touchmove="handleTouchMove"
          @touchend="handleTouchEnd"
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
                    opacity: getItemOpacity(index)
                  }"
                >
                  <div class="carousel-image-wrapper">
                    <img
                      :src="image.imageUrl"
                      :alt="image.title"
                      loading="lazy"
                      class="carousel-image"
                      @error="handleImageError($event)"
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
   Home Page - 温暖拥抱主题 (Warm Embrace)
   设计理念：有机流动 + 光影层次 + 微交互叙事
   ============================================ */

.home-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--space-6) var(--space-12);
}

/* ============================================
   Hero Section - 沉浸式渐变 + 有机装饰
   ============================================ */
.hero-section {
  position: relative;
  padding: var(--space-8) var(--space-8) var(--space-12);
  margin: var(--space-6);
  margin-bottom: 0;
  border-radius: 32px;
  overflow: hidden;
  background: linear-gradient(135deg, #FFF5F0 0%, #FFE8E0 50%, #FFE0D5 100%);
  transition: background 0.8s var(--easing-soft);
}

/* 左对齐布局 */
.hero-organic__content--left {
  text-align: left;
  padding-left: 48px;
  padding-top: 24px;
}

.hero-organic__content--left .welcome-badge {
  margin-left: 0;
}

.hero-organic__content--left .hero-title {
  text-align: left;
}

.hero-organic__content--left .hero-subtitle {
  text-align: left;
  margin-left: 0;
  margin-right: auto;
}

.hero-section::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 80% 50% at 50% -20%, rgba(232, 90, 60, 0.22) 0%, transparent 50%),
    radial-gradient(ellipse 60% 40% at 100% 100%, rgba(232, 90, 60, 0.15) 0%, transparent 50%);
  pointer-events: none;
  z-index: 0;
}

.hero-bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

/* 有机Blob样式由 OrganicBlob 组件提供 */

.hero-bg-decoration .grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle at 1px 1px, rgba(232, 90, 60, 0.12) 1px, transparent 0);
  background-size: 40px 40px;
  mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 20%, transparent 70%);
  -webkit-mask-image: radial-gradient(ellipse 80% 80% at 50% 50%, black 20%, transparent 70%);
}

/* Sparkle样式由 SparkleEffect 组件提供 */

.hero-content {
  position: relative;
  z-index: 1;
}

.hero-center {
  text-align: center;
}

.welcome-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  background: linear-gradient(135deg, #E85A3C, #FF6B4A);
  color: white;
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  margin-bottom: var(--space-6);
  box-shadow: 0 4px 16px rgba(232, 90, 60, 0.3), 0 0 20px rgba(232, 90, 60, 0.15);
  position: relative;
  overflow: hidden;
}

.welcome-badge::before {
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

.welcome-badge .el-icon {
  animation: iconPulse 2s ease-in-out infinite;
}

@keyframes iconPulse {
  0%, 100% { transform: scale(1) rotate(0deg); }
  50% { transform: scale(1.1) rotate(15deg); }
}

.hero-title {
  font-size: clamp(1.875rem, 4vw, 2.5rem);
  line-height: 1.2;
  margin-bottom: var(--space-4);
}

.name-highlight {
  background: linear-gradient(135deg, #E85A3C 0%, #FF6B4A 50%, #E85A3C 100%);
  background-size: 200% auto;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: gradientShift 4s ease infinite;
  display: inline-block;
}

@keyframes gradientShift {
  0%, 100% { background-position: 0% center; }
  50% { background-position: 100% center; }
}

.welcome-text {
  color: var(--text-primary);
}

.hero-subtitle {
  color: var(--text-secondary);
  font-size: var(--text-lg);
  max-width: 600px;
  margin: 0 auto;
  line-height: 1.7;
}

/* Floating Elements - 增强版 */
.floating-elements {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 0;
}

.hero-content {
  position: relative;
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
  border-radius: 12px;
  font-size: 32px;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  filter: drop-shadow(0 4px 8px rgba(255, 107, 74, 0.25));
}

.float-item::before {
  content: '';
  position: absolute;
  inset: -4px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(232, 90, 60, 0.15), rgba(232, 90, 60, 0.1));
  opacity: 0;
  z-index: -1;
  transition: opacity 0.3s ease;
  filter: blur(8px);
}

.float-item .el-icon {
  animation: iconBounceEnhanced 3s ease-in-out infinite;
}

@keyframes iconBounceEnhanced {
  0%, 100% {
    transform: translateY(0) scale(1);
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
  }
  25% {
    transform: translateY(-3px) scale(1.05);
  }
  50% {
    transform: translateY(-6px) scale(1.08);
    filter: drop-shadow(0 6px 12px rgba(232, 90, 60, 0.2));
  }
  75% {
    transform: translateY(-3px) scale(1.05);
  }
}

/* 浮动元素 - 整齐排列在右侧 */
.item-1 {
  top: 15%;
  right: 8%;
  animation-delay: 0s;
  color: #E85A3C;
}

.item-2 {
  top: 45%;
  right: 5%;
  animation-delay: -1s;
  color: #F97316;
}

.item-3 {
  bottom: 15%;
  right: 10%;
  animation-delay: -2s;
  color: #F59E0B;
}

/* 中等屏幕调整浮动元素位置 */
@media (max-width: 1024px) {
  .item-1 {
    top: 3%;
    left: 2%;
  }
  
  .item-2 {
    top: 3%;
    right: 2%;
  }
  
  .item-3 {
    bottom: 3%;
    right: 3%;
  }
}

/* ============================================
   Section Common Styles - 增强版
   ============================================ */
section {
  margin-bottom: var(--space-10);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
  position: relative;
}

.section-header::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 0;
  width: 60px;
  height: 3px;
  background: linear-gradient(90deg, #E85A3C, transparent);
  border-radius: 2px;
}

.section-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  font-size: var(--text-xl);
  position: relative;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.section-icon::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.4), transparent);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.section-header:hover .section-icon::before {
  opacity: 1;
}

.section-header:hover .section-icon {
  transform: scale(1.05) rotate(-3deg);
}

.intro-icon {
  background: linear-gradient(135deg, rgba(232, 90, 60, 0.15), rgba(232, 90, 60, 0.05));
  color: #E85A3C;
  box-shadow: 0 4px 16px rgba(232, 90, 60, 0.15);
}

.daily-icon {
  background: linear-gradient(135deg, rgba(249, 115, 22, 0.15), rgba(249, 115, 22, 0.05));
  color: #F97316;
  box-shadow: 0 4px 16px rgba(249, 115, 22, 0.15);
}

.section-title-group {
  flex: 1;
}

.section-title {
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin: 0 0 var(--space-1) 0;
  transition: color 0.3s ease;
}

.section-header:hover .section-title {
  color: #E85A3C;
}

.section-subtitle {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin: 0;
  letter-spacing: 0.5px;
}

/* ============================================
   部门简介板块 - 增强版
   ============================================ */
.intro-section {
  animation: fadeInUp 0.6s ease forwards;
  margin-top: var(--space-8);
}

.intro-card {
  position: relative;
  padding: var(--space-8);
  background: white;
  border-radius: 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.intro-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #E85A3C, #F97316, #F59E0B);
  background-size: 200% 100%;
  animation: gradientFlow 4s linear infinite;
}

.intro-card:hover {
  transform: translateY(-6px);
  box-shadow: 
    0 12px 48px rgba(0, 0, 0, 0.1),
    0 0 0 1px rgba(232, 90, 60, 0.1);
}

.intro-card:hover::before {
  animation-duration: 2s;
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
  transition: all 0.5s ease;
}

.intro-card:hover .deco-circle {
  opacity: 0.15;
  transform: scale(1.1);
}

.circle-1 {
  width: 150px;
  height: 150px;
  background: linear-gradient(135deg, #E85A3C, #FF6B4A);
  top: -50px;
  right: -50px;
  animation: decoFloat 8s ease-in-out infinite;
}

.circle-2 {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, #F97316, #F59E0B);
  top: 30px;
  right: 60px;
  animation: decoFloat 6s ease-in-out infinite reverse;
}

@keyframes decoFloat {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(-10px) scale(1.05); }
}

.deco-line {
  position: absolute;
  width: 2px;
  height: 80px;
  background: linear-gradient(to bottom, rgba(232, 90, 60, 0.4), transparent);
  top: 20px;
  right: 100px;
  transform: rotate(15deg);
  animation: decoLinePulse 3s ease-in-out infinite;
}

@keyframes decoLinePulse {
  0%, 100% { opacity: 0.3; height: 80px; }
  50% { opacity: 0.6; height: 90px; }
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
  background: linear-gradient(135deg, rgba(232, 90, 60, 0.08), rgba(232, 90, 60, 0.15));
  border: 1px solid rgba(232, 90, 60, 0.2);
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  color: #C4472E;
  font-weight: var(--font-medium);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  cursor: default;
  position: relative;
  overflow: hidden;
}

.feature-tag::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, var(--coral-100), var(--amber-100));
  opacity: 0;
  transition: opacity 0.3s ease;
}

.feature-tag:hover {
  background: linear-gradient(135deg, var(--coral-100), var(--coral-200));
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 6px 20px rgba(255, 107, 74, 0.2);
}

.feature-tag:hover::before {
  opacity: 1;
}

.feature-tag .el-icon {
  font-size: 16px;
  position: relative;
  z-index: 1;
  transition: transform 0.3s ease;
}

.feature-tag:hover .el-icon {
  transform: scale(1.2) rotate(10deg);
}

.feature-tag span {
  position: relative;
  z-index: 1;
}

/* ============================================
   我们的日常板块 - 增强版
   ============================================ */
.daily-section {
  margin-top: var(--space-12);
  animation: fadeInUp 0.6s ease 0.2s forwards;
  opacity: 0;
}

/* 文字卡片 - 增强渐变背景 */
.daily-text-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: var(--space-8);
  background: linear-gradient(135deg,
    rgba(255, 107, 74, 0.04) 0%,
    rgba(255, 107, 74, 0.06) 50%,
    rgba(255, 107, 74, 0.03) 100%);
  border-radius: 24px;
  border: 1px solid rgba(255, 107, 74, 0.12);
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;
}

.daily-text-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #FF8A70, #FF8A70, #FF8A70);
  background-size: 200% 100%;
  animation: gradientFlow 3s linear infinite;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.daily-text-card:hover {
  border-color: rgba(255, 107, 74, 0.25);
  box-shadow: 
    0 8px 32px rgba(255, 107, 74, 0.1),
    0 0 0 1px rgba(255, 107, 74, 0.08) inset;
  transform: translateY(-4px);
}

.daily-text-card:hover::before {
  opacity: 1;
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
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  overflow: hidden;
}

.highlight-item::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--gradient-primary);
  transform: scaleY(0);
  transition: transform 0.3s ease;
}

.highlight-item:hover {
  transform: translateY(-4px) translateX(4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.highlight-item:hover::before {
  transform: scaleY(1);
}

.highlight-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--gradient-primary);
  border-radius: var(--radius-md);
  color: white;
  font-size: 20px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(255, 107, 74, 0.25);
}

.highlight-item:hover .highlight-icon {
  transform: scale(1.1) rotate(-5deg);
  box-shadow: 0 6px 16px rgba(255, 107, 74, 0.35);
}

.highlight-item span {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--text-primary);
  transition: color 0.3s ease;
}

.highlight-item:hover span {
  color: #E85A3C;
}

/* ============================================
   3D轮播图样式 - 增强版
   ============================================ */
.carousel-container {
  position: relative;
  height: 380px;
  perspective: 1200px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: var(--space-10);
  background: linear-gradient(135deg,
    rgba(255, 107, 74, 0.04) 0%,
    rgba(255, 107, 74, 0.06) 50%,
    rgba(255, 107, 74, 0.03) 100%);
  border-radius: 24px;
  border: 1px solid rgba(255, 107, 74, 0.12);
  overflow: hidden;
}

.carousel-container::before {
  content: '';
  position: absolute;
  inset: 0;
  background: 
    radial-gradient(ellipse 100% 60% at 50% 0%, rgba(255, 107, 74, 0.08) 0%, transparent 50%),
    radial-gradient(ellipse 80% 50% at 0% 50%, rgba(255, 107, 74, 0.05) 0%, transparent 50%),
    radial-gradient(ellipse 80% 50% at 100% 50%, rgba(255, 107, 74, 0.05) 0%, transparent 50%);
  pointer-events: none;
}

/* 管理入口按钮 - 精致悬浮风格增强版 */
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
  backdrop-filter: blur(8px);
}

.management-entry:hover {
  transform: translateY(-3px) scale(1.02);
  border-color: rgba(255, 107, 74, 0.4);
  box-shadow: 
    0 8px 28px rgba(255, 107, 74, 0.25),
    0 0 0 1px rgba(255, 107, 74, 0.1) inset;
}

.management-entry:active {
  transform: translateY(-1px) scale(0.98);
}

.entry-glow {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, 
    rgba(255, 107, 74, 0.15) 0%, 
    rgba(255, 107, 74, 0.15) 100%);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.management-entry:hover .entry-glow {
  opacity: 1;
}

.entry-icon {
  font-size: 18px;
  color: #FF6B4A;
  transition: transform 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  z-index: 1;
}

.management-entry:hover .entry-icon {
  transform: rotate(120deg) scale(1.1);
}

.entry-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  white-space: nowrap;
  position: relative;
  z-index: 1;
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
  background: #FF8A70;
  border-radius: 50%;
  opacity: 0;
}

.management-entry:hover .particle {
  animation: particleFloatEnhanced 1.5s ease-out infinite;
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
  background: #FF8A70;
}

.particle:nth-child(3) {
  bottom: 25%;
  left: 30%;
  animation-delay: 0.6s;
  background: #FF8A70;
}

@keyframes particleFloatEnhanced {
  0% {
    opacity: 0;
    transform: translateY(0) scale(0) rotate(0deg);
  }
  30% {
    opacity: 0.8;
    transform: translateY(-8px) scale(1) rotate(90deg);
  }
  100% {
    opacity: 0;
    transform: translateY(-20px) scale(0.5) rotate(180deg);
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
  width: 240px;
  height: 240px;
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
  width: 220px;
  height: 220px;
  left: 50%;
  top: 50%;
  margin-left: -110px;
  margin-top: -110px;
  transform-style: preserve-3d;
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.carousel-image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.3),
    0 0 0 1px rgba(255, 255, 255, 0.1) inset;
  background: linear-gradient(135deg, var(--coral-100), var(--amber-100));
  transition: box-shadow 0.4s ease;
}

.carousel-item:hover .carousel-image-wrapper {
  box-shadow: 
    0 25px 70px rgba(0, 0, 0, 0.35),
    0 0 30px rgba(255, 107, 74, 0.2),
    0 0 0 1px rgba(255, 255, 255, 0.2) inset;
}

.carousel-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s ease;
}

.carousel-item:hover .carousel-image {
  transform: scale(1.08);
}

.carousel-image-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: var(--space-4);
  background: linear-gradient(to top, rgba(0, 0, 0, 0.8) 0%, rgba(0, 0, 0, 0.4) 50%, transparent 100%);
  color: white;
  transform: translateY(100%);
  transition: transform 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.carousel-item:hover .carousel-image-overlay {
  transform: translateY(0);
}

.carousel-image-overlay h4 {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  margin: 0 0 var(--space-1) 0;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
}

.carousel-image-overlay p {
  font-size: var(--text-sm);
  margin: 0;
  opacity: 0.9;
}

/* 轮播控制按钮 - 增强版 + 触摸优化 */
.carousel-btn {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 48px;
  height: 48px;
  min-width: 48px;
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.95);
  border: none;
  border-radius: 50%;
  box-shadow: 
    0 4px 16px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.8) inset;
  cursor: pointer;
  color: var(--text-primary);
  font-size: 20px;
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  z-index: 10;
  backdrop-filter: blur(8px);
  -webkit-tap-highlight-color: transparent;
  touch-action: manipulation;
}

.carousel-btn:hover {
  background: var(--gradient-primary);
  color: white;
  transform: translateY(-50%) scale(1.15);
  box-shadow: 
    var(--shadow-coral-lg),
    0 0 20px rgba(255, 107, 74, 0.3);
}

.carousel-btn:active {
  transform: translateY(-50%) scale(1.05);
}

.prev-btn {
  left: 24px;
}

.next-btn {
  right: 24px;
}

/* 指示器 - 增强版 + 移动端触摸优化 */
.carousel-indicators {
  position: absolute;
  bottom: 16px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  background: rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-full);
  backdrop-filter: blur(8px);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.indicator-dot {
  width: 44px;
  height: 44px;
  min-width: 44px;
  min-height: 44px;
  border-radius: 50%;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.indicator-dot::before {
  content: '';
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--border-medium);
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.indicator-dot:hover::before {
  background: var(--coral-300);
  transform: scale(1.3);
}

.indicator-dot.active::before {
  background: var(--gradient-primary);
  width: 28px;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(255, 107, 74, 0.3);
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
  color: #FF6B4A;
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
   Animations - 增强版动画库
   ============================================ */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
    filter: blur(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
    filter: blur(0);
  }
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) rotate(0deg);
  }
  33% {
    transform: translateY(-8px) rotate(2deg);
  }
  66% {
    transform: translateY(-16px) rotate(-2deg);
  }
}

@keyframes blobFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  33% {
    transform: translate(30px, -30px) scale(1.05);
  }
  66% {
    transform: translate(-20px, 20px) scale(0.95);
  }
}

@keyframes gradientFlow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.05);
  }
}

@keyframes shimmer {
  0% {
    background-position: -200% center;
  }
  100% {
    background-position: 200% center;
  }
}

/* ============================================
   Responsive Design - 增强版移动端优化
   ============================================ */
@media (max-width: 1024px) {
  .carousel-container {
    height: 320px;
  }

  .carousel-3d {
    width: 180px;
    height: 180px;
  }

  .carousel-item {
    width: 160px;
    height: 160px;
    margin-left: -80px;
    margin-top: -80px;
  }
  
  .hero-section .blob-1 {
    width: 300px;
    height: 300px;
  }
  
  .hero-section .blob-2 {
    width: 200px;
    height: 200px;
  }
  
  .hero-section .blob-3 {
    width: 150px;
    height: 150px;
  }

  .hero-organic__content--left {
    padding-left: var(--space-4);
    padding-top: var(--space-4);
  }
}

@media (max-width: 768px) {
  .home-page {
    padding: 0 var(--space-4) var(--space-8);
  }

  .hero-section {
    padding: var(--space-6) var(--space-4) var(--space-8);
    border-radius: 24px;
    margin: var(--space-4);
    margin-bottom: 0;
  }
  
  .hero-section::before {
    background: 
      radial-gradient(ellipse 100% 40% at 50% -10%, rgba(255, 107, 74, 0.12) 0%, transparent 50%);
  }

  .hero-organic__content--left {
    padding-left: 0;
    padding-top: var(--space-2);
    text-align: center;
  }

  .hero-organic__content--left .welcome-badge {
    margin-left: auto;
    margin-right: auto;
  }

  .hero-organic__content--left .hero-title {
    text-align: center;
  }

  .hero-organic__content--left .hero-subtitle {
    text-align: center;
    margin-left: auto;
    margin-right: auto;
  }
  
  .hero-title {
    font-size: clamp(1.375rem, 6vw, 1.75rem);
    line-height: 1.3;
  }

  .hero-subtitle {
    font-size: var(--text-base);
    line-height: 1.8;
  }

  .welcome-badge {
    font-size: var(--text-sm);
    padding: var(--space-2) var(--space-3);
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }

  .section-icon {
    width: 44px;
    height: 44px;
    font-size: var(--text-lg);
  }

  .intro-card {
    padding: var(--space-5);
  }

  .intro-text {
    font-size: var(--text-base);
    line-height: 2;
  }
  
  .intro-decoration {
    width: 120px;
    height: 120px;
  }
  
  .circle-1 {
    width: 80px;
    height: 80px;
  }
  
  .circle-2 {
    width: 60px;
    height: 60px;
  }

  .intro-features {
    gap: var(--space-2);
  }

  .feature-tag {
    padding: var(--space-3) var(--space-4);
    min-height: 44px;
  }

  .daily-text-card {
    padding: var(--space-5);
  }

  .daily-text-content p {
    font-size: var(--text-base);
    line-height: 2;
  }

  .daily-highlights {
    grid-template-columns: 1fr;
    gap: var(--space-3);
  }

  .highlight-item {
    padding: var(--space-3) var(--space-4);
    min-height: 56px;
  }

  .highlight-icon {
    width: 44px;
    height: 44px;
    font-size: 20px;
  }

  .carousel-container {
    height: 320px;
    margin-top: var(--space-8);
  }

  .management-entry {
    padding: 10px 16px;
    top: 12px;
    right: 12px;
    min-height: 44px;
  }

  .entry-text {
    display: none;
  }

  .entry-icon {
    font-size: 18px;
  }

  .carousel-3d {
    width: 180px;
    height: 180px;
  }

  .carousel-item {
    width: 160px;
    height: 160px;
    margin-left: -80px;
    margin-top: -80px;
  }

  .carousel-btn {
    width: 48px;
    height: 48px;
    font-size: 20px;
  }
  
  .prev-btn {
    left: 8px;
  }
  
  .next-btn {
    right: 8px;
  }

  .carousel-indicators {
    padding: var(--space-2) var(--space-4);
    bottom: 20px;
  }

  .indicator-dot {
    width: 44px;
    height: 44px;
    min-width: 44px;
    min-height: 44px;
  }

  .indicator-dot::before {
    inset: 0;
  }

  .floating-elements {
    display: none;
  }
  
  .section-header::after {
    width: 40px;
  }
}

@media (max-width: 480px) {
  .home-page {
    padding: 0 var(--space-3) var(--space-6);
  }

  .hero-section {
    padding: var(--space-5) var(--space-4) var(--space-7);
    margin: var(--space-3);
    border-radius: 20px;
  }

  .hero-title {
    font-size: clamp(1.25rem, 7vw, 1.5rem);
  }

  .hero-subtitle {
    font-size: var(--text-sm);
  }

  .intro-card {
    padding: var(--space-4);
    border-radius: 20px;
  }

  .intro-features {
    flex-direction: column;
  }

  .feature-tag {
    justify-content: center;
    width: 100%;
  }

  .daily-text-card {
    padding: var(--space-4);
    border-radius: 20px;
  }

  .carousel-container {
    height: 300px;
    border-radius: 20px;
  }

  .carousel-3d {
    width: 160px;
    height: 160px;
  }

  .carousel-item {
    width: 140px;
    height: 140px;
    margin-left: -70px;
    margin-top: -70px;
  }

  .carousel-btn {
    width: 48px;
    height: 48px;
  }

  .prev-btn {
    left: 4px;
  }

  .next-btn {
    right: 4px;
  }

  .indicator-dot {
    width: 44px;
    height: 44px;
  }
  
  .section-title {
    font-size: var(--text-xl);
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
  
  .hero-bg-decoration .blob,
  .sparkle,
  .float-item .el-icon,
  .welcome-badge::before,
  .welcome-badge .el-icon,
  .name-highlight,
  .intro-card::before,
  .deco-circle,
  .deco-line,
  .daily-text-card::before,
  .feature-tag,
  .highlight-item::before,
  .highlight-icon,
  .carousel-image-wrapper,
  .carousel-image,
  .carousel-image-overlay,
  .carousel-btn,
  .indicator-dot,
  .management-entry .entry-icon,
  .particle {
    animation: none !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
