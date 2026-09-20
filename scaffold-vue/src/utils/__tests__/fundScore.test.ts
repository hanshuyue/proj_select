import test from 'node:test'
import assert from 'node:assert/strict'
import { calculateFundScores, fundHistoryRows, governmentFundCurrentRows, mappedFundAverageScore, mappedFundScore } from '../fundScore.ts'

test('fund history item selection carries the configured deduction', () => {
  assert.equal(mappedFundScore(fundHistoryRows[0].options, '工业能源'), 18.22)
  assert.equal(mappedFundScore(fundHistoryRows[1].options, '央、国企'), 8.9)
  assert.equal(mappedFundAverageScore(fundHistoryRows[2].options, ['客户自筹资金', '其他（JD、银校合作）']), 27.5)
})

test('fund score rounds decimal deductions for display and export', () => {
  const result = calculateFundScores({
    fundHistoryIndustryDeduction: 18.22,
    fundHistoryCustomerDeduction: 8.9,
    fundHistorySourceDeduction: 50,
  }, 'GOVERNMENT')
  assert.equal(result.historyDeduction, 77.12)
  assert.equal(result.history, 22.88)
})

test('current project risk selection carries a fixed deduction', () => {
  const historyRow = governmentFundCurrentRows[0]
  assert.equal(mappedFundScore(historyRow.options, '不涉及'), 0)
  assert.equal(mappedFundScore(historyRow.options, '3次及以上'), 30)
})

test('fund score uses only the current project type rows', () => {
  const sections = {
    fundCurrentAuditDeduction: 80,
    fundCurrentHistoryDeduction: 10,
    fundCurrentSourceDeduction: 5,
    fundCurrentDebtDeduction: 5,
    fundCurrentCounterpartyDeduction: 80,
    fundSpecialBonusScore: 20,
  }
  const government = calculateFundScores(sections, 'GOVERNMENT')
  assert.equal(government.current, 80)
  assert.equal(government.finalScore, 100)
  assert.equal(government.riskLevel, '低')
  const enterprise = calculateFundScores(sections, 'ENTERPRISE')
  assert.equal(enterprise.current, 0)
  assert.equal(enterprise.finalScore, 60)
})

test('detailed current rows are summed and override legacy aggregates', () => {
  const sections = {
    fundCurrentHistoryDeduction: 99,
    fundGovernmentHistory1Item: '3次及以上',
    fundGovernmentHistory1Deduction: 30,
    fundGovernmentSource1Item: '资金来源不明确',
    fundGovernmentSource1Deduction: 20,
  }
  const result = calculateFundScores(sections, 'GOVERNMENT')
  assert.equal(result.currentDeduction, 50)
  assert.equal(result.current, 50)
})
