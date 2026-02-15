<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, Back, Compass } from '@element-plus/icons-vue'

const router = useRouter()
const isVisible = ref(false)
const mousePosition = ref({ x: 0, y: 0 })

onMounted(() => {
  setTimeout(() => {
    isVisible.value = true
  }, 100)

  document.addEventListener('mousemove', handleMouseMove)
})

const handleMouseMove = (e: MouseEvent) => {
  mousePosition.value = {
    x: (e.clientX / window.innerWidth - 0.5) * 20,
    y: (e.clientY / window.innerHeight - 0.5) * 20
  }
}

const goHome = () => {
  router.push('/home')
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="not-found-page" :class="{ visible: isVisible }">
    <div class="bg-decoration">
      <div class="gradient-layer"></div>
      <div class="grid-pattern"></div>
      <div class="floating-shapes">
        <div class="shape shape-1"></div>
        <div class="shape shape-2"></div>
        <div class="shape shape-3"></div>
        <div class="shape shape-4"></div>
        <div class="shape shape-5"></div>
      </div>
    </div>

    <div class="content-wrapper" :style="{ transform: `translate(${mousePosition.x}px, ${mousePosition.y}px)` }">
      <div class="error-code">
        <span class="digit digit-1">4</span>
        <div class="digit-center">
          <div class="compass-container">
            <el-icon :size="80" class="compass-icon"><Compass /></el-icon>
            <div class="compass-ring"></div>
          </div>
        </div>
        <span class="digit digit-2">4</span>
      </div>

      <h1 class="error-title">
        <span class="title-line title-line-1">哎呀，迷路了</span>
        <span class="title-line title-line-2">Oops, Lost in Space</span>
      </h1>

      <p class="error-description">
        看起来你探索到了一个未知的领域<br />
        这个页面可能已经移动或不存在
      </p>

      <div class="action-buttons">
        <button class="btn btn-primary" @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          <span>返回首页</span>
        </button>
        <button class="btn btn-secondary" @click="goBack">
          <el-icon><Back /></el-icon>
          <span>返回上页</span>
        </button>
      </div>

      <div class="suggestions">
        <p class="suggestion-title">或许你可以：</p>
        <ul class="suggestion-list">
          <li>检查网址是否正确</li>
          <li>返回首页重新开始</li>
          <li>联系管理员报告问题</li>
        </ul>
      </div>
    </div>

    <div class="particles">
      <div v-for="i in 20" :key="i" class="particle" :style="{
        '--delay': `${Math.random() * 5}s`,
        '--duration': `${5 + Math.random() * 10}s`,
        '--x': `${Math.random() * 100}%`,
        '--size': `${2 + Math.random() * 4}px`
      }"></div>
    </div>
  </div>
</template>

<style scoped>
.not-found-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, var(--bg-warm) 0%, var(--coral-50) 50%, var(--amber-50) 100%);
  opacity: 0;
  transform: scale(0.95);
  transition: all 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.not-found-page.visible {
  opacity: 1;
  transform: scale(1);
}

.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.gradient-layer {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at 30% 20%,
    rgba(255, 107, 74, 0.15) 0%,
    transparent 50%
  ),
  radial-gradient(
    ellipse at 70% 80%,
    rgba(245, 158, 11, 0.12) 0%,
    transparent 50%
  ),
  radial-gradient(
    ellipse at 50% 50%,
    rgba(244, 63, 94, 0.08) 0%,
    transparent 60%
  );
}

.grid-pattern {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 107, 74, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 107, 74, 0.03) 1px, transparent 1px);
  background-size: 60px 60px;
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove {
  0% { transform: translate(0, 0); }
  100% { transform: translate(60px, 60px); }
}

.floating-shapes {
  position: absolute;
  inset: 0;
}

.shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.4;
  animation: shapeFloat 15s ease-in-out infinite;
}

.shape-1 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, var(--coral-400), var(--coral-500));
  top: -100px;
  right: -50px;
  animation-delay: 0s;
}

.shape-2 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, var(--amber-400), var(--amber-500));
  bottom: -80px;
  left: -60px;
  animation-delay: -5s;
}

.shape-3 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, var(--rose-400), var(--rose-500));
  top: 40%;
  left: 10%;
  animation-delay: -10s;
}

.shape-4 {
  width: 180px;
  height: 180px;
  background: linear-gradient(135deg, var(--terracotta-400), var(--terracotta-500));
  top: 20%;
  right: 20%;
  animation-delay: -7s;
}

.shape-5 {
  width: 150px;
  height: 150px;
  background: linear-gradient(135deg, var(--primary-400), var(--primary-500));
  bottom: 30%;
  right: 10%;
  animation-delay: -12s;
}

