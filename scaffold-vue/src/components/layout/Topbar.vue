<script setup lang="ts">
import { computed, reactive, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import Icon from '@/components/Icon.vue'
import AppearanceModal from './AppearanceModal.vue'
import { authApi, portalApi } from '@/api'
import { currentUser } from '@/composables/auth'
import { pushToast } from '@/composables/toast'
import { avatarText, avatarColor } from '@/utils/avatar'

defineProps<{ collapsed: boolean }>()
const emit = defineEmits<{ (e: 'toggle'): void; (e: 'logout'): void }>()

const router = useRouter()
const bellOpen = ref(false)
const appOpen = ref(false)
const userOpen = ref(false)
const profileOpen = ref(false)
const passwordOpen = ref(false)
const appearanceOpen = ref(false)
const bellRef = ref<HTMLElement | null>(null)
const appRef = ref<HTMLElement | null>(null)
const userRef = ref<HTMLElement | null>(null)
const searchKeyword = ref('')
const todoLoading = ref(false)
const topTodos = ref<Array<{ id: number | string; title: string; from: string; time: string; level: string }>>([])
const passwordSubmitting = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const passwordErr = reactive<{ oldPassword?: string; newPassword?: string; confirmPassword?: string; general?: string }>({})

const menuEntries = [
  { label: '工作台', group: '首页', routeName: 'dashboard', icon: 'dashboard', keywords: ['dashboard', '首页', '概览'] },
  { label: '用户管理', group: '系统管理', routeName: 'user', icon: 'users', keywords: ['用户', '账号', 'user'] },
  { label: '角色管理', group: '系统管理', routeName: 'role', icon: 'role', keywords: ['角色', '权限', 'role'] },
  { label: '菜单管理', group: '系统管理', routeName: 'menu', icon: 'menu', keywords: ['菜单', '路由', '权限标识'] },
  { label: '部门管理', group: '系统管理', routeName: 'dept', icon: 'dept', keywords: ['部门', '组织'] },
  { label: '岗位管理', group: '系统管理', routeName: 'post', icon: 'post', keywords: ['岗位', '职位'] },
  { label: '字典管理', group: '系统管理', routeName: 'dict', icon: 'dict', keywords: ['字典', '枚举'] },
  { label: '参数配置', group: '系统管理', routeName: 'config', icon: 'gear', keywords: ['参数', '配置'] },
  { label: '操作日志', group: '系统监控', routeName: 'operlog', icon: 'log', keywords: ['操作日志', '审计'] },
  { label: '登录日志', group: '系统监控', routeName: 'loginlog', icon: 'lock', keywords: ['登录日志', '登录'] },
  { label: '流程模型', group: '工作流程', routeName: 'model', icon: 'flow', keywords: ['流程模型', '工作流模型'] },
  { label: '流程定义', group: '工作流程', routeName: 'def', icon: 'layers', keywords: ['流程定义', '部署'] },
  { label: '我的待办', group: '工作流程', routeName: 'todo', icon: 'bell', keywords: ['待办', '审批'] },
  { label: '已办任务', group: '工作流程', routeName: 'done', icon: 'check', keywords: ['已办', '审批记录'] },
  { label: '代码生成', group: '开发工具', routeName: 'gen', icon: 'database', keywords: ['生成器', '代码生成', '表配置'] },
  { label: '通知渠道', group: '系统管理', routeName: 'systemNoticeChannel', icon: 'bell', keywords: ['通知', '短信', '邮件', '企业微信', '语音'] },
] as const

const appShortcuts = menuEntries.filter(item => ['dashboard', 'user', 'todo', 'gen', 'operlog', 'model'].includes(item.routeName))

function onDoc(e: MouseEvent) {
  if (bellRef.value && !bellRef.value.contains(e.target as Node)) bellOpen.value = false
  if (appRef.value && !appRef.value.contains(e.target as Node)) appOpen.value = false
  if (userRef.value && !userRef.value.contains(e.target as Node)) userOpen.value = false
}

onMounted(() => {
  document.addEventListener('mousedown', onDoc)
  loadTopTodos()
})

onBeforeUnmount(() => {
  document.removeEventListener('mousedown', onDoc)
})

const displayName = computed(() => currentUser.value?.nickname || currentUser.value?.username || '当前用户')
const displaySub = computed(() => currentUser.value?.deptName || currentUser.value?.username || '')
const roleText = computed(() => currentUser.value?.roles?.join(' / ') || '未分配角色')

function dotColor(level: string) {
  return level === 'danger' ? 'var(--danger)' : level === 'warn' ? 'var(--warn)' : 'var(--primary)'
}

async function loadTopTodos() {
  todoLoading.value = true
  try {
    const data = await portalApi.dashboard.summary()
    topTodos.value = data.todos.slice(0, 3).map(t => ({
      id: String(t.id ?? t.bizTitle ?? Math.random()),
      title: String(t.bizTitle ?? '待处理任务'),
      from: String(t.starter ?? '-'),
      time: String(t.due ?? '').slice(5) || '-',
      level: t.priority === 'high' ? 'danger' : t.priority === 'mid' ? 'warn' : 'info',
    }))
  } catch {
    topTodos.value = []
  } finally {
    todoLoading.value = false
  }
}

function goRoute(routeName: string) {
  bellOpen.value = false
  appOpen.value = false
  userOpen.value = false
  router.push({ name: routeName })
}

function searchMenu() {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return
  const match = menuEntries.find(item => {
    const haystack = [item.label, item.group, item.routeName, ...item.keywords].join(' ').toLowerCase()
    return haystack.includes(keyword)
  })
  if (!match) {
    pushToast('没有匹配的菜单', 'info')
    return
  }
  goRoute(match.routeName)
  pushToast(`已打开${match.label}`, 'ok')
}

function clearPasswordErr() {
  passwordErr.oldPassword = undefined
  passwordErr.newPassword = undefined
  passwordErr.confirmPassword = undefined
  passwordErr.general = undefined
}

function openPassword() {
  userOpen.value = false
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  clearPasswordErr()
  passwordOpen.value = true
}

async function submitPassword() {
  clearPasswordErr()
  passwordErr.oldPassword = passwordForm.oldPassword ? undefined : '请输入旧密码'
  passwordErr.newPassword = passwordForm.newPassword.length >= 6 ? undefined : '新密码至少 6 位'
  passwordErr.confirmPassword = passwordForm.confirmPassword === passwordForm.newPassword ? undefined : '两次输入不一致'
  if (passwordErr.oldPassword || passwordErr.newPassword || passwordErr.confirmPassword) return

  passwordSubmitting.value = true
  try {
    await authApi.changePassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword })
    passwordOpen.value = false
    pushToast('密码已修改，请重新登录', 'ok')
    emit('logout')
  } catch (error) {
    passwordErr.general = error instanceof Error ? error.message : '密码修改失败'
  } finally {
    passwordSubmitting.value = false
  }
}
</script>

