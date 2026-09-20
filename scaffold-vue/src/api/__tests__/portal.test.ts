import test from 'node:test'
import assert from 'node:assert/strict'
import { createHttpClient } from '../http.ts'
import { createPortalApi } from '../portal.ts'

test('portal api maps dashboard workflow and generator endpoints', async () => {
  const calls: Array<{ url: string; method: string; body?: string }> = []
  const fetcher = async (url: string | URL | Request, init?: RequestInit) => {
    calls.push({ url: String(url), method: init?.method ?? 'GET', body: init?.body as string | undefined })
    if (String(url).includes('/download')) {
      return new Response('zip-bytes', {
        status: 200,
        headers: { 'content-type': 'application/zip' },
      })
    }
    return new Response(JSON.stringify({ code: 200, message: 'success', data: { rows: [], total: 0 }, traceId: 't' }), {
      status: 200,
      headers: { 'content-type': 'application/json' },
    })
  }
  const api = createPortalApi(createHttpClient({ baseUrl: '/api', tokenProvider: () => 'token', fetcher }))

  await api.dashboard.summary()
  await api.workflow.models.list({ pageNum: 1, pageSize: 20, keyword: '采购' })
  await api.workflow.models.create({ modelKey: 'leave', modelName: '请假流程', category: '人事审批', desc: '请假审批' })
  await api.workflow.models.update(7, { modelName: '请假流程更新', category: '人事审批', desc: '已更新' })
  await api.workflow.models.deploy(7)
  await api.workflow.models.remove(7)
  await api.workflow.definitions.suspend(7, true)
  await api.workflow.instances.start({ processDefinitionId: 'leave:1:abc', businessType: 'leave', businessKey: 'L-001' })
  await api.workflow.instances.trace('2501')
  await api.workflow.tasks.todo({ pageNum: 1, pageSize: 10 })
  await api.workflow.tasks.approve(9, { result: 'pass', comment: '同意' })
  await api.workflow.tasks.done({ keyword: '采购' })
  await api.workflow.tasks.trace(9)
  await api.generator.tables.list({ keyword: 'sys_user' })
  await api.generator.tables.importable({ keyword: 'sys_file' })
  await api.generator.tables.import({ tableNames: ['sys_file'] })
  await api.generator.tables.columns(3)
  await api.generator.tables.preview(3)
  await api.generator.tables.update(3, { tableComment: '用户表', synced: false })
  await api.generator.tables.generate(3)
  const zip = await api.generator.tables.download(3)

  assert.deepEqual(calls.map(call => `${call.method} ${call.url}`), [
    'GET /api/dashboard/summary',
    'GET /api/workflow/models?pageNum=1&pageSize=20&keyword=%E9%87%87%E8%B4%AD',
    'POST /api/workflow/models',
    'PUT /api/workflow/models/7',
    'POST /api/workflow/models/7/deploy',
    'DELETE /api/workflow/models/7',
    'PUT /api/workflow/definitions/7/suspend',
    'POST /api/workflow/instances/start',
    'GET /api/workflow/instances/2501/trace',
    'GET /api/workflow/tasks/todo?pageNum=1&pageSize=10',
    'POST /api/workflow/tasks/9/approve',
    'GET /api/workflow/tasks/done?keyword=%E9%87%87%E8%B4%AD',
    'GET /api/workflow/tasks/9/trace',
    'GET /api/tool/generator/tables?keyword=sys_user',
    'GET /api/tool/generator/importable-tables?keyword=sys_file',
    'POST /api/tool/generator/import',
    'GET /api/tool/generator/tables/3/columns',
    'GET /api/tool/generator/tables/3/preview',
    'PUT /api/tool/generator/tables/3',
    'POST /api/tool/generator/tables/3/generate',
    'GET /api/tool/generator/tables/3/download',
  ])
  assert.equal(await zip.text(), 'zip-bytes')
  assert.deepEqual(JSON.parse(calls[2].body ?? '{}'), { modelKey: 'leave', modelName: '请假流程', category: '人事审批', desc: '请假审批' })
  assert.deepEqual(JSON.parse(calls[3].body ?? '{}'), { modelName: '请假流程更新', category: '人事审批', desc: '已更新' })
  assert.deepEqual(JSON.parse(calls[7].body ?? '{}'), { processDefinitionId: 'leave:1:abc', businessType: 'leave', businessKey: 'L-001' })
  assert.deepEqual(JSON.parse(calls[10].body ?? '{}'), { result: 'pass', comment: '同意' })
  assert.deepEqual(JSON.parse(calls[15].body ?? '{}'), { tableNames: ['sys_file'] })
  assert.deepEqual(JSON.parse(calls[18].body ?? '{}'), { tableComment: '用户表', synced: false })
})
