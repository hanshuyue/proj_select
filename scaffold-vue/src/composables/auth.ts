import { ref, type Component } from 'vue'
import type { RouteRecordRaw } from 'vue-router'
import { ApiError, authApi, authSession, type CurrentUser, type RouteItem } from '@/api'
import { setPermissions } from '@/directives/permission'
import type { NavGroup } from '@/data'

export const authed = ref<boolean>(authSession.isAuthenticated())
export const currentUser = ref<CurrentUser | null>(authSession.user() as CurrentUser | null)
export const dynamicNav = ref<NavGroup[]>([])
export const dynamicRoutes = ref<RouteRecordRaw[]>([])

const ROUTE_PATH_MAP: Record<string, string> = {
  dashboard: 'dashboard',
  user: 'system/user',
  role: 'system/role',
  menu: 'system/menu',
  dept: 'system/dept',
  post: 'system/post',
  dict: 'system/dict',
  config: 'system/config',
  systemNoticeChannel: 'system/notice-channel',
  operlog: 'monitor/oper-log',
  loginlog: 'monitor/login-log',
  model: 'workflow/model',
  def: 'workflow/definition',
  todo: 'workflow/todo',
  done: 'workflow/done',
  gen: 'tool/generator',
  selection: 'selection',
  selectionForm: 'selection/form',
  selectionTemplate: 'selection/template',
  initiation: 'initiation',
  initiationForm: 'initiation/form',
  initiationTemplate: 'initiation/template',
  documents: 'documents',
  registrationReview: 'registration-review',
}

function routeNameOf(item: RouteItem) {
  if (item.routeName) return item.routeName
  if (item.path && !item.path.startsWith('/')) return item.path
  return item.component?.replace(/[^\w]/g, '_') || item.name
}

