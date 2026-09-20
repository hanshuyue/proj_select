export interface NavLeaf {
  key: string
  label: string
  icon: string
  route: string
  badge?: number
}

export interface NavGroup {
  group?: string
  key?: string
  label?: string
  icon?: string
  route?: string
  badge?: number
  items?: NavLeaf[]
}

export interface BaseRow {
  id: number
  name?: string
  status: number
  createTime: string
  [key: string]: any
}

export interface User extends BaseRow {
  username: string
  nickname: string
}

export interface Role extends BaseRow {
  name: string
  code: string
  dataScope?: string
  builtin?: boolean
  menuIds?: number[]
  userCount?: number
}

export interface Menu extends BaseRow {
  name: string
  parentId?: number
  type: 'dir' | 'menu' | 'btn' | string
  icon?: string
  path?: string
  component?: string
  permission?: string
  visible?: number
  sort?: number
  children?: Menu[]
}

export interface Dept extends BaseRow {
  name: string
  parentId?: number
  leader?: string
  phone?: string
  email?: string
  sort?: number
  children?: Dept[]
}

export interface Post extends BaseRow {}
export interface DictType extends BaseRow {}
export interface DictItem extends BaseRow {}
export interface Config extends BaseRow {
  name: string
  key: string
  value: string
  type: string
  builtin?: boolean
}
export interface OperLog extends BaseRow {}
export interface LoginLog extends BaseRow {}
export interface WfModel extends BaseRow {}
export interface WfDef extends BaseRow {}
export interface WfTodo extends BaseRow {}
export interface WfDone extends BaseRow {}
export interface WfTrace extends BaseRow {}
export interface GenTable extends BaseRow {}
export interface GenField extends BaseRow {}

export const AVATAR_TINTS = ['#0066cc', '#34c759', '#ff9500', '#af52de', '#ff3b30', '#5ac8fa']

/**
 * 静态菜单已废弃 —— 所有菜单由 auth.ts → mock-menus.ts 动态生成。
 * 保留 NavGroup / NavLeaf 类型供 Sidebar 使用。
 */
