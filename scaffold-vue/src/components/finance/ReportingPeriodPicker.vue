<script setup lang="ts">
import { parseMonthRange, formatMonthRange } from '@/utils/initiationOverview'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <div class="reporting-period">
    <el-date-picker
      :model-value="parseMonthRange(modelValue)"
      type="monthrange"
      value-format="YYYY-MM"
      format="YYYY年MM月"
      range-separator="至"
      start-placeholder="开始年月"
      end-placeholder="结束年月"
      unlink-panels
      @update:model-value="emit('update:modelValue', formatMonthRange($event))"
    />
  </div>
</template>

<style scoped>
.reporting-period { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; }
.reporting-period :deep(.el-date-editor--monthrange) { width: 360px; max-width: 100%; }
</style>
