<script setup lang="ts">
import { computed } from 'vue'

interface Segment { label: string; value: number; color: string }

const props = withDefaults(defineProps<{
  segments: Segment[]
  size?: number
}>(), {
  size: 132,
})

const total = computed(() => props.segments.reduce((s, x) => s + x.value, 0))
const r = computed(() => props.size / 2 - 13)
const cx = computed(() => props.size / 2)
const cy = computed(() => props.size / 2)
const C = computed(() => 2 * Math.PI * r.value)

const arcs = computed(() => {
  let offset = 0
  return props.segments.map((s) => {
    const len = (s.value / total.value) * C.value
    const arc = { color: s.color, dash: `${len} ${C.value - len}`, offset: -offset }
    offset += len
    return arc
  })
})
</script>

<template>
  <svg :width="size" :height="size" :viewBox="`0 0 ${size} ${size}`">
    <circle :cx="cx" :cy="cy" :r="r" fill="none" stroke="var(--hairline)" stroke-width="13" />
    <circle
      v-for="(a, i) in arcs" :key="i"
      :cx="cx" :cy="cy" :r="r" fill="none" :stroke="a.color" stroke-width="13"
      :stroke-dasharray="a.dash" :stroke-dashoffset="a.offset"
      :transform="`rotate(-90 ${cx} ${cy})`" stroke-linecap="round"
    />
    <text :x="cx" :y="cy - 2" text-anchor="middle" font-size="22" font-weight="700" fill="var(--ink)" font-family="var(--font-num)">{{ total }}</text>
    <text :x="cx" :y="cy + 16" text-anchor="middle" font-size="11" fill="var(--ink-muted-48)">角色总数</text>
  </svg>
</template>
