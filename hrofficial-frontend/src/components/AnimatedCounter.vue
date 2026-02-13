<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'

interface Props {
  value: number
  duration?: number
  prefix?: string
  suffix?: string
  decimals?: number
}

const props = withDefaults(defineProps<Props>(), {
  duration: 2000,
  prefix: '',
  suffix: '',
  decimals: 0
})

const displayValue = ref(0)
const isAnimating = ref(false)

let animationId: number | null = null

const animate = (targetValue: number) => {
  // 如果有正在进行的动画，取消它
  if (animationId !== null) {
    cancelAnimationFrame(animationId)
  }

  isAnimating.value = true

  const startTime = performance.now()
  const startValue = displayValue.value
  const diff = targetValue - startValue

  const step = (currentTime: number) => {
    const elapsed = currentTime - startTime
    const progress = Math.min(elapsed / props.duration, 1)

    // 使用 easeOutExpo 缓动函数
    const easeProgress = 1 - Math.pow(2, -10 * progress)

    displayValue.value = startValue + diff * easeProgress

    if (progress < 1) {
      animationId = requestAnimationFrame(step)
    } else {
      displayValue.value = targetValue
      isAnimating.value = false
      animationId = null
    }
  }

  animationId = requestAnimationFrame(step)
}

onMounted(() => {
  animate(props.value)
})

watch(() => props.value, (newValue) => {
  animate(newValue)
})

// 组件卸载时取消动画，防止内存泄漏
onUnmounted(() => {
  if (animationId !== null) {
    cancelAnimationFrame(animationId)
  }
})

const formattedValue = () => {
  return displayValue.value.toFixed(props.decimals)
}
</script>

<template>
  <span class="animated-counter">
    <span class="prefix" v-if="prefix">{{ prefix }}</span>
    <span class="number">{{ formattedValue() }}</span>
    <span class="suffix" v-if="suffix">{{ suffix }}</span>
  </span>
</template>

<style scoped>
.animated-counter {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
}

.prefix, .suffix {
  font-size: 0.7em;
  opacity: 0.8;
}

.number {
  font-variant-numeric: tabular-nums;
}
</style>