function routePathOf(item: RouteItem) {
  const routeName = routeNameOf(item)
  const raw = item.routePath || (item.path?.startsWith('/') ? item.path : ROUTE_PATH_MAP[routeName]) || item.path || routeName
  return raw.replace(/^\//, '')
}

function navigationRouteOf(item: RouteItem) {
  const path = routePathOf(item)
  if (path === 'selection') return '/selection'
  if (path === 'selection/form') return '/selection/form'
  if (path === 'selection/template') return '/selection/template'
  if (path === 'initiation') return '/initiation'
  if (path === 'initiation/form') return '/initiation/form'
  if (path === 'initiation/template') return '/initiation/template'
  if (path === 'documents') return '/documents'
  if (path === 'registration-review') return '/registration-review'
  return routeNameOf(item)
}

/** 根据用户权限过滤菜单树 */
function filterByPermission(tree: RouteItem[], permissions: Set<string>): RouteItem[] {
  if (permissions.has('*')) return tree
  return tree
    .map(group => ({
      ...group,
      children: group.children?.filter(item => !item.permission || permissions.has(item.permission)),
    }))
    .filter(group => group.children && group.children.length > 0)
}

/** 菜单树 → 侧边栏 NavGroup[]（携带 group 级 icon） */
function menusToNav(tree: RouteItem[]): NavGroup[] {
  return tree.map(group => ({
    group: group.name,
    icon: group.icon || 'file',
    items: (group.children || []).map(item => ({
      key: navigationRouteOf(item),
      label: item.name,
      icon: item.icon || 'file',
      route: navigationRouteOf(item),
    })),
  }))
}

/** 静态组件映射 —— 后端 sys_menu.component 字段的值必须在此注册 */
const COMPONENT_MAP: Record<string, () => Promise<{ default: Component }>> = {
  // ─── 系统管理（V2 种子数据 component 字段） ───
  'dashboard/index':       () => import('@/views/Dashboard.vue'),
  'system/user/index':    () => import('@/views/Users.vue'),
  'system/role/index':    () => import('@/views/Roles.vue'),
  'system/menu/index':    () => import('@/views/Menus.vue'),
  'system/dept/index':    () => import('@/views/Depts.vue'),
  'system/post/index':    () => import('@/views/Post.vue'),
  'system/dict/index':    () => import('@/views/Dict.vue'),
  'system/config/index':  () => import('@/views/Config.vue'),
  'monitor/operlog/index':  () => import('@/views/OperLog.vue'),
  'monitor/loginlog/index': () => import('@/views/LoginLog.vue'),
  'workflow/model/index':   () => import('@/views/WfModel.vue'),
  'workflow/def/index':     () => import('@/views/WfDef.vue'),
  'workflow/todo/index':    () => import('@/views/WfTodo.vue'),
  'workflow/done/index':    () => import('@/views/WfDone.vue'),
  'tool/gen/index':         () => import('@/views/Generator.vue'),
  'selection/index':        () => import('@/views/selection/index.vue'),
  'selection/form':         () => import('@/views/selection/form.vue'),
  'selection/template':     () => import('@/views/selection/template.vue'),
  'initiation/index':       () => import('@/views/initiation/index.vue'),
  'initiation/form':        () => import('@/views/initiation/form.vue'),
  'initiation/template':    () => import('@/views/initiation/template.vue'),
  'documents/index':        () => import('@/views/Documents.vue'),
  'registration-review/index': () => import('@/views/RegistrationReview.vue'),
  // ─── 兼容旧 key ───
  'Users':    () => import('@/views/Users.vue'),
  'Roles':    () => import('@/views/Roles.vue'),
  'Menus':    () => import('@/views/Menus.vue'),
  'Depts':    () => import('@/views/Depts.vue'),
  'Post':     () => import('@/views/Post.vue'),
  'Dict':     () => import('@/views/Dict.vue'),
  'Config':   () => import('@/views/Config.vue'),
  'OperLog':  () => import('@/views/OperLog.vue'),
  'LoginLog': () => import('@/views/LoginLog.vue'),
  'WfModel':  () => import('@/views/WfModel.vue'),
  'WfDef':    () => import('@/views/WfDef.vue'),
  'WfTodo':   () => import('@/views/WfTodo.vue'),
  'WfDone':   () => import('@/views/WfDone.vue'),
  'Generator':() => import('@/views/Generator.vue'),
}

function resolveComponent(compPath: string) {
  return COMPONENT_MAP[compPath]
}

/** 菜单树 → Vue Router RouteRecordRaw[] */
function menusToRoutes(tree: RouteItem[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  for (const group of tree) {
    for (const item of group.children || []) {
      if (!item.component) continue
      const comp = resolveComponent(item.component)
      if (!comp) continue
      routes.push({
        path: routePathOf(item),
        name: routeNameOf(item),
        component: comp,
        meta: { title: item.name },
      })
    }
  }
  return routes
}

export async function loadRoutes() {
  try {
    const freshUser = await authApi.me()
    currentUser.value = freshUser
    authSession.updateUser(freshUser)
    const data = await authApi.routes()
    const tree = data.routes

    // 按权限过滤
    // 超级管理员始终使用后端返回的完整菜单树。旧会话里缓存的权限列表
    // 可能不包含后来新增的菜单权限，不能因此把新管理入口从侧边栏过滤掉。
    const perms = currentUser.value?.roles?.includes('super_admin')
      ? new Set<string>(['*'])
      : currentUser.value?.permissions
        ? new Set(currentUser.value.permissions)
        : new Set<string>(['*'])
    const filtered = filterByPermission(tree, perms)

    dynamicNav.value = menusToNav(filtered)
    dynamicRoutes.value = menusToRoutes(filtered)
  } catch (error) {
    console.error('Failed to load routes:', error)
    dynamicNav.value = []
    dynamicRoutes.value = []
    throw error
  }
}

export function resetAuthState() {
  authed.value = false
  currentUser.value = null
  dynamicNav.value = []
  dynamicRoutes.value = []
  setPermissions(['*'])
  authSession.clear()
}

export async function login(credentials: { username: string; password: string; captcha: string; captchaUuid: string }, remember: boolean) {
  const loginResponse = await authApi.login(credentials)
  authSession.save(loginResponse, { username: credentials.username, nickname: credentials.username }, remember)
  const user = await authApi.me()
  authSession.save(loginResponse, user, remember)
  authed.value = true
  currentUser.value = user

  // 同步权限到权限指令
  if (user.permissions) setPermissions(user.permissions)

  if (!user.mustChangePassword) await loadRoutes()
  return user
}

export async function logout() {
  try {
    if (authSession.isAuthenticated()) {
      await authApi.logout()
    }
  } catch (error) {
    if (!(error instanceof ApiError && error.code === 401)) {
      console.warn('logout failed', error)
    }
  }
  resetAuthState()
}
