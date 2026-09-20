<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import Icon from '@/components/Icon.vue'
import type { Column } from '@/types/table'
import type { DictType, DictItem } from '@/data'
import { ApiError, systemApi } from '@/api'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'

/* 字典管理：左侧字典类型 + 右侧字典数据（原型缺失 dict.jsx，按设计系统补齐） */
const types = ref<DictType[]>([])
const dataItems = ref<DictItem[]>([])
const typeKw = ref('')
const appliedTypeKw = ref('')
const activeType = ref<DictType>({ id: 0, dictName: '未选择字典', dictType: '', status: 1, createTime: '', remark: '' })
const loading = ref(false)
const saving = ref(false)

const TONES = ['info', 'ok', 'warn', 'danger', 'purple', 'neutral']

const filteredTypes = computed(() => types.value.filter(t =>
  !appliedTypeKw.value || t.dictName.includes(appliedTypeKw.value) || t.dictType.includes(appliedTypeKw.value)))
const items = computed(() => dataItems.value)

// —— 字典类型表单 ——
type TypeModal = { type: 'add' | 'edit' | 'delete'; row?: DictType } | null
const typeModal = ref<TypeModal>(null)
const typeForm = reactive<Partial<DictType>>({})
const typeErr = reactive<Record<string, string>>({})
function openAddType() { Object.assign(typeForm, { dictName: '', dictType: '', status: 1, remark: '' }); clearTypeErr(); typeModal.value = { type: 'add' } }
function openEditType(t: DictType) { Object.assign(typeForm, { ...t }); clearTypeErr(); typeModal.value = { type: 'edit', row: t } }
function clearTypeErr() { Object.keys(typeErr).forEach(k => delete typeErr[k]) }
async function reloadTypes(selectTypeCode?: string) {
  loading.value = true
  try {
    const data = await systemApi.dictTypes.list({ pageNum: 1, pageSize: 200, keyword: appliedTypeKw.value })
    types.value = data.rows as unknown as DictType[]
    const next = types.value.find(t => t.dictType === selectTypeCode)
      || types.value.find(t => t.id === activeType.value.id)
      || types.value[0]
    if (next) {
      await selectType(next)
    } else {
      activeType.value = { id: 0, dictName: '未选择字典', dictType: '', status: 1, createTime: '', remark: '' }
      dataItems.value = []
    }
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '字典类型加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

function applyTypeFilter() {
  appliedTypeKw.value = typeKw.value.trim()
  reloadTypes(activeType.value.dictType)
}

function resetTypeFilter() {
  typeKw.value = ''
  appliedTypeKw.value = ''
  reloadTypes(activeType.value.dictType)
}
async function selectType(row: DictType) {
  activeType.value = row
  if (!row.dictType) {
    dataItems.value = []
    return
  }
  try {
    dataItems.value = await systemApi.dictData.list(row.dictType) as unknown as DictItem[]
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '字典数据加载失败', 'danger')
  }
}
async function saveType() {
  clearTypeErr()
  if (!typeForm.dictName?.trim()) typeErr.dictName = '请输入字典名称'
  if (!typeForm.dictType?.trim()) typeErr.dictType = '请输入字典类型编码'
  else if (!/^[a-z][a-z0-9_]*$/.test(typeForm.dictType)) typeErr.dictType = '仅限小写字母、数字与下划线'
  if (Object.keys(typeErr).length) return
  saving.value = true
  try {
    const payload = {
      dictName: typeForm.dictName,
      dictType: typeForm.dictType,
      status: typeForm.status,
      remark: typeForm.remark,
    }
    if (typeModal.value?.row) {
      await systemApi.dictTypes.update(typeModal.value.row.id, payload)
      pushToast('字典类型已保存', 'ok')
    } else {
      await systemApi.dictTypes.create(payload)
      pushToast('字典类型已新增', 'ok')
    }
    typeModal.value = null
    await reloadTypes(typeForm.dictType)
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '字典类型保存失败', 'danger')
  } finally {
    saving.value = false
  }
}
async function deleteType() {
  try {
    const row = typeModal.value!.row!
    const rows = await systemApi.dictData.list(row.dictType) as unknown as DictItem[]
    await Promise.all(rows.map(item => systemApi.dictData.remove(item.id)))
    await systemApi.dictTypes.remove(row.id)
    pushToast('字典类型已删除', 'ok')
    typeModal.value = null
    await reloadTypes()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '字典类型删除失败', 'danger')
  }
}

