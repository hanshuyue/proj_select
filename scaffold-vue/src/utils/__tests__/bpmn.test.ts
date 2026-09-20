import test from 'node:test'
import assert from 'node:assert/strict'
import { createDefaultBpmnXml } from '../bpmn.ts'

test('createDefaultBpmnXml includes executable process and approval nodes', () => {
  const xml = createDefaultBpmnXml('purchase_apply', '采购审批')

  assert.match(xml, /<process id="purchase_apply" name="采购审批" isExecutable="true">/)
  assert.match(xml, /flowable:candidateGroups="super_admin,sys_admin"/)
  assert.match(xml, /flowable:candidateGroups="super_admin,biz_admin"/)
  assert.match(xml, /部门负责人审批/)
  assert.match(xml, /财务复核/)
})

test('createDefaultBpmnXml sanitizes invalid process keys and escapes names', () => {
  const xml = createDefaultBpmnXml('123 flow', 'A&B')

  assert.match(xml, /<process id="process_123_flow"/)
  assert.match(xml, /name="A&amp;B"/)
})
