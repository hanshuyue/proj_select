import type { App, Directive } from 'vue'

const userPermissions = new Set<string>()

export function setPermissions(perms: string[]) {
  userPermissions.clear()
  perms.forEach(permission => userPermissions.add(permission))
}

export function hasPermission(permission: string): boolean {
  return userPermissions.has('*') || userPermissions.has(permission)
}

function allowed(value?: string | string[]) {
  if (!value) return true
  const permissions = Array.isArray(value) ? value : [value]
  return permissions.some(permission => hasPermission(permission))
}

function applyPermission(el: HTMLElement, value?: string | string[]) {
  const isAllowed = allowed(value)
  el.style.display = isAllowed ? (el.dataset.permissionDisplay || '') : 'none'
  el.setAttribute('aria-hidden', String(!isAllowed))
}

const permissionDirective: Directive<HTMLElement, string | string[]> = {
  mounted(el, binding) {
    el.dataset.permissionDisplay = el.style.display || ''
    applyPermission(el, binding.value)
  },
  updated(el, binding) {
    applyPermission(el, binding.value)
  },
}

export function installPermissionDirective(app: App) {
  app.directive('permission', permissionDirective)
  app.directive('hasPermi', permissionDirective)
}
