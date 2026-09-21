import { watch } from 'vue'
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { authed, currentUser, dynamicRoutes, loadRoutes, resetAuthState } from '@/composables/auth'
import AppShell from '@/components/layout/AppShell.vue'

const staticRoutes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  { path: '/register', name: 'register', component: () => import('@/views/Register.vue'), meta: { public: true } },
  {
    path: '/',
    name: 'root',
    component: AppShell,
    children: [
      { path: 'change-password', name: 'changePasswordRequired', component: () => import('@/views/ChangePasswordRequired.vue'), meta: { title: '修改初始密码' } },
      { path: 'documents', name: 'documents', component: () => import('@/views/Documents.vue'), meta: { title: 'PPT提交与审核' } },
      { path: 'registration-review', name: 'registrationReview', component: () => import('@/views/RegistrationReview.vue'), meta: { title: '管理员设置' } },
      { path: 'initiation', name: 'initiation', component: () => import('@/views/initiation/index.vue'), meta: { title: '立项项目管理' } },
      { path: 'initiation/form', name: 'initiationForm', component: () => import('@/views/initiation/form.vue'), meta: { title: '新建立项材料' } },
      { path: 'initiation/template', name: 'initiationTemplate', component: () => import('@/views/initiation/template.vue'), meta: { title: 'PPT模板管理' } },
      { path: 'selection/form', name: 'selectionForm', component: () => import('@/views/selection/form.vue'), meta: { title: '甄选结果填报' } },
      { path: 'selection/new', name: 'selectionNew', component: () => import('@/views/selection/form.vue'), meta: { title: '新建甄选结果' } },
      { path: 'selection/:id/edit', name: 'selectionEdit', component: () => import('@/views/selection/form.vue'), meta: { title: '编辑甄选结果' } },
      { path: 'initiation/new', name: 'initiationNew', component: () => import('@/views/initiation/form.vue'), meta: { title: '新建立项材料' } },
      { path: 'initiation/:id/edit', name: 'initiationEdit', component: () => import('@/views/initiation/form.vue'), meta: { title: '编辑立项材料' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes: staticRoutes,
  scrollBehavior() { return { top: 0 } },
})

const injectedNames = new Set<string>()
const removeInjectedRoutes: (() => void)[] = []
let routesLoaded = false

watch(authed, (loggedIn) => {
  if (!loggedIn) {
    removeInjectedRoutes.splice(0).forEach(remove => remove())
    injectedNames.clear()
    routesLoaded = false
  }
}, { flush: 'sync' })

function defaultProtectedRoute() {
  const primaryRoute = dynamicRoutes.value.find((route) => route.name === 'selection' || route.name === 'initiation')
  if (primaryRoute?.path) return { path: `/${String(primaryRoute.path).replace(/^\//, '')}`, replace: true }
  const firstRoute = dynamicRoutes.value[0]
  if (firstRoute?.path) return { path: `/${String(firstRoute.path).replace(/^\//, '')}`, replace: true }
  return { path: '/documents', replace: true }
}

function hasDynamicMatch(path: string, name?: string | symbol | null) {
  return dynamicRoutes.value.some((route) => {
    if (name && route.name === name) return true
    return `/${String(route.path).replace(/^\//, '')}` === path
  })
}

export function injectRoutes(routes: RouteRecordRaw[]) {
  routesLoaded = true
  for (const route of routes) {
    const name = route.name as string | undefined
    if (!name || injectedNames.has(name) || !route.component) continue
    const normalizedPath = `/${String(route.path).replace(/^\//, '')}`
    const alreadyRegistered = router.getRoutes().some(existing => existing.path === normalizedPath)
    if (router.hasRoute(name) || alreadyRegistered) {
      injectedNames.add(name)
      continue
    }
    injectedNames.add(name)
    removeInjectedRoutes.push(router.addRoute('root', {
      path: route.path,
      name,
      component: route.component as NonNullable<RouteRecordRaw['component']>,
      meta: route.meta,
    }))
  }
}

export function hasRoute(name: string) {
  return injectedNames.has(name)
}

const businessPermissions: Record<string, string> = {
  initiation: 'initiation:project:list', initiationForm: 'initiation:project:add',
  initiationNew: 'initiation:project:add', initiationEdit: 'initiation:project:edit',
  initiationTemplate: 'initiation:template:list', selectionForm: 'selection:project:add',
  selectionNew: 'selection:project:add', selectionEdit: 'selection:project:edit',
}

function canVisit(path: string, name?: string | symbol | null) {
  const user = currentUser.value
  if (name === 'documents') return true
  if (name === 'registrationReview') return !!user?.roles?.includes('super_admin')
  const permission = businessPermissions[String(name)]
  if (permission) return !!user?.roles?.includes('super_admin') || !!user?.permissions?.some(p => p === '*' || p === permission)
  return hasDynamicMatch(path, name)
}

router.beforeEach(async (to) => {
  if (!authed.value) {
    if (to.meta.public) return true
    return { name: 'login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : undefined, replace: true }
  }
  if (to.meta.public && to.name !== 'login') return true

  let loadedNow = false
  if (!routesLoaded) {
    try {
      await loadRoutes()
      injectRoutes(dynamicRoutes.value)
      loadedNow = true
    } catch {
      resetAuthState()
      return to.meta.public ? true : { name: 'login', replace: true }
    }
  }
  // Password policy must apply on the first navigation as well as later ones.
  if (currentUser.value?.mustChangePassword) {
    return to.name === 'changePasswordRequired' ? true : { name: 'changePasswordRequired', replace: true }
  }
  if (to.name === 'login') {
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : ''
    if (redirect.startsWith('/') && !redirect.startsWith('//')) {
      const resolved = router.resolve(redirect)
      if (canVisit(resolved.path, resolved.name)) return { path: redirect, replace: true }
    }
    return defaultProtectedRoute()
  }
  if (to.path === '/' || to.name === 'changePasswordRequired') return defaultProtectedRoute()
  if (!canVisit(to.path, to.name)) return defaultProtectedRoute()
  if (loadedNow) return { path: to.fullPath, replace: true }
  return true
})

export default router
