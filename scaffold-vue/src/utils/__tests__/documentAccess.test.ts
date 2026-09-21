import test from 'node:test'
import assert from 'node:assert/strict'
import { canResubmitDocument } from '../documentAccess.ts'

test('PPT owners can resubmit after review regardless of their employee or administrator role', () => {
  const own = { submitterId: 12, projectId: 1, businessType: 'SELECTION', status: 'REVISION_REQUIRED' }
  assert.equal(canResubmitDocument(12, own), true)
  assert.equal(canResubmitDocument(12, { ...own, businessType: 'INITIATION', status: 'APPROVED' }), true)
  assert.equal(canResubmitDocument(13, own), false)
  assert.equal(canResubmitDocument(undefined, own), false)
  assert.equal(canResubmitDocument(12, { ...own, status: 'SUBMITTED' }), false)
  assert.equal(canResubmitDocument(12, { ...own, businessType: 'OTHER' }), false)
  assert.equal(canResubmitDocument(12, { ...own, projectId: undefined }), false)
})
