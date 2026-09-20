<script setup lang="ts">
import { computed } from 'vue'
import {
  economicBenefitInputRows,
  economicBenefitOutputRows,
  type EconomicBenefitMatrix,
} from '@/utils/economicBenefit'

const props = withDefaults(defineProps<{
  title: string
  yearCount: number
  inputs: EconomicBenefitMatrix
  outputs: EconomicBenefitMatrix
  allowYearChange?: boolean
  calculatedInputKeys?: string[]
  projectYearCount?: number
  restoreLabel?: string
}>(), { allowYearChange: false, calculatedInputKeys: () => [], projectYearCount: 10, restoreLabel: '' })

const emit = defineEmits<{
  (event: 'update-input', payload: { key: string; index: number; value: number | null }): void
  (event: 'change-years', delta: number): void
  (event: 'calculate'): void
  (event: 'restore'): void
}>()

const columns = computed(() => Array.from({ length: props.yearCount }, (_, index) => `第${index + 1}年`))
const editableInputKeys = computed<Set<string>>(() => new Set(
  economicBenefitInputRows.map(row => row.key).filter(key => !props.calculatedInputKeys.includes(key)),
))
const totalIndex = computed(() => props.yearCount)
const output = (key: string, index: number) => props.outputs?.[key]?.[index] ?? ''
const benefitRowClassName = ({ row }: { row: { key: string } }) => editableInputKeys.value.has(row.key) ? 'input-row' : 'formula-row'
const updateInput = (key: string, index: number, value: number | undefined) => {
  emit('update-input', { key, index, value: Number(value) || null })
}
const kpis = computed(() => [
  { label: '内部收益率（IRR）', value: output('netCashInflow', totalIndex.value) || '暂不可计算' },
  { label: '净利润率', value: output('netProfitRate', totalIndex.value) || '暂不可计算' },
  { label: '净现值率', value: output('discountedCumulativeNetInflow', totalIndex.value) || '暂不可计算' },
  { label: '静态回收期', value: output('staticPayback', totalIndex.value) ? `${output('staticPayback', totalIndex.value)} 年` : '暂不可计算' },
  { label: '动态回收期', value: output('dynamicPayback', totalIndex.value) ? `${output('dynamicPayback', totalIndex.value)} 年` : '暂不可计算' },
])
</script>

<template>
  <section class="benefit-calculator">
    <div class="table-head calculator-head">
      <div>
        <h3>{{ title }}</h3>
        <div class="form-tip">项目期内黄色输入框可填写；项目期后的年度用于延续折旧计算，其他指标严格套用收益率测算表公式自动生成。</div>
      </div>
      <div class="calculator-actions">
        <el-button v-if="restoreLabel" @click="emit('restore')">{{ restoreLabel }}</el-button>
        <el-button v-if="allowYearChange" :disabled="yearCount <= 1" @click="emit('change-years', -1)">减少年度</el-button>
        <el-button v-if="allowYearChange" :disabled="yearCount >= 10" @click="emit('change-years', 1)">增加年度</el-button>
        <el-button type="primary" @click="emit('calculate')">一键计算</el-button>
      </div>
    </div>

    <div class="calculator-legend">
      <span><i class="legend-input" /> 黄色：需要填写</span>
      <span><i class="legend-formula" /> 灰白：公式自动计算</span>
    </div>

    <el-table :data="economicBenefitOutputRows" border row-key="key" :row-class-name="benefitRowClassName">
      <el-table-column prop="label" label="项目/年限" fixed min-width="220" />
      <el-table-column v-for="(label, index) in columns" :key="label" :label="label" min-width="130">
        <template #default="{ row }">
          <el-input-number
            v-if="editableInputKeys.has(row.key)"
            class="benefit-input"
            :model-value="inputs?.[row.key]?.[index] == null ? undefined : Number(inputs[row.key][index])"
            :min="0"
            :controls="false"
            :disabled="index >= projectYearCount"
            placeholder="请输入"
            style="width: 100%"
            @update:model-value="updateInput(row.key, index, $event)"
          />
          <span v-else class="formula-value">{{ output(row.key, index) || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="合计/指标" fixed="right" min-width="140">
        <template #default="{ row }"><strong>{{ output(row.key, totalIndex) }}</strong></template>
      </el-table-column>
    </el-table>

    <div class="benefit-kpis">
      <div v-for="item in kpis" :key="item.label" class="benefit-kpi">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </div>
    </div>

  </section>
</template>

<style scoped>
.benefit-calculator { margin-top: 28px; }
.calculator-head { gap: 20px; align-items: flex-end; }
.calculator-head h3 { margin: 0 0 4px; }
.calculator-actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.calculator-legend { display: flex; gap: 20px; margin: 10px 0; color: var(--el-text-color-secondary); font-size: 13px; }
.calculator-legend span { display: inline-flex; gap: 7px; align-items: center; }
.calculator-legend i { width: 16px; height: 16px; border: 1px solid var(--el-border-color); border-radius: 3px; }
.legend-input { background: #ffff00; }
.legend-formula { background: #f6f8fa; }
.benefit-kpis { display: grid; grid-template-columns: repeat(5, minmax(150px, 1fr)); gap: 12px; margin: 16px 0; }
.benefit-kpi { padding: 14px 16px; border: 1px solid var(--el-border-color-lighter); border-radius: 8px; background: var(--el-fill-color-lighter); }
.benefit-kpi span { display: block; color: var(--el-text-color-secondary); font-size: 13px; margin-bottom: 7px; }
.benefit-kpi strong { color: var(--el-color-primary); font-size: 20px; }
.benefit-input :deep(.el-input__wrapper) { background: #ffff00; box-shadow: 0 0 0 1px #d6b800 inset; }
.formula-value { color: var(--el-text-color-primary); font-variant-numeric: tabular-nums; }
:deep(.formula-row td.el-table__cell) { background: #f8fafc; }
:deep(.formula-row:hover td.el-table__cell) { background: #f1f5f9 !important; }
@media (max-width: 1200px) { .benefit-kpis { grid-template-columns: repeat(2, 1fr); } }
</style>