// —— 字典数据表单 ——
type DataModal = { type: 'add' | 'edit' | 'delete'; row?: DictItem } | null
const dataModal = ref<DataModal>(null)
const dataForm = reactive<Partial<DictItem>>({})
const dataErr = reactive<Record<string, string>>({})
function openAddData() { Object.assign(dataForm, { label: '', value: '', sort: items.value.length + 1, status: 1, tone: 'info', def: false }); clearDataErr(); dataModal.value = { type: 'add' } }
function openEditData(it: DictItem) { Object.assign(dataForm, { ...it }); clearDataErr(); dataModal.value = { type: 'edit', row: it } }
function clearDataErr() { Object.keys(dataErr).forEach(k => delete dataErr[k]) }
async function saveData() {
  clearDataErr()
  if (!dataForm.label?.trim()) dataErr.label = '请输入字典标签'
  if (!dataForm.value?.trim()) dataErr.value = '请输入字典键值'
  if (Object.keys(dataErr).length) return
  saving.value = true
  try {
    const payload = {
      dictType: activeType.value.dictType,
      label: dataForm.label,
      value: dataForm.value,
      sort: dataForm.sort,
      status: dataForm.status,
      tone: dataForm.tone,
      def: dataForm.def,
    }
    if (dataModal.value?.row) {
      await systemApi.dictData.update(dataModal.value.row.id, payload)
      pushToast('字典数据已保存', 'ok')
    } else {
      await systemApi.dictData.create(payload)
      pushToast('字典数据已新增', 'ok')
    }
    dataModal.value = null
    await selectType(activeType.value)
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '字典数据保存失败', 'danger')
  } finally {
    saving.value = false
  }
}
async function deleteData() {
  try {
    await systemApi.dictData.remove(dataModal.value!.row!.id)
    pushToast('字典数据已删除', 'ok')
    dataModal.value = null
    await selectType(activeType.value)
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '字典数据删除失败', 'danger')
  }
}

async function exportDictData() {
  try {
    await systemApi.dictTypes.export()
    pushToast('字典数据已导出', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '导出失败', 'danger')
  }
}

onMounted(() => reloadTypes())

const sort = ref<{ key: string; dir: 'asc' | 'desc' } | null>(null)
function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

