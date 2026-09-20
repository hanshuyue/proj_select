<script setup lang="ts">
import { computed } from 'vue'
import { ICON_PATHS } from '@/data/icons'

const props = withDefaults(defineProps<{
  name: string
  size?: number
  stroke?: number
  fill?: boolean
}>(), {
  size: 18,
  stroke: 1.6,
  fill: false,
})

const d = computed(() => ICON_PATHS[props.name])
</script>

<template>
  <svg
    v-if="d"
    :width="size"
    :height="size"
    viewBox="0 0 24 24"
    :fill="fill ? 'currentColor' : 'none'"
    :stroke="fill ? 'none' : 'currentColor'"
    :stroke-width="stroke"
    stroke-linecap="round"
    stroke-linejoin="round"
    style="flex-shrink: 0; display: block;"
    aria-hidden="true"
  >
    <path :d="d" />
  </svg>
</template>
