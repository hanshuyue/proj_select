import assert from 'node:assert/strict'
import test from 'node:test'
import { yearOnYear, parseMonthRange, formatMonthRange, buildCapabilityDemand } from '../initiationOverview.ts'

test('growth is calculated, handles blanks and never divides by zero', () => {
  assert.equal(yearOnYear(100, 125), '25.00')
  assert.equal(yearOnYear(100, 80), '-20.00')
  assert.equal(yearOnYear('1,000', '1,500'), '50.00')
  assert.equal(yearOnYear('', 100), '')
  assert.equal(yearOnYear(0, 100), '—')
  assert.equal(yearOnYear('说明', 100), '')
})
test('month ranges retain year and start/end months including legacy format', () => {
  assert.deepEqual(parseMonthRange('2025年1-6月'), ['2025-01', '2025-06'])
  assert.equal(formatMonthRange(['2026-02', '2026-07']), '2026年2-7月')
  assert.deepEqual(parseMonthRange(formatMonthRange(['2025-12', '2026-02'])), ['2025-12', '2026-02'])
  assert.equal(formatMonthRange(null), '')
})
test('demand export uses only selected items while retaining explicit additional notes', () => {
  const detail = { IT: '视频平台，融入9One', CLOUD: '开通2台云主机', NETWORK: '开通1条100M专线' }
  assert.equal(buildCapabilityDemand(['CLOUD', 'NETWORK'], detail), '公有云：开通2台云主机\n数据专线：开通1条100M专线')
  assert.equal(buildCapabilityDemand([], detail), '')
  assert.equal(buildCapabilityDemand(['IT'], {}, '原项目补充'), '原项目补充')
})
