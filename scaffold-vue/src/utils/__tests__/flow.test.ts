import test from 'node:test'
import assert from 'node:assert/strict'
import { normalizeFlowStep } from '../flow.ts'

test('normalizeFlowStep preserves bounded numeric steps', () => {
  assert.equal(normalizeFlowStep(2, 3), 2)
  assert.equal(normalizeFlowStep('3', 3), 3)
  assert.equal(normalizeFlowStep(8, 3), 3)
})

test('normalizeFlowStep maps backend node names to progress indexes', () => {
  assert.equal(normalizeFlowStep('部门负责人审批', 3), 1)
  assert.equal(normalizeFlowStep('财务复核', 3), 2)
  assert.equal(normalizeFlowStep('归档', 3), 3)
})
