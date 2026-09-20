<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  data: number[]
  height?: number
}>(), {
  height: 200,
})

const w = 720
const pad = 8

const geom = computed(() => {
  const h = props.height
  const data = props.data
  const max = Math.max(...data)
  const min = Math.min(...data)
  const range = max - min || 1
  const stepX = (w - pad * 2) / (data.length - 1)
  const pts = data.map((v, i) => [pad + i * stepX, h - pad - ((v - min) / range) * (h - pad * 2 - 16) - 8])
  const line = pts.map((p, i) => `${i ? 'L' : 'M'}${p[0].toFixed(1)} ${p[1].toFixed(1)}`).join(' ')
  const area = `${line} L${pts[pts.length - 1][0].toFixed(1)} ${h} L${pts[0][0].toFixed(1)} ${h} Z`
  const last = pts[pts.length - 1]
  return { h, line, area, last }
})
</script>

<template>
  <svg :viewBox="`0 0 ${w} ${geom.h}`" width="100%" :height="height" preserveAspectRatio="none" style="display: block">
    <defs>
      <linearGradient id="tg" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%" stop-color="#0066cc" stop-opacity="0.16" />
        <stop offset="100%" stop-color="#0066cc" stop-opacity="0" />
      </linearGradient>
    </defs>
    <line v-for="g in [0.25, 0.5, 0.75]" :key="g" :x1="pad" :y1="geom.h * g" :x2="w - pad" :y2="geom.h * g" stroke="var(--hairline-soft)" stroke-width="1" />
    <path :d="geom.area" fill="url(#tg)" />
    <path :d="geom.line" fill="none" stroke="var(--primary)" stroke-width="2.2" stroke-linejoin="round" stroke-linecap="round" />
    <circle :cx="geom.last[0]" :cy="geom.last[1]" r="4" fill="#fff" stroke="var(--primary)" stroke-width="2.4" />
  </svg>
</template>
