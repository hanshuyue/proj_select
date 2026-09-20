import type { HttpClient, QueryParams } from './http'

export interface PageResult<T> {
  pageNum: number
  pageSize: number
  total: number
  rows: T[]
}

export interface IdResponse {
  id: number
}

export interface UserRow {
  id: number
  username: string
  nickname: string
  phone: string
  email: string
  deptName: string
  postName: string
  roleName: string
  status: number
  createTime: string
  lastLogin: string
}

export interface UserWriteRequest {
  username?: string
  password?: string
  nickname?: string
  phone?: string
  email?: string
  deptId?: number
  status?: number
  roleIds?: number[]
  postIds?: number[]
}

export interface RoleWriteRequest {
  name?: string
  code?: string
  dataScope?: string
  sort?: number
  status?: number
  remark?: string
  menuIds?: number[]
}

export interface MenuWriteRequest {
  parentId?: number
  name?: string
  type?: string
  icon?: string
  path?: string
  component?: string
  permission?: string
  sort?: number
  visible?: number
  status?: number
}

export interface DeptWriteRequest {
  parentId?: number
  name?: string
  leader?: string
  phone?: string
  email?: string
  sort?: number
  status?: number
}

export interface PostWriteRequest {
  postCode?: string
  postName?: string
  sort?: number
  status?: number
  remark?: string
}

export interface DictTypeWriteRequest {
  dictName?: string
  dictType?: string
  status?: number
  remark?: string
}

export interface DictDataWriteRequest {
  dictType?: string
  label?: string
  value?: string
  sort?: number
  status?: number
  tone?: string
  def?: boolean
  remark?: string
}

export interface ConfigWriteRequest {
  name?: string
  key?: string
  value?: string
  type?: 'Y' | 'N' | string
  builtin?: boolean
  remark?: string
}

export function createSystemApi(client: HttpClient) {
  return {
    users: {
      ...crudPage<UserRow, UserWriteRequest>(client, '/system/users'),
      export: (query?: QueryParams) => client.download('/system/users/export', query),
      updateStatus: (id: number, status: number) => client.put<void>(`/system/users/${id}/status`, { status }),
      resetPassword: (id: number, password: string) => client.put<void>(`/system/users/${id}/password`, { password }),
    },
    roles: {
      ...crudPage<Record<string, unknown>, RoleWriteRequest>(client, '/system/roles'),
      menus: (id: number) => client.get<{ menuIds: number[] }>(`/system/roles/${id}/menus`),
      export: (query?: QueryParams) => client.download('/system/roles/export', query),
    },
    menus: {
      tree: (query?: QueryParams) => client.get<Record<string, unknown>[]>('/system/menus/tree', query),
      create: (body: MenuWriteRequest) => client.post<IdResponse>('/system/menus', body),
      update: (id: number, body: MenuWriteRequest) => client.put<void>(`/system/menus/${id}`, body),
      remove: (id: number) => client.delete<void>(`/system/menus/${id}`),
      export: (query?: QueryParams) => client.download('/system/menus/export', query),
    },
    depts: {
      tree: (query?: QueryParams) => client.get<Record<string, unknown>[]>('/system/depts/tree', query),
      create: (body: DeptWriteRequest) => client.post<IdResponse>('/system/depts', body),
      update: (id: number, body: DeptWriteRequest) => client.put<void>(`/system/depts/${id}`, body),
      remove: (id: number) => client.delete<void>(`/system/depts/${id}`),
      export: (query?: QueryParams) => client.download('/system/depts/export', query),
    },
    posts: {
      ...crudPage<Record<string, unknown>, PostWriteRequest>(client, '/system/posts'),
      export: (query?: QueryParams) => client.download('/system/posts/export', query),
    },
    dictTypes: {
      ...crudPage<Record<string, unknown>, DictTypeWriteRequest>(client, '/system/dict/types'),
      export: (query?: QueryParams) => client.download('/system/dict/types/export', query),
    },
    dictData: {
      list: (dictType: string) => client.get<Record<string, unknown>[]>(`/system/dict/data/${dictType}`),
      create: (body: DictDataWriteRequest) => client.post<IdResponse>('/system/dict/data', body),
      update: (id: number, body: DictDataWriteRequest) => client.put<void>(`/system/dict/data/${id}`, body),
      remove: (id: number) => client.delete<void>(`/system/dict/data/${id}`),
    },
    configs: {
      ...crudPage<Record<string, unknown>, ConfigWriteRequest>(client, '/system/configs'),
      export: (query?: QueryParams) => client.download('/system/configs/export', query),
    },
    operLogs: {
      list: (query?: QueryParams) => client.get<PageResult<Record<string, unknown>>>('/monitor/oper-logs', query),
      export: (query?: QueryParams) => client.download('/monitor/oper-logs/export', query),
      remove: (id: number | string) => client.delete<void>(`/monitor/oper-logs/${id}`),
      clear: () => client.delete<void>('/monitor/oper-logs'),
    },
    loginLogs: {
      list: (query?: QueryParams) => client.get<PageResult<Record<string, unknown>>>('/monitor/login-logs', query),
      export: (query?: QueryParams) => client.download('/monitor/login-logs/export', query),
      remove: (id: number | string) => client.delete<void>(`/monitor/login-logs/${id}`),
      clear: () => client.delete<void>('/monitor/login-logs'),
    },
  }
}

function crudPage<Row, WriteBody>(client: HttpClient, path: string) {
  return {
    list: (query?: QueryParams) => client.get<PageResult<Row>>(path, query),
    create: (body: WriteBody) => client.post<IdResponse>(path, body),
    update: (id: number, body: Partial<WriteBody>) => client.put<void>(`${path}/${id}`, body),
    remove: (id: number) => client.delete<void>(`${path}/${id}`),
  }
}
