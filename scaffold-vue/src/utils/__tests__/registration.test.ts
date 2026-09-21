import assert from 'node:assert/strict'
import test from 'node:test'
import { registrationErrors } from '../registration.ts'

const valid = { realName: '张三丰', phone: '13800000000', deptId: 1, password: '123456', confirm: '123456', captcha: 'eyvx' }
test('registration accepts six digit passwords and trims non-password fields', () => {
  assert.deepEqual(registrationErrors(valid), [])
  assert.deepEqual(registrationErrors({ ...valid, realName: ' 张三丰 ', phone: ' 13800000000 ' }), [])
})
test('registration reports each invalid field including the screenshot phone', () => {
  const errors = registrationErrors({ ...valid, realName: ' ', phone: '11111111111', deptId: undefined, password: '12345', confirm: '', captcha: '' })
  assert.equal(errors.length, 6)
  assert.ok(errors.some(message => message.includes('手机号格式不正确')))
  assert.ok(errors.some(message => message.includes('6–64')))
})
test('registration enforces password boundaries and matching confirmation', () => {
  for (const length of [6, 7, 64]) {
    assert.deepEqual(registrationErrors({ ...valid, password: 'a'.repeat(length), confirm: 'a'.repeat(length) }), [])
  }
  assert.match(registrationErrors({ ...valid, password: 'a'.repeat(65), confirm: 'a'.repeat(65) }).join(), /6–64/)
  assert.deepEqual(registrationErrors({ ...valid, confirm: '654321' }), ['两次密码输入不一致'])
  assert.ok(registrationErrors({ ...valid, password: '      ', confirm: '      ' }).includes('请输入密码'))
})