<template>
  <header class="topbar">
    <div style="display: flex; align-items: center; gap: 12px">
      <el-tooltip :content="collapsed ? '展开侧栏' : '收起侧栏'" placement="top">
        <el-button :aria-label="collapsed ? '展开侧栏' : '收起侧栏'" :title="collapsed ? '展开侧栏' : '收起侧栏'" text circle @click="emit('toggle')">
          <Icon name="panelLeft" :size="19" />
        </el-button>
      </el-tooltip>
      <div class="topbar-search input-wrap has-icon">
        <Icon name="search" :size="15" />
        <input
          v-model="searchKeyword"
          class="input input-search"
          placeholder="搜索菜单、用户、功能..."
          style="width: 260px"
          @keydown.enter="searchMenu"
        />
        <span class="kbd">⌘K</span>
      </div>
    </div>

    <div style="display: flex; align-items: center; gap: 6px">
      <div ref="appRef" style="position: relative">
        <el-tooltip content="应用" placement="top">
          <el-button aria-label="应用" title="应用" text circle @click="appOpen = !appOpen">
            <Icon name="grid" :size="19" />
          </el-button>
        </el-tooltip>
        <div v-if="appOpen" class="pop-menu app-pop" style="width: 300px">
          <div class="pop-head">快捷入口</div>
          <div class="shortcut-grid">
            <button v-for="item in appShortcuts" :key="item.routeName" class="shortcut" @click="goRoute(item.routeName)">
              <span class="shortcut-ico"><Icon :name="item.icon" :size="17" /></span>
              <span>{{ item.label }}</span>
            </button>
          </div>
        </div>
      </div>

      <el-tooltip content="外观设置" placement="top">
        <el-button aria-label="外观设置" title="外观设置" text circle @click="appearanceOpen = true">
          <Icon name="gear" :size="19" />
        </el-button>
      </el-tooltip>

      <div ref="bellRef" style="position: relative">
        <el-tooltip content="通知" placement="top">
          <el-button aria-label="通知" title="通知" text circle style="position: relative" @click="bellOpen = !bellOpen">
            <Icon name="bell" :size="19" />
            <span v-if="topTodos.length" class="bell-dot" />
          </el-button>
        </el-tooltip>
        <div v-if="bellOpen" class="pop-menu" style="width: 320px">
          <div class="pop-head">通知 <span class="tag tag-info" style="height: 20px">{{ topTodos.length }} 条待办</span></div>
          <div v-if="todoLoading" class="pop-empty">加载中...</div>
          <div v-else-if="!topTodos.length" class="pop-empty">暂无待办</div>
          <div v-for="t in topTodos" :key="t.id" class="pop-item" @click="goRoute('todo')">
            <span class="pop-dot" :style="{ background: dotColor(t.level) }" />
            <div style="flex: 1; min-width: 0">
              <div style="font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis">{{ t.title }}</div>
              <div style="font-size: 12px; color: var(--ink-muted-48); margin-top: 2px">{{ t.from }} / {{ t.time }}</div>
            </div>
          </div>
          <div class="pop-foot" @click="goRoute('todo')">查看全部待办</div>
        </div>
      </div>

      <div class="topbar-sep" />

      <div ref="userRef" style="position: relative">
        <button class="user-chip" @click="userOpen = !userOpen">
          <el-avatar :size="30" :style="{ background: avatarColor(displayName, '#0066cc'), fontSize: 30 * 0.4 + 'px' }">{{ avatarText(displayName) }}</el-avatar>
          <span class="user-chip-name">{{ displayName }}</span>
          <Icon name="chevronDown" :size="14" style="color: var(--ink-muted-48)" />
        </button>
        <div v-if="userOpen" class="pop-menu" style="width: 200px; right: 0">
          <div class="pop-userhead">
            <el-avatar :size="40" :style="{ background: avatarColor(displayName, '#0066cc'), fontSize: 40 * 0.4 + 'px' }">{{ avatarText(displayName) }}</el-avatar>
            <div>
              <div style="font-size: 14px; font-weight: 600">{{ displayName }}</div>
              <div style="font-size: 12px; color: var(--ink-muted-48)">{{ displaySub }}</div>
            </div>
          </div>
          <div class="pop-line" @click="userOpen = false; profileOpen = true"><Icon name="user" :size="16" />个人中心</div>
          <div class="pop-line" @click="userOpen = false; appearanceOpen = true"><Icon name="gear" :size="16" />外观设置</div>
          <div class="pop-line" @click="openPassword"><Icon name="key" :size="16" />修改密码</div>
          <div class="pop-divider" />
          <div class="pop-line danger" @click="userOpen = false; emit('logout')"><Icon name="logout" :size="16" />退出登录</div>
        </div>
      </div>
    </div>

    <AppearanceModal v-if="appearanceOpen" @close="appearanceOpen = false" />

    <el-dialog v-if="profileOpen" title="个人中心" width="440px" destroy-on-close append-to-body @close="profileOpen = false">
      <div class="profile-list">
        <div><span>账号</span><b>{{ currentUser?.username || '-' }}</b></div>
        <div><span>昵称</span><b>{{ currentUser?.nickname || '-' }}</b></div>
        <div><span>部门</span><b>{{ currentUser?.deptName || '-' }}</b></div>
        <div><span>角色</span><b>{{ roleText }}</b></div>
      </div>
      <template #footer>
        <el-button type="primary" @click="profileOpen = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-if="passwordOpen" title="修改密码" width="460px" destroy-on-close append-to-body @close="passwordOpen = false">
      <div class="form-grid password-grid">
        <el-form-item label="旧密码" required :error="passwordErr.oldPassword" class="col-2">
          <input v-model="passwordForm.oldPassword" type="password" class="input" autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" required :error="passwordErr.newPassword" class="col-2">
          <input v-model="passwordForm.newPassword" type="password" class="input" autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" required :error="passwordErr.confirmPassword" class="col-2">
          <input v-model="passwordForm.confirmPassword" type="password" class="input" autocomplete="new-password" @keydown.enter="submitPassword" />
        </el-form-item>
        <div v-if="passwordErr.general" class="form-error col-2">{{ passwordErr.general }}</div>
      </div>
      <template #footer>
        <el-button plain @click="passwordOpen = false">取消</el-button>
        <el-button type="primary" :disabled="passwordSubmitting" @click="submitPassword">保存</el-button>
      </template>
    </el-dialog>
  </header>
</template>

<style scoped>
.app-pop { right: -108px; }

.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
  padding: 4px;
}

.shortcut {
  border: 1px solid transparent;
  background: transparent;
  border-radius: var(--r-sm);
  padding: 10px 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  color: var(--ink);
  font-size: 12.5px;
  transition: background .12s, border-color .12s;
}

.shortcut:hover {
  background: var(--surface-hover);
  border-color: var(--hairline-soft);
}

.shortcut-ico {
  width: 30px;
  height: 30px;
  border-radius: 9px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--primary-soft);
  color: var(--primary);
}

.pop-empty {
  padding: 18px 10px;
  text-align: center;
  font-size: 13px;
  color: var(--ink-muted-48);
}

.profile-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.profile-list div {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid var(--hairline-soft);
  font-size: 14px;
}

.profile-list span {
  color: var(--ink-muted-48);
}

.profile-list b {
  color: var(--ink);
  font-weight: 600;
  text-align: right;
  overflow-wrap: anywhere;
}

.password-grid {
  gap: 14px;
}

.form-error {
  color: var(--danger);
  font-size: 13px;
}
</style>
