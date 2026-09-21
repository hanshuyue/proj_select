<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import { authed, currentUser, dynamicNav, dynamicRoutes, loadRoutes } from '@/composables/auth'
import { injectRoutes } from '@/router'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'
import { isNavActive, navLocation } from '@/utils/navigation'
import { avatarText, avatarColor } from '@/utils/avatar'

defineProps<{ collapsed: boolean }>()

const route = useRoute()
const router = useRouter()
const openedGroups = ref<string[]>([])

onMounted(async () => {
  if (dynamicNav.value.length === 0) {
    await loadRoutes()
    injectRoutes(dynamicRoutes.value)
    await nextTick()
    // 面向业务人员的入口保留甄选方案、甄选结果、项目立项，登录后优先进入甄选结果。
    if (route.path === '/' && authed.value) {
      const primary = dynamicRoutes.value.find(item => item.name === 'selection' || item.name === 'initiation')
      if (primary?.path) router.replace({ path: `/${String(primary.path).replace(/^\//, '')}` })
    }
  }
})

const light = computed(() => appearance.sidebarTone === 'light')
const bg = computed(() =>
  appearance.sidebarTone === 'black' ? '#000'
  : appearance.sidebarTone === 'light' ? '#ffffff'
  : 'var(--side-bg)')
const inkColor = computed(() => light.value ? 'var(--ink)' : 'var(--side-ink)')
const muted = computed(() => light.value ? 'var(--ink-muted-48)' : 'var(--side-ink-muted)')
const hairline = computed(() => light.value ? 'var(--hairline)' : 'var(--side-hairline)')
const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.username || '当前用户')
const roleText = computed(() => currentUser.value?.roles?.includes('super_admin') ? '超级管理员' : currentUser.value?.roles?.some(role => ['biz_admin', 'sys_admin'].includes(role)) ? '采购专员（管理员）' : '普通员工')

interface NavLeaf { key: string; label: string; icon: string; route: string; badge?: number }
interface NavNode { group: string; icon?: string; items?: NavLeaf[] }

const isAdministrator = computed(() => currentUser.value?.roles?.some(role => ['super_admin', 'sys_admin', 'biz_admin'].includes(role)) ?? false)

