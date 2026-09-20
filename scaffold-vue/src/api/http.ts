import { authSession } from './session.ts'

type ToastTone = 'ok' | 'info' | 'danger'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface HttpClientOptions {
  baseUrl?: string
  tokenProvider?: () => string | null
  fetcher?: typeof fetch
}

export interface HttpClient {
  get<T>(path: string, query?: QueryParams): Promise<T>
  post<T>(path: string, body?: unknown): Promise<T>
  put<T>(path: string, body?: unknown): Promise<T>
  delete<T>(path: string): Promise<T>
  download(path: string, query?: QueryParams): Promise<Blob>
  upload<T>(path: string, form: FormData): Promise<T>
}

export type QueryParams = Record<string, string | number | boolean | null | undefined>

export class ApiError extends Error {
  code: number
  status: number
  traceId?: string

  constructor(message: string, code: number, status: number, traceId?: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.status = status
    this.traceId = traceId
  }
}

// 全局错误提示函数由应用入口注入，避免 API 层直接依赖 UI 层。
let showErrorToast: (message: string, type?: ToastTone) => void = (message: string) => console.warn('[API Error]', message)

function getShowErrorToast(): (message: string, type?: ToastTone) => void {
  return showErrorToast
}

export function setHttpErrorToast(handler: (message: string, type?: ToastTone) => void) {
  showErrorToast = handler
}

// HTTP状态码对应的中文提示
const HTTP_STATUS_MESSAGES: Record<number, string> = {
  400: '请求参数错误',
  401: '登录已过期，请重新登录',
  403: '没有访问权限',
  404: '请求的资源不存在',
  405: '请求方法不支持',
  408: '请求超时',
  409: '数据冲突',
  413: '请求数据过大',
  415: '不支持的媒体类型',
  422: '请求参数校验失败',
  429: '请求过于频繁，请稍后重试',
  500: '服务器内部错误',
  502: '网关错误',
  503: '服务暂时不可用',
  504: '网关超时',
}

function getHttpStatusMessage(status: number): string {
  return HTTP_STATUS_MESSAGES[status] || `请求失败 (${status})`
}

export function createHttpClient(options: HttpClientOptions = {}): HttpClient {
  const baseUrl = normalizeBaseUrl(options.baseUrl ?? defaultBaseUrl())
  const tokenProvider = options.tokenProvider ?? authSession.token
  const fetcher = options.fetcher ?? fetch

  async function request<T>(method: string, path: string, body?: unknown, query?: QueryParams): Promise<T> {
    const headers: Record<string, string> = { Accept: 'application/json' }
    const token = tokenProvider()
    if (token) headers.Authorization = `Bearer ${token}`
    const init: RequestInit = { method, headers }
    if (body !== undefined) {
      headers['Content-Type'] = 'application/json'
      init.body = JSON.stringify(body)
    }

    let response: Response
    try {
      response = await fetcher(buildUrl(baseUrl, path, query), init)
    } catch (error) {
      // 网络错误
      const toast = getShowErrorToast()
      toast('网络连接失败，请检查网络', 'danger')
      throw new ApiError('网络连接失败', 0, 0)
    }

    // 处理HTTP错误状态码
    if (!response.ok) {
      const status = response.status
      let message = getHttpStatusMessage(status)

      // 尝试解析响应体获取更详细的错误信息
      try {
        const payload = await parseJson<ApiResponse<unknown>>(response)
        if (payload.message) {
          message = payload.message
        }
      } catch {
        // 解析失败使用默认提示
      }

      // 401错误特殊处理：跳转登录页
      if (status === 401) {
        setTimeout(() => {
          window.location.href = '/login'
        }, 1500)
      }

      const toast = getShowErrorToast()
      toast(message, 'danger')
      throw new ApiError(message, status, status)
    }

    const payload = await parseJson<ApiResponse<T>>(response)
    if (payload.code !== 200) {
      const message = payload.message || '请求失败'
      const toast = getShowErrorToast()
      toast(message, 'danger')
      throw new ApiError(message, payload.code, response.status, payload.traceId)
    }
    return payload.data
  }

  async function download(path: string, query?: QueryParams): Promise<Blob> {
    const headers: Record<string, string> = { Accept: 'text/csv,application/octet-stream,*/*' }
    const token = tokenProvider()
    if (token) headers.Authorization = `Bearer ${token}`

    let response: Response
    try {
      response = await fetcher(buildUrl(baseUrl, path, query), { method: 'GET', headers })
    } catch (error) {
      const toast = getShowErrorToast()
      toast('网络连接失败，请检查网络', 'danger')
      throw new ApiError('网络连接失败', 0, 0)
    }

    if (!response.ok) {
      const status = response.status
      let message = getHttpStatusMessage(status)

      try {
        const payload = await parseJson<ApiResponse<unknown>>(response)
        if (payload.message) {
          message = payload.message
        }
      } catch {
        // 解析失败使用默认提示
      }

      const toast = getShowErrorToast()
      toast(message, 'danger')
      throw new ApiError(message, status, status)
    }
    return response.blob()
  }

  async function upload<T>(path: string, form: FormData): Promise<T> {
    const headers: Record<string, string> = { Accept: 'application/json' }
    const token = tokenProvider()
    if (token) headers.Authorization = `Bearer ${token}`
    const response = await fetcher(buildUrl(baseUrl, path), { method: 'POST', headers, body: form })
    const payload = await parseJson<ApiResponse<T>>(response)
    if (!response.ok || payload.code !== 200) throw new ApiError(payload.message || getHttpStatusMessage(response.status), payload.code, response.status)
    return payload.data
  }

  return {
    get: (path, query) => request('GET', path, undefined, query),
    post: (path, body) => request('POST', path, body),
    put: (path, body) => request('PUT', path, body),
    delete: (path) => request('DELETE', path),
    download,
    upload,
  }
}

function normalizeBaseUrl(value: string) {
  return value.endsWith('/') ? value.slice(0, -1) : value
}

function defaultBaseUrl() {
  return ((import.meta as ImportMeta & { env?: { VITE_API_BASE_URL?: string } }).env?.VITE_API_BASE_URL) || '/api'
}

function buildUrl(baseUrl: string, path: string, query?: QueryParams) {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  const queryString = query ? toQueryString(query) : ''
  return `${baseUrl}${normalizedPath}${queryString ? `?${queryString}` : ''}`
}

function toQueryString(query: QueryParams) {
  const params = new URLSearchParams()
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params.set(key, String(value))
    }
  })
  return params.toString()
}

async function parseJson<T>(response: Response): Promise<T> {
  const text = await response.text()
  if (!text) {
    return { code: response.status, message: response.statusText, data: null } as T
  }
  return JSON.parse(text) as T
}

export const httpClient = createHttpClient()
