<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import Icon from '@/components/Icon.vue'
import type { Column } from '@/types/table'
import PermNode from '@/components/tree/PermNode.vue'
import type { Role, Menu } from '@/data'
import { ApiError, systemApi } from '@/api'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'

const SCOPES = [
  { key: 'all', name: '全部数据权限', desc: '可查看系统内所有部门的数据' },
  { key: 'dept_below', name: '本部门及以下数据权限', desc: '可查看本部门及其所有下级部门数据' },
  { key: 'dept', name: '本部门数据权限', desc: '仅可查看本部门数据' },
  { key: 'self', name: '仅本人数据权限', desc: '仅可查看本人创建的数据' },
  { key: 'custom', name: '自定义数据权限', desc: '手动指定可查看的部门范围' },
]

const list = ref<Role[]>([])
const menus = ref<Menu[]>([])
const depts = ref<{ id: number; name: string }[]>([])
const kw = ref('')
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const saving = ref(false)
const checkedSet = computed(() => new Set(checked.value))
const menuIndex = computed(() => {
  const nodeMap = new Map<number, Menu>()
  const subtreeMap = new Map<number, number[]>()
  const allIds: number[] = []

  const walk = (nodes: Menu[]) => {
    nodes.forEach(node => {
      nodeMap.set(node.id, node)
      allIds.push(node.id)

      const childIds = (node.children || []).flatMap(child => walkNode(child))
      subtreeMap.set(node.id, [node.id, ...childIds])
    })
  }

  const walkNode = (node: Menu): number[] => {
    nodeMap.set(node.id, node)
    allIds.push(node.id)

    const childIds = (node.children || []).flatMap(child => walkNode(child))
    const ids = [node.id, ...childIds]
    subtreeMap.set(node.id, ids)
    return ids
  }

  walk(menus.value)
  return { nodeMap, subtreeMap, allIds }
})
const allIds = computed(() => menuIndex.value.allIds)
const checkedMenuCount = computed(() => checked.value.filter(id => menuIndex.value.nodeMap.get(id)?.type !== 'btn').length)
const checkedButtonCount = computed(() => checked.value.filter(id => menuIndex.value.nodeMap.get(id)?.type === 'btn').length)
const checkedPermissionCodes = computed(() => checked.value
  .map(id => menuIndex.value.nodeMap.get(id)?.permission)
  .filter((permission): permission is string => Boolean(permission)))

type ModalState = { type: 'add' | 'edit' | 'delete'; role?: Role } | null
const modal = ref<ModalState>(null)
const drawer = ref<Role | null>(null)

const filtered = computed(() => list.value.filter(r => !kw.value || r.name.includes(kw.value) || r.code.includes(kw.value)))
const paged = computed(() => filtered.value.slice((page.value - 1) * pageSize.value, page.value * pageSize.value))

// —— 权限抽屉 ——
const permTab = ref<'menu' | 'scope'>('menu')
const checked = ref<number[]>([])
const scope = ref('dept')
const permTabOptions = [
  { label: '菜单与按钮', value: 'menu', icon: 'menu' },
  { label: '数据权限', value: 'scope', icon: 'shield' },
]
async function openDrawer(r: Role) {
  drawer.value = r
  permTab.value = 'menu'
  checked.value = []
  try {
    const data = await systemApi.roles.menus(r.id)
    checked.value = Array.isArray(data.menuIds) ? data.menuIds.map(Number) : []
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '角色权限加载失败', 'danger')
  }
  scope.value = scopeKeyFromLabel(r.dataScope)
}
function togglePerm(id: number) {
  const ids = menuIndex.value.subtreeMap.get(id) ?? [id]
  const nextChecked = new Set(checked.value)
  const allChecked = ids.every(item => nextChecked.has(item))

  ids.forEach(item => {
    if (allChecked) nextChecked.delete(item)
    else nextChecked.add(item)
  })

  checked.value = Array.from(nextChecked)
}
async function savePerm() {
  if (!drawer.value) return
  saving.value = true
  try {
    await systemApi.roles.update(drawer.value.id, { menuIds: checked.value, dataScope: scopeCode(scope.value) })
    pushToast('权限已保存', 'ok')
    drawer.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '权限保存失败', 'danger')
  } finally {
    saving.value = false
  }
}

