export type EconomicBenefitMatrix = Record<string, Array<number | string | null | undefined>>

export const economicBenefitInputRows = [
  { key: 'initialInvestment', label: '初始项目投资' },
  { key: 'revenue', label: '收入' },
  { key: 'expense', label: '支出' },
  { key: 'terminalResources', label: '终端及其他营销资源投入' },
  { key: 'depreciation', label: '折旧与摊销' },
] as const

export const economicBenefitOutputRows = [
  ...economicBenefitInputRows,
  { key: 'totalProfit', label: '利润总额' },
  { key: 'netProfit', label: '净利润（所得税率25%）' },
  { key: 'netProfitRate', label: '净利润率' },
  { key: 'depreciationAddBack', label: '折旧与摊销（加回）' },
  { key: 'netCashInflow', label: '净现金流入（内部收益率IRR）' },
  { key: 'cumulativeNetInflow', label: '累计净流入' },
  { key: 'discountRate', label: '折现率（4%）' },
  { key: 'discountedCashInflow', label: '折现后现金流入' },
  { key: 'discountedCashOutflow', label: '折现后现金流出' },
  { key: 'discountedNetInflow', label: '折现后净流入' },
  { key: 'discountedCumulativeNetInflow', label: '折现后累计净流入（净现值率）' },
  { key: 'staticPayback', label: '静态回收期' },
  { key: 'dynamicPayback', label: '动态回收期' },
] as const

export interface EconomicBenefitResult {
  values: EconomicBenefitMatrix
  irr: number | null
  netProfitRate: number | null
  npvRate: number | null
  staticPayback: number | null
  dynamicPayback: number | null
}

export function calculateEconomicBaseData(
  incomeItems: Array<{ amountExTax?: number | null }> = [],
  costItems: Array<{ mode?: string | null; amountExTax?: number | null }> = [],
) {
  const sum = (items: Array<{ amountExTax?: number | null }>) => rounded(items.reduce((total, item) => total + numberValue(item.amountExTax), 0))
  return {
    estimatedInvestment: sum(costItems.filter(item => item.mode === '投资部分')),
    directRevenue: sum(incomeItems),
    operatingExpense: sum(costItems.filter(item => item.mode !== '投资部分')),
  }
}

const numberValue = (value: unknown) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

const rounded = (value: number, digits = 2) => {
  const factor = 10 ** digits
  return Math.round((value + Number.EPSILON) * factor) / factor
}

export function distributeEvenly(totalValue: unknown, yearCount: number): Array<number | null> {
  const years = Math.max(1, Math.min(10, Math.trunc(numberValue(yearCount) || 1)))
  const total = Math.abs(numberValue(totalValue))
  if (!total) return Array(years).fill(null)
  const base = Math.floor((total / years) * 100) / 100
  const values = Array(years).fill(base) as number[]
  values[years - 1] = rounded(total - base * (years - 1))
  return values
}

const amountText = (value: number) => rounded(value).toFixed(2)
const percentText = (value: number | null) => value == null ? '' : `${rounded(value * 100).toFixed(2)}%`
const yearsText = (value: number | null) => value == null ? '' : rounded(value).toFixed(2)

function irr(cashFlows: number[], guess = -0.3) {
  if (cashFlows.length < 2 || !cashFlows.some(value => value < 0) || !cashFlows.some(value => value > 0)) return null
  const npv = (rate: number) => cashFlows.reduce((sum, value, index) => sum + value / ((1 + rate) ** index), 0)
  let candidate = guess
  for (let iteration = 0; iteration < 100; iteration++) {
    const value = npv(candidate)
    const derivative = cashFlows.reduce((sum, cashFlow, index) => index === 0 ? sum : sum - index * cashFlow / ((1 + candidate) ** (index + 1)), 0)
    if (!Number.isFinite(value) || !Number.isFinite(derivative) || Math.abs(derivative) < 1e-12) break
    const next = candidate - value / derivative
    if (!Number.isFinite(next) || next <= -1) break
    if (Math.abs(next - candidate) < 1e-10 && Math.abs(npv(next)) < 1e-7) return next
    candidate = next
  }
  let previousRate = -0.99
  let previousValue = npv(previousRate)
  for (let step = 1; step <= 2400; step++) {
    const rate = -0.99 + step * (10.99 / 2400)
    const value = npv(rate)
    if (!Number.isFinite(value)) continue
    if (value === 0) return rate
    if (previousValue * value < 0) {
      let low = previousRate
      let high = rate
      let lowValue = previousValue
      for (let iteration = 0; iteration < 100; iteration++) {
        const middle = (low + high) / 2
        const middleValue = npv(middle)
        if (Math.abs(middleValue) < 1e-9) return middle
        if (lowValue * middleValue <= 0) high = middle
        else {
          low = middle
          lowValue = middleValue
        }
      }
      return (low + high) / 2
    }
    previousRate = rate
    previousValue = value
  }
  return null
}

function paybackCandidates(
  cashFlows: number[],
  cumulative: number[],
  firstYearOutflow: number,
  firstYearInflow: number,
  maximumFormulaYears = cashFlows.length,
) {
  const candidates: Array<number | null> = Array(cashFlows.length).fill(null)
  if (cumulative[0] >= 0 && firstYearInflow !== 0) candidates[0] = -firstYearOutflow / firstYearInflow
  for (let index = 1; index < Math.min(cashFlows.length, maximumFormulaYears); index++) {
    if (cumulative[index - 1] < 0 && cumulative[index] > 0 && cashFlows[index] !== 0) {
      // Excel uses the previous year label minus previous cumulative cash flow
      // divided by the current year's cash flow (for example B53-B64/C63).
      candidates[index] = index - cumulative[index - 1] / cashFlows[index]
    }
  }
  return candidates
}