const columns: Column[] = [
  { key: 'label', title: '字典标签' },
  { key: 'value', title: '字典键值', width: 140 },
  { key: 'sort', title: '排序', width: 80 },
  { key: 'def', title: '默认', width: 90 },
  { key: 'status', title: '状态', width: 90 },
  { key: 'actions', title: '操作', width: 100, className: 'col-actions' },
]
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb v-if="['系统管理', '字典管理'].filter(item => item !== '字典管理').length" separator="/" class="page-crumb">
        <el-breadcrumb-item v-for="(item, index) in ['系统管理', '字典管理'].filter(item => item !== '字典管理')" :key="index">{{ item }}</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">字典管理</h1>
          <div class="page-desc">统一维护系统枚举与下拉选项。左侧选择字典类型，右侧维护字典数据项。</div>
        </div>
        <div class="page-actions"><el-button plain @click="reloadTypes(activeType.dictType)"><Icon name="refresh" :size="16" />刷新</el-button>
        <el-button type="primary" @click="openAddType"><Icon name="plus" :size="16" />新增字典类型</el-button></div>
      </div>
    </div>

    <div class="dict-layout">
      <!-- 左：字典类型 -->
      <el-card  class="dict-type-card" shadow="never">
        <div class="panel-head">
          <span class="panel-title">字典类型</span>
          <span class="cell-muted" style="font-size: 13px">{{ filteredTypes.length }}</span>
        </div>
        <div style="padding: 12px 12px 0">
          <el-input v-model="typeKw" clearable placeholder="搜索字典名称 / 编码" @keyup.enter="applyTypeFilter" />
          <div style="display: flex; gap: 8px; margin-top: 8px">
            <el-button type="primary" size="small" @click="applyTypeFilter"><Icon name="search" :size="15" />查询</el-button>
            <el-button plain size="small" @click="resetTypeFilter"><Icon name="refresh" :size="15" />重置</el-button>
          </div>
        </div>
        <div class="dict-type-list">
          <div
            v-for="t in filteredTypes" :key="t.id"
            :class="['dict-type-row', activeType.id === t.id ? 'active' : '']"
            @click="selectType(t)"
          >
            <span class="dict-type-ico"><Icon name="dict" :size="16" /></span>
            <div style="flex: 1; min-width: 0">
              <div class="dict-type-name">{{ t.dictName }}<el-tag v-if="!t.status" type="info" effect="light" round>停用</el-tag></div>
              <div class="dict-type-code">{{ t.dictType }}</div>
            </div>
            <div class="dict-type-actions">
              <el-tooltip content="编辑" placement="top"><el-button aria-label="编辑" title="编辑" text circle @click.stop="openEditType(t)"><Icon name="edit" :size="15" /></el-button></el-tooltip>
              <el-tooltip content="删除" placement="top"><el-button aria-label="删除" title="删除" text circle @click.stop="typeModal = { type: 'delete', row: t }"><Icon name="trash" :size="15" /></el-button></el-tooltip>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 右：字典数据 -->
      <el-card shadow="never">
        <div class="panel-head">
          <div style="display: flex; align-items: center; gap: 12px">
            <span class="menu-ico-box" style="width: 36px; height: 36px; background: var(--primary-soft); color: var(--primary)"><Icon name="dict" :size="19" /></span>
            <div>
              <div class="panel-title" style="display: flex; align-items: center; gap: 8px">{{ activeType.dictName }}<el-tag :type="(Number(activeType.status) === 1) ? 'success' : 'info'" effect="light" round><span class="el-tag-dot-inline" />{{ (Number(activeType.status) === 1) ? '正常' : '停用' }}</el-tag></div>
              <div class="panel-sub"><span class="code-chip">{{ activeType.dictType }}</span> · {{ activeType.remark }}</div>
            </div>
          </div>
          <div style="display: flex; gap: 8px">
            <el-tooltip content="导出" placement="top"><el-button aria-label="导出" title="导出" plain circle @click="exportDictData"><Icon name="download" :size="17" /></el-button></el-tooltip>
            <el-button type="primary" size="small" @click="openAddData"><Icon name="plus" :size="15" />新增数据</el-button>
          </div>
        </div>
        <el-table
        v-loading="loading"
        :data="items"
        :size="appearance.density === 'compact' ? 'small' : appearance.density === 'comfy' ? 'large' : 'default'"
        empty-text="该字典类型暂无数据"
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
      <template v-if="col.key === 'label'">
        <el-tag :type="row.tone === 'ok' ? 'success' : row.tone === 'warn' ? 'warning' : row.tone === 'danger' ? 'danger' : row.tone === 'info' || row.tone === 'purple' ? 'primary' : 'info'" effect="light" round>{{ row.label }}</el-tag>
      </template>
<template v-else-if="col.key === 'value'">
        <span class="code-chip">{{ row.value }}</span>
      </template>
<template v-else-if="col.key === 'sort'">
        <span class="tnum cell-muted">{{ row.sort }}</span>
      </template>
<template v-else-if="col.key === 'def'">
        <el-tag v-if="row.def" type="primary" effect="light" round>默认</el-tag><span v-else class="cell-muted">—</span>
      </template>
<template v-else-if="col.key === 'status'">
        <el-tag :type="(Number(row.status) === 1) ? 'success' : 'info'" effect="light" round><span class="el-tag-dot-inline" />{{ (Number(row.status) === 1) ? '正常' : '停用' }}</el-tag>
      </template>
