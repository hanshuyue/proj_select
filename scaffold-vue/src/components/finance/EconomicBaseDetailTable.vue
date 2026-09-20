<script setup lang="ts">
import { computed } from 'vue'
import { depreciationCategories } from '@/utils/depreciation'
import {
  calculateEconomicBaseDetailTotals,
  calculateEconomicBaseRow,
  economicBaseDetailDefinitions,
  type EconomicBaseDetailDefinition,
  type EconomicBaseDetailValues,
  type EconomicBaseSection,
} from '@/utils/economicBaseDetail'

const props = defineProps<{ modelValue: EconomicBaseDetailValues }>()
const emit = defineEmits<{ 'update:modelValue': [value: EconomicBaseDetailValues] }>()

type DisplayRow = EconomicBaseDetailDefinition & {
  rowType: 'detail' | 'subtotal' | 'total'
  categoryLabel: string
}

function categoryLabel(section: EconomicBaseSection) {
  if (section === 'INCOME') return '项目收入（万元）'
  if (section === 'INVESTMENT') return '投资（万元）'
  return '成本（万元）'
}

const rows = computed<DisplayRow[]>(() => {
  const result: DisplayRow[] = []
  let previousGroup = ''
  let previousSection: EconomicBaseSection | '' = ''
  for (const definition of economicBaseDetailDefinitions) {
    if (previousGroup && (definition.group !== previousGroup || definition.section !== previousSection)) {
      result.push({ key: `subtotal-${previousSection}-${previousGroup}`, section: previousSection as EconomicBaseSection, sectionLabel: previousSection === 'INCOME' ? '直接收入' : '直接支出', categoryLabel: categoryLabel(previousSection as EconomicBaseSection), group: previousGroup, item: `${previousGroup}小计`, taxRate: 0, description: '', rowType: 'subtotal' })
    }
    if (previousSection && definition.section !== previousSection) {
      result.push({ key: `total-${previousSection}`, section: previousSection, sectionLabel: previousSection === 'INCOME' ? '直接收入' : '直接支出', categoryLabel: '', group: '', item: previousSection === 'INCOME' ? '项目收入小计' : '投资小计', taxRate: 0, description: '', rowType: 'total' })
    }
    result.push({ ...definition, categoryLabel: categoryLabel(definition.section), rowType: 'detail' })
    previousGroup = definition.group
    previousSection = definition.section
  }
  if (previousGroup && previousSection) result.push({ key: `subtotal-${previousSection}-${previousGroup}`, section: previousSection, sectionLabel: '直接支出', categoryLabel: categoryLabel(previousSection), group: previousGroup, item: `${previousGroup}小计`, taxRate: 0, description: '', rowType: 'subtotal' })
  result.push({ key: 'total-COST', section: 'COST', sectionLabel: '直接支出', categoryLabel: '', group: '', item: '成本小计', taxRate: 0, description: '', rowType: 'total' })
  return result
})

const totals = computed(() => calculateEconomicBaseDetailTotals(props.modelValue))

function update(row: DisplayRow, key: 'ctIncTax' | 'itIncTax' | 'taxRate', value: number | null | undefined) {
  emit('update:modelValue', {
    ...props.modelValue,
    [row.key]: { ...props.modelValue[row.key], [key]: value ?? null },
  })
}

function detailResult(row: DisplayRow) {
  return calculateEconomicBaseRow(row, props.modelValue)
}

function aggregate(row: DisplayRow) {
  if (row.rowType === 'total') return totals.value.sections[row.section]
  const definitions = economicBaseDetailDefinitions.filter(item => item.section === row.section && item.group === row.group)
  return definitions.reduce((sum, definition) => {
    const value = calculateEconomicBaseRow(definition, props.modelValue)
    // Excel sums the full-precision row formulas, then formats the subtotal.
    // Do not round each VAT row before adding it or subtotal cents can drift.
    for (const key of ['ctIncTax', 'itIncTax', 'contractIncTax', 'ctVat', 'itVat', 'amountExTax'] as const) sum[key] += value[key]
    return sum
  }, { ctIncTax: 0, itIncTax: 0, contractIncTax: 0, ctVat: 0, itVat: 0, amountExTax: 0 })
}

function cell(row: DisplayRow, key: 'ctIncTax' | 'itIncTax' | 'contractIncTax' | 'ctVat' | 'itVat' | 'amountExTax') {
  return (row.rowType === 'detail' ? detailResult(row)[key] : aggregate(row)[key]).toFixed(2)
}

function rowClass({ row }: { row: DisplayRow }) {
  return row.rowType === 'detail' ? '' : `economic-${row.rowType}-row`
}

