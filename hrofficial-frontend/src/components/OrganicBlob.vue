<template>
  <div
    class="organic-blob"
    :class="[
      `organic-blob--${size}`,
      {
        'organic-blob--glow': glow,
        'organic-blob--float': float
      }
    ]"
    :style="blobStyle"
  ></div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  /** Blob尺寸: small(180-250px), medium(280-380px), large(380-480px) */
  size?: 'small' | 'medium' | 'large'
  /** 主颜色 */
  color: string
  /** 浅色（用于渐变） */
  colorLight?: string
  /** 位置 */
  position?: {
    top?: string
    right?: string
    bottom?: string
    left?: string
  }
  /** 动画延迟（秒） */
  delay?: number
  /** 透明度 */
  opacity?: number
  /** 是否发光 */
  glow?: boolean
  /** 是否浮动 */
  float?: boolean
  /** 随机种子（用于生成不同的形状） */
  seed?: number
  /** 自定义样式 */
  customStyle?: Record<string, string>
}

const props = withDefaults(defineProps<Props>(), {
  size: 'medium',
  colorLight: '',
  position: () => ({}),
  delay: 0,
  opacity: undefined,
  glow: false,
  float: false,
  seed: () => Math.floor(Math.random() * 1000),
  customStyle: () => ({})
})

// 基于种子生成伪随机数
const seededRandom = (seed: number) => {
  const x = Math.sin(seed) * 10000
  return x - Math.floor(x)
}

// 生成随机border-radius值（有机形状）
const generateOrganicShape = (seed: number) => {
  const r = (offset: number) => 30 + Math.floor(seededRandom(seed + offset) * 50)
  return `${r(1)}% ${r(2)}% ${r(3)}% ${r(4)}% / ${r(5)}% ${r(6)}% ${r(7)}% ${r(8)}%`
}

// 生成随机动画关键帧
const generateRandomKeyframes = (seed: number) => {
  const rand = (min: number, max: number, offset: number) => 
    min + seededRandom(seed + offset) * (max - min)
  
  return {
    phase1: {
      radius: generateOrganicShape(seed + 10),
      translateX: rand(-30, 30, 20),
      translateY: rand(-40, 40, 30),
      scale: rand(0.9, 1.15, 40),
      rotate: rand(-15, 15, 50)
    },
    phase2: {
      radius: generateOrganicShape(seed + 60),
      translateX: rand(-25, 25, 70),
      translateY: rand(-35, 35, 80),
      scale: rand(0.85, 1.1, 90),
      rotate: rand(-12, 12, 100)
    },
    phase3: {
      radius: generateOrganicShape(seed + 110),
      translateX: rand(-20, 20, 120),
      translateY: rand(-30, 30, 130),
      scale: rand(0.95, 1.08, 140),
      rotate: rand(-8, 8, 150)
    }
  }
}

// 生成动态CSS变量
const dynamicStyles = computed(() => {
  const seed = props.seed
  const keyframes = generateRandomKeyframes(seed)
  
  return {
    '--blob-radius-initial': generateOrganicShape(seed),
    '--blob-radius-phase1': keyframes.phase1.radius,
    '--blob-radius-phase2': keyframes.phase2.radius,
    '--blob-radius-phase3': keyframes.phase3.radius,
    '--blob-tx-1': `${keyframes.phase1.translateX}px`,
    '--blob-ty-1': `${keyframes.phase1.translateY}px`,
    '--blob-scale-1': keyframes.phase1.scale,
    '--blob-rotate-1': `${keyframes.phase1.rotate}deg`,
    '--blob-tx-2': `${keyframes.phase2.translateX}px`,
    '--blob-ty-2': `${keyframes.phase2.translateY}px`,
    '--blob-scale-2': keyframes.phase2.scale,
    '--blob-rotate-2': `${keyframes.phase2.rotate}deg`,
    '--blob-tx-3': `${keyframes.phase3.translateX}px`,
    '--blob-ty-3': `${keyframes.phase3.translateY}px`,
    '--blob-scale-3': keyframes.phase3.scale,
    '--blob-rotate-3': `${keyframes.phase3.rotate}deg`,
    '--blob-anim-duration': `${6 + seededRandom(seed + 200) * 4}s`,
    '--blob-anim-delay': `${props.delay + seededRandom(seed + 300) * 2}s`
  }
})