/** 普通用户聚焦业务入口；管理员显示完整管理菜单。 */
function isBusinessRoute(route: string, label = '') {
  if (isAdministrator.value) return true
  const path = String(route || '').replace(/^\//, '')
  if (String(label).includes('甄选方案')) return true
  return path === 'documents' || path === 'selection' || path.startsWith('selection/')
    || path === 'selection-plan' || path.startsWith('selection-plan/')
    || path === 'selectionPlan' || path.startsWith('selectionPlan/')
    || path === 'selection_plan' || path.startsWith('selection_plan/')
    || path === 'initiation' || path.startsWith('initiation/')
}

const navNodes = computed<NavNode[]>(() => (dynamicNav.value as NavNode[])
  .map(node => ({ ...node, items: node.items?.filter(item => isBusinessRoute(item.route, item.label)) }))
  .filter(node => node.items?.length))

function isActive(item: NavLeaf) {
  return isNavActive(route, item.route)
}
function rowStyle(item: NavLeaf) {
  if (!isActive(item)) return { color: muted.value }
  if (appearance.navActive === 'pill') {
    return { background: 'var(--primary)', color: '#fff' }
  } else if (appearance.navActive === 'bar') {
    return {
      background: light.value ? 'var(--primary-soft)' : 'var(--side-active)',
      boxShadow: 'inset 3px 0 0 var(--primary)',
      color: light.value ? 'var(--primary)' : '#fff',
    }
  }
  return { background: light.value ? 'var(--primary-soft)' : 'var(--side-active)', color: light.value ? 'var(--primary)' : '#fff' }
}
function iconColor(item: NavLeaf) {
  if (!isActive(item)) return muted.value
  if (appearance.navActive === 'pill') return '#fff'
  return light.value ? 'var(--primary)' : 'var(--primary-on-dark)'
}
function groupIconColor(node: NavNode) {
  const anyActive = node.items?.some(item => isActive(item))
  return anyActive ? (light.value ? 'var(--primary)' : 'var(--primary-on-dark)') : muted.value
}
function onNav(item: NavLeaf) {
  if (!item.route) { pushToast('「' + item.label + '」本期暂未开放', 'info'); return }
  router.push(navLocation(item.route))
}
function groupKey(node: NavNode, index: number) {
  return String(node.group || index)
}
function groupActive(node: NavNode) {
  return !!node.items?.some((item: NavLeaf) => isActive(item))
}
function groupOpen(node: NavNode, index: number) {
  return openedGroups.value.includes(groupKey(node, index)) || groupActive(node)
}
async function toggleGroup(node: NavNode, index: number, event: MouseEvent) {
  const key = groupKey(node, index)
  const opening = !openedGroups.value.includes(key)
  openedGroups.value = opening
    ? [...openedGroups.value, key]
    : openedGroups.value.filter(item => item !== key)
  if (opening) {
    await nextTick()
    const group = (event.currentTarget as HTMLElement | null)?.parentElement
    group?.querySelector('.nav-group-body')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }
}
function syncOpenGroups() {
  const activeKeys = navNodes.value
    .map((node, index) => ({ node, index }))
    .filter(({ node }) => groupActive(node))
    .map(({ node, index }) => groupKey(node, index))
  openedGroups.value = Array.from(new Set([...openedGroups.value, ...activeKeys]))
}

watch(navNodes, syncOpenGroups, { immediate: true })
watch(() => route.fullPath, syncOpenGroups)
</script>

<template>
  <aside
    class="sidebar"
    :style="{
      width: collapsed ? 'var(--side-w-collapsed)' : 'var(--side-w)',
      background: bg,
      borderRight: light ? '1px solid var(--hairline)' : 'none',
    }"
  >
    <!-- 品牌 -->
    <div class="side-brand" :style="{ borderBottom: `1px solid ${hairline}`, justifyContent: collapsed ? 'center' : 'flex-start' }">
      <span class="brand-mark"><Icon name="command" :size="20" /></span>
      <div v-if="!collapsed" style="overflow: hidden">
        <div class="brand-name" :style="{ color: inkColor }">政企项目材料平台</div>
        <div class="brand-sub" :style="{ color: muted }">标准化与智能生成</div>
      </div>
    </div>

    <!-- 导航 -->
    <nav class="side-nav">
      <template v-for="(node, i) in navNodes" :key="i">
        <!-- 有子项 → 分组 -->
        <div v-if="node.items?.length" class="nav-group">
          <button
            v-if="!collapsed"
            class="nav-group-toggle"
            :class="{ open: groupOpen(node, i), active: groupActive(node) }"
            :style="{ color: groupActive(node) ? inkColor : muted }"
            @click="toggleGroup(node, i, $event)"
          >
            <span class="nav-group-icon"><Icon :name="node.icon || 'file'" :size="17" :style="{ color: groupIconColor(node) }" /></span>
            <span style="flex:1;text-align:left">{{ node.group }}</span>
            <Icon name="chevronRight" :size="13" class="nav-group-arrow" />
          </button>
          <div v-if="collapsed && i > 0" class="nav-divider" :style="{ background: hairline }" />
          <div v-show="collapsed || groupOpen(node, i)" class="nav-group-body">
            <button
              v-for="it in node.items"
              :key="it.key"
              class="nav-item"
              :style="{ ...rowStyle(it as NavLeaf), justifyContent: collapsed ? 'center' : 'flex-start' }"
              :title="collapsed ? it.label : ''"
              @click="onNav(it as NavLeaf)"
            >
              <Icon :name="it.icon" :size="19" :style="{ color: iconColor(it as NavLeaf) }" />
              <span v-if="!collapsed" class="nav-label">{{ it.label }}</span>
              <span v-if="!collapsed && it.badge" class="nav-badge">{{ it.badge }}</span>
            </button>
          </div>
        </div>

        <!-- 无子项 → 独立菜单（如工作台） -->
        <div v-else class="nav-group">
          <button
            class="nav-item"
            :style="{ ...rowStyle(node as any), justifyContent: collapsed ? 'center' : 'flex-start' }"
            :title="collapsed ? node.group : ''"
            @click="onNav({ key: node.group, label: node.group, icon: node.icon || 'dashboard', route: (node as any).route || '' })"
          >
            <Icon :name="node.icon!" :size="19" :style="{ color: iconColor(node as any) }" />
            <span v-if="!collapsed" class="nav-label">{{ node.group }}</span>
          </button>
        </div>
      </template>

      <!-- 菜单为空提示 -->
      <div v-if="navNodes.length === 0" style="padding:24px 12px;text-align:center">
        <div :style="{ color: muted, fontSize: '13px' }">菜单加载中...</div>
      </div>
    </nav>

    <!-- 底部用户 -->
    <div class="side-foot" :style="{ borderTop: `1px solid ${hairline}` }">
      <el-avatar :size="collapsed ? 32 : 36" :style="{ background: avatarColor(displayName, '#0066cc'), fontSize: (collapsed ? 32 : 36) * 0.4 + 'px' }">{{ avatarText(displayName) }}</el-avatar>
      <div v-if="!collapsed" style="overflow: hidden; flex: 1">
        <div :style="{ fontSize: '13.5px', fontWeight: 600, color: inkColor, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }">{{ displayName }}</div>
        <div :style="{ fontSize: '12px', color: muted }">{{ roleText }}</div>
      </div>
      <Icon v-if="!collapsed" name="chevronRight" :size="15" :style="{ color: muted }" />
    </div>
  </aside>
</template>
