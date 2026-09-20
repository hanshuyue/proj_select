<script setup lang="ts">
import { computed } from 'vue'
import { economicBaseDetailDefinitions } from '@/utils/economicBaseDetail'
import {
  calculateDepreciation,
  depreciationCategories,
  type DepreciationSchedule,
} from '@/utils/depreciation'

const props = defineProps<{
  yearCount: number
  schedule: DepreciationSchedule
  projectYearCount?: number
  categoryTotals?: Record<string, number>
}>()

const emit = defineEmits<{
  (event: 'update-input', payload: { categoryKey: string; field: 'commissioningMonths' | 'investments'; index: number; value: number | null }): void
}>()

const columns = computed(() => Array.from({ length: props.yearCount }, (_, index) => `第${index + 1}年`))
const result = computed(() => calculateDepreciation(props.schedule, props.yearCount))
const update = (categoryKey: string, field: 'commissioningMonths' | 'investments', index: number, value: number | undefined) => {
  emit('update-input', { categoryKey, field, index, value: value == null ? null : Number(value) })
}
const amount = (value: number) => value.toFixed(2)
const linkedCategories = computed(() => depreciationCategories.map(category => ({
  ...category,
  sources: economicBaseDetailDefinitions
    .filter(item => item.section === 'INVESTMENT' && item.depreciationCategory === category.key)
    .map(item => `${item.group} / ${item.item}`),
  baseAmount: props.categoryTotals?.[category.key] || 0,
})))
</script>

<template>
  <section class="depreciation-calculator">
    <div class="table-head depreciation-head">
      <div>
        <h3>折旧和摊销计算</h3>
        <div class="form-tip">年度对应原表：第1—20年为折旧计算列，资产从各自投资当年开始计提；项目年限仅控制投资填写年度，不缩短资产寿命。收益测算表第1—10年逐年引用本表前10列，后续折旧仅在本附表展示。</div>
        <div class="form-tip">按指定年限自动计提：平台设备5年、传输网设备7年、光缆及管道10年、软件5年。基础表填写投资后自动生成折旧；尚无年度计划时默认第1年投资，已有手工计划不覆盖。各模式按自己的年度投资额及基础表资产类别占比计算，结果自动回填“折旧与摊销”。</div>
        <div class="form-tip">沿用测算表直线法（不计残值）：年折旧=不含税投资÷折旧年限；首年按投产月份折算，留空或0按全年计提，12表示次年开始计提。最后一年不超过剩余资产价值。其他类投资沿用原模板5年规则。</div>
      </div>
    </div>

    <div class="calculator-legend">
      <span><i class="legend-input" /> 黄色：当年投产月份</span>
      <span><i class="legend-formula" /> 灰白：自动分配投资额及折旧/摊销结果</span>
    </div>

    <el-table :data="linkedCategories" border row-key="key" style="margin-bottom: 16px">
      <el-table-column prop="label" label="折旧/摊销类别" min-width="190" />
      <el-table-column label="关联投资明细（自动汇总）" min-width="360">
        <template #default="{ row }"><div v-for="source in row.sources" :key="source">{{ source }}</div></template>
      </el-table-column>
      <el-table-column label="固定年限" width="110" class-name="fixed-life">
        <template #default="{ row }">{{ row.lifeYears }}年</template>
      </el-table-column>
      <el-table-column label="来源投资金额（万元，不含税）" min-width="200">
        <template #default="{ row }">{{ amount(row.baseAmount) }}</template>
      </el-table-column>
    </el-table>
    <div class="form-tip">以上金额来自基础数据“投资”明细：定制软件从平台类单独归入软件；管道、光缆从传输网络配套投资中单独归入10年类别。各笔金额只归类一次，成本明细不计入此表。</div>

    <el-table :data="depreciationCategories" border row-key="key">
      <el-table-column prop="label" label="资产类别" fixed min-width="190" />
      <el-table-column label="资产折旧/摊销年限（固定）" fixed min-width="160" class-name="fixed-life">
        <template #default="{ row }">{{ row.lifeYears }}年</template>
      </el-table-column>
      <el-table-column v-for="(label, index) in columns" :key="label" :label="label" min-width="150">
        <template #default="{ row }">
          <div class="year-inputs">
            <div class="auto-investment">投资额：{{ amount(schedule?.[row.key]?.investments?.[index] || 0) }}</div>
            <el-input-number
              :model-value="schedule?.[row.key]?.commissioningMonths?.[index] ?? undefined"
              :min="0"
              :max="12"
              :precision="0"
              :controls="false"
              :disabled="index >= (projectYearCount ?? yearCount) || !(schedule?.[row.key]?.investments?.[index] || 0)"
              placeholder="投产月0-12"
              @update:model-value="update(row.key, 'commissioningMonths', index, $event)"
            />
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-table :data="depreciationCategories" border row-key="key" class="depreciation-result-table">
      <el-table-column prop="label" label="自动计算结果" fixed min-width="280">
        <template #default="{ row }">{{ row.label }}当年{{ row.methodLabel }}</template>
      </el-table-column>
      <el-table-column label="20年合计" min-width="130">
        <template #default="{ row }">{{ amount(result.byCategory[row.key].reduce((sum, value) => sum + value, 0)) }}</template>
      </el-table-column>
      <el-table-column v-for="(label, index) in columns" :key="`result-${label}`" :label="label" min-width="150">
        <template #default="{ row }">{{ amount(result.byCategory[row.key]?.[index] || 0) }}</template>
      </el-table-column>
    </el-table>

    <div class="depreciation-total">
      <strong>各年折旧及摊销合计</strong>
      <span v-for="(value, index) in result.totals" :key="index">第{{ index + 1 }}年：<b>{{ amount(value) }}</b> 万元</span>
    </div>
  </section>
</template>

<style scoped>
.depreciation-calculator { margin-top: 28px; }
.depreciation-head h3 { margin: 0 0 4px; }
.calculator-legend { display: flex; gap: 20px; margin: 10px 0; color: var(--el-text-color-secondary); font-size: 13px; }
.calculator-legend span { display: inline-flex; gap: 7px; align-items: center; }
.calculator-legend i { width: 16px; height: 16px; border: 1px solid var(--el-border-color); border-radius: 3px; }
.legend-input { background: #ffff00; }
.legend-formula { background: #f6f8fa; }
.year-inputs { display: grid; gap: 6px; }
.year-inputs :deep(.el-input__wrapper) { background: #ffff00; box-shadow: 0 0 0 1px #d6b800 inset; }
.year-inputs :deep(.is-disabled .el-input__wrapper) { background: #f6f8fa; box-shadow: 0 0 0 1px var(--el-border-color) inset; }
.year-inputs :deep(.el-input-number) { width: 100%; }
.auto-investment { padding: 7px 10px; border: 1px solid var(--el-border-color); border-radius: 4px; background: #f6f8fa; color: var(--el-text-color-regular); font-variant-numeric: tabular-nums; }
.depreciation-result-table { margin-top: 12px; }
.depreciation-calculator :deep(td.fixed-life) { background: #f4b183; font-weight: 600; }
.depreciation-result-table :deep(td.el-table__cell) { background: #f8fafc; font-variant-numeric: tabular-nums; }
.depreciation-total { display: flex; flex-wrap: wrap; gap: 10px 20px; padding: 14px 16px; border: 1px solid var(--el-border-color-lighter); border-top: 0; background: var(--el-color-primary-light-9); }
.depreciation-total span { color: var(--el-text-color-secondary); }
.depreciation-total b { color: var(--el-color-primary); }
</style>