@keyframes shapeFloat {
  0%, 100% {
    transform: translate(0, 0) scale(1);
  }
  25% {
    transform: translate(30px, -30px) scale(1.1);
  }
  50% {
    transform: translate(-20px, 20px) scale(0.95);
  }
  75% {
    transform: translate(20px, 10px) scale(1.05);
  }
}

.content-wrapper {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: var(--space-8);
  transition: transform 0.1s ease-out;
}

.error-code {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.digit {
  font-size: 140px;
  font-weight: 900;
  line-height: 1;
  background: linear-gradient(135deg, var(--coral-500), var(--terracotta-500));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: digitBounce 2s ease-in-out infinite;
  text-shadow: 0 20px 40px rgba(255, 107, 74, 0.2);
}

.digit-1 {
  animation-delay: 0s;
}

.digit-2 {
  animation-delay: 0.2s;
}

@keyframes digitBounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-15px);
  }
}

.digit-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.compass-container {
  position: relative;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.compass-icon {
  color: var(--coral-500);
  animation: compassSpin 10s linear infinite;
}

@keyframes compassSpin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.compass-ring {
  position: absolute;
  inset: 0;
  border: 3px solid var(--coral-300);
  border-radius: 50%;
  animation: ringPulse 2s ease-in-out infinite;
}

@keyframes ringPulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.5;
  }
}

.error-title {
  margin-bottom: var(--space-6);
}

.title-line {
  display: block;
}

.title-line-1 {
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin-bottom: var(--space-2);
  animation: fadeInUp 0.6s ease forwards;
  animation-delay: 0.2s;
  opacity: 0;
}

.title-line-2 {
  font-size: var(--text-xl);
  font-weight: var(--font-medium);
  color: var(--text-tertiary);
  animation: fadeInUp 0.6s ease forwards;
  animation-delay: 0.4s;
  opacity: 0;
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

.error-description {
  font-size: var(--text-lg);
  color: var(--text-secondary);
  line-height: var(--leading-relaxed);
  margin-bottom: var(--space-10);
  animation: fadeInUp 0.6s ease forwards;
  animation-delay: 0.6s;
  opacity: 0;
}

.action-buttons {
  display: flex;
  gap: var(--space-4);
  justify-content: center;
  margin-bottom: var(--space-10);
  animation: fadeInUp 0.6s ease forwards;
  animation-delay: 0.8s;
  opacity: 0;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-4) var(--space-8);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: none;
}

.btn-primary {
  background: linear-gradient(135deg, var(--coral-500), var(--terracotta-500));
  color: white;
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.35);
}

.btn-primary:hover {
  transform: translateY(-3px) scale(1.02);
  box-shadow: 0 12px 32px rgba(255, 107, 74, 0.45);
}

.btn-secondary {
  background: white;
  color: var(--text-primary);
  border: 2px solid var(--border-medium);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.btn-secondary:hover {
  border-color: var(--coral-400);
  color: var(--coral-600);
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(255, 107, 74, 0.15);
}

.suggestions {
  animation: fadeInUp 0.6s ease forwards;
  animation-delay: 1s;
  opacity: 0;
}

.suggestion-title {
  font-size: var(--text-sm);
  color: var(--text-tertiary);
  margin-bottom: var(--space-3);
}

.suggestion-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  gap: var(--space-6);
  justify-content: center;
}

.suggestion-list li {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  position: relative;
  padding-left: var(--space-4);
}

.suggestion-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 6px;
  height: 6px;
  background: var(--coral-400);
  border-radius: 50%;
}

.particles {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.particle {
  position: absolute;
  left: var(--x);
  width: var(--size);
  height: var(--size);
  background: var(--coral-400);
  border-radius: 50%;
  opacity: 0;
  animation: particleRise var(--duration) ease-in-out var(--delay) infinite;
}

@keyframes particleRise {
  0% {
    opacity: 0;
    transform: translateY(100vh) scale(0);
  }
  10% {
    opacity: 0.6;
  }
  90% {
    opacity: 0.6;
  }
  100% {
    opacity: 0;
    transform: translateY(-100px) scale(1);
  }
}

@media (max-width: 768px) {
  .digit {
    font-size: 80px;
  }

  .compass-container {
    width: 80px;
    height: 80px;
  }

  .compass-icon {
    font-size: 50px !important;
  }

  .title-line-1 {
    font-size: var(--text-2xl);
  }

  .title-line-2 {
    font-size: var(--text-base);
  }

  .error-description {
    font-size: var(--text-base);
  }

  .action-buttons {
    flex-direction: column;
    align-items: center;
  }

  .btn {
    width: 100%;
    max-width: 200px;
    justify-content: center;
  }

  .suggestion-list {
    flex-direction: column;
    gap: var(--space-2);
  }
}
</style>
