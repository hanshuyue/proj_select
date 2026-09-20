import assert from 'node:assert/strict'
import test from 'node:test'
import { financeAmountExTax, summarizeFinanceItems } from '../financeSummary.ts'

test('totals follow the details and ignore stale VAT outputs and legacy subtotals', () => {
  const items = [
    { mode: '投资部分', name: 'DICT投资', amountIncTax: 113, taxRate: 13, amountExTax: 999 },
    { mode: '合作服务模式', name: 'ICT成本', amountIncTax: 106, taxRate: 6 },
    { mode: '投资部分', name: '小计', amountIncTax: 113 },
    { mode: '合计', amountIncTax: 219 },
  ]
  assert.deepEqual(summarizeFinanceItems(items), { amountIncTax: 219, amountExTax: 200 })
  assert.deepEqual(summarizeFinanceItems(items.slice(0, 1)), { amountIncTax: 113, amountExTax: 100 })
  assert.deepEqual(summarizeFinanceItems([]), { amountIncTax: 0, amountExTax: 0 })
})

test('editing or clearing amounts and tax rates immediately changes results', () => {
  const row = { amountIncTax: 106, taxRate: 6 }
  assert.equal(financeAmountExTax(row), 100)
  row.amountIncTax = 212
  assert.equal(financeAmountExTax(row), 200)
  row.taxRate = 0
  assert.equal(financeAmountExTax(row), 212)
  assert.equal(financeAmountExTax({}), undefined)
})

test('calculation keeps input precision and leaves rounding to the display layer', () => {
  const amount = 12.345678901234
  const result = financeAmountExTax({ amountIncTax: amount, taxRate: 6.123456789 })!
  assert.ok(Math.abs(result - amount / (1 + 6.123456789 / 100)) < 1e-13)
  assert.equal(summarizeFinanceItems([
    { name: '明细1', amountIncTax: 0.123456789, taxRate: 6 },
    { name: '明细2', amountIncTax: 0.000000011, taxRate: 6 },
  ]).amountIncTax, 0.1234568)
})
