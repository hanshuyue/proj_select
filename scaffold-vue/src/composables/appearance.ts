import { reactive, watch } from 'vue'

/* ============================================================
   外观设置 —— 取代原型 claude.ai/design 的 Tweaks 面板
   将其默认值固化为可在「外观」弹层中调整的真实设置。
   - sidebarTone: 侧边栏底色  ink | black | light
   - navActive:   选中样式    pill | bar | tint
   - listLayout:  用户页排布  table | cards
   - density:     行密度      comfy | regular | compact
   - accent:      强调色
   ============================================================ */
export type SidebarTone = 'ink' | 'black' | 'light'
export type NavActive = 'pill' | 'bar' | 'tint'
export type ListLayout = 'table' | 'cards'
export type Density = 'comfy' | 'regular' | 'compact'

export interface Appearance {
  sidebarTone: SidebarTone
  navActive: NavActive
  listLayout: ListLayout
  density: Density
  accent: string
}

const DEFAULTS: Appearance = {
  sidebarTone: 'ink',
  navActive: 'pill',
  listLayout: 'table',
  density: 'regular',
  accent: '#0066cc',
}

export const ACCENTS: Record<string, { soft: string; onDark: string; focus: string }> = {
  '#0066cc': { soft: '#e8f1fb', onDark: '#2997ff', focus: '#0071e3' },
  '#1d1d1f': { soft: '#ededee', onDark: '#bdbdbf', focus: '#3a3a3c' },
  '#1f8a4c': { soft: '#e7f4ec', onDark: '#34c759', focus: '#23a058' },
  '#6b46d9': { soft: '#f0ecfb', onDark: '#a78bfa', focus: '#7c52f0' },
}

const STORAGE_KEY = 'yunyuan.appearance'

function load(): Appearance {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return { ...DEFAULTS, ...JSON.parse(raw) }
  } catch { /* ignore */ }
  return { ...DEFAULTS }
}

export const appearance = reactive<Appearance>(load())

export function applyAccent(accent: string) {
  const a = ACCENTS[accent] || ACCENTS['#0066cc']
  const r = document.documentElement.style
  r.setProperty('--primary', accent)
  r.setProperty('--primary-soft', a.soft)
  r.setProperty('--primary-on-dark', a.onDark)
  r.setProperty('--primary-focus', a.focus)
}

// 强调色变化时实时写入 CSS 变量
watch(() => appearance.accent, applyAccent, { immediate: true })

// 持久化
watch(appearance, (v) => {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(v)) } catch { /* ignore */ }
}, { deep: true })

export function useAppearance() {
  return appearance
}
