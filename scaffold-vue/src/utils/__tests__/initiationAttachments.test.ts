import assert from 'node:assert/strict'
import test from 'node:test'
import { initiationAttachmentCatalog, requiredInitiationAttachments, missingInitiationAttachments } from '../initiationAttachments.ts'

const types = (s: Record<string, any>) => requiredInitiationAttachments(s).map(row => row.type)

test('catalog has unique upload types and covers missing template attachment slots', () => {
  assert.equal(new Set(initiationAttachmentCatalog.map(row => row.type)).size, initiationAttachmentCatalog.length)
  for (const type of ['REQUESTER_CONTRACT_FILE', 'NONSTANDARD_PROCUREMENT_FILE', 'FUND_BONUS_PROOF_FILE', 'MAINTENANCE_LIST_FILE', 'CITY_DECISION_FILE', 'PROVINCE_DECISION_FILE', 'SELECTION_RECHECK_IMAGE']) {
    assert.ok(initiationAttachmentCatalog.some(row => row.type === type && row.page && row.hint))
  }
})

test('requirements follow project conditions rather than all optional attachments', () => {
  const basic = types({ acquisitionMethod: '洽谈' })
  assert.ok(!basic.includes('TENDER_DOCUMENT'))
  assert.ok(!basic.includes('BENEFIT_CALCULATION_FILE'))
  assert.ok(!basic.includes('MAINTENANCE_LIST_FILE'))
  const enabled = types({ acquisitionMethod: '公开投标', preDecisionOpinionsEnabled: true, partnerAppendixEnabled: true, fundSpecialBonusScore: 20, attachmentRequirementSelections: ['MAINTENANCE_LIST_FILE'] })
  for (const type of ['TENDER_DOCUMENT', 'AWARD_NOTICE', 'CITY_DECISION_FILE', 'PROVINCE_DECISION_FILE', 'SELECTION_RECHECK_IMAGE', 'FUND_BONUS_PROOF_FILE', 'MAINTENANCE_LIST_FILE']) assert.ok(enabled.includes(type))
})

test('funding types activate corresponding supporting documents', () => {
  assert.ok(types({ fundType: '专项债' }).includes('FUND_BOND_ISSUANCE_FILE'))
  assert.ok(types({ fundType: '财政资金（计划外追加预算）' }).includes('FUND_FINANCE_APPROVAL_FILE'))
  assert.ok(types({ fundType: '奖补资金' }).includes('FUND_SUBSIDY_APPLICATION_FILE'))
  assert.ok(types({ fundType: '自筹资金' }).includes('FUND_ACCOUNT_BALANCE_FILE'))
  assert.ok(!types({ fundType: '奖补资金' }).includes('FUND_BOND_ISSUANCE_FILE'))
})

test('upload satisfies only its own type, deleting the last file restores the reminder', () => {
  const s = { attachmentRequirementSelections: ['REQUESTER_CONTRACT_FILE'] }
  const files = [{ attachmentType: 'REQUESTER_CONTRACT_FILE' }, { attachmentType: 'REQUESTER_CONTRACT_FILE' }]
  assert.ok(!missingInitiationAttachments(s, files).some(row => row.type === 'REQUESTER_CONTRACT_FILE'))
  assert.ok(!missingInitiationAttachments(s, files.slice(1)).some(row => row.type === 'REQUESTER_CONTRACT_FILE'))
  assert.ok(missingInitiationAttachments(s, []).some(row => row.type === 'REQUESTER_CONTRACT_FILE'))
  assert.ok(missingInitiationAttachments(s, files).some(row => row.type === 'CLIENT_DUE_DILIGENCE_FILE'))
})
