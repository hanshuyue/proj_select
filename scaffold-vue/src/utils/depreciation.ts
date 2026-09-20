export interface DepreciationCategory {
  key: string
  label: string
  lifeYears: number
  methodLabel: string
}

export interface DepreciationCategoryInput {
  commissioningMonths: Array<number | null>
  investments: Array<number | null>
}

export type DepreciationSchedule = Record<string, DepreciationCategoryInput>

// Source sheet 折旧和摊销计算表 E:X; economic assessment references E:N only.
export const DEPRECIATION_HORIZON_YEARS = 20

export const depreciationCategories: DepreciationCategory[] = [
  { key: 'platform', label: '平台类设备投资', lifeYears: 5, methodLabel: '折旧' },
  { key: 'transmission', label: '传输网设备类投资', lifeYears: 7, methodLabel: '折旧' },
  { key: 'fiberPipeline', label: '光缆及管道类投资', lifeYears: 10, methodLabel: '折旧' },
  { key: 'software', label: '软件类投资', lifeYears: 5, methodLabel: '摊销' },
  { key: 'other', label: '其他类投资', lifeYears: 5, methodLabel: '折旧' },
]

const numeric = (value: unknown) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const nullableNumber = (value: unknown) => {
  if (value === '' || value == null) return null
  return numeric(value)
}

export function createDepreciationSchedule(yearCount: number, source?: DepreciationSchedule): DepreciationSchedule {
  const years = Math.max(1, Math.min(DEPRECIATION_HORIZON_YEARS, Math.trunc(numeric(yearCount) || 1)))
  return Object.fromEntries(depreciationCategories.map(category => {
    const current = source?.[category.key]
    return [category.key, {
      commissioningMonths: Array.from({ length: years }, (_, index) => nullableNumber(current?.commissioningMonths?.[index])),
      investments: Array.from({ length: years }, (_, index) => nullableNumber(current?.investments?.[index])),
    }]
  }))
}

export function hasDepreciationScheduleInput(schedule: DepreciationSchedule) {
  return depreciationCategories.some(category => {
    const input = schedule?.[category.key]
    return input?.investments?.some(value => numeric(value) !== 0)
      || input?.commissioningMonths?.some(value => value != null)
  })
}

/** Allocate the annual investment plan by the base table's asset mix. */
export function allocateDepreciationInvestments(
  categoryTotals: Record<string, number>,
  annualInvestment: Array<unknown>,
  yearCount: number,
  source?: DepreciationSchedule,
): DepreciationSchedule {
  const schedule = createDepreciationSchedule(yearCount, source)
  const total = depreciationCategories.reduce((sum, category) => sum + Math.max(0, numeric(categoryTotals[category.key])), 0)
  for (const category of depreciationCategories) {
    schedule[category.key].investments = schedule[category.key].investments.map((_, index) => {
      const annual = Math.abs(numeric(annualInvestment[index]))
      return total > 0 && annual > 0 ? annual * Math.max(0, numeric(categoryTotals[category.key])) / total : null
    })
  }
  return schedule
}

export function calculateDepreciation(schedule: DepreciationSchedule, yearCount: number) {
  const years = Math.max(1, Math.min(DEPRECIATION_HORIZON_YEARS, Math.trunc(numeric(yearCount) || 1)))
  const byCategory: Record<string, number[]> = {}
  const totals = Array(years).fill(0) as number[]

  for (const category of depreciationCategories) {
    const annual = Array(years).fill(0) as number[]
    const input = schedule?.[category.key]
    for (let investmentYear = 0; investmentYear < years; investmentYear++) {
      const investment = Math.abs(numeric(input?.investments?.[investmentYear]))
      if (!investment) continue
      const month = Math.max(0, Math.min(12, numeric(input?.commissioningMonths?.[investmentYear])))
      const fullYearDepreciation = investment / category.lifeYears
      let remaining = investment
      for (let outputYear = investmentYear; outputYear < years && remaining > 1e-9; outputYear++) {
        const amount = outputYear === investmentYear
          ? Math.min(remaining, fullYearDepreciation * (1 - month / 12))
          : Math.min(remaining, fullYearDepreciation)
        annual[outputYear] += amount
        remaining -= amount
      }
    }
    byCategory[category.key] = annual
    byCategory[category.key].forEach((value, index) => { totals[index] += value })
  }

  return {
    byCategory,
    totals,
  }
}
