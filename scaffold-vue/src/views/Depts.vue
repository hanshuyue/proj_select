<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import Icon from '@/components/Icon.vue'
import DeptTreeNode from '@/components/tree/DeptTreeNode.vue'
import type { Dept } from '@/data'
import { ApiError, systemApi } from '@/api'
import { pushToast } from '@/composables/toast'
import { removeDeptFromTree } from '@/utils/deptTree'

const depts = ref<Dept[]>([])
const active = ref<Dept | null>(null)
const expanded = ref<number[]>([1, 2, 3, 4])
const saving = ref(false)

type ModalState = { type: 'add' | 'edit' | 'delete'; dept?: Dept; parent?: Dept } | null
const modal = ref<ModalState>(null)

function toggle(id: number) {
  expanded.value = expanded.value.includes(id) ? expanded.value.filter(x => x !== id) : [...expanded.value, id]
}

function expandAll() {
  expanded.value = flattenDeptIds(depts.value)
}

function collapseAll() {
  expanded.value = [1]
}

const memberCount = computed(() => {
  const a = active.value as (Dept & { memberCount?: number }) | null
  if (!a) return 0
  return a.memberCount ?? 0
})

const regionStats = computed(() => {
  const rows: Array<Dept & { memberCount?: number }> = []
  const walk = (nodes: Dept[]) => nodes.forEach((node) => {
    rows.push(node as Dept & { memberCount?: number })
    if (node.children?.length) walk(node.children)
  })
  walk(depts.value)
  return rows.filter(row => row.name === '德州市公司' || row.parentId !== 0)
})

function selectChild(c: Dept) {
  active.value = c
}

const form = reactive<Partial<Dept>>({})
const err = reactive<Record<string, string>>({})

function openAdd(parent: Dept | null) {
  Object.assign(form, { name: '', leader: '', phone: '', email: '', sort: 1, status: 1 })
  clearErr()
  modal.value = { type: 'add', parent: parent || undefined }
}

function openEdit(d: Dept) {
  Object.assign(form, { ...d })
  clearErr()
  modal.value = { type: 'edit', dept: d }
}

function clearErr() {
  Object.keys(err).forEach(k => delete err[k])
}

async function reload(selectId?: number) {
  try {
    const data = await systemApi.depts.tree()
    depts.value = data as unknown as Dept[]
    active.value = findDept(depts.value, selectId || active.value?.id) || depts.value[0] || null
    expanded.value = Array.from(new Set([1, ...expanded.value, active.value?.parentId || 0])).filter(Boolean)
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '部门树加载失败', 'danger')
  }
}

async function saveForm() {
  clearErr()
  if (!form.name?.trim()) {
    err.name = '请输入部门名称'
    return
  }

  saving.value = true
  try {
    const payload = {
      parentId: modal.value?.type === 'add' ? (modal.value.parent?.id || 0) : form.parentId,
      name: form.name,
      leader: form.leader,
      phone: form.phone,
      email: form.email,
      sort: form.sort,
      status: form.status,
    }
    let selectId = modal.value?.dept?.id
    if (modal.value?.type === 'add') {
      const created = await systemApi.depts.create(payload)
      selectId = created.id
      pushToast('部门已新增', 'ok')
    } else {
      await systemApi.depts.update(modal.value!.dept!.id, payload)
      pushToast('部门已保存', 'ok')
    }
    modal.value = null
    await reload(selectId)
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '部门保存失败', 'danger')
  } finally {
    saving.value = false
  }
}

async function doDelete() {
  try {
    const deleted = modal.value!.dept!
    await systemApi.depts.remove(deleted.id)
    const pruned = removeDeptFromTree(depts.value, deleted.id)
    depts.value = pruned.tree as Dept[]
    active.value = findDept(depts.value, pruned.parentId) || depts.value[0] || null
    expanded.value = expanded.value.filter(id => id !== deleted.id)
    pushToast('部门已删除', 'ok')
    const parentId = pruned.parentId || active.value?.id || 1
    modal.value = null
    await reload(parentId)
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '部门删除失败', 'danger')
  }
}

