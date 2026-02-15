<template>
  <div class="sparkle-container" :style="containerStyle">
    <div
      v-for="i in count"
      :key="i"
      class="sparkle"
      :style="getSparkleStyle(i - 1)"
    ></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** Sparkle数量 */
  count?: number
  /** 容器宽度 */
  width?: string
  /** 容器高度 */
  height?: string
  /** 自定义位置数组 [{top, left}, ...] */
  positions?: Array<{ top: string; left: string }>
  /** 随机生成位置 */
  random?: boolean
  /** 颜色 */
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  count: 12,
  width: '100%',
  height: '100%',
  positions: () => [],
  random: true,
  color: 'rgba(255, 255, 255, 0.9)'
})

const containerStyle = computed(() => ({
  width: props.width,
  height: props.height
}))

// 预定义的错落位置
const defaultPositions = [
  { top: '15%', left: '10%' },
  { top: '25%', left: '85%' },
  { top: '35%', left: '5%' },
  { top: '45%', left: '90%' },
  { top: '55%', left: '15%' },
  { top: '65%', left: '80%' },
  { top: '75%', left: '8%' },
  { top: '85%', left: '88%' },
  { top: '20%', left: '50%' },
  { top: '40%', left: '25%' },
  { top: '60%', left: '70%' },
  { top: '80%', left: '45%' }
]

// 生成随机位置
const generateRandomPosition = (index: number): { top: string; left: string } => {
  // 使用伪随机确保可复现性
  const seed = index * 137.5 // 黄金角
  const top = 10 + (Math.sin(seed) * 0.5 + 0.5) * 80
  const left = 5 + (Math.cos(seed) * 0.5 + 0.5) * 90
  return {
    top: `${top.toFixed(1)}%`,
    left: `${left.toFixed(1)}%`
  }
}

const getSparkleStyle = (index: number): Record<string, string> => {
  let position: { top: string; left: string }

  if (props.positions && props.positions[index]) {
    // 使用自定义位置
    position = props.positions[index]
  } else if (props.random) {
    // 随机位置
    position = generateRandomPosition(index)
  } else {
    // 使用默认位置
    position = defaultPositions[index % defaultPositions.length]
  }

  // 错落延迟计算
  const delays = [0, 0.3, 0.7, 1.1, 0.5, 0.9, 0.2, 1.3, 0.6, 1.0, 0.4, 0.8]
  const delay = delays[index % delays.length]

  return {
    top: position.top,
    left: position.left,
    animationDelay: `${delay}s`,
    background: `radial-gradient(circle, ${props.color} 0%, rgba(255, 200, 150, 0.6) 50%, transparent 70%)`
  }
}
</script>

<style scoped>
.sparkle-container {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  z-index: 1;
}

.sparkle {
  position: absolute;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  animation: sparkleTwinkle 3s ease-in-out infinite;
  will-change: opacity, transform;
}

@keyframes sparkleTwinkle {
  0%, 100% {
    opacity: 0;
    transform: scale(0) rotate(0deg);
  }
  50% {
    opacity: 1;
    transform: scale(1) rotate(180deg);
  }
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .sparkle {
    animation: none !important;
    opacity: 0.5;
  }
}
</style>