// —— 角色表单 ——
const form = reactive<Partial<Role>>({})
const err = reactive<Record<string, string>>({})
function openAdd() { Object.assign(form, { name: '', code: '', sort: 9, status: 1, remark: '' }); clearErr(); modal.value = { type: 'add' } }
function openEdit(r: Role) { Object.assign(form, { ...r }); clearErr(); modal.value = { type: 'edit', role: r } }
function clearErr() { Object.keys(err).forEach(k => delete err[k]) }
async function reload() {
  loading.value = true
  try {
    const data = await systemApi.roles.list({ pageNum: 1, pageSize: 200, keyword: kw.value })
    list.value = data.rows as unknown as Role[]
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '角色列表加载失败', 'danger')
  } finally {
    loading.value = false
  }
}
async function loadCatalogs() {
  const [menuTree, deptTree] = await Promise.all([
    systemApi.menus.tree(),
    systemApi.depts.tree(),
  ])
  menus.value = menuTree as unknown as Menu[]
  depts.value = flattenDepts(deptTree as any[])
}
async function saveForm() {
  clearErr()
  if (!form.name?.trim()) err.name = '请输入角色名称'
  if (!form.code?.trim()) err.code = '请输入角色编码'
  if (Object.keys(err).length) return
  saving.value = true
  try {
    const payload = {
      name: form.name,
      code: form.code,
      dataScope: scopeCode(scopeKeyFromLabel(form.dataScope)),
      sort: form.sort,
      status: form.status,
      remark: form.remark,
    }
    if (modal.value?.type === 'add') {
      await systemApi.roles.create(payload)
      pushToast('角色已新增', 'ok')
    } else {
      await systemApi.roles.update(modal.value!.role!.id, payload)
      pushToast('角色已保存', 'ok')
    }
    modal.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '角色保存失败', 'danger')
  } finally {
    saving.value = false
  }
}
async function doDelete() {
  try {
    await systemApi.roles.remove(modal.value!.role!.id)
    pushToast('角色已删除', 'ok')
    modal.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '角色删除失败', 'danger')
  }
}

async function exportRoles() {
  try {
    await systemApi.roles.export({ keyword: kw.value || undefined })
    pushToast('角色数据已导出', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '导出失败', 'danger')
  }
}

function scopeCode(key: string) {
  return key === 'all' ? 'ALL'
    : key === 'dept_below' ? 'DEPT_AND_CHILD'
    : key === 'dept' ? 'DEPT'
    : key === 'custom' ? 'CUSTOM'
    : 'SELF'
}
function scopeKeyFromLabel(label?: string) {
  if (label === '全部数据权限' || label === 'ALL') return 'all'
  if (label === '本部门及以下' || label === '本部门及以下数据权限' || label === 'DEPT_AND_CHILD') return 'dept_below'
  if (label === '本部门数据权限' || label === 'DEPT') return 'dept'
  if (label === '自定义数据权限' || label === 'CUSTOM') return 'custom'
  return 'self'
}
function flattenDepts(nodes: any[]) {
  const rows: { id: number; name: string }[] = []
  const walk = (items: any[]) => items.forEach(item => {
    rows.push({ id: Number(item.id), name: String(item.name) })
    if (Array.isArray(item.children)) walk(item.children)
  })
  walk(nodes)
  return rows
}

onMounted(async () => {
  loading.value = true
  try {
    await loadCatalogs()
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '角色页面初始化失败', 'danger')
  } finally {
    loading.value = false
  }
})

const sort = ref<{ key: string; dir: 'asc' | 'desc' } | null>(null)
function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

