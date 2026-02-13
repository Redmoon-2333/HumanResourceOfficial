<script setup lang="ts">
interface Props {
  blur?: number
  opacity?: number
  borderColor?: string
  borderWidth?: number
  borderRadius?: number
  padding?: string
}

const props = withDefaults(defineProps<Props>(), {
  blur: 20,
  opacity: 0.85,
  borderColor: 'rgba(255, 237, 230, 0.8)',
  borderWidth: 1,
  borderRadius: 24,
  padding: '24px'
})
</script>

<template>
  <div 
    class="glass-panel"
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
  background: rgba(255, 255, 255, var(--opacity));
  backdrop-filter: blur(var(--blur));
  -webkit-backdrop-filter: blur(var(--blur));
  border: var(--border-width) solid var(--border-color);
  border-radius: var(--border-radius);
  padding: var(--padding);
  box-shadow: 
    0 8px 32px rgba(249, 115, 22, 0.1),
    0 0 0 1px rgba(255, 255, 255, 0.5) inset;
  transition: all 0.3s ease;
}

.glass-panel:hover {
  box-shadow: 
    0 12px 40px rgba(249, 115, 22, 0.15),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset;
}
</style>
