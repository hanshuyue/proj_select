export interface SummaryItem {
  mode?: string
  name?: string
  amountIncTax?: number
  amountExTax?: number
  taxRate?: number
}

const numeric = (value: unknown) => Number.isFinite(Number(value)) ? Number(value) : 0
// Excel and JavaScript both store numeric cells as IEEE-754 doubles. Normalizing to
// Excel's 15 significant digits removes binary tail noise without imposing a
// fixed number of decimal places on user input.
const excelPrecision = (value: number) => Number(value.toPrecision(15))

export function financeAmountExTax(item: SummaryItem): number | undefined {
  if (item.amountIncTax == null) return undefined
  return excelPrecision(numeric(item.amountIncTax) / (1 + Math.max(0, numeric(item.taxRate)) / 100))
}

export function summarizeFinanceItems(items: SummaryItem[]) {
  const details = items.filter(item => item.name !== '小计' && item.mode !== '合计')
  return {
    amountIncTax: excelPrecision(details.reduce((sum, item) => sum + numeric(item.amountIncTax), 0)),
    amountExTax: excelPrecision(details.reduce((sum, item) => sum + (financeAmountExTax(item) ?? 0), 0)),
  }
}
