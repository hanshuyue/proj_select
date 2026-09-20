import type { HttpClient, QueryParams } from './http'
import type { PageResult } from './system'

export interface DashboardSummary {
  stats: Array<Record<string, unknown>>
  trend: number[]
  todos: Array<Record<string, unknown>>
  activities: Array<Record<string, unknown>>
  roleDist: Array<{ label: string; value: number }>
  trendTotal: number
  successRate: number
}

export interface ApproveRequest {
  result: 'pass' | 'reject' | 'return' | 'revoke' | string
  comment?: string
}

export interface WorkflowModelWriteRequest {
  modelKey?: string
  modelName?: string
  category?: string
  desc?: string
  bpmnXml?: string
}

export interface WorkflowStartRequest {
  processDefinitionId: string
  businessKey?: string
  businessType: string
  businessTitle?: string
  variables?: Record<string, unknown>
}

export interface GeneratorTableUpdate {
  tableComment?: string
  synced?: boolean
}

export interface GeneratorImportRequest {
  tableNames: string[]
}

export function createPortalApi(client: HttpClient) {
  return {
    dashboard: {
      summary: (query?: QueryParams) => client.get<DashboardSummary>('/dashboard/summary', query),
    },
    workflow: {
      models: {
        list: (query?: QueryParams) => client.get<PageResult<Record<string, unknown>>>('/workflow/models', query),
        create: (body: WorkflowModelWriteRequest) => client.post<{ id: number }>('/workflow/models', body),
        update: (id: number, body: WorkflowModelWriteRequest) => client.put<void>(`/workflow/models/${id}`, body),
        deploy: (id: number) => client.post<void>(`/workflow/models/${id}/deploy`),
        remove: (id: number) => client.delete<void>(`/workflow/models/${id}`),
      },
      definitions: {
        list: (query?: QueryParams) => client.get<PageResult<Record<string, unknown>>>('/workflow/definitions', query),
        suspend: (id: number, suspended: boolean) => client.put<void>(`/workflow/definitions/${id}/suspend`, { suspended }),
      },
      instances: {
        start: (body: WorkflowStartRequest) => client.post<Record<string, unknown>>('/workflow/instances/start', body),
        trace: (processInstanceId: string) => client.get<Record<string, unknown>[]>(`/workflow/instances/${processInstanceId}/trace`),
      },
      tasks: {
        todo: (query?: QueryParams) => client.get<PageResult<Record<string, unknown>>>('/workflow/tasks/todo', query),
        done: (query?: QueryParams) => client.get<PageResult<Record<string, unknown>>>('/workflow/tasks/done', query),
        approve: (id: number | string, body: ApproveRequest) => client.post<void>(`/workflow/tasks/${id}/approve`, body),
        trace: (id: number | string) => client.get<Record<string, unknown>[]>(`/workflow/tasks/${id}/trace`),
      },
    },
    generator: {
      tables: {
        list: (query?: QueryParams) => client.get<PageResult<Record<string, unknown>>>('/tool/generator/tables', query),
        importable: (query?: QueryParams) => client.get<Record<string, unknown>[]>('/tool/generator/importable-tables', query),
        import: (body: GeneratorImportRequest) => client.post<Record<string, unknown>>('/tool/generator/import', body),
        columns: (id: number) => client.get<Record<string, unknown>[]>(`/tool/generator/tables/${id}/columns`),
        preview: (id: number) => client.get<Record<string, unknown>>(`/tool/generator/tables/${id}/preview`),
        update: (id: number, body: GeneratorTableUpdate) => client.put<void>(`/tool/generator/tables/${id}`, body),
        generate: (id: number) => client.post<Record<string, unknown>>(`/tool/generator/tables/${id}/generate`),
        download: (id: number) => client.download(`/tool/generator/tables/${id}/download`),
      },
    },
  }
}
