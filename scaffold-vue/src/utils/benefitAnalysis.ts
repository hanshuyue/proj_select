import type { EconomicBenefitMatrix } from './economicBenefit.ts'

export interface BenefitAnalysisRow {
  no?: string
  mode?: string
  metric?: string
}

export interface BenefitAnalysisSources {
  overall: EconomicBenefitMatrix
  investment: EconomicBenefitMatrix
  cooperation: EconomicBenefitMatrix
}

function sourceForRow(row: BenefitAnalysisRow, sources: BenefitAnalysisSources) {
  const no = String(row.no || '').trim()
  const mode = String(row.mode || '')
  if (no === '1.2' || mode.includes('合作部分IT净利润率')) return sources.cooperation
  if (no === '1.1') return sources.investment
  return sources.overall
}

export function calculatedBenefitAnalysisValue(
  row: BenefitAnalysisRow,
  sources: BenefitAnalysisSources,
  totalIndex: number,
) {
  const values = sourceForRow(row, sources)
  const metric = String(row.metric || '')
  if (metric.includes('内部收益率')) return values.netCashInflow?.[totalIndex] || ''
  if (metric.includes('净利润率')) return values.netProfitRate?.[totalIndex] || ''
  if (metric.includes('净现值率')) return values.discountedCumulativeNetInflow?.[totalIndex] || ''
  if (metric.includes('静态回收期')) return values.staticPayback?.[totalIndex] ? `${values.staticPayback[totalIndex]}年` : ''
  if (metric.includes('动态回收期')) return values.dynamicPayback?.[totalIndex] ? `${values.dynamicPayback[totalIndex]}年` : ''
  return ''
}
