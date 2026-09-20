import assert from 'node:assert/strict'
import test from 'node:test'
import { allocateDepreciationInvestments, calculateDepreciation } from '../depreciation.ts'
import {
  calculateEconomicBaseDetailTotals,
  calculateEconomicBaseRow,
  economicBaseDetailDefinitions,
  normalizeEconomicBaseDetailValues,
} from '../economicBaseDetail.ts'

test('standard base table calculates contract amount, VAT and amount excluding tax', () => {
  const values = normalizeEconomicBaseDetailValues({
    incomeIntegration: { ctIncTax: 60, itIncTax: 46, taxRate: 6 },
  })
  const definition = economicBaseDetailDefinitions.find(item => item.key === 'incomeIntegration')!
  const result = calculateEconomicBaseRow(definition, values)
  assert.equal(result.contractIncTax, 106)
  assert.equal(result.taxRate, 6)
  assert.ok(Math.abs(result.ctVat - 60 / 1.06 * 0.06) < 1e-10)
  assert.ok(Math.abs(result.itVat - 46 / 1.06 * 0.06) < 1e-10)
  assert.ok(Math.abs(result.amountExTax - 100) < 1e-10)
})

test('income, investment and cost subtotals drive the three estimated values', () => {
  const values = normalizeEconomicBaseDetailValues({
    incomeIntegration: { ctIncTax: 106, taxRate: 6 },
    investmentPlatformHardware: { itIncTax: 113, taxRate: 13 },
    costConstructionIntegration: { ctIncTax: 53, taxRate: 6 },
  })
  const totals = calculateEconomicBaseDetailTotals(values)
  assert.equal(totals.directRevenue, 100)
  assert.equal(totals.estimatedInvestment, 100)
  assert.equal(totals.operatingExpense, 50)
  assert.equal(totals.sections.INCOME.contractIncTax, 106)
  assert.equal(totals.sections.INVESTMENT.contractIncTax, 113)
  assert.equal(totals.sections.COST.contractIncTax, 53)
  assert.deepEqual(totals.investmentCategories, {
    platform: 100,
    transmission: 0,
    fiberPipeline: 0,
    software: 0,
    other: 0,
  })
})

test('tax rates are fixed by detail definition and ignore persisted overrides', () => {
  const values = normalizeEconomicBaseDetailValues({
    incomeIntegration: { ctIncTax: 106, taxRate: 99 },
  })
  const definition = economicBaseDetailDefinitions.find(item => item.key === 'incomeIntegration')!
  assert.equal(values.incomeIntegration.taxRate, 6)
  assert.equal(calculateEconomicBaseRow(definition, values).amountExTax, 100)
})

test('other income follows the blank, user-entered tax rate in the current reference workbook', () => {
  const definition = economicBaseDetailDefinitions.find(item => item.key === 'incomeOther')!
  assert.equal(definition.taxRate, null)
  const row = calculateEconomicBaseRow(definition, { incomeOther: { itIncTax: 10.9, taxRate: 9 } })
  assert.ok(Math.abs(row.itVat - 0.9) < 1e-10)
  assert.ok(Math.abs(row.amountExTax - 10) < 1e-10)
})

test('construction integration and bid service use current workbook tax rates', () => {
  assert.equal(economicBaseDetailDefinitions.find(item => item.key === 'costConstructionIntegration')!.taxRate, 6)
  assert.equal(economicBaseDetailDefinitions.find(item => item.key === 'costConstructionBid')!.taxRate, 6)
})

test('rows whose workbook tax rate is blank retain a user-entered tax rate', () => {
  const values = normalizeEconomicBaseDetailValues({
    investmentPlatformSupporting: { ctIncTax: 109, taxRate: 9 },
  })
  const definition = economicBaseDetailDefinitions.find(item => item.key === 'investmentPlatformSupporting')!
  assert.equal(definition.taxRate, null)
  assert.equal(values.investmentPlatformSupporting.taxRate, 9)
  assert.equal(calculateEconomicBaseRow(definition, values).amountExTax, 100)
})

test('other network investment follows the workbook single-detail structure', () => {
  assert.equal(economicBaseDetailDefinitions.some(item => item.key === 'investmentOtherNetwork'), false)
  const row = economicBaseDetailDefinitions.find(item => item.key === 'investmentOtherConstruction')!
  assert.equal(row.group, '3、其他网络配套投资')
  assert.equal(row.depreciationCategory, 'other')
})

test('investment groups link their own detail amounts to the correct depreciation category and lifetime', () => {
  const values = normalizeEconomicBaseDetailValues({
    investmentPlatformHardware: { ctIncTax: 113 },
    investmentPlatformSoftware: { itIncTax: 106 },
    investmentPlatformIntegration: { itIncTax: 10.6 },
    investmentPlatformSupporting: { ctIncTax: 10 },
    investmentPlatformInstallation: { itIncTax: 10 },
    investmentPlatformDesign: { ctIncTax: 10.6 },
    investmentTransmissionPipeline: { ctIncTax: 113 },
    investmentTransmissionCable: { itIncTax: 226 },
    investmentTransmissionEquipment: { ctIncTax: 79.1 },
    // Customer-owned equipment is a cost, not our depreciable asset.
    costConstructionEquipment: { itIncTax: 1130 },
  })
  const totals = calculateEconomicBaseDetailTotals(values)
  assert.deepEqual(totals.investmentCategories, {
    platform: 140, software: 100, fiberPipeline: 300, transmission: 70, other: 0,
  })
  const schedule = allocateDepreciationInvestments(totals.investmentCategories, [610], 20)
  const result = calculateDepreciation(schedule, 20)
  assert.equal(result.byCategory.platform[0], 28)
  assert.equal(result.byCategory.software[0], 20)
  assert.equal(result.byCategory.fiberPipeline[0], 30)
  assert.equal(result.byCategory.transmission[0], 10)
  assert.equal(result.byCategory.platform[5], 0)
  assert.equal(result.byCategory.transmission[7], 0)
  assert.equal(result.byCategory.fiberPipeline[10], 0)
  assert.equal(result.totals.reduce((sum, value) => sum + value, 0), 610)
  values.investmentTransmissionEquipment = { ctIncTax: 0, itIncTax: 0 }
  assert.equal(calculateEconomicBaseDetailTotals(values).investmentCategories.transmission, 0)
})
