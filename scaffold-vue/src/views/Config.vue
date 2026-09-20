<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import Icon from '@/components/Icon.vue'
import type { Config } from '@/data'
import { ApiError, systemApi } from '@/api'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'
import type { Column, SortState } from '@/types/table'

type ModalState = { type: 'add' | 'edit' | 'delete'; row?: Config } | null

interface ConfigForm extends Partial<Config> {
  key?: string
  value?: string
  type?: 'Y' | 'N' | string
  remark?: string
}

const list = ref<Config[]>([])
const kw = ref('')
const type = ref('')
const appliedFilters = reactive({ kw: '', type: '' })

const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const saving = ref(false)
const sort = ref<SortState | null>(null)

const modal = ref<ModalState>(null)
const form = reactive<ConfigForm>({})
const err = reactive<Record<string, string>>({})

const filtered = computed(() => {
  let rows = list.value.filter(item =>
    (!appliedFilters.kw || item.name.includes(appliedFilters.kw) || item.key.includes(appliedFilters.kw)) &&
    (appliedFilters.type === '' || item.type === appliedFilters.type),
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
  { key: 'name', title: '参数名称', sortable: true },
  { key: 'value', title: '参数值' },
  { key: 'type', title: '系统内置', width: 110 },
  { key: 'remark', title: '备注' },
  { key: 'createTime', title: '创建时间', width: 160, sortable: true },
  { key: 'actions', title: '操作', width: 100, className: 'col-actions' },
]

function clearErr() {
  Object.keys(err).forEach(key => delete err[key])
}

function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

async function reload() {
  loading.value = true
  try {
    const data = await systemApi.configs.list({
      pageNum: 1,
      pageSize: 200,
      keyword: appliedFilters.kw || undefined,
    })
    list.value = data.rows as unknown as Config[]
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '参数列表加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  appliedFilters.kw = kw.value.trim()
  appliedFilters.type = type.value
  page.value = 1
  reload()
}

function resetFilters() {
  kw.value = ''
  type.value = ''
  appliedFilters.kw = ''
  appliedFilters.type = ''
  page.value = 1
  reload()
}

async function exportConfigs() {
  try {
    await systemApi.configs.export({ keyword: appliedFilters.kw || undefined })
    pushToast('参数配置已导出', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '参数导出失败', 'danger')
  }
}

function openAdd() {
  Object.assign(form, { name: '', key: '', value: '', type: 'N', remark: '' })
  clearErr()
  modal.value = { type: 'add' }
}

function openEdit(row: Config) {
  Object.assign(form, { ...row })
  clearErr()
  modal.value = { type: 'edit', row }
}

async function saveForm() {
  clearErr()

  if (!form.name?.trim()) err.name = '请输入参数名称'
  if (!form.key?.trim()) err.key = '请输入参数键名'
  if (Object.keys(err).length) return

  saving.value = true
  try {
    const payload = {
      name: form.name,
      key: form.key,
      value: form.value || '',
      type: form.type,
      builtin: form.type === 'Y',
      remark: form.remark,
    }

    if (modal.value?.type === 'edit' && modal.value.row) {
      await systemApi.configs.update(modal.value.row.id, payload)
      pushToast('参数已更新', 'ok')
    } else {
      await systemApi.configs.create(payload)
      pushToast('参数已新增', 'ok')
    }

    modal.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '参数保存失败', 'danger')
  } finally {
    saving.value = false
  }
}

async function doDelete() {
  if (!modal.value?.row) return

  try {
    await systemApi.configs.remove(modal.value.row.id)
    pushToast('参数已删除', 'ok')
    modal.value = null
    await reload()
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '参数删除失败', 'danger')
  }
}

onMounted(reload)
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item>系统管理</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">参数配置</h1>
          <div class="page-desc">维护系统运行参数，系统内置参数不可删除，可用于统一控制业务规则。</div>
        </div>
        <div class="page-actions">
          <el-button plain @click="reload">
            <Icon name="refresh" :size="16" />
            刷新
          </el-button>
          <el-button type="primary" @click="openAdd">
            <Icon name="plus" :size="16" />
            新增参数
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
            style="width: 240px"
            placeholder="参数名称 / 键名"
            @keyup.enter="applyFilters"
          />
        </el-form-item>
        <el-form-item label="参数类型" class="filter-item">
          <el-select v-model="type" filterable style="width: 150px">
            <el-option label="全部类型" value="" />
            <el-option label="系统内置" value="Y" />
            <el-option label="自定义" value="N" />
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
          <span class="panel-title">参数列表</span>
          <span class="cell-muted" style="font-size: 13px">共 {{ filtered.length }} 项</span>
        </div>
        <el-tooltip content="导出当前结果" placement="top">
          <el-button aria-label="导出当前结果" title="导出当前结果" plain circle @click="exportConfigs">
            <Icon name="download" :size="17" />
          </el-button>
        </el-tooltip>
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
              <div>
                <div class="cell-strong">{{ row.name }}</div>
                <div style="margin-top: 3px"><span class="code-chip">{{ row.key }}</span></div>
              </div>
            </template>
            <template v-else-if="col.key === 'value'">
              <span v-if="row.value" class="tnum" style="font-size: 13px">{{ row.value }}</span>
              <span v-else class="cell-muted">（空）</span>
            </template>
            <template v-else-if="col.key === 'type'">
              <el-tag v-if="row.type === 'Y'" type="primary" effect="light" round>内置</el-tag>
              <el-tag v-else type="info" effect="light" round>自定义</el-tag>
            </template>
            <template v-else-if="col.key === 'remark'">
              <span class="cell-muted" style="font-size: 13px">{{ row.remark || '—' }}</span>
            </template>
            <template v-else-if="col.key === 'createTime'">
              <span class="tnum cell-muted" style="font-size: 12.5px">{{ row.createTime }}</span>
            </template>
            <template v-else-if="col.key === 'actions'">
              <div class="row-actions">
                <el-tooltip content="编辑" placement="top">
                  <el-button aria-label="编辑" title="编辑" text circle @click="openEdit(row)">
                    <Icon name="edit" :size="16" />
                  </el-button>
                </el-tooltip>
                <el-tooltip content="删除" placement="top">
                  <el-button aria-label="删除" title="删除" text circle :disabled="row.builtin" @click="modal = { type: 'delete', row }">
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
      :title="modal.type === 'edit' ? '编辑参数' : '新增参数'"
      :width="560"
      destroy-on-close
      append-to-body
      @close="modal = null"
    >
      <div class="form-grid">
        <el-form-item label="参数名称" required :error="err.name" class="col-2">
          <input v-model="form.name" :class="['input', err.name ? 'invalid' : '']" placeholder="如：账号初始密码" />
        </el-form-item>
        <el-form-item label="参数键名" required :error="err.key" class="col-2">
          <input
            v-model="form.key"
            :class="['input', err.key ? 'invalid' : '']"
            :disabled="modal.type === 'edit' && modal.row?.builtin"
            placeholder="如：sys.user.initPassword"
          />
        </el-form-item>
        <el-form-item label="参数值" class="col-2">
          <input v-model="form.value" class="input" placeholder="参数值" />
        </el-form-item>
        <el-form-item label="系统内置" class="col-2">
          <div style="display: flex; align-items: center; gap: 12px">
            <el-switch v-model="form.type" active-value="Y" inactive-value="N" />
            <span style="font-size: 13.5px; color: var(--ink-72)">
              {{ form.type === 'Y' ? '是，内置参数不可删除' : '否，业务参数' }}
            </span>
          </div>
        </el-form-item>
        <el-form-item label="备注" class="col-2">
          <textarea v-model="form.remark" class="input" placeholder="参数用途说明" />
        </el-form-item>
      </div>
      <template #footer>
        <el-button plain @click="modal = null">取消</el-button>
        <el-button type="primary" :disabled="saving" @click="saveForm">{{ saving ? '保存中…' : '保存' }}</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="true"
      v-if="modal && modal.type === 'delete'"
      title="删除参数"
      width="420px"
      destroy-on-close
      append-to-body
      @close="modal = null"
    >
      <div class="confirm-body">
        <div>确认删除参数 <b>{{ modal.row?.name }}</b> 吗？</div>
      </div>
      <template #footer>
        <el-button @click="modal = null">取消</el-button>
        <el-button type="danger" @click="doDelete">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>
