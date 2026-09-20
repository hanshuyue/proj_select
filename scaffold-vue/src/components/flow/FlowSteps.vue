<script setup lang="ts">
import { computed } from 'vue'
import Icon from '@/components/Icon.vue'
import { normalizeFlowStep } from '@/utils/flow'

const props = defineProps<{
  current: number | string
  total: number | string
  nodes?: string[]
}>()

const names = computed(() => props.nodes?.length ? props.nodes : ['发起', '部门审批', '财务审核', '总经理', '归档'].slice(0, Math.max(Number(props.total) || 0, 0) + 1))
const total = computed(() => Math.max(names.value.length - 1, 0))
const current = computed(() => {
  if (typeof props.current === 'string') {
    const index = names.value.indexOf(props.current)
    if (index >= 0) return index
  }
  return normalizeFlowStep(props.current, total.value)
})
</script>

<template>
  <div class="flow-steps">
    <template v-for="(n, i) in names" :key="i">
      <div :class="['flow-step', i < current ? 'done' : i === current ? 'current' : '']">
        <span class="flow-dot">
          <Icon v-if="i < current" name="check" :size="12" :stroke="3" /><template v-else>{{ i + 1 }}</template>
        </span>
        <span class="flow-step-label">{{ n }}</span>
      </div>
      <span v-if="i < names.length - 1" :class="['flow-line', i < current ? 'done' : '']" />
    </template>
  </div>
</template>