function spanMethod({ row, columnIndex, rowIndex }: { row: DisplayRow, columnIndex: number, rowIndex: number }) {
  if (columnIndex !== 0 && columnIndex !== 1) return
  const key = columnIndex === 0 ? 'sectionLabel' : 'categoryLabel'
  const value = row[key]
  if (!value) return [1, 1]
  const previous = rows.value[rowIndex - 1]
  if (previous?.[key] === value) return [0, 0]
  let rowspan = 1
  while (rows.value[rowIndex + rowspan]?.[key] === value) rowspan += 1
  return [rowspan, 1]
}

function depreciationLabel(row: DisplayRow) {
  const category = depreciationCategories.find(item => item.key === row.depreciationCategory)
  return category ? `${category.label} · ${category.lifeYears}年${category.methodLabel}` : '—'
}
</script>

<template>
  <div class="economic-detail-scroll">
    <el-table :data="rows" border :row-class-name="rowClass" :span-method="spanMethod" table-layout="fixed" class="economic-detail-table">
      <el-table-column prop="sectionLabel" label="基础数据" width="105" fixed="left" class-name="fixed-content-cell" />
      <el-table-column prop="categoryLabel" label="分类" width="130" fixed="left" class-name="fixed-content-cell" />
      <el-table-column label="明细项目（单位：万元）" width="260" fixed="left" class-name="fixed-content-cell">
        <template #default="{ row }">
          <strong v-if="row.rowType !== 'detail'">{{ row.item }}</strong>
          <div v-else>
            <small>{{ row.group }}</small>
            <div>{{ row.item }}</div>
            <div v-if="row.section === 'INVESTMENT'" class="investment-life-label">{{ depreciationLabel(row) }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="CT（含税）" width="135">
        <template #default="{ row }">
          <el-input-number v-if="row.rowType === 'detail'" :model-value="modelValue[row.key]?.ctIncTax" :min="0" :controls="false" class="amount-input" @update:model-value="update(row, 'ctIncTax', $event)" />
          <strong v-else>{{ cell(row, 'ctIncTax') }}</strong>
        </template>
      </el-table-column>
      <el-table-column label="IT（含税）" width="135">
        <template #default="{ row }">
          <el-input-number v-if="row.rowType === 'detail'" :model-value="modelValue[row.key]?.itIncTax" :min="0" :controls="false" class="amount-input" @update:model-value="update(row, 'itIncTax', $event)" />
          <strong v-else>{{ cell(row, 'itIncTax') }}</strong>
        </template>
      </el-table-column>
      <el-table-column label="合同金额（含税）" width="145"><template #default="{ row }"><span class="calculated-cell">{{ cell(row, 'contractIncTax') }}</span></template></el-table-column>
      <el-table-column label="税率" width="105" class-name="fixed-content-cell">
        <template #default="{ row }">
          <template v-if="row.rowType === 'detail'">
            <span v-if="row.taxRate != null" class="fixed-tax-rate">{{ row.taxRate }}%</span>
            <el-input-number v-else :model-value="modelValue[row.key]?.taxRate" :min="0" :max="100" :controls="false" class="amount-input" @update:model-value="update(row, 'taxRate', $event)" />
          </template>
        </template>
      </el-table-column>
      <el-table-column label="CT增值税" width="120"><template #header>CT增值税<br><small>CT÷(1+税率)×税率</small></template><template #default="{ row }"><span class="calculated-cell">{{ cell(row, 'ctVat') }}</span></template></el-table-column>
      <el-table-column label="IT增值税" width="120"><template #header>IT增值税<br><small>IT÷(1+税率)×税率</small></template><template #default="{ row }"><span class="calculated-cell">{{ cell(row, 'itVat') }}</span></template></el-table-column>
      <el-table-column label="不含税金额" width="130"><template #default="{ row }"><strong class="calculated-cell">{{ cell(row, 'amountExTax') }}</strong></template></el-table-column>
      <el-table-column label="关联折旧类别及年限" width="230" class-name="fixed-content-cell">
        <template #default="{ row }">{{ depreciationLabel(row) }}</template>
      </el-table-column>
      <el-table-column prop="description" label="备注/口径说明" min-width="360" class-name="fixed-content-cell" />
    </el-table>
  </div>
</template>

<style scoped>
.economic-detail-scroll { width: 100%; overflow-x: auto; }
.economic-detail-table { min-width: 1650px; }
.economic-detail-table small { display: block; color: var(--el-text-color-secondary); margin-bottom: 3px; }
.investment-life-label { margin-top: 5px; font-size: 12px; font-weight: 700; color: #694000; line-height: 1.5; white-space: normal; }
.amount-input { width: 100%; }
.amount-input :deep(.el-input__wrapper) { background: #fff; }
.economic-detail-table :deep(td.el-table__cell) { background: #fff !important; }
.fixed-tax-rate { display: inline-block; width: 100%; text-align: center; font-weight: 700; color: var(--el-text-color-regular); }
.calculated-cell { font-variant-numeric: tabular-nums; color: var(--el-text-color-primary); }
:deep(.economic-subtotal-row td) { font-weight: 700; }
:deep(.economic-total-row td) { font-weight: 800; }
</style>
