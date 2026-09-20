<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import Icon from '@/components/Icon.vue'
import type { User } from '@/data'
import { ApiError, systemApi } from '@/api'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'
import type { Column, SortState } from '@/types/table'
import { downloadBlob } from '@/utils/download'
import { avatarColor, avatarText } from '@/utils/avatar'
import { useRouter } from 'vue-router'
import { currentUser } from '@/composables/auth'

interface OptionRow {
  id: number
  name: string
}

type UserForm = Partial<User> & {
  deptName?: string
  postName?: string
  roleName?: string
  email?: string
  phone?: string
}

type ModalState =
  | { type: 'add' | 'edit'; user?: User }
  | { type: 'reset'; user: User }
  | { type: 'delete'; user?: User; batch?: boolean }
  | null

const list = ref<User[]>([])
const router = useRouter()
const superAdmin = computed(() => currentUser.value?.roles?.includes('super_admin') ?? false)
const deptOptions = ref<OptionRow[]>([])
const postOptions = ref<OptionRow[]>([])
const roleOptions = ref<OptionRow[]>([])

const kw = ref('')
const dept = ref('')
const post = ref('')
const role = ref('')
const status = ref('')
const appliedFilters = reactive({ kw: '', dept: '', post: '', role: '', status: '' })

const page = ref(1)
const pageSize = ref(10)
const selected = ref<(string | number)[]>([])
const sort = ref<SortState | null>(null)

const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const batching = ref(false)
const resetting = ref(false)

const modal = ref<ModalState>(null)
const form = reactive<UserForm>({})
const err = reactive<Record<string, string>>({})

const layout = computed(() => appearance.listLayout)
const deptSelectOptions = computed(() => [
  { label: '全部部门', value: '' },
  ...deptOptions.value.map(item => ({ label: item.name, value: item.name })),
])
const postNames = computed(() => postOptions.value.map(item => item.name))
const roleNames = computed(() => roleOptions.value.map(item => item.name))

const filtered = computed(() => {
  let rows = list.value.filter(item =>
    (!appliedFilters.kw ||
      item.nickname.includes(appliedFilters.kw) ||
      item.username.includes(appliedFilters.kw) ||
      String(item.phone || '').includes(appliedFilters.kw)) &&
    (!appliedFilters.dept || item.deptName === appliedFilters.dept) &&
    (!appliedFilters.post || item.postName === appliedFilters.post) &&
    (!appliedFilters.role || item.roleName === appliedFilters.role) &&
    (appliedFilters.status === '' || item.status === Number(appliedFilters.status)),
  )

  if (sort.value) {
    const current = sort.value
    rows = [...rows].sort((a, b) => {
      const av = (a as Record<string, unknown>)[current.key]
      const bv = (b as Record<string, unknown>)[current.key]
      const result = av === bv ? 0 : av! > bv! ? 1 : -1
      return current.dir === 'asc' ? result : -result
    })
  }

  return rows
})

const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

const columns: Column[] = [
  { key: 'nickname', title: '用户', sortable: true },
  { key: 'deptName', title: '部门' },
  { key: 'postName', title: '岗位' },
  { key: 'roleName', title: '角色' },
  { key: 'phone', title: '手机号' },
  { key: 'status', title: '状态', width: 90 },
  { key: 'lastLogin', title: '最近登录', sortable: true },
  { key: 'actions', title: '操作', width: 132, className: 'col-actions' },
]

function clearErr() {
  Object.keys(err).forEach(key => delete err[key])
}

function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

function deptIdByName(name?: string) {
  return deptOptions.value.find(item => item.name === name)?.id
}

function roleIdByName(name?: string) {
  return roleOptions.value.find(item => item.name === name)?.id
}

function postIdByName(name?: string) {
  return postOptions.value.find(item => item.name === name)?.id
}

function flattenDepts(nodes: any[]) {
  const rows: OptionRow[] = []
  const walk = (items: any[]) => {
    items.forEach(item => {
      rows.push({ id: Number(item.id), name: String(item.name) })
      if (Array.isArray(item.children)) walk(item.children)
    })
  }
  walk(nodes)
  return rows
}

