<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

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
  colors?: string[]
  minSize?: number
  maxSize?: number
  speed?: number
}

const props = withDefaults(defineProps<Props>(), {
  count: 20,
  colors: () => ['#FF6B4A', '#F59E0B', '#FB7185', '#FFA07A', '#FFD93D'],
  minSize: 4,
  maxSize: 12,
  speed: 0.5
})

const particles = ref<Particle[]>([])
const containerRef = ref<HTMLElement | null>(null)
let animationId: number | null = null
let particleIdCounter = 0

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
    speedX: (Math.random() - 0.5) * props.speed,
    speedY: (Math.random() - 0.5) * props.speed,
    opacity: 0.3 + Math.random() * 0.4,
    color: props.colors[Math.floor(Math.random() * props.colors.length)] || '#FF6B4A'
  }
}

const initParticles = () => {
  particles.value = Array.from({ length: props.count }, createParticle)
}

const updateParticles = () => {
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
  initParticles()
  updateParticles()
})

onUnmounted(() => {
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
        left: `${particle.x}px`,
        top: `${particle.y}px`,
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
  transition: transform 0.1s linear;
}
</style>
