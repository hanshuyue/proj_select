import type { HttpClient, ApiResponse } from './http'
import { authSession } from './session'

export interface SelectionBidder {
  id?: number
  bidderName: string
  serviceExTax?: number
  serviceTaxRate?: number
  serviceIncTax?: number
  resaleExTax?: number
  resaleTaxRate?: number
  resaleIncTax?: number
  feeAmount?: number
  priceScore?: number
  businessScore?: number
  totalScore?: number
  ranking?: number
}

export interface SelectionProject {
  id?: number
  projectName: string
  opportunityNo: string
  departmentName?: string
  reportYear: number
  reportMonth: number
  serviceLimitExTax?: number
  serviceLimitIncTax?: number
  serviceTaxRate?: number
  resaleBudgetExTax?: number
  resaleBudgetIncTax?: number
  resaleTaxRate?: number
  feeMinExTax?: number
  feeMinIncTax?: number
  feeTaxRate?: number
  selectedCompany?: string
  candidateCompany?: string
  publicityMethod?: string
  publicityWebsite?: string
  publicityEnabled: boolean
  executionMethod?: string
  cooperationCompany?: string
  expansionProject?: string
  reviewReportName?: string
  status: string
  bidders: SelectionBidder[]
  updateTime?: string
}

export interface SelectionTemplate {
  id: number
  templateName: string
  originalFilename: string
  fileSize: number
  placeholderKeys?: string
  defaultFlag: boolean | number
  status: number
  createTime: string
}

export function createSelectionApi(http: HttpClient) {
  return {
    projects: {
      list: (query: Record<string, string | number | undefined>) =>
        http.get<{ rows: SelectionProject[]; total: number; pageNum: number; pageSize: number }>('/selection/projects', query),
      detail: (id: number) => http.get<SelectionProject>(`/selection/projects/${id}`),
      create: (data: SelectionProject) => http.post<{ id: number }>('/selection/projects', data),
      update: (id: number, data: SelectionProject) => http.put<void>(`/selection/projects/${id}`, data),
      remove: (id: number) => http.delete<void>(`/selection/projects/${id}`),
      exportPpt: (id: number, templateId?: number) =>
        http.download(`/selection/projects/${id}/ppt`, { templateId }),
      downloadReviewReport: (id: number) =>
        http.download(`/selection/projects/${id}/review-report`),
      async uploadReviewReport(id: number, file: File) {
        const form = new FormData()
        form.append('file', file)
        const headers: Record<string, string> = { Accept: 'application/json' }
        const token = authSession.token()
        if (token) headers.Authorization = `Bearer ${token}`
        const response = await fetch(`/api/selection/projects/${id}/review-report`, { method: 'POST', headers, body: form })
        const text = await response.text()
        let payload: ApiResponse<{ filename: string }>
        try {
          payload = JSON.parse(text) as ApiResponse<{ filename: string }>
        } catch {
          throw new Error(response.status === 413 ? '服务器或代理拒绝了过大的上传文件' : `评审报告上传失败（HTTP ${response.status}）`)
        }
        if (!response.ok || payload.code !== 200) throw new Error(payload.message || '评审报告上传失败')
        return payload.data
      },
    },
    templates: {
      list: () => http.get<SelectionTemplate[]>('/selection/templates'),
      setDefault: (id: number) => http.put<void>(`/selection/templates/${id}/default`),
      remove: (id: number) => http.delete<void>(`/selection/templates/${id}`),
      download: (id: number) => http.download(`/selection/templates/${id}/file`),
      async upload(name: string, file: File) {
        const form = new FormData()
        form.append('name', name)
        form.append('file', file)
        const headers: Record<string, string> = { Accept: 'application/json' }
        const token = authSession.token()
        if (token) headers.Authorization = `Bearer ${token}`
        const response = await fetch('/api/selection/templates', { method: 'POST', headers, body: form })
        const payload = await response.json() as ApiResponse<{ id: number }>
        if (!response.ok || payload.code !== 200) throw new Error(payload.message || '模板上传失败')
        return payload.data
      },
    },
  }
}