const columns: Column[] = [
  { key: 'name', title: '角色' },
  { key: 'dataScope', title: '数据权限' },
  { key: 'userCount', title: '用户数', align: 'left' },
  { key: 'createTime', title: '创建时间' },
  { key: 'status', title: '状态', width: 90 },
  { key: 'actions', title: '操作', width: 170, className: 'col-actions' },
]
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb v-if="['系统管理', '角色管理'].filter(item => item !== '角色管理').length" separator="/" class="page-crumb">
        <el-breadcrumb-item v-for="(item, index) in ['系统管理', '角色管理'].filter(item => item !== '角色管理')" :key="index">{{ item }}</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">角色管理</h1>
          <div class="page-desc">系统固定为超级管理员、审批专员和普通员工三个角色。</div>
        </div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="panel-head">
        <div style="width: 260px"><el-input v-model="kw" clearable placeholder="搜索角色名称 / 编码" /></div>
        <div style="display: flex; gap: 8px">
          <el-tooltip content="刷新" placement="top"><el-button aria-label="刷新" title="刷新" plain circle @click="reload"><Icon name="refresh" :size="17" /></el-button></el-tooltip>
          <el-tooltip content="导出" placement="top"><el-button aria-label="导出" title="导出" plain circle @click="exportRoles"><Icon name="download" :size="17" /></el-button></el-tooltip>
        </div>
      </div>
      <el-table
        v-loading="loading"
        :data="paged"
        :size="appearance.density === 'compact' ? 'small' : appearance.density === 'comfy' ? 'large' : 'default'"
        
        row-key="id"
        
        border
        style="width: 100%"
        @sort-change="onSortChange"
        
        
      >
        
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
      <template v-if="col.key === 'name'">
        <div style="display: flex; align-items: center; gap: 11px">
            <span class="menu-ico-box" :style="{ background: row.builtin ? '#f0ecfb' : 'var(--primary-soft)', color: row.builtin ? '#6b46d9' : 'var(--primary)' }"><Icon name="role" :size="16" /></span>
            <div>
              <div class="cell-strong" style="display: flex; align-items: center; gap: 6px">{{ row.name }}<el-tag v-if="row.builtin" type="primary" effect="light" round>内置</el-tag></div>
              <div style="margin-top: 2px"><span class="code-chip">{{ row.code }}</span></div>
            </div>
          </div>
      </template>
<template v-else-if="col.key === 'dataScope'">
        <el-tag type="primary" effect="light" round>{{ row.dataScope }}</el-tag>
      </template>
<template v-else-if="col.key === 'userCount'">
        <span class="tnum cell-strong">{{ row.userCount }}</span>
      </template>
<template v-else-if="col.key === 'createTime'">
        <span class="tnum cell-muted" style="font-size: 12.5px">{{ row.createTime }}</span>
      </template>
<template v-else-if="col.key === 'status'">
        <el-tag :type="(Number(row.status) === 1) ? 'success' : 'info'" effect="light" round><span class="el-tag-dot-inline" />{{ (Number(row.status) === 1) ? '正常' : '停用' }}</el-tag>
      </template>
