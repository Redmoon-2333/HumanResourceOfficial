<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'

interface Particle {
  id: number
  x: number
  y: number
  size: number
  speedX: number
  speedY: number
  opacity: number
  color: string
}

interface Props {
  count?: number
  mobileCount?: number
  colors?: string[]
  minSize?: number
  maxSize?: number
  speed?: number
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  count: 20,
  mobileCount: 8,
  colors: () => ['#FF6B4A', '#F59E0B', '#FB7185', '#FFA07A', '#FFD93D'],
  minSize: 4,
  maxSize: 12,
  speed: 0.5,
  disabled: false
})

const particles = ref<Particle[]>([])
const containerRef = ref<HTMLElement | null>(null)
let animationId: number | null = null
let particleIdCounter = 0
const isMobile = ref(false)
const prefersReducedMotion = ref(false)

const effectiveCount = computed(() => {
  if (prefersReducedMotion.value) return 0
  return isMobile.value ? props.mobileCount : props.count
})

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

const checkPrefersReducedMotion = () => {
  prefersReducedMotion.value = window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

const createParticle = (): Particle => {
  const container = containerRef.value
  if (!container) return {
    id: particleIdCounter++,
    x: 0,
    y: 0,
    size: props.minSize,
    speedX: 0,
    speedY: 0,
    opacity: 0.3,
    color: props.colors[0] || '#FF6B4A'
  }
  
  return {
    id: particleIdCounter++,
    x: Math.random() * container.offsetWidth,
    y: Math.random() * container.offsetHeight,
    size: props.minSize + Math.random() * (props.maxSize - props.minSize),
    speedX: (Math.random() - 0.5) * props.speed * (isMobile.value ? 0.7 : 1),
    speedY: (Math.random() - 0.5) * props.speed * (isMobile.value ? 0.7 : 1),
    opacity: 0.3 + Math.random() * 0.4,
    color: props.colors[Math.floor(Math.random() * props.colors.length)] || '#FF6B4A'
  }
}

const initParticles = () => {
  if (props.disabled || prefersReducedMotion.value) {
    particles.value = []
    return
  }
  particles.value = Array.from({ length: effectiveCount.value }, createParticle)
}

const updateParticles = () => {
  if (props.disabled || prefersReducedMotion.value) {
    return
  }
  
  const container = containerRef.value
  if (!container) return
  
  const width = container.offsetWidth
  const height = container.offsetHeight
  
  particles.value = particles.value.map(particle => {
    let newX = particle.x + particle.speedX
    let newY = particle.y + particle.speedY
    
    // 边界反弹
    if (newX < 0 || newX > width) {
      particle.speedX *= -1
      newX = Math.max(0, Math.min(width, newX))
    }
    if (newY < 0 || newY > height) {
      particle.speedY *= -1
      newY = Math.max(0, Math.min(height, newY))
    }
    
    return {
      ...particle,
      x: newX,
      y: newY
    }
  })
  
  animationId = requestAnimationFrame(updateParticles)
}

onMounted(() => {
  checkMobile()
  checkPrefersReducedMotion()
  
  window.addEventListener('resize', checkMobile)
  mediaQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
  mediaQuery.addEventListener('change', checkPrefersReducedMotion)
  
  initParticles()
  if (!props.disabled && !prefersReducedMotion.value) {
    updateParticles()
  }
})

let mediaQuery: MediaQueryList | null = null

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
  if (mediaQuery) {
    mediaQuery.removeEventListener('change', checkPrefersReducedMotion)
  }
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>

<template>
  <div ref="containerRef" class="floating-particles">
    <div
      v-for="particle in particles"
      :key="particle.id"
      class="particle"
      :style="{
        transform: `translate(${particle.x}px, ${particle.y}px)`,
        width: `${particle.size}px`,
        height: `${particle.size}px`,
        opacity: particle.opacity,
        background: particle.color
      }"
    />
  </div>
</template>

<style scoped>
.floating-particles {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.particle {
  position: absolute;
  border-radius: 50%;
  filter: blur(1px);
  will-change: transform, opacity;
  backface-visibility: hidden;
  transform: translateZ(0);
  left: 0;
  top: 0;
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .floating-particles {
    display: none;
  }
}

/* 移动端优化 */
@media (max-width: 767px) {
  .particle {
    filter: blur(0.5px);
  }
}
</style>
