import test from 'node:test'
import assert from 'node:assert/strict'
import { calculateEconomicBaseData, calculateEconomicBenefit, createEconomicBenefitInputs, distributeEvenly } from '../economicBenefit.ts'
import { calculateDepreciation, createDepreciationSchedule } from '../depreciation.ts'

test('direct revenue and expenditure details map to the three estimated totals', () => {
  assert.deepEqual(calculateEconomicBaseData(
    [{ amountExTax: 60 }, { amountExTax: 40 }],
    [{ mode: '投资部分', amountExTax: 30 }, { mode: '合作服务模式', amountExTax: 20 }],
  ), { estimatedInvestment: 30, directRevenue: 100, operatingExpense: 20 })
})

test('base totals are distributed across the selected project years without losing cents', () => {
  assert.deepEqual(distributeEvenly(100, 3), [33.33, 33.33, 33.34])
  assert.deepEqual(distributeEvenly(0, 2), [null, null])
})

test('economic benefit model calculates audited annual totals and profitability', () => {
  const inputs = createEconomicBenefitInputs(3)
  inputs.initialInvestment = [100, null, null]
  inputs.revenue = [80, 80, 80]
  inputs.expense = [20, 20, 20]
  inputs.terminalResources = [5, 5, 5]
  inputs.depreciation = [20, 20, 20]
  const result = calculateEconomicBenefit(inputs, 3)

  assert.equal(result.values.initialInvestment[0], '100.00')
  assert.equal(result.values.revenue[3], '240.00')
  assert.equal(result.values.netProfit[3], '191.25')
  assert.equal(result.values.netProfitRate[3], '79.69%')
  assert.equal(result.values.depreciationAddBack[3], '')
  assert.equal(result.values.cumulativeNetInflow[3], '')
  assert.equal(result.values.discountedCashInflow[3], '')
  assert.ok(result.staticPayback != null && result.staticPayback < 0)
})

test('three entered amounts reproduce the current workbook example exactly', () => {
  const inputs = createEconomicBenefitInputs(10)
  inputs.initialInvestment[0] = 30
  inputs.revenue[0] = 100
  inputs.expense[0] = 5
  const result = calculateEconomicBenefit(inputs, 10)

  assert.equal(result.values.initialInvestment[10], '30.00')
  assert.equal(result.values.revenue[10], '100.00')
  assert.equal(result.values.expense[10], '5.00')
  assert.equal(result.values.totalProfit[0], '105.00')
  assert.equal(result.values.netProfit[0], '78.75')
  assert.equal(result.values.netProfitRate[0], '78.75%')
  assert.equal(result.values.netCashInflow[0], '108.75')
  assert.equal(result.values.discountedCashInflow[0], '96.15')
  assert.equal(result.values.discountedCashOutflow[0], '8.41')
  assert.equal(result.values.discountedNetInflow[0], '104.57')
  assert.equal(result.values.discountedCumulativeNetInflow[10], '-1242.86%')
  assert.equal(result.values.staticPayback[0], '-0.09')
  assert.equal(result.values.staticPayback[10], '-0.09')
  assert.equal(result.values.dynamicPayback[0], '-0.09')
  assert.equal(result.values.dynamicPayback[10], '-0.09')
  assert.equal(result.values.netCashInflow[10], '')
})

test('economic benefit model guards return metrics when cash-flow signs are invalid', () => {
  const inputs = createEconomicBenefitInputs(2)
  inputs.revenue = [100, 100]
  const result = calculateEconomicBenefit(inputs, 2)
  assert.equal(result.irr, null)
  assert.equal(result.values.netCashInflow[2], '')
})

test('economic benefit model starts with blank inputs and no calculated payback period', () => {
  const inputs = createEconomicBenefitInputs(2)
  assert.deepEqual(inputs.revenue, [null, null])
  const result = calculateEconomicBenefit(inputs, 2)
  assert.equal(result.staticPayback, null)
  assert.equal(result.dynamicPayback, null)
})

test('depreciation schedule remains linked through the workbook ten-year horizon', () => {
  const schedule = createDepreciationSchedule(10)
  schedule.platform.investments = [33.33, 33.33, 33.34, null, null, null, null, null, null, null]
  schedule.platform.commissioningMonths = [6, 3, 9, null, null, null, null, null, null, null]
  const depreciation = calculateDepreciation(schedule, 10)
  const inputs = createEconomicBenefitInputs(10)
  inputs.initialInvestment = [33.33, 33.33, 33.34, null, null, null, null, null, null, null]
  inputs.revenue = [59, 59, 59, null, null, null, null, null, null, null]
  inputs.expense = [16.66, 16.66, 16.68, null, null, null, null, null, null, null]
  inputs.depreciation = depreciation.totals
  const result = calculateEconomicBenefit(inputs, 10)

  assert.deepEqual(depreciation.totals.map(value => Number(value.toFixed(4))), [3.333, 11.6655, 14.999, 20, 20, 16.667, 8.3345, 5.001, 0, 0])
  assert.equal(result.values.depreciation[0], '-3.33')
  assert.equal(result.values.depreciation[9], '0.00')
  assert.equal(result.values.totalProfit[10], '127.00')
  assert.equal(result.values.netProfit[10], '95.25')
  assert.equal(result.values.netProfitRate[10], '53.81%')
})