async function loadOptions() {
  const [deptTree, posts, roles] = await Promise.all([
    systemApi.depts.tree(),
    systemApi.posts.list({ pageNum: 1, pageSize: 200 }),
    systemApi.roles.list({ pageNum: 1, pageSize: 200 }),
  ])

  deptOptions.value = flattenDepts(deptTree as any[])
  postOptions.value = posts.rows.map((row: any) => ({ id: Number(row.id), name: String(row.postName) }))
  roleOptions.value = roles.rows.map((row: any) => ({ id: Number(row.id), name: String(row.name) }))
}

async function reload() {
  loading.value = true
  try {
    const data = await systemApi.users.list({
      pageNum: 1,
      pageSize: 200,
      keyword: appliedFilters.kw || undefined,
      status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
      deptId: deptIdByName(appliedFilters.dept) || undefined,
    })
    list.value = data.rows as User[]
    selected.value = []
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '用户列表加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  Object.assign(appliedFilters, {
    kw: kw.value.trim(),
    dept: dept.value,
    post: post.value,
    role: role.value,
    status: status.value,
  })
  page.value = 1
  reload()
}

function resetFilters() {
  kw.value = ''
  dept.value = ''
  post.value = ''
  role.value = ''
  status.value = ''
  Object.assign(appliedFilters, { kw: '', dept: '', post: '', role: '', status: '' })
  page.value = 1
  reload()
}

async function exportCsv() {
  exporting.value = true
  try {
    const blob = await systemApi.users.export({
      keyword: appliedFilters.kw || undefined,
      status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
      deptId: deptIdByName(appliedFilters.dept) || undefined,
    })
    downloadBlob(blob, 'users.csv')
    pushToast('用户数据已导出', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '用户导出失败', 'danger')
  } finally {
    exporting.value = false
  }
}

function openAdd() {
  Object.assign(form, {
    username: '',
    nickname: '',
    phone: '',
    email: '',
    deptName: deptOptions.value[0]?.name || '',
    postName: postOptions.value[0]?.name || '',
    roleName: roleOptions.value[0]?.name || '',
    status: 1,
  })
  clearErr()
  modal.value = { type: 'add' }
}

function openEdit(user: User) {
  Object.assign(form, { ...user })
  clearErr()
  modal.value = { type: 'edit', user }
}

async function saveForm() {
  clearErr()

  if (!form.username?.trim()) err.username = '请输入登录账号'
  if (!form.nickname?.trim()) err.nickname = '请输入用户昵称'
  if (form.phone && !/^[\d-]{7,}$/.test(form.phone)) err.phone = '手机号格式不正确'
  if (form.email && !/^\S+@\S+\.\S+$/.test(form.email)) err.email = '邮箱格式不正确'
  if (Object.keys(err).length) return

  saving.value = true
  try {
    const payload = {
      username: form.username,
      nickname: form.nickname,
      password: 'admin123',
      phone: form.phone,
      email: form.email,
      deptId: deptIdByName(form.deptName),
      status: form.status,
      roleIds: roleIdByName(form.roleName) ? [roleIdByName(form.roleName)!] : [],
      postIds: postIdByName(form.postName) ? [postIdByName(form.postName)!] : [],
    }

    if (modal.value?.type === 'edit' && modal.value.user) {
      await systemApi.users.update(modal.value.user.id, payload)
      pushToast('用户已更新', 'ok')
    } else {
      await systemApi.users.create(payload)
      pushToast('用户已新增', 'ok')
    }

    modal.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '用户保存失败', 'danger')
  } finally {
    saving.value = false
  }
}

async function doDelete() {
  try {
    if (modal.value?.type === 'delete' && modal.value.batch) {
      const ids = selected.value.map(Number)
      await Promise.all(ids.map(id => systemApi.users.remove(id)))
      pushToast(`已删除 ${ids.length} 个用户`, 'ok')
      selected.value = []
    } else if (modal.value?.type === 'delete' && modal.value.user) {
      await systemApi.users.remove(modal.value.user.id)
      pushToast('用户已删除', 'ok')
    }

    modal.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '用户删除失败', 'danger')
  }
}

async function batchStatus(nextStatus: 0 | 1) {
  const ids = selected.value.map(Number)
  if (!ids.length) return

  batching.value = true
  try {
    await Promise.all(ids.map(id => systemApi.users.updateStatus(id, nextStatus)))
    pushToast(`已${nextStatus === 1 ? '启用' : '停用'} ${ids.length} 个用户`, 'ok')
    selected.value = []
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '批量更新用户状态失败', 'danger')
  } finally {
    batching.value = false
  }
}

