<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import Icon from '@/components/Icon.vue'
import { ICON_PATHS } from '@/data/icons'
import type { Menu } from '@/data'
import { ApiError, systemApi } from '@/api'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'

const MENU_TYPE_TAG: Record<string, [string, string]> = { dir: ['neutral', '目录'], menu: ['info', '菜单'], btn: ['purple', '按钮'] }
const ICONS = Object.keys(ICON_PATHS)

const expanded = ref<number[]>([100, 200, 300, 400, 101, 102])
const visMap = reactive<Record<number, boolean>>({})
const menus = ref<Menu[]>([])
const saving = ref(false)

type ModalState = { type: 'add' | 'edit' | 'delete'; menu?: Menu; parent?: Menu } | null
const modal = ref<ModalState>(null)

// 拖拽状态
const dragging = ref<number | null>(null)
const dragOver = ref<number | null>(null)
const dragOverPosition = ref<'before' | 'after' | 'inside' | null>(null)

function toggle(id: number) {
  expanded.value = expanded.value.includes(id) ? expanded.value.filter(x => x !== id) : [...expanded.value, id]
}

interface Row { node: Menu; depth: number; parentId: number | null }
const rows = computed(() => {
  const out: Row[] = []
  const walk = (nodes: Menu[], depth: number, parentId: number | null) => {
    nodes.forEach(n => {
      out.push({ node: n, depth, parentId })
      if (n.children && expanded.value.includes(n.id)) walk(n.children, depth + 1, n.id)
    })
  }
  walk(menus.value, 0, null)
  return out
})

const allIds = computed(() => {
  const a: number[] = []
  const w = (ns: Menu[]) => ns.forEach(n => { if (n.children) { a.push(n.id); w(n.children) } })
  w(menus.value)
  return a
})

function collectExpandableIds(nodes: Menu[]) {
  const ids: number[] = []
  const walk = (items: Menu[]) => items.forEach(item => {
    if (item.children && item.children.length) {
      ids.push(item.id)
      walk(item.children)
    }
  })
  walk(nodes)
  return ids
}

function visOf(node: Menu) {
  return visMap[node.id] !== undefined ? visMap[node.id] : node.visible === 1
}

// 查找节点的父节点
function findParentNode(nodes: Menu[], targetId: number, parent: Menu | null = null): Menu | null {
  for (const node of nodes) {
    if (node.id === targetId) return parent
    if (node.children) {
      const found = findParentNode(node.children, targetId, node)
      if (found) return found
    }
  }
  return null
}

// 查找节点
function findNode(nodes: Menu[], targetId: number): Menu | null {
  for (const node of nodes) {
    if (node.id === targetId) return node
    if (node.children) {
      const found = findNode(node.children, targetId)
      if (found) return found
    }
  }
  return null
}

// 获取同级节点列表
function getSiblingList(nodes: Menu[], targetId: number): Menu[] | null {
  for (const node of nodes) {
    if (node.id === targetId) return nodes
    if (node.children) {
      const found = getSiblingList(node.children, targetId)
      if (found) return found
    }
  }
  return null
}

// 拖拽事件处理
function onDragStart(e: DragEvent, node: Menu) {
  dragging.value = node.id
  if (e.dataTransfer) {
    e.dataTransfer.effectAllowed = 'move'
    e.dataTransfer.setData('text/plain', String(node.id))
  }
}

function onDragOver(e: DragEvent, node: Menu, parentId: number | null) {
  e.preventDefault()
  if (dragging.value === null || dragging.value === node.id) return

  // 检查是否可以拖拽到目标位置（只允许同级拖拽）
  const sourceParent = findParentNode(menus.value, dragging.value)
  const sourceParentId = sourceParent?.id ?? null

  // 判断是否同级：两个节点的父节点ID相同
  if (sourceParentId !== parentId) {
    if (e.dataTransfer) e.dataTransfer.dropEffect = 'none'
    return
  }

  if (e.dataTransfer) e.dataTransfer.dropEffect = 'move'
  dragOver.value = node.id

  // 判断拖拽位置
  const rect = (e.target as HTMLElement).getBoundingClientRect()
  const midY = rect.top + rect.height / 2
  if (e.clientY < midY - 10) {
    dragOverPosition.value = 'before'
  } else if (e.clientY > midY + 10) {
    dragOverPosition.value = 'after'
  } else {
    dragOverPosition.value = 'inside'
  }
}

function onDragLeave() {
  dragOver.value = null
  dragOverPosition.value = null
}

