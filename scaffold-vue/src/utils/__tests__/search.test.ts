import test from 'node:test'
import assert from 'node:assert/strict'
import { includesKeyword } from '../search.ts'

test('includesKeyword treats empty keywords as matched', () => {
  assert.equal(includesKeyword('', null, undefined), true)
})

test('includesKeyword skips nullish values and matches any text value', () => {
  assert.equal(includesKeyword('smoke', null, 'ui_smoke_monitor', undefined), true)
  assert.equal(includesKeyword('missing', null, 'ui_smoke_monitor', undefined), false)
})
