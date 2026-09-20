import test from 'node:test'
import assert from 'node:assert/strict'
import { allocateDepreciationInvestments, calculateDepreciation, createDepreciationSchedule } from '../depreciation.ts'

test('year ten fiber investment continues to year twenty, matching source E:X columns', () => {
  const schedule = createDepreciationSchedule(20)
  schedule.fiberPipeline.investments[9] = 100
  schedule.fiberPipeline.commissioningMonths[9] = 6
  const result = calculateDepreciation(schedule, 20)
  assert.equal(result.totals.length, 20)
  assert.deepEqual(result.totals.slice(0, 9), Array(9).fill(0))
  assert.equal(result.totals[9], 5)
  assert.deepEqual(result.totals.slice(10, 19), Array(9).fill(10))
  assert.equal(result.totals[19], 5)
  assert.equal(result.totals.reduce((sum, value) => sum + value, 0), 100)
  assert.deepEqual(calculateDepreciation(schedule, 10).totals, result.totals.slice(0, 10))
})

test('each investment year begins its own asset lifetime without shifting annual columns', () => {
  const schedule = createDepreciationSchedule(20)
  schedule.platform.investments[2] = 50
  schedule.transmission.investments[4] = 70
  const result = calculateDepreciation(schedule, 20)
  assert.deepEqual(result.byCategory.platform, [...Array(2).fill(0), ...Array(5).fill(10), ...Array(13).fill(0)])
  assert.deepEqual(result.byCategory.transmission, [...Array(4).fill(0), ...Array(7).fill(10), ...Array(9).fill(0)])
})

test('four asset classes depreciate over the specified 5, 7, 10 and 5 years', () => {
  const schedule = createDepreciationSchedule(10)
  for (const [key, amount] of Object.entries({ platform: 50, transmission: 70, fiberPipeline: 100, software: 50 })) {
    schedule[key].investments[0] = amount
  }
  const result = calculateDepreciation(schedule, 10)
  assert.deepEqual(result.byCategory.platform, [10, 10, 10, 10, 10, 0, 0, 0, 0, 0])
  assert.deepEqual(result.byCategory.transmission, [10, 10, 10, 10, 10, 10, 10, 0, 0, 0])
  assert.deepEqual(result.byCategory.fiberPipeline, Array(10).fill(10))
  assert.deepEqual(result.byCategory.software, result.byCategory.platform)
  assert.equal(result.totals.reduce((sum, value) => sum + value, 0), 270)
})

test('asset mix follows manual annual plans and preserves commissioning months', () => {
  const source = createDepreciationSchedule(10)
  source.platform.commissioningMonths[1] = 6
  const schedule = allocateDepreciationInvestments({ platform: 50, software: 50 }, [20, 80], 10, source)
  assert.deepEqual(schedule.platform.investments.slice(0, 3), [10, 40, null])
  assert.equal(calculateDepreciation(schedule, 10).byCategory.platform[1], 6)
  const empty = allocateDepreciationInvestments({}, [100], 10, schedule)
  assert.deepEqual(calculateDepreciation(empty, 10).totals, Array(10).fill(0))
})

test('depreciation follows the source workbook commissioning-month formula', () => {
  const schedule = createDepreciationSchedule(6)
  schedule.platform.investments[0] = 100
  schedule.platform.commissioningMonths[0] = 6
  const result = calculateDepreciation(schedule, 6)
  assert.deepEqual(result.byCategory.platform, [10, 20, 20, 20, 20, 10])
  assert.deepEqual(result.totals, [10, 20, 20, 20, 20, 10])
})

test('depreciation combines asset classes and annual investment vintages', () => {
  const schedule = createDepreciationSchedule(3)
  schedule.platform.investments[0] = 50
  schedule.software.investments[1] = 100
  const result = calculateDepreciation(schedule, 3)
  assert.deepEqual(result.totals, [10, 30, 30])
})