async function onDrop(e: DragEvent, targetNode: Menu, targetParentId: number | null) {
  e.preventDefault()
  if (dragging.value === null || dragging.value === targetNode.id) {
    resetDragState()
    return
  }

  // 检查是否同级
  const sourceParent = findParentNode(menus.value, dragging.value)
  const sourceParentId = sourceParent?.id ?? null

  if (sourceParentId !== targetParentId) {
    pushToast('只允许在同一层级内拖拽排序', 'danger')
    resetDragState()
    return
  }

  // 获取同级列表
  const siblings = getSiblingList(menus.value, dragging.value)
  if (!siblings) {
    resetDragState()
    return
  }

  // 找到源节点和目标节点在同级列表中的位置
  const sourceIndex = siblings.findIndex(n => n.id === dragging.value)
  const targetIndex = siblings.findIndex(n => n.id === targetNode.id)

  if (sourceIndex === -1 || targetIndex === -1) {
    resetDragState()
    return
  }

  // 计算新位置
  let newIndex = targetIndex
  if (dragOverPosition.value === 'after') {
    newIndex = sourceIndex < targetIndex ? targetIndex : targetIndex + 1
  } else if (dragOverPosition.value === 'before') {
    newIndex = sourceIndex < targetIndex ? targetIndex - 1 : targetIndex
  }

  // 重新排序
  const [movedItem] = siblings.splice(sourceIndex, 1)
  siblings.splice(newIndex, 0, movedItem)

  // 更新排序字段
  const sortUpdates = siblings.map((item, index) => ({
    id: item.id,
    sort: index + 1
  }))

  resetDragState()

  // 保存排序
  try {
    for (const update of sortUpdates) {
      await systemApi.menus.update(update.id, { sort: update.sort })
    }
    pushToast('排序已更新', 'ok')
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '排序保存失败', 'danger')
    await reload()
  }
}

function onDragEnd() {
  resetDragState()
}

function resetDragState() {
  dragging.value = null
  dragOver.value = null
  dragOverPosition.value = null
}

// 表单
const form = reactive<Partial<Menu>>({})
const err = reactive<Record<string, string>>({})
function openAdd(parent?: Menu) {
  const type = parent?.type === 'menu' ? 'btn' : 'menu'
  Object.assign(form, {
    name: '',
    type,
    icon: type === 'btn' ? '' : 'file',
    path: '',
    component: '',
    permission: '',
    sort: 1,
    visible: 1,
    status: 1,
  })
  clearErr(); modal.value = { type: 'add', parent }
}
function openEdit(m: Menu) { Object.assign(form, { ...m }); clearErr(); modal.value = { type: 'edit', menu: m } }
function clearErr() { Object.keys(err).forEach(k => delete err[k]) }
async function reload() {
  try {
    menus.value = await systemApi.menus.tree() as unknown as Menu[]
    expanded.value = collectExpandableIds(menus.value)
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '菜单树加载失败', 'danger')
  }
}
async function saveForm() {
  clearErr()
  if (!form.name?.trim()) err.name = '请输入菜单名称'
  if (form.type !== 'btn' && !form.path?.trim()) err.path = '请输入路由路径'
  if (form.type === 'btn' && !form.permission?.trim()) err.permission = '请输入权限标识'
  if (Object.keys(err).length) return
  saving.value = true
  try {
    const payload = {
      parentId: modal.value?.type === 'add' ? (modal.value.parent?.id || 0) : (form as any).parentId,
      name: form.name,
      type: form.type,
      icon: form.icon,
      path: form.path,
      component: form.component,
      permission: form.permission,
      sort: form.sort,
      visible: form.visible,
      status: form.status,
    }
    if (modal.value?.type === 'add') {
      await systemApi.menus.create(payload)
      pushToast('菜单已新增', 'ok')
    } else {
      await systemApi.menus.update(modal.value!.menu!.id, payload)
      pushToast('菜单已保存', 'ok')
    }
    modal.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '菜单保存失败', 'danger')
  } finally {
    saving.value = false
  }
}
async function updateVisible(node: Menu, visible: boolean) {
  visMap[node.id] = visible
  try {
    await systemApi.menus.update(node.id, { visible: visible ? 1 : 0 })
    pushToast(visible ? '已显示' : '已隐藏', 'info')
  } catch (error) {
    visMap[node.id] = node.visible === 1
    pushToast(error instanceof ApiError ? error.message : '显示状态保存失败', 'danger')
  }
}
async function doDelete() {
  try {
    await systemApi.menus.remove(modal.value!.menu!.id)
    pushToast('菜单已删除', 'ok')
    modal.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '菜单删除失败', 'danger')
  }
}

async function exportMenus() {
  try {
    await systemApi.menus.export()
    pushToast('菜单数据已导出', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '导出失败', 'danger')
  }
}