async function resetPassword() {
  if (modal.value?.type !== 'reset') return

  resetting.value = true
  try {
    await systemApi.users.resetPassword(modal.value.user.id, '123456789')
    pushToast('密码已重置', 'ok')
    modal.value = null
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '密码重置失败', 'danger')
  } finally {
    resetting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  try {
    await loadOptions()
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '用户页面初始化失败', 'danger')
    selected.value = []
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">用户管理</h1>
          <div class="page-desc">维护后台系统用户信息与用户角色关系，支持按部门、岗位、角色和状态筛选。</div>
        </div>
        <div class="page-actions">
          <el-button v-if="superAdmin" type="warning" plain @click="router.push('/registration-review')">
            <Icon name="users" :size="16" />管理员设置
          </el-button>
          <el-button plain :disabled="exporting" @click="exportCsv">
            <Icon name="download" :size="16" />
            {{ exporting ? '导出中…' : '导出' }}
          </el-button>
          <el-button type="primary" @click="openAdd">
            <Icon name="plus" :size="16" />
            新增用户
          </el-button>
        </div>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <el-form class="filter-form" :inline="true" label-position="right" @submit.prevent>
        <el-form-item label="关键词" class="filter-item filter-item-keyword">
          <el-input
            v-model="kw"
            clearable
            style="width: 220px"
            placeholder="昵称 / 账号 / 手机号"
            @keyup.enter="applyFilters"
          />
        </el-form-item>
        <el-form-item label="部门" class="filter-item">
          <el-select v-model="dept" filterable style="width: 150px">
            <el-option v-for="option in deptSelectOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="岗位" class="filter-item">
          <el-select v-model="post" filterable style="width: 140px">
            <el-option label="全部岗位" value="" />
            <el-option v-for="option in postNames" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色" class="filter-item">
          <el-select v-model="role" filterable style="width: 140px">
            <el-option label="全部角色" value="" />
            <el-option v-for="option in roleNames" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" class="filter-item">
          <el-select v-model="status" filterable style="width: 120px">
            <el-option label="全部状态" value="" />
            <el-option label="正常" value="1" />
            <el-option label="停用" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" @click="applyFilters">
            <Icon name="search" :size="16" />
            查询
          </el-button>
          <el-button plain @click="resetFilters">
            <Icon name="refresh" :size="16" />
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <div class="panel-head">
        <div style="display: flex; align-items: center; gap: 12px">
          <span class="panel-title">用户列表</span>
          <span class="cell-muted" style="font-size: 13px">共 {{ filtered.length }} 个用户</span>
        </div>
        <div style="display: flex; align-items: center; gap: 8px">
          <el-tooltip content="刷新" placement="top">
            <el-button aria-label="刷新" title="刷新" plain circle @click="reload">
              <Icon name="refresh" :size="17" />
            </el-button>
          </el-tooltip>
          <el-tooltip content="导出当前结果" placement="top">
            <el-button aria-label="导出当前结果" title="导出当前结果" plain circle :disabled="exporting" @click="exportCsv">
              <Icon name="download" :size="17" />
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <div v-if="selected.length" class="selbar">
        已选择 <b>{{ selected.length }}</b> 项
        <el-button text size="small" :disabled="batching" @click="batchStatus(1)">批量启用</el-button>
        <el-button text size="small" :disabled="batching" @click="batchStatus(0)">批量停用</el-button>
        <el-button text size="small" class="danger" @click="modal = { type: 'delete', batch: true }">批量删除</el-button>
        <div style="flex: 1" />
        <el-button text size="small" @click="selected = []">取消选择</el-button>
      </div>

      <template v-if="layout === 'cards'">
        <div v-if="loading" style="padding: 40px; text-align: center; color: var(--ink-muted-48)">加载中…</div>
        <div v-else-if="paged.length === 0" class="tbl-empty">
          <div class="em-ico"><Icon name="search" :size="24" /></div>
          暂无数据
        </div>
        <div v-else class="ucard-grid">
          <div v-for="user in paged" :key="user.id" class="ucard">
            <div class="ucard-top">
              <el-avatar :size="46" :style="{ background: avatarColor(user.nickname), fontSize: `${46 * 0.4}px` }">
                {{ avatarText(user.nickname) }}
              </el-avatar>
              <div style="flex: 1; min-width: 0">
                <div class="ucard-name">
                  {{ user.nickname }}
                  <el-tag v-if="user.username === 'admin'" type="primary" effect="light" round>超管</el-tag>
                </div>
                <div class="ucard-user">@{{ user.username }}</div>
              </div>
              <el-tag :type="Number(user.status) === 1 ? 'success' : 'info'" effect="light" round>
                <span class="el-tag-dot-inline" />
                {{ Number(user.status) === 1 ? '正常' : '停用' }}
              </el-tag>
            </div>
            <div class="ucard-rows">
              <div class="ucard-row"><Icon name="dept" :size="14" /><span>{{ user.deptName }}</span></div>
              <div class="ucard-row"><Icon name="post" :size="14" /><span>{{ user.postName }}</span></div>
              <div class="ucard-row"><Icon name="role" :size="14" /><span>{{ user.roleName }}</span></div>
              <div class="ucard-row"><Icon name="phone" :size="14" /><span class="tnum">{{ user.phone }}</span></div>
              <div class="ucard-row"><Icon name="mail" :size="14" /><span style="overflow: hidden; text-overflow: ellipsis">{{ user.email }}</span></div>
            </div>
            <div class="ucard-foot">
              <span class="cell-muted" style="font-size: 12px">登录 {{ user.lastLogin }}</span>
              <div style="display: flex; gap: 2px">
                <el-tooltip content="编辑" placement="top">
                  <el-button aria-label="编辑" title="编辑" text circle @click="openEdit(user)">
                    <Icon name="edit" :size="16" />
                  </el-button>
                </el-tooltip>
                <el-tooltip content="重置密码" placement="top">
                  <el-button aria-label="重置密码" title="重置密码" text circle @click="modal = { type: 'reset', user }">
                    <Icon name="key" :size="16" />
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button aria-label="删除" title="删除" text circle @click="modal = { type: 'delete', user }">
                    <Icon name="trash" :size="16" />
                  </el-button>
                </el-tooltip>
              </div>
            </div>
          </div>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="paged"
        :size="appearance.density === 'compact' ? 'small' : appearance.density === 'comfy' ? 'large' : 'default'"
        row-key="id"
        border
        style="width: 100%"
        @sort-change="onSortChange"
        @selection-change="selected = $event.map((row: any) => row.id)"
      >
        <el-table-column type="selection" width="46" reserve-selection />
        <el-table-column
          v-for="col in columns"
          :key="col.key"
          :prop="col.key"
          :label="col.title"
          :width="col.width"
          :align="col.align"
          :class-name="col.className"
          :sortable="col.sortable ? 'custom' : false"
        >
          <template #default="{ row }">
            <template v-if="col.key === 'nickname'">
              <div style="display: flex; align-items: center; gap: 11px">
                <el-avatar :size="36" :style="{ background: avatarColor(row.nickname), fontSize: `${36 * 0.4}px` }">
                  {{ avatarText(row.nickname) }}
                </el-avatar>
                <div>
                  <div class="cell-strong" style="display: flex; align-items: center; gap: 6px">
                    {{ row.nickname }}
                    <el-tag v-if="row.username === 'admin'" type="primary" effect="light" round>超管</el-tag>
                  </div>
                  <div class="cell-muted" style="font-size: 12.5px; margin-top: 1px">@{{ row.username }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="col.key === 'deptName'">
              <span>{{ row.deptName }}</span>
            </template>
            <template v-else-if="col.key === 'postName'">
              <span class="cell-muted">{{ row.postName }}</span>
            </template>
            <template v-else-if="col.key === 'roleName'">
              <el-tag :type="String(row.roleName || '').includes('管理员') ? 'primary' : 'info'" effect="light" round>
                {{ row.roleName }}
              </el-tag>
            </template>
            <template v-else-if="col.key === 'phone'">
              <span class="tnum cell-muted">{{ row.phone }}</span>
            </template>
            <template v-else-if="col.key === 'status'">
              <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'" effect="light" round>
                <span class="el-tag-dot-inline" />
                {{ Number(row.status) === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
            <template v-else-if="col.key === 'lastLogin'">
              <span class="tnum cell-muted" style="font-size: 12.5px">{{ row.lastLogin }}</span>
            </template>
            <template v-else-if="col.key === 'actions'">
              <div class="row-actions">
                <el-tooltip content="编辑" placement="top">
                  <el-button aria-label="编辑" title="编辑" text circle @click="openEdit(row)">
                    <Icon name="edit" :size="16" />
                  </el-button>
                </el-tooltip>
                <el-tooltip content="重置密码" placement="top">
                  <el-button aria-label="重置密码" title="重置密码" text circle @click="modal = { type: 'reset', user: row }">
                    <Icon name="key" :size="16" />
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button aria-label="删除" title="删除" text circle @click="modal = { type: 'delete', user: row }">
                    <Icon name="trash" :size="16" />
                  </el-button>
                </el-tooltip>
              </div>
            </template>
            <template v-else>{{ row[col.key] }}</template>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="filtered.length"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @update:page-size="page = 1"
        />
      </div>
    </el-card>

    <el-dialog :model-value="true"
      v-if="modal && (modal.type === 'add' || modal.type === 'edit')"
      :title="modal.type === 'edit' ? '编辑用户' : '新增用户'"
      width="580px"
      destroy-on-close
      append-to-body
      @close="modal = null"
    >
      <div class="form-grid">
        <el-form-item label="登录账号" required :error="err.username">
          <input
            v-model="form.username"
            :class="['input', err.username ? 'invalid' : '']"
            :disabled="modal.type === 'edit'"
            placeholder="字母 / 数字组合"
          />
        </el-form-item>
        <el-form-item label="用户昵称" required :error="err.nickname">
          <input v-model="form.nickname" :class="['input', err.nickname ? 'invalid' : '']" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="手机号码" :error="err.phone">
          <input v-model="form.phone" :class="['input', err.phone ? 'invalid' : '']" placeholder="13800000000" />
        </el-form-item>
        <el-form-item label="电子邮箱" :error="err.email">
          <input v-model="form.email" :class="['input', err.email ? 'invalid' : '']" placeholder="name@company.com" />
        </el-form-item>
        <el-form-item label="所属部门" required>
          <el-select v-model="form.deptName" filterable>
            <el-option v-for="option in deptOptions" :key="option.id" :label="option.name" :value="option.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="任职岗位" required>
          <el-select v-model="form.postName" filterable>
            <el-option v-for="option in postNames" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户角色" required class="col-2">
          <el-select v-model="form.roleName" filterable>
            <el-option v-for="option in roleNames" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态" class="col-2">
          <div style="display: flex; align-items: center; gap: 12px">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
            <span style="font-size: 13.5px; color: var(--ink-72)">
              {{ form.status === 1 ? '正常，允许登录系统' : '停用，禁止登录系统' }}
            </span>
          </div>
        </el-form-item>
      </div>

      <div
        v-if="modal.type === 'add'"
        style="margin-top: 18px; padding: 11px 14px; background: var(--primary-soft); border-radius: var(--r-sm); font-size: 12.5px; color: var(--primary); display: flex; gap: 8px; align-items: center"
      >
        <Icon name="key" :size="14" />
        新用户初始密码为 <b style="margin: 0 2px">123456789</b>，首次登录必须修改密码。
      </div>

      <template #footer>
        <el-button plain @click="modal = null">取消</el-button>
        <el-button type="primary" :disabled="saving" @click="saveForm">
          {{ saving ? '保存中…' : modal.type === 'edit' ? '保存修改' : '确认新增' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="true"
      v-if="modal && modal.type === 'reset'"
      title="重置密码"
      width="420px"
      destroy-on-close
      append-to-body
      @close="modal = null"
    >
      <div class="confirm-body">
        <div>
          确认将用户 <b>{{ modal.user.nickname }}</b> 的密码重置为初始密码 <b>123456789</b> 吗？用户下次登录必须修改密码。
        </div>
      </div>
      <template #footer>
        <el-button @click="modal = null">取消</el-button>
        <el-button type="primary" :disabled="resetting" @click="resetPassword">
          {{ resetting ? '重置中…' : '确认重置' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="true"
      v-if="modal && modal.type === 'delete'"
      title="删除用户"
      width="420px"
      destroy-on-close
      append-to-body
      @close="modal = null"
    >
      <div class="confirm-body">
        <div v-if="modal.batch">
          确认删除选中的 <b>{{ selected.length }}</b> 个用户吗？此操作不可恢复。
        </div>
        <div v-else-if="modal.user">
          确认删除用户 <b>{{ modal.user.nickname }}</b> 吗？此操作不可恢复。
        </div>
      </div>
      <template #footer>
        <el-button @click="modal = null">取消</el-button>
        <el-button type="danger" @click="doDelete">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>
