<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import Icon from '@/components/Icon.vue'
import type { OperLog } from '@/data'
import { ApiError, systemApi } from '@/api'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'
import type { Column, SortState } from '@/types/table'
import { downloadBlob } from '@/utils/download'
import { includesKeyword } from '@/utils/search'

const METHOD_TONE: Record<string, string> = {
  GET: 'info',
  POST: 'success',
  PUT: 'warning',
  DELETE: 'danger',
}

const list = ref<OperLog[]>([])
const kw = ref('')
const mod = ref('')
const result = ref('')
const appliedFilters = reactive({ kw: '', mod: '', result: '' })

const page = ref(1)
const pageSize = ref(10)
const selected = ref<(string | number)[]>([])
const sort = ref<SortState | null>({ key: 'operTime', dir: 'desc' })

const detail = ref<OperLog | null>(null)
const clearOpen = ref(false)
const loading = ref(false)
const exporting = ref(false)

const modules = computed(() => Array.from(new Set(list.value.map(item => item.module))).filter(Boolean))

const filtered = computed(() => {
  let rows = list.value.filter(item =>
    includesKeyword(appliedFilters.kw, item.module, item.operName, item.url, item.params) &&
    (!appliedFilters.mod || item.module === appliedFilters.mod) &&
    (appliedFilters.result === '' || item.result === Number(appliedFilters.result)),
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
  { key: 'module', title: '操作模块', width: 110 },
  { key: 'action', title: '操作类型', width: 96 },
  { key: 'url', title: '请求地址' },
  { key: 'operName', title: '操作人员', width: 110 },
  { key: 'ip', title: '主机 / 地点' },
  { key: 'cost', title: '耗时', width: 90, sortable: true },
  { key: 'result', title: '结果', width: 80 },
  { key: 'operTime', title: '操作时间', width: 160, sortable: true },
  { key: 'actions', title: '操作', width: 70, className: 'col-actions' },
]

function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

async function reload() {
  loading.value = true
  try {
    const data = await systemApi.operLogs.list({
      pageNum: 1,
      pageSize: 200,
      keyword: appliedFilters.kw || undefined,
      result: appliedFilters.result === '' ? undefined : Number(appliedFilters.result),
    })
    list.value = data.rows as unknown as OperLog[]
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '操作日志加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

async function batchDelete() {
  const ids = [...selected.value]
  try {
    await Promise.all(ids.map(id => systemApi.operLogs.remove(id)))
    selected.value = []
    await reload()
    pushToast('已删除所选操作日志', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '操作日志删除失败', 'danger')
  }
}

async function clearLogs() {
  try {
    await systemApi.operLogs.clear()
    clearOpen.value = false
    selected.value = []
    await reload()
    pushToast('操作日志已清空', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '操作日志清空失败', 'danger')
  }
}

async function exportLogs() {
  exporting.value = true
  try {
    const blob = await systemApi.operLogs.export({
      keyword: appliedFilters.kw || undefined,
      result: appliedFilters.result === '' ? undefined : Number(appliedFilters.result),
    })
    downloadBlob(blob, 'oper-logs.csv')
    pushToast('操作日志已导出', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '操作日志导出失败', 'danger')
  } finally {
    exporting.value = false
  }
}

function applyFilters() {
  appliedFilters.kw = kw.value.trim()
  appliedFilters.mod = mod.value
  appliedFilters.result = result.value
  page.value = 1
  reload()
}

function resetFilters() {
  kw.value = ''
  mod.value = ''
  result.value = ''
  Object.assign(appliedFilters, { kw: '', mod: '', result: '' })
  page.value = 1
  reload()
}

onMounted(reload)
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb separator="/" class="page-crumb">
        <el-breadcrumb-item>系统监控</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">操作日志</h1>
          <div class="page-desc">记录后台关键业务操作，支持按模块和执行结果筛选，方便追溯问题。</div>
        </div>
        <div class="page-actions">
          <el-button plain @click="clearOpen = true">
            <Icon name="trash" :size="16" />
            清空
          </el-button>
          <el-button plain :disabled="exporting" @click="exportLogs">
            <Icon name="download" :size="16" />
            导出
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
            placeholder="模块 / 操作人 / 请求参数"
            @keyup.enter="applyFilters"
          />
        </el-form-item>
        <el-form-item label="操作模块" class="filter-item">
          <el-select v-model="mod" filterable style="width: 140px">
            <el-option label="全部模块" value="" />
            <el-option v-for="option in modules" :key="option" :label="option" :value="option" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行结果" class="filter-item">
          <el-select v-model="result" filterable style="width: 120px">
            <el-option label="全部结果" value="" />
            <el-option label="成功" value="1" />
            <el-option label="失败" value="0" />
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
          <span class="panel-title">操作记录</span>
          <span class="cell-muted" style="font-size: 13px">共 {{ filtered.length }} 条</span>
        </div>
        <el-tooltip content="导出当前结果" placement="top">
          <el-button aria-label="导出当前结果" title="导出当前结果" plain circle @click="exportLogs">
            <Icon name="download" :size="17" />
          </el-button>
        </el-tooltip>
      </div>

      <div v-if="selected.length" class="selbar">
        已选择 <b>{{ selected.length }}</b> 项
        <el-button text size="small" class="danger" @click="batchDelete">批量删除</el-button>
        <div style="flex: 1" />
        <el-button text size="small" @click="selected = []">取消选择</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="paged"
        :size="appearance.density === 'compact' ? 'small' : appearance.density === 'comfy' ? 'large' : 'default'"
        row-key="id"
        class="table-row-clickable"
        border
        style="width: 100%"
        @sort-change="onSortChange"
        @selection-change="selected = $event.map((row: any) => row.id)"
        @row-click="detail = $event"
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
            <template v-if="col.key === 'module'">
              <span class="cell-strong">{{ row.module }}</span>
            </template>
            <template v-else-if="col.key === 'action'">
              <el-tag
                :type="row.tone === 'ok' ? 'success' : row.tone === 'warn' ? 'warning' : row.tone === 'danger' ? 'danger' : row.tone === 'info' ? 'primary' : 'info'"
                effect="light"
                round
              >
                {{ row.action }}
              </el-tag>
            </template>
            <template v-else-if="col.key === 'url'">
              <div style="display: flex; align-items: center; gap: 8px">
                <el-tag :type="METHOD_TONE[row.method] || 'info'" effect="light" round>{{ row.method }}</el-tag>
                <span class="tnum cell-muted" style="font-size: 12.5px; overflow: hidden; text-overflow: ellipsis; max-width: 220px; white-space: nowrap">
                  {{ row.url }}
                </span>
              </div>
            </template>
            <template v-else-if="col.key === 'operName'">
              <span>{{ row.operName }}</span>
            </template>
            <template v-else-if="col.key === 'ip'">
              <div>
                <div class="tnum" style="font-size: 13px">{{ row.ip }}</div>
                <div class="cell-muted" style="font-size: 12px">{{ row.location }}</div>
              </div>
            </template>
            <template v-else-if="col.key === 'cost'">
              <span class="tnum cell-muted">{{ row.cost }} ms</span>
            </template>
            <template v-else-if="col.key === 'result'">
              <el-tag v-if="row.result" type="success" effect="light" round>
                <span class="el-tag-dot-inline" />
                成功
              </el-tag>
              <el-tag v-else type="danger" effect="light" round>
                <span class="el-tag-dot-inline" />
                失败
              </el-tag>
            </template>
            <template v-else-if="col.key === 'operTime'">
              <span class="tnum cell-muted" style="font-size: 12.5px">{{ row.operTime }}</span>
            </template>
            <template v-else-if="col.key === 'actions'">
              <div class="row-actions">
                <el-tooltip content="详情" placement="top">
                  <el-button aria-label="详情" title="详情" text circle @click.stop="detail = row">
                    <Icon name="eye" :size="16" />
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

    <el-drawer :model-value="true"
      v-if="detail"
      title="操作日志详情"
      size="500px"
      destroy-on-close
      append-to-body
      @close="detail = null"
    >
      <p class="panel-sub">日志 ID #{{ detail.id }}</p>
      <div class="kv-grid" style="margin-bottom: 18px">
        <div class="kv-cell">
          <div class="kv-label">操作模块</div>
          <div class="kv-value">{{ detail.module }}</div>
        </div>
        <div class="kv-cell">
          <div class="kv-label">操作类型</div>
          <div class="kv-value">
            <el-tag
              :type="detail.tone === 'ok' ? 'success' : detail.tone === 'warn' ? 'warning' : detail.tone === 'danger' ? 'danger' : detail.tone === 'info' ? 'primary' : 'info'"
              effect="light"
              round
            >
              {{ detail.action }}
            </el-tag>
          </div>
        </div>
        <div class="kv-cell">
          <div class="kv-label">操作人员</div>
          <div class="kv-value">{{ detail.operName }}</div>
        </div>
        <div class="kv-cell">
          <div class="kv-label">所属部门</div>
          <div class="kv-value">{{ detail.dept }}</div>
        </div>
        <div class="kv-cell">
          <div class="kv-label">请求方式</div>
          <div class="kv-value"><span class="code-chip">{{ detail.method }}</span></div>
        </div>
        <div class="kv-cell">
          <div class="kv-label">执行结果</div>
          <div class="kv-value">
            <el-tag v-if="detail.result" type="success" effect="light" round><span class="el-tag-dot-inline" />成功</el-tag>
            <el-tag v-else type="danger" effect="light" round><span class="el-tag-dot-inline" />失败</el-tag>
          </div>
        </div>
        <div class="kv-cell">
          <div class="kv-label">主机地址</div>
          <div class="kv-value tnum">{{ detail.ip }}</div>
        </div>
        <div class="kv-cell">
          <div class="kv-label">操作地点</div>
          <div class="kv-value">{{ detail.location }}</div>
        </div>
        <div class="kv-cell">
          <div class="kv-label">耗时</div>
          <div class="kv-value tnum">{{ detail.cost }} ms</div>
        </div>
        <div class="kv-cell">
          <div class="kv-label">操作时间</div>
          <div class="kv-value tnum" style="font-size: 13px">{{ detail.operTime }}</div>
        </div>
      </div>

      <div class="log-field">
        <div class="log-field-label">请求地址</div>
        <pre class="log-pre">{{ detail.method }} {{ detail.url }}</pre>
      </div>
      <div class="log-field">
        <div class="log-field-label">请求参数</div>
        <pre class="log-pre">{{ detail.params }}</pre>
      </div>
      <div v-if="!detail.result" class="log-field">
        <div class="log-field-label" style="color: var(--danger)">异常信息</div>
        <pre class="log-pre danger">{{ detail.errorMsg }}</pre>
      </div>
      <template #footer>
        <el-button plain @click="detail = null">关闭</el-button>
      </template>
    </el-drawer>

    <el-dialog :model-value="true"
      v-if="clearOpen"
      title="清空操作日志"
      width="420px"
      destroy-on-close
      append-to-body
      @close="clearOpen = false"
    >
      <div class="confirm-body">
        <div>确认清空全部操作日志吗？此操作不可恢复。</div>
      </div>
      <template #footer>
        <el-button @click="clearOpen = false">取消</el-button>
        <el-button type="danger" @click="clearLogs">清空</el-button>
      </template>
    </el-dialog>
  </div>
</template>