onMounted(reload)
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb v-if="['系统管理', '菜单管理'].filter(item => item !== '菜单管理').length" separator="/" class="page-crumb">
        <el-breadcrumb-item v-for="(item, index) in ['系统管理', '菜单管理'].filter(item => item !== '菜单管理')" :key="index">{{ item }}</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">菜单管理</h1>
          <div class="page-desc">维护前端菜单、页面路由与按钮权限。支持目录、菜单、按钮三类节点的树形维护。拖拽可调整同级菜单顺序。</div>
        </div>
        <div class="page-actions"><el-button plain @click="exportMenus"><Icon name="download" :size="16" />导出</el-button>
        <el-button type="primary" @click="openAdd()"><Icon name="plus" :size="16" />新增菜单</el-button></div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="panel-head">
        <span class="panel-title">菜单树</span>
        <div style="display: flex; gap: 8px">
          <el-button plain size="small" @click="expanded = [...allIds]"><Icon name="arrowDown" :size="15" />展开全部</el-button>
          <el-button plain size="small" @click="expanded = []"><Icon name="arrowUp" :size="15" />收起全部</el-button>
        </div>
      </div>
      <div class="tablewrap">
        <table :class="`tbl density-${appearance.density}`">
          <thead>
            <tr>
              <th style="min-width: 240px">菜单名称</th>
              <th style="width: 80px">类型</th>
              <th style="width: 180px">路由 / 权限标识</th>
              <th style="width: 70px">排序</th>
              <th style="width: 80px">显示</th>
              <th style="width: 90px">状态</th>
              <th style="width: 150px" class="col-actions">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="{ node, depth, parentId } in rows"
              :key="node.id"
              :class="{
                'dragging': dragging === node.id,
                'drag-over-before': dragOver === node.id && dragOverPosition === 'before',
                'drag-over-after': dragOver === node.id && dragOverPosition === 'after',
                'drag-over-inside': dragOver === node.id && dragOverPosition === 'inside'
              }"
              draggable="true"
              @dragstart="onDragStart($event, node)"
              @dragover="onDragOver($event, node, parentId)"
              @dragleave="onDragLeave"
              @drop="onDrop($event, node, parentId)"
              @dragend="onDragEnd"
            >
              <td>
                <div class="menu-name-cell">
                  <span class="drag-handle" title="拖拽排序">
                    <Icon name="grip" :size="14" />
                  </span>
                  <span class="tree-indent" :style="{ width: depth * 22 + 'px' }" />
                  <span
                    :class="['tree-toggle', node.children && node.children.length ? '' : 'leaf', expanded.includes(node.id) ? 'open' : '']"
                    @click="node.children && node.children.length && toggle(node.id)"
                  >
                    <Icon name="chevronRight" :size="14" />
                  </span>
                  <span v-if="node.type !== 'btn'" class="menu-ico-box"><Icon :name="node.icon || 'file'" :size="15" /></span>
                  <span v-else style="width: 28px; display: inline-flex; justify-content: center; color: var(--ink-muted-30)"><Icon name="dot" :size="14" :stroke="4" /></span>
                  <span :style="{ fontWeight: node.type === 'dir' ? 600 : 500 }">{{ node.name }}</span>
                </div>
              </td>
              <td><el-tag :type="(MENU_TYPE_TAG[node.type][0] as any) === 'ok' ? 'success' : (MENU_TYPE_TAG[node.type][0] as any) === 'warn' ? 'warning' : (MENU_TYPE_TAG[node.type][0] as any) === 'danger' ? 'danger' : (MENU_TYPE_TAG[node.type][0] as any) === 'info' || (MENU_TYPE_TAG[node.type][0] as any) === 'purple' ? 'primary' : 'info'" effect="light" round>{{ MENU_TYPE_TAG[node.type][1] }}</el-tag></td>
              <td>
                <span v-if="node.type === 'btn'" class="code-chip">{{ node.permission }}</span>
                <span v-else class="tnum cell-muted" style="font-size: 12.5px">{{ node.path }}</span>
              </td>
              <td><span class="tnum cell-muted">{{ node.sort }}</span></td>
              <td>
                <el-switch v-if="node.type !== 'btn'" :model-value="visOf(node)" @update:model-value="updateVisible(node, $event)" />
                <span v-else class="cell-muted">—</span>
              </td>
              <td><el-tag :type="(Number(node.status) === 1) ? 'success' : 'info'" effect="light" round><span class="el-tag-dot-inline" />{{ (Number(node.status) === 1) ? '正常' : '停用' }}</el-tag></td>
              <td class="col-actions">
                <div class="row-actions">
                  <el-tooltip v-if="node.type !== 'btn'" content="新增子项" placement="top"><el-button aria-label="新增子项" title="新增子项" text circle @click="openAdd(node)"><Icon name="plus" :size="16" /></el-button></el-tooltip>
                  <el-tooltip content="编辑" placement="top"><el-button aria-label="编辑" title="编辑" text circle @click="openEdit(node)"><Icon name="edit" :size="16" /></el-button></el-tooltip>
                  <el-tooltip content="删除" placement="top"><el-button aria-label="删除" title="删除" text circle @click="modal = { type: 'delete', menu: node }"><Icon name="trash" :size="16" /></el-button></el-tooltip>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </el-card>

    <!-- 菜单表单 -->
    <el-dialog :model-value="true" v-if="modal && (modal.type === 'add' || modal.type === 'edit')"
      :title="modal.type === 'edit' ? '编辑菜单' : '新增菜单'" :width="580" @close="modal = null">
      <div v-if="modal.parent" style="margin-bottom: 18px; font-size: 13px; color: var(--ink-muted-48)">
        上级菜单：<span class="code-chip">{{ modal.parent.name }}</span>
      </div>
      <el-form-item label="菜单类型" class="col-2">
        <div style="margin-bottom: 4px">
          <el-segmented :model-value="form.type!" :options="[{ label: '目录', value: 'dir' }, { label: '菜单', value: 'menu' }, { label: '按钮', value: 'btn' }]" @update:model-value="form.type = $event as any"  />
        </div>
      </el-form-item>
      <div class="form-grid" style="margin-top: 16px">
        <el-form-item label="菜单名称" required :error="err.name"><input :class="['input', err.name ? 'invalid' : '']" v-model="form.name" placeholder="如：用户管理" /></el-form-item>
        <el-form-item label="显示排序"><input class="input" type="number" v-model.number="form.sort" /></el-form-item>
        <template v-if="form.type !== 'btn'">
          <el-form-item label="路由路径" required :error="err.path"><input :class="['input', err.path ? 'invalid' : '']" v-model="form.path" placeholder="/system/user" /></el-form-item>
          <el-form-item label="图标">
            <div style="display: flex; flex-wrap: wrap; gap: 6px">
              <span
                v-for="ic in ICONS" :key="ic"
                class="menu-ico-box"
                :style="{ cursor: 'pointer', width: '32px', height: '32px', background: form.icon === ic ? 'var(--primary)' : 'var(--parchment)', color: form.icon === ic ? '#fff' : 'var(--ink-72)' }"
                @click="form.icon = ic"
              >
                <Icon :name="ic" :size="16" />
              </span>
            </div>
          </el-form-item>
        </template>
        <el-form-item v-if="form.type === 'menu'" label="组件路径" class="col-2"><input class="input" v-model="form.component" placeholder="system/user/index" /></el-form-item>
        <el-form-item v-if="form.type !== 'dir'" label="权限标识" :required="form.type === 'btn'" :error="err.permission" class="col-2">
          <input :class="['input', err.permission ? 'invalid' : '']" v-model="form.permission" placeholder="system:user:list" />
        </el-form-item>
        <el-form-item label="显示状态">
          <div style="display: flex; align-items: center; gap: 12px; height: 38px">
            <el-switch :model-value="form.visible === 1" @update:model-value="form.visible = $event ? 1 : 0" />
            <span style="font-size: 13.5px; color: var(--ink-72)">{{ form.visible ? '显示' : '隐藏' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="菜单状态">
          <div style="display: flex; align-items: center; gap: 12px; height: 38px">
            <el-switch :model-value="form.status === 1" @update:model-value="form.status = $event ? 1 : 0" />
            <span style="font-size: 13.5px; color: var(--ink-72)">{{ form.status ? '正常' : '停用' }}</span>
          </div>
        </el-form-item>
      </div>
      <template #footer>
        <el-button plain @click="modal = null">取消</el-button>
        <el-button type="primary" :disabled="saving" @click="saveForm">{{ saving ? '保存中…' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="true" v-if="modal && modal.type === 'delete'"
      title="删除菜单" width="420px" destroy-on-close append-to-body @close="modal = null">
      <div class="confirm-body">
        <div>确认删除 <b>{{ modal.menu!.name }}</b> 吗？<template v-if="modal.menu!.children">请先删除或调整其下级菜单。</template></div>
      </div>
      <template #footer>
        <el-button @click="modal = null">取消</el-button>
        <el-button type="danger" @click="doDelete">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.drag-handle {
  cursor: grab;
  color: var(--ink-muted-30);
  margin-right: 4px;
  display: inline-flex;
  align-items: center;
  transition: color 0.15s;
}

.drag-handle:hover {
  color: var(--ink-72);
}

.drag-handle:active {
  cursor: grabbing;
}

tr.dragging {
  opacity: 0.5;
  background: var(--surface-muted);
}

tr.drag-over-before {
  box-shadow: inset 0 2px 0 0 var(--primary);
}

tr.drag-over-after {
  box-shadow: inset 0 -2px 0 0 var(--primary);
}

tr.drag-over-inside {
  background: var(--primary-soft);
}

.menu-name-cell {
  display: flex;
  align-items: center;
  gap: 0;
}
</style>