function flattenDeptIds(nodes: Dept[], acc: number[] = []) {
  nodes.forEach(node => {
    acc.push(node.id)
    if (node.children) flattenDeptIds(node.children, acc)
  })
  return acc
}

function findDept(nodes: Dept[], id?: number): Dept | null {
  if (!id) return null
  for (const node of nodes) {
    if (node.id === id) return node
    const hit = node.children ? findDept(node.children, id) : null
    if (hit) return hit
  }
  return null
}

async function exportDepts() {
  try {
    await systemApi.depts.export()
    pushToast('部门数据已导出', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '导出失败', 'danger')
  }
}

onMounted(() => reload())
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb v-if="['系统管理'].length" separator="/" class="page-crumb">
        <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">部门管理</h1>
          <div class="page-desc">查看各地区注册人数，并维护用户所属地区。</div>
        </div>
        <div class="page-actions">
          <el-button plain @click="exportDepts"><Icon name="download" :size="16" />导出</el-button>
          <el-button type="primary" @click="openAdd(active)"><Icon name="plus" :size="16" />新增部门</el-button>
        </div>
      </div>
    </div>

    <el-card shadow="never" style="margin-bottom: 16px">
      <template #header><div class="panel-title">各地区注册人数</div></template>
      <el-table :data="regionStats" stripe>
        <el-table-column prop="name" label="地区" min-width="180" />
        <el-table-column label="注册人数" width="140" align="center">
          <template #default="{ row }"><el-tag type="primary" effect="light" round>{{ row.memberCount || 0 }} 人</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }"><el-tag :type="Number(row.status) === 1 ? 'success' : 'info'" effect="light">{{ Number(row.status) === 1 ? '正常' : '停用' }}</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>

    <div class="tree-layout">
      <el-card class="dept-tree-card" shadow="never">
        <div class="tree-toolbar">
          <span style="font-size: 13px; font-weight: 600; color: var(--ink-72); flex: 1; padding-left: 4px">组织架构</span>
          <el-tooltip content="展开全部" placement="top"><el-button aria-label="展开全部" title="展开全部" text circle @click="expandAll"><Icon name="arrowDown" :size="15" /></el-button></el-tooltip>
          <el-tooltip content="收起全部" placement="top"><el-button aria-label="收起全部" title="收起全部" text circle @click="collapseAll"><Icon name="arrowUp" :size="15" /></el-button></el-tooltip>
        </div>
        <DeptTreeNode
          v-for="n in depts"
          :key="n.id"
          :node="n"
          :depth="0"
          :active-id="active && active.id"
          :expanded="expanded"
          @select="active = $event"
          @toggle="toggle"
        />
      </el-card>

      <el-card v-if="active" shadow="never">
        <div class="panel-head">
          <div style="display: flex; align-items: center; gap: 12px">
            <span class="menu-ico-box" style="width: 36px; height: 36px; background: var(--primary-soft); color: var(--primary)"><Icon name="dept" :size="19" /></span>
            <div>
              <div class="panel-title" style="display: flex; align-items: center; gap: 8px">
                {{ active.name }}
                <el-tag :type="Number(active.status) === 1 ? 'success' : 'info'" effect="light" round><span class="el-tag-dot-inline" />{{ Number(active.status) === 1 ? '正常' : '停用' }}</el-tag>
              </div>
              <div class="panel-sub">地区 ID #{{ active.id }} / 注册人数 {{ memberCount }} 人</div>
            </div>
          </div>
          <div style="display: flex; gap: 8px">
            <el-button plain size="small" @click="openAdd(active)"><Icon name="plus" :size="15" />新增下级</el-button>
            <el-button plain size="small" @click="openEdit(active)"><Icon name="edit" :size="15" />编辑</el-button>
            <el-tooltip v-if="active.id !== 1" content="删除" placement="top"><el-button aria-label="删除" title="删除" plain circle @click="modal = { type: 'delete', dept: active }"><Icon name="trash" :size="17" /></el-button></el-tooltip>
          </div>
        </div>
        <div style="padding: 22px">
          <div class="kv-grid">
            <div class="kv-cell"><div class="kv-label">部门名称</div><div class="kv-value">{{ active.name }}</div></div>
            <div class="kv-cell"><div class="kv-label">负责人</div><div class="kv-value">{{ active.leader || '暂无' }}</div></div>
            <div class="kv-cell"><div class="kv-label">联系电话</div><div class="kv-value tnum">{{ active.phone || '暂无' }}</div></div>
            <div class="kv-cell"><div class="kv-label">邮箱</div><div class="kv-value">{{ active.email || '暂无' }}</div></div>
            <div class="kv-cell"><div class="kv-label">显示排序</div><div class="kv-value tnum">{{ active.sort }}</div></div>
            <div class="kv-cell"><div class="kv-label">下级部门数</div><div class="kv-value tnum">{{ active.children ? active.children.length : 0 }}</div></div>
          </div>

          <div v-if="active.children && active.children.length" style="margin-top: 22px">
            <div style="font-size: 13px; font-weight: 600; color: var(--ink-72); margin-bottom: 12px">下级部门</div>
            <div style="display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 10px">
              <div
                v-for="c in active.children"
                :key="c.id"
                class="ucard-row"
                style="padding: 11px 14px; border: 1px solid var(--hairline); border-radius: var(--r-sm); cursor: pointer"
                @click="selectChild(c)"
              >
                <Icon name="dept" :size="15" />
                <span style="flex: 1; font-weight: 500; color: var(--ink)">{{ c.name }}</span>
                <Icon name="chevronRight" :size="14" />
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <el-card v-else shadow="never">
        <div class="dept-detail-empty"><div class="em-ico"><Icon name="dept" :size="28" /></div>请选择左侧部门查看详情</div>
      </el-card>
    </div>

    <el-dialog :model-value="true" v-if="modal && (modal.type === 'add' || modal.type === 'edit')" :title="modal.type === 'edit' ? '编辑部门' : '新增部门'" @close="modal = null">
      <div v-if="modal.type === 'add' && modal.parent" style="margin-bottom: 18px; font-size: 13px; color: var(--ink-muted-48)">
        上级部门：<span class="code-chip">{{ modal.parent.name }}</span>
      </div>
      <div class="form-grid">
        <el-form-item label="部门名称" required :error="err.name" class="col-2">
          <input :class="['input', err.name ? 'invalid' : '']" v-model="form.name" placeholder="如：德城区" />
        </el-form-item>
        <el-form-item label="负责人"><input class="input" v-model="form.leader" placeholder="姓名" /></el-form-item>
        <el-form-item label="联系电话"><input class="input" v-model="form.phone" placeholder="010-0000-0000" /></el-form-item>
        <el-form-item label="邮箱"><input class="input" v-model="form.email" placeholder="dept@company.com" /></el-form-item>
        <el-form-item label="显示排序"><input class="input" type="number" v-model.number="form.sort" /></el-form-item>
        <el-form-item label="部门状态" class="col-2">
          <div style="display: flex; align-items: center; gap: 12px">
            <el-switch :model-value="form.status === 1" @update:model-value="form.status = $event ? 1 : 0" />
            <span style="font-size: 13.5px; color: var(--ink-72)">{{ form.status === 1 ? '正常' : '停用' }}</span>
          </div>
        </el-form-item>
      </div>
      <template #footer>
        <el-button plain @click="modal = null">取消</el-button>
        <el-button type="primary" :disabled="saving" @click="saveForm">{{ saving ? '保存中...' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="true" v-if="modal && modal.type === 'delete'" title="删除部门" width="420px" destroy-on-close append-to-body @close="modal = null">
      <div class="confirm-body">
        <div>确认删除部门 <b>{{ modal.dept!.name }}</b> 吗？其下级部门和关联用户需要先处理。</div>
      </div>
      <template #footer>
        <el-button @click="modal = null">取消</el-button>
        <el-button type="danger" @click="doDelete">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>
