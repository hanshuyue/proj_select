import type { RouteLocationNormalizedLoaded, RouteLocationRaw } from 'vue-router'

export function navLocation(target: string): RouteLocationRaw {
  return target.startsWith('/') ? { path: target } : { name: target }
}

export function isNavActive(route: RouteLocationNormalizedLoaded, target: string) {
  return target.startsWith('/') ? route.path === target : route.name === target
}