<template v-else-if="col.key === 'actions'">
          <div class="row-actions">
            <el-button text size="small" :disabled="row.builtin && row.id === 1" @click="openDrawer(row)"><Icon name="shield" :size="15" />权限</el-button>
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
        />
      </div>
    </el-card>

    <!-- 角色表单 -->
    <el-dialog :model-value="true" v-if="modal && (modal.type === 'add' || modal.type === 'edit')"
      :title="modal.type === 'edit' ? '编辑角色' : '新增角色'" @close="modal = null">
      <div class="form-grid">
        <el-form-item label="角色名称" required :error="err.name"><input :class="['input', err.name ? 'invalid' : '']" v-model="form.name" placeholder="如：业务管理员" /></el-form-item>
        <el-form-item label="角色编码" required :error="err.code"><input :class="['input', err.code ? 'invalid' : '']" v-model="form.code" placeholder="如：biz_admin" /></el-form-item>
        <el-form-item label="显示排序"><input class="input" type="number" v-model.number="form.sort" /></el-form-item>
        <el-form-item label="角色状态">
          <div style="display: flex; align-items: center; gap: 12px; height: 38px">
            <el-switch :model-value="form.status === 1" @update:model-value="form.status = $event ? 1 : 0" />
            <span style="font-size: 13.5px; color: var(--ink-72)">{{ form.status === 1 ? '正常' : '停用' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="备注" class="col-2"><textarea class="input" v-model="form.remark" placeholder="角色职责说明" /></el-form-item>
      </div>
      <template #footer>
        <el-button plain @click="modal = null">取消</el-button>
        <el-button type="primary" :disabled="saving" @click="saveForm">{{ saving ? '保存中…' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <!-- 权限抽屉 -->
    <el-drawer :model-value="true" v-if="drawer" title="`分配权限 · ${drawer.name}`" size="520px" destroy-on-close append-to-body @close="drawer = null">
      <p class="panel-sub">角色编码 {{ drawer.code }}</p>
      <div style="margin-bottom: 18px">
        <el-segmented v-model="permTab" :options="permTabOptions">
          <template #default="{ item }">
            <span style="display: inline-flex; align-items: center; gap: 5px">
              <Icon v-if="item.icon" :name="item.icon" :size="15" />
              {{ item.label }}
            </span>
          </template>
        </el-segmented>
      </div>

      <div v-if="permTab === 'menu'">
        <div style="display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px">
          <span style="font-size: 13px; color: var(--ink-muted-48)">菜单 <b style="color: var(--primary)">{{ checkedMenuCount }}</b> 项，按钮 <b style="color: var(--primary)">{{ checkedButtonCount }}</b> 项</span>
          <div style="display: flex; gap: 6px">
            <el-button text size="small" @click="checked = [...allIds]">全选</el-button>
            <el-button text size="small" @click="checked = []">清空</el-button>
          </div>
        </div>
        <div style="margin-bottom: 12px; padding: 10px 12px; border: 1px solid var(--hairline); border-radius: var(--r-sm); background: var(--surface-muted)">
          <div style="font-size: 12.5px; color: var(--ink-muted-48); margin-bottom: 8px">权限预览</div>
          <div style="display: flex; flex-wrap: wrap; gap: 6px; max-height: 78px; overflow: auto">
            <span v-for="code in checkedPermissionCodes.slice(0, 80)" :key="code" class="code-chip">{{ code }}</span>
            <span v-if="checkedPermissionCodes.length > 80" class="cell-muted">+{{ checkedPermissionCodes.length - 80 }}</span>
          </div>
        </div>
        <div class="perm-tree">
          <PermNode v-for="n in menus" :key="n.id" :node="n" :depth="0" :checked="checked" :checked-set="checkedSet" @toggle="togglePerm" />
        </div>
      </div>

      <div v-else>
        <div style="font-size: 13px; color: var(--ink-muted-48); margin-bottom: 14px">选择该角色在业务列表中可查看的数据范围：</div>
        <div class="scope-list">
          <div v-for="s in SCOPES" :key="s.key" :class="['scope-opt', scope === s.key ? 'on' : '']" @click="scope = s.key">
            <span class="scope-radio" />
            <div><div class="scope-name">{{ s.name }}</div><div class="scope-desc">{{ s.desc }}</div></div>
          </div>
        </div>
        <div v-if="scope === 'custom'" style="margin-top: 14px; padding: 16px; border: 1px solid var(--hairline); border-radius: var(--r-md)">
          <div style="font-size: 13px; font-weight: 600; margin-bottom: 12px">指定可见部门</div>
          <div style="display: flex; flex-wrap: wrap; gap: 8px">
            <span v-for="d in depts" :key="d.id" class="perm-btn-chip">{{ d.name }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button plain @click="drawer = null">取消</el-button>
        <el-button type="primary" :disabled="saving" @click="savePerm">{{ saving ? '保存中…' : '保存权限' }}</el-button>
      </template>
    </el-drawer>

    <el-dialog :model-value="true" v-if="modal && modal.type === 'delete'"
      title="删除角色" width="420px" destroy-on-close append-to-body @close="modal = null">
      <div class="confirm-body">
        <div>确认删除角色 <b>{{ modal.role!.name }}</b> 吗？该角色下 {{ modal.role!.userCount }} 名用户将失去对应权限。</div>
      </div>
      <template #footer>
        <el-button @click="modal = null">取消</el-button>
        <el-button type="danger" @click="doDelete">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>