const blobStyle = computed(() => {
  const style: Record<string, string> = {
    ...props.customStyle,
    ...dynamicStyles.value
  }

  // 位置
  if (props.position.top) style.top = props.position.top
  if (props.position.right) style.right = props.position.right
  if (props.position.bottom) style.bottom = props.position.bottom
  if (props.position.left) style.left = props.position.left

  // 透明度
  if (props.opacity !== undefined) {
    style.opacity = String(props.opacity)
  }

  // 背景渐变 - 随机椭圆形状和角度
  const lightColor = props.colorLight || props.color
  const gradientAngle = Math.floor(seededRandom(props.seed + 400) * 360)
  const gradientShape = seededRandom(props.seed + 500) > 0.5 ? 'ellipse 60% 50%' : 'ellipse 50% 60%'
  const gradientPosX = 30 + Math.floor(seededRandom(props.seed + 600) * 40)
  const gradientPosY = 30 + Math.floor(seededRandom(props.seed + 700) * 40)
  
  style.background = `radial-gradient(${gradientShape} at ${gradientPosX}% ${gradientPosY}%, ${props.color} 0%, ${lightColor}40 45%, transparent 70%)`

  return style
})
</script>

<style scoped>
.organic-blob {
  position: absolute;
  border-radius: var(--blob-radius-initial, 60% 40% 30% 70% / 60% 30% 70% 40%);
  animation: blobMorph var(--blob-anim-duration, 8s) ease-in-out infinite;
  animation-delay: var(--blob-anim-delay, 0s);
  pointer-events: none;
  will-change: transform, border-radius;
}

/* Blob尺寸 - 增加随机范围 */
.organic-blob--small {
  width: var(--blob-width, 220px);
  height: var(--blob-height, 220px);
  filter: blur(var(--blob-blur, 45px));
  opacity: var(--blob-opacity, 0.3);
}

.organic-blob--medium {
  width: var(--blob-width, 320px);
  height: var(--blob-height, 320px);
  filter: blur(var(--blob-blur, 55px));
  opacity: var(--blob-opacity, 0.35);
}

.organic-blob--large {
  width: var(--blob-width, 420px);
  height: var(--blob-height, 420px);
  filter: blur(var(--blob-blur, 65px));
  opacity: var(--blob-opacity, 0.4);
}

/* Blob发光 - 随机频率 */
.organic-blob--glow {
  animation: 
    blobMorph var(--blob-anim-duration, 8s) ease-in-out infinite,
    blobGlow calc(var(--blob-anim-duration, 8s) * 0.6) ease-in-out infinite;
  animation-delay: var(--blob-anim-delay, 0s);
}

/* Blob浮动 - 随机轨迹 */
.organic-blob--float {
  animation: 
    blobMorph var(--blob-anim-duration, 8s) ease-in-out infinite,
    floatOrganic calc(var(--blob-anim-duration, 8s) * 0.8) ease-in-out infinite;
  animation-delay: var(--blob-anim-delay, 0s);
}

/* Blob形态变化 - 使用CSS变量实现随机性 */
@keyframes blobMorph {
  0%, 100% {
    border-radius: var(--blob-radius-initial);
    transform: translate(0, 0) scale(1) rotate(0deg);
  }
  25% {
    border-radius: var(--blob-radius-phase1);
    transform: translate(var(--blob-tx-1), var(--blob-ty-1)) scale(var(--blob-scale-1)) rotate(var(--blob-rotate-1));
  }
  50% {
    border-radius: var(--blob-radius-phase2);
    transform: translate(var(--blob-tx-2), var(--blob-ty-2)) scale(var(--blob-scale-2)) rotate(var(--blob-rotate-2));
  }
  75% {
    border-radius: var(--blob-radius-phase3);
    transform: translate(var(--blob-tx-3), var(--blob-ty-3)) scale(var(--blob-scale-3)) rotate(var(--blob-rotate-3));
  }
}

/* Blob发光呼吸 - 随机幅度 */
@keyframes blobGlow {
  0%, 100% {
    filter: blur(var(--blob-blur, 60px)) brightness(1);
  }
  50% {
    filter: blur(calc(var(--blob-blur, 60px) + 10px)) brightness(1.15);
  }
}

/* 浮动动画 - 有机随机轨迹 */
@keyframes floatOrganic {
  0%, 100% {
    transform: translate(0, 0) rotate(0deg);
  }
  15% {
    transform: translate(calc(var(--blob-tx-1) * 0.3), calc(var(--blob-ty-1) * -0.5)) rotate(calc(var(--blob-rotate-1) * 0.2));
  }
  30% {
    transform: translate(calc(var(--blob-tx-2) * -0.2), calc(var(--blob-ty-2) * 0.4)) rotate(calc(var(--blob-rotate-2) * -0.15));
  }
  45% {
    transform: translate(calc(var(--blob-tx-3) * 0.4), calc(var(--blob-ty-3) * 0.2)) rotate(calc(var(--blob-rotate-3) * 0.25));
  }
  60% {
    transform: translate(calc(var(--blob-tx-1) * -0.3), calc(var(--blob-ty-1) * 0.3)) rotate(calc(var(--blob-rotate-1) * -0.2));
  }
  80% {
    transform: translate(calc(var(--blob-tx-2) * 0.2), calc(var(--blob-ty-2) * -0.3)) rotate(calc(var(--blob-rotate-2) * 0.15));
  }
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .organic-blob {
    animation: none !important;
  }
}
</style>