export function createEconomicBenefitInputs(yearCount: number, source?: EconomicBenefitMatrix): EconomicBenefitMatrix {
  const years = Math.max(1, Math.min(10, Math.trunc(numberValue(yearCount) || 1)))
  return Object.fromEntries(economicBenefitInputRows.map(row => {
    const existing = source?.[row.key] || []
    return [row.key, Array.from({ length: years }, (_, index) => Math.abs(numberValue(existing[index])) || null)]
  }))
}

export function calculateEconomicBenefit(
  source: EconomicBenefitMatrix,
  yearCount: number,
  discountRate = 0.04,
  incomeTaxRate = 0.25,
): EconomicBenefitResult {
  const years = Math.max(1, Math.min(10, Math.trunc(numberValue(yearCount) || 1)))
  const input = (key: string) => Array.from({ length: years }, (_, index) => Math.abs(numberValue(source[key]?.[index])))
  // Match the workbook literally: manually entered investment, revenue,
  // expense and terminal-resource amounts keep their entered positive signs.
  // Only depreciation is negated because the workbook references the
  // depreciation schedule with a leading minus sign.
  const initialInvestment = input('initialInvestment')
  const revenue = input('revenue')
  const expense = input('expense')
  const terminalResources = input('terminalResources')
  const depreciation = input('depreciation').map(value => -value)
  const totalProfit = revenue.map((value, index) => value + expense[index] + terminalResources[index] + depreciation[index])
  const netProfit = totalProfit.map(value => value * (1 - incomeTaxRate))
  const annualProfitRate = netProfit.map((value, index) => revenue[index] ? value / revenue[index] : 0)
  const depreciationAddBack = depreciation.map(value => -value)
  const netCashInflow = netProfit.map((value, index) => value + depreciationAddBack[index] + initialInvestment[index])
  const cumulativeNetInflow: number[] = []
  netCashInflow.forEach((value, index) => cumulativeNetInflow.push(value + (cumulativeNetInflow[index - 1] || 0)))
  const discountFactor = Array.from({ length: years }, (_, index) => (1 + discountRate) ** (index + 1))
  const discountedCashInflow = revenue.map((value, index) => value / discountFactor[index])
  const discountedCashOutflow = totalProfit.map((profit, index) =>
    (initialInvestment[index] + expense[index] + terminalResources[index] - profit * incomeTaxRate) / discountFactor[index])
  const discountedNetInflow = netCashInflow.map((value, index) => value / discountFactor[index])
  const discountedCumulativeNetInflow: number[] = []
  discountedNetInflow.forEach((value, index) => discountedCumulativeNetInflow.push(value + (discountedCumulativeNetInflow[index - 1] || 0)))

  const sum = (values: number[]) => values.reduce((total, value) => total + value, 0)
  const totalRevenue = sum(revenue)
  const totalNetProfit = sum(netProfit)
  const overallNetProfitRate = totalRevenue ? totalNetProfit / totalRevenue : null
  const totalDiscountedOutflow = sum(discountedCashOutflow)
  const npvRate = totalDiscountedOutflow ? -sum(discountedNetInflow) / totalDiscountedOutflow : null
  const irrValue = irr(netCashInflow)
  // The current workbook contains static-payback formulas in years 1-7 and
  // dynamic-payback formulas in years 1-10.
  const staticPaybackCandidates = paybackCandidates(
    netCashInflow,
    cumulativeNetInflow,
    initialInvestment[0] + expense[0] + terminalResources[0] - totalProfit[0] * incomeTaxRate,
    revenue[0],
    7,
  )
  const dynamicPaybackCandidates = paybackCandidates(
    discountedNetInflow,
    discountedCumulativeNetInflow,
    discountedCashOutflow[0],
    discountedCashInflow[0],
  )
  const lastCandidate = (values: Array<number | null>) => [...values].reverse().find(value => value != null) ?? null
  const staticPayback = lastCandidate(staticPaybackCandidates)
  const dynamicPayback = lastCandidate(dynamicPaybackCandidates)

  const totals = (values: number[]) => [...values.map(amountText), amountText(sum(values))]
  const values: EconomicBenefitMatrix = {
    initialInvestment: totals(initialInvestment),
    revenue: totals(revenue),
    expense: totals(expense),
    terminalResources: totals(terminalResources),
    depreciation: totals(depreciation),
    totalProfit: totals(totalProfit),
    netProfit: totals(netProfit),
    netProfitRate: [...annualProfitRate.map(percentText), percentText(overallNetProfitRate)],
    depreciationAddBack: [...depreciationAddBack.map(amountText), ''],
    netCashInflow: [...netCashInflow.map(amountText), percentText(irrValue)],
    cumulativeNetInflow: [...cumulativeNetInflow.map(amountText), ''],
    discountRate: [...discountFactor.map(value => rounded(value, 3).toFixed(3)), ''],
    discountedCashInflow: [...discountedCashInflow.map(amountText), ''],
    discountedCashOutflow: totals(discountedCashOutflow),
    discountedNetInflow: totals(discountedNetInflow),
    discountedCumulativeNetInflow: [...discountedCumulativeNetInflow.map(amountText), percentText(npvRate)],
    staticPayback: [...staticPaybackCandidates.map(yearsText), yearsText(staticPayback)],
    dynamicPayback: [...dynamicPaybackCandidates.map(yearsText), yearsText(dynamicPayback)],
  }
  return { values, irr: irrValue, netProfitRate: overallNetProfitRate, npvRate, staticPayback, dynamicPayback }
}
