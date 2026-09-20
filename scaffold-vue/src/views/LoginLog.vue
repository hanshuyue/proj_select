<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import Icon from '@/components/Icon.vue'
import type { LoginLog } from '@/data'
import { ApiError, systemApi } from '@/api'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'
import type { Column, SortState } from '@/types/table'
import { downloadBlob } from '@/utils/download'
import { avatarColor, avatarText } from '@/utils/avatar'
import { includesKeyword } from '@/utils/search'

const list = ref<LoginLog[]>([])
const kw = ref('')
const status = ref('')
const appliedFilters = reactive({ kw: '', status: '' })

const page = ref(1)
const pageSize = ref(10)
const selected = ref<(string | number)[]>([])
const sort = ref<SortState | null>({ key: 'loginTime', dir: 'desc' })

const clearOpen = ref(false)
const loading = ref(false)
const exporting = ref(false)

const filtered = computed(() => {
  let rows = list.value.filter(item =>
    includesKeyword(appliedFilters.kw, item.username, item.ip) &&
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
  { key: 'username', title: '登录账号' },
  { key: 'ip', title: '登录 IP' },
  { key: 'location', title: '登录地点', width: 110 },
  { key: 'browser', title: '浏览器 / 系统' },
  { key: 'status', title: '状态', width: 90 },
  { key: 'msg', title: '结果说明', width: 110 },
  { key: 'loginTime', title: '登录时间', width: 160, sortable: true },
]

function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

async function reload() {
  loading.value = true
  try {
    const data = await systemApi.loginLogs.list({
      pageNum: 1,
      pageSize: 200,
      keyword: appliedFilters.kw || undefined,
      status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
    })
    list.value = data.rows as unknown as LoginLog[]
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '登录日志加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

async function batchDelete() {
  const ids = [...selected.value]
  try {
    await Promise.all(ids.map(id => systemApi.loginLogs.remove(id)))
    selected.value = []
    await reload()
    pushToast('已删除所选登录日志', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '登录日志删除失败', 'danger')
  }
}

async function clearLogs() {
  try {
    await systemApi.loginLogs.clear()
    clearOpen.value = false
    selected.value = []
    await reload()
    pushToast('登录日志已清空', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '登录日志清空失败', 'danger')
  }
}

async function exportLogs() {
  exporting.value = true
  try {
    const blob = await systemApi.loginLogs.export({
      keyword: appliedFilters.kw || undefined,
      status: appliedFilters.status === '' ? undefined : Number(appliedFilters.status),
    })
    downloadBlob(blob, 'login-logs.csv')
    pushToast('登录日志已导出', 'ok')
  } catch (error) {
    pushToast(error instanceof ApiError ? error.message : '登录日志导出失败', 'danger')
  } finally {
    exporting.value = false
  }
}

function applyFilters() {
  appliedFilters.kw = kw.value.trim()
  appliedFilters.status = status.value
  page.value = 1
  reload()
}

function resetFilters() {
  kw.value = ''
  status.value = ''
  appliedFilters.kw = ''
  appliedFilters.status = ''
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
          <h1 class="page-title">登录日志</h1>
          <div class="page-desc">记录用户登录结果、来源地址、浏览器和系统信息，便于安全排查与账号审计。</div>
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
            style="width: 240px"
            placeholder="登录账号 / IP"
            @keyup.enter="applyFilters"
          />
        </el-form-item>
        <el-form-item label="登录状态" class="filter-item">
          <el-select v-model="status" filterable style="width: 140px">
            <el-option label="全部状态" value="" />
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
          <span class="panel-title">登录记录</span>
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
            <template v-if="col.key === 'username'">
              <div style="display: flex; align-items: center; gap: 10px">
                <el-avatar :size="32" :style="{ background: avatarColor(row.username), fontSize: `${32 * 0.4}px` }">
                  {{ avatarText(row.username) }}
                </el-avatar>
                <span class="code-chip">{{ row.username }}</span>
              </div>
            </template>
            <template v-else-if="col.key === 'ip'">
              <span class="tnum" style="font-size: 13px">{{ row.ip }}</span>
            </template>
            <template v-else-if="col.key === 'location'">
              <span>{{ row.location }}</span>
            </template>
            <template v-else-if="col.key === 'browser'">
              <div>
                <div style="font-size: 13px">{{ row.browser }}</div>
                <div class="cell-muted" style="font-size: 12px">{{ row.os }}</div>
              </div>
            </template>
            <template v-else-if="col.key === 'status'">
              <el-tag v-if="row.status" type="success" effect="light" round>
                <span class="el-tag-dot-inline" />
                成功
              </el-tag>
              <el-tag v-else type="danger" effect="light" round>
                <span class="el-tag-dot-inline" />
                失败
              </el-tag>
            </template>
            <template v-else-if="col.key === 'msg'">
              <span class="cell-muted" style="font-size: 13px">{{ row.msg }}</span>
            </template>
            <template v-else-if="col.key === 'loginTime'">
              <span class="tnum cell-muted" style="font-size: 12.5px">{{ row.loginTime }}</span>
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
      v-if="clearOpen"
      title="清空登录日志"
      width="420px"
      destroy-on-close
      append-to-body
      @close="clearOpen = false"
    >
      <div class="confirm-body">
        <div>确认清空全部登录日志吗？此操作不可恢复。</div>
      </div>
      <template #footer>
        <el-button @click="clearOpen = false">取消</el-button>
        <el-button type="danger" @click="clearLogs">清空</el-button>
      </template>
    </el-dialog>
  </div>
</template>
