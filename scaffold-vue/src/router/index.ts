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

function defaultProtectedRoute() {
  const primaryRoute = dynamicRoutes.value.find((route) => route.name === 'selection' || route.name === 'initiation')
  if (primaryRoute?.name) return { name: primaryRoute.name as string, replace: true }
  const firstRoute = dynamicRoutes.value[0]
  if (firstRoute?.name) return { name: firstRoute.name as string, replace: true }
  return { path: '/', replace: true }
}

function hasDynamicMatch(path: string, name?: string | symbol | null) {
  return dynamicRoutes.value.some((route) => {
    if (name && route.name === name) return true
    return `/${String(route.path).replace(/^\//, '')}` === path
  })
}

export function injectRoutes(routes: RouteRecordRaw[]) {
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
    router.addRoute('root', {
      path: route.path,
      name,
      component: route.component as NonNullable<RouteRecordRaw['component']>,
      meta: route.meta,
    })
  }
}

export function hasRoute(name: string) {
  return injectedNames.has(name)
}

router.beforeEach(async (to) => {
  if (to.meta.public) {
    if (authed.value && to.name === 'login') {
      if (injectedNames.size === 0) {
        try {
          await loadRoutes()
        } catch {
          resetAuthState()
          return true
        }
        injectRoutes(dynamicRoutes.value)
      }

      const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : ''
      if (redirect) {
        const resolved = router.resolve(redirect)
        if (hasDynamicMatch(resolved.path, resolved.name)) {
          return { path: redirect, replace: true }
        }
      }

      return defaultProtectedRoute()
    }
    return true
  }

  if (!authed.value) {
    return {
      name: 'login',
      query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : undefined,
      replace: true,
    }
  }

  if (injectedNames.size === 0) {
    try {
      await loadRoutes()
    } catch {
      resetAuthState()
      return {
        name: 'login',
        query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : undefined,
        replace: true,
      }
    }
    injectRoutes(dynamicRoutes.value)
    if (to.path === '/') return defaultProtectedRoute()
    if (to.name === 'selectionForm' || to.name === 'selectionNew' || to.name === 'selectionEdit'
      || to.name === 'initiation' || to.name === 'initiationForm' || to.name === 'initiationTemplate'
      || to.name === 'initiationNew' || to.name === 'initiationEdit'
      || to.name === 'documents' || to.name === 'registrationReview') return true
    if (!hasDynamicMatch(to.path, to.name)) return defaultProtectedRoute()
    return { path: to.fullPath, replace: true }
  }

  if (to.path === '/') {
    return defaultProtectedRoute()
  }

  if (currentUser.value?.mustChangePassword) {
    return to.name === 'changePasswordRequired' ? true : { name: 'changePasswordRequired', replace: true }
  }
  if (to.name === 'changePasswordRequired') return defaultProtectedRoute()

  return true
})

export default router