<template v-else-if="col.key === 'actions'">
        <div class="row-actions">
              <el-tooltip content="编辑" placement="top"><el-button aria-label="编辑" title="编辑" text circle @click="openEditData(row)"><Icon name="edit" :size="16" /></el-button></el-tooltip>
              <el-tooltip content="删除" placement="top"><el-button aria-label="删除" title="删除" text circle @click="dataModal = { type: 'delete', row }"><Icon name="trash" :size="16" /></el-button></el-tooltip>
            </div>
      </template>
      <template v-else>{{ row[col.key] }}</template>
          </template>
        </el-table-column>
      </el-table>
      </el-card>
    </div>

    <!-- 字典类型表单 -->
    <el-dialog :model-value="true" v-if="typeModal && (typeModal.type === 'add' || typeModal.type === 'edit')"
      :title="typeModal.type === 'edit' ? '编辑字典类型' : '新增字典类型'" @close="typeModal = null">
      <div class="form-grid">
        <el-form-item label="字典名称" required :error="typeErr.dictName" class="col-2"><input :class="['input', typeErr.dictName ? 'invalid' : '']" v-model="typeForm.dictName" placeholder="如：用户性别" /></el-form-item>
        <el-form-item label="字典类型编码" required :error="typeErr.dictType" class="col-2"><input :class="['input', typeErr.dictType ? 'invalid' : '']" v-model="typeForm.dictType" :disabled="typeModal.type === 'edit'" placeholder="如：sys_user_sex" /></el-form-item>
        <el-form-item label="字典状态">
          <div style="display: flex; align-items: center; gap: 12px; height: 38px">
            <el-switch :model-value="typeForm.status === 1" @update:model-value="typeForm.status = $event ? 1 : 0" />
            <span style="font-size: 13.5px; color: var(--ink-72)">{{ typeForm.status === 1 ? '正常' : '停用' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="备注" class="col-2"><textarea class="input" v-model="typeForm.remark" placeholder="字典用途说明" /></el-form-item>
      </div>
      <template #footer>
        <el-button plain @click="typeModal = null">取消</el-button>
        <el-button type="primary" :disabled="saving" @click="saveType">{{ saving ? '保存中…' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="true" v-if="typeModal && typeModal.type === 'delete'"
      title="删除字典类型" width="420px" destroy-on-close append-to-body @close="typeModal = null">
      <div class="confirm-body">
        <div>确认删除字典类型 <b>{{ typeModal.row!.dictName }}</b> 吗？其下字典数据将一并删除。</div>
      </div>
      <template #footer>
        <el-button @click="typeModal = null">取消</el-button>
        <el-button type="danger" @click="deleteType">删除</el-button>
      </template>
    </el-dialog>

    <!-- 字典数据表单 -->
    <el-dialog :model-value="true" v-if="dataModal && (dataModal.type === 'add' || dataModal.type === 'edit')"
      :title="dataModal.type === 'edit' ? '编辑字典数据' : '新增字典数据'" @close="dataModal = null">
      <div class="form-grid">
        <el-form-item label="字典标签" required :error="dataErr.label"><input :class="['input', dataErr.label ? 'invalid' : '']" v-model="dataForm.label" placeholder="如：男" /></el-form-item>
        <el-form-item label="字典键值" required :error="dataErr.value"><input :class="['input', dataErr.value ? 'invalid' : '']" v-model="dataForm.value" placeholder="如：0" /></el-form-item>
        <el-form-item label="显示排序"><input class="input" type="number" v-model.number="dataForm.sort" /></el-form-item>
        <el-form-item label="标签样式">
          <el-select :model-value="dataForm.tone!" filterable @update:model-value="dataForm.tone = String($event)">
            <el-option v-for="option in TONES" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否默认">
          <div style="display: flex; align-items: center; gap: 12px; height: 38px">
            <el-switch :model-value="!!dataForm.def" @update:model-value="dataForm.def = Boolean($event)" />
            <span style="font-size: 13.5px; color: var(--ink-72)">{{ dataForm.def ? '默认项' : '非默认' }}</span>
          </div>
        </el-form-item>
        <el-form-item label="状态">
          <div style="display: flex; align-items: center; gap: 12px; height: 38px">
            <el-switch :model-value="dataForm.status === 1" @update:model-value="dataForm.status = $event ? 1 : 0" />
            <span style="font-size: 13.5px; color: var(--ink-72)">{{ dataForm.status === 1 ? '正常' : '停用' }}</span>
          </div>
        </el-form-item>
      </div>
      <template #footer>
        <el-button plain @click="dataModal = null">取消</el-button>
        <el-button type="primary" :disabled="saving" @click="saveData">{{ saving ? '保存中…' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="true" v-if="dataModal && dataModal.type === 'delete'"
      title="删除字典数据" width="420px" destroy-on-close append-to-body @close="dataModal = null">
      <div class="confirm-body">
        <div>确认删除字典数据 <b>{{ dataModal.row!.label }}</b> 吗？</div>
      </div>
      <template #footer>
        <el-button @click="dataModal = null">取消</el-button>
        <el-button type="danger" @click="deleteData">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>
