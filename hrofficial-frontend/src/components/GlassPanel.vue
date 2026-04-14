<script setup lang="ts">
interface Props {
  blur?: number
  opacity?: number
  borderColor?: string
  borderWidth?: number
  borderRadius?: number
  padding?: string
  shadow?: 'sm' | 'md' | 'lg'
}

const props = withDefaults(defineProps<Props>(), {
  blur: 20,
  opacity: 0.85,
  borderColor: 'rgba(255, 237, 230, 0.8)',
  borderWidth: 1,
  borderRadius: 24,
  padding: '',
  shadow: 'md'
})
</script>

<template>
  <div 
    class="glass-panel"
    :class="['glass-shadow-' + shadow]"
    :style="{
      '--blur': `${blur}px`,
      '--opacity': opacity,
      '--border-color': borderColor,
      '--border-width': `${borderWidth}px`,
      '--border-radius': `${borderRadius}px`,
      '--padding': padding
    }"
  >
    <slot></slot>
  </div>
</template>

<style scoped>
.glass-panel {
  background: 
    rgba(255, 255, 255, var(--opacity)),
    linear-gradient(135deg, rgba(255, 255, 255, 0.1), rgba(255, 255, 255, 0.05));
  backdrop-filter: blur(calc(var(--blur) * 1.2));
  -webkit-backdrop-filter: blur(calc(var(--blur) * 1.2));
  border: 
    var(--border-width) solid var(--border-color),
    inset 0 1px 2px rgba(255, 255, 255, 0.5);
  border-radius: var(--border-radius);
  padding: var(--padding, var(--padding-card, 24px));
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

/* Top highlight */
.glass-panel::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, 
    transparent 0%, 
    rgba(255, 255, 255, 0.8) 50%, 
    transparent 100%);
  pointer-events: none;
}

/* Shadow variants */
.glass-shadow-sm {
  box-shadow: 
    0 8px 32px rgba(255, 107, 74, 0.08),
    0 0 0 1px rgba(255, 255, 255, 0.3) inset,
    0 1px 3px rgba(0, 0, 0, 0.02);
}

.glass-shadow-md {
  box-shadow: 
    0 12px 40px rgba(255, 107, 74, 0.12),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset,
    0 2px 6px rgba(0, 0, 0, 0.03);
}

.glass-shadow-lg {
  box-shadow: 
    0 16px 48px rgba(255, 107, 74, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset,
    0 4px 12px rgba(0, 0, 0, 0.04);
}

/* Enhanced hover */
.glass-panel:hover {
  box-shadow: 
    0 16px 48px rgba(255, 107, 74, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset,
    0 4px 12px rgba(0, 0, 0, 0.04);
  backdrop-filter: blur(calc(var(--blur) * 1.3));
  -webkit-backdrop-filter: blur(calc(var(--blur) * 1.3));
}
</style>
