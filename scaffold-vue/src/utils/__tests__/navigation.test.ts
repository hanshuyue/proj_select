import test from 'node:test'
import assert from 'node:assert/strict'
import { isNavActive, navLocation } from '../navigation.ts'

test('navLocation treats backend dynamic route paths as paths', () => {
  assert.deepEqual(navLocation('/system/role'), { path: '/system/role' })
})

test('navLocation keeps local static route keys as names', () => {
  assert.deepEqual(navLocation('role'), { name: 'role' })
})

test('isNavActive supports path and name based menu entries', () => {
  assert.equal(isNavActive({ path: '/system/role', name: 'role' } as any, '/system/role'), true)
  assert.equal(isNavActive({ path: '/system/role', name: 'role' } as any, 'role'), true)
  assert.equal(isNavActive({ path: '/system/dept', name: 'dept' } as any, '/system/role'), false)
})
