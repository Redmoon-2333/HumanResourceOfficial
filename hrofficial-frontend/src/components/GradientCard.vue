<script setup lang="ts">
import { ref } from 'vue'

interface Props {
  gradient?: 'primary' | 'secondary' | 'accent' | 'warm' | 'custom'
  customGradient?: string
  hoverEffect?: boolean
  glowEffect?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  gradient: 'primary',
  customGradient: '',
  hoverEffect: true,
  glowEffect: false
})

const cardRef = ref<HTMLElement | null>(null)
const mouseX = ref(0)
const mouseY = ref(0)

const handleMouseMove = (e: MouseEvent) => {
  if (!cardRef.value || !props.hoverEffect) return
  const rect = cardRef.value.getBoundingClientRect()
  mouseX.value = e.clientX - rect.left
  mouseY.value = e.clientY - rect.top
}

const gradientMap = {
  primary: 'linear-gradient(135deg, #FF6B4A 0%, #E35532 100%)',
  secondary: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)',
  accent: 'linear-gradient(135deg, #FB7185 0%, #F43F5E 100%)',
  warm: 'linear-gradient(135deg, #FF6B4A 0%, #F59E0B 50%, #FB7185 100%)',
  custom: props.customGradient
}
</script>

<template>
  <div
    ref="cardRef"
    class="gradient-card"
    :class="{ 'hover-effect': hoverEffect, 'glow-effect': glowEffect }"
    :style="{ '--card-gradient': gradientMap[gradient] }"
    @mousemove="handleMouseMove"
  >
    <div class="card-background" :style="gradientMap[gradient]"></div>
    <div 
      v-if="hoverEffect"
      class="card-shine"
      :style="{
        left: `${mouseX}px`,
        top: `${mouseY}px`
      }"
    ></div>
    <div class="card-content">
      <slot></slot>
    </div>
  </div>
</template>

<style scoped>
.gradient-card {
  position: relative;
  border-radius: 24px;
  overflow: hidden;
  transform-style: preserve-3d;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.gradient-card.hover-effect:hover {
  transform: translateY(-4px) scale(1.02);
  box-shadow: 0 20px 40px rgba(255, 107, 74, 0.25);
}

.gradient-card.glow-effect {
  box-shadow: 0 0 30px rgba(255, 107, 74, 0.3);
}

.card-background {
  position: absolute;
  inset: 0;
  z-index: 0;
}

.card-shine {
  position: absolute;
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
  transform: translate(-50%, -50%);
  pointer-events: none;
  opacity: 0;
  transition: opacity 0.3s ease;
  z-index: 1;
}

.gradient-card:hover .card-shine {
  opacity: 1;
}

.card-content {
  position: relative;
  z-index: 2;
  padding: 24px;
}
</style>
