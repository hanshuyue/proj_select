<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Icon from '@/components/Icon.vue'
import type { Column } from '@/types/table'
import type { WfDone } from '@/data'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'
import { portalApi } from '@/api'
import { downloadCsv } from '@/utils/csv'
import { avatarText, avatarColor } from '@/utils/avatar'

const kw = ref('')
const page = ref(1)
const pageSize = ref(10)
const list = ref<WfDone[]>([])
const total = ref(0)
const loading = ref(false)

onMounted(loadDone)

async function loadDone(targetPage = page.value) {
  page.value = targetPage
  loading.value = true
  try {
    const data = await portalApi.workflow.tasks.done({ pageNum: page.value, pageSize: pageSize.value, keyword: kw.value.trim() || undefined })
    list.value = data.rows as unknown as WfDone[]
    total.value = data.total
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '已办任务加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

function exportDone() {
  downloadCsv('workflow-done.csv', [
    { title: '业务单据', key: 'bizTitle' },
    { title: '流程名称', key: 'procName' },
    { title: '任务节点', key: 'taskName' },
    { title: '发起人', key: 'starter' },
    { title: '审批结果', key: 'result' },
    { title: '审批意见', key: 'comment' },
    { title: '处理时间', key: 'approveTime' },
  ], list.value)
  pushToast(`已导出 ${list.value.length} 条已办`, 'ok')
}

function resetSearch() {
  kw.value = ''
  loadDone(1)
}

const sort = ref<{ key: string; dir: 'asc' | 'desc' } | null>(null)
function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

const columns: Column[] = [
  { key: 'bizTitle', title: '业务单据' },
  { key: 'starter', title: '发起人', width: 120 },
  { key: 'result', title: '审批结果', width: 100 },
  { key: 'comment', title: '审批意见' },
  { key: 'approveTime', title: '处理时间', width: 160 },
]
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb v-if="['工作流程', '我的已办'].filter(item => item !== '我的已办').length" separator="/" class="page-crumb">
        <el-breadcrumb-item v-for="(item, index) in ['工作流程', '我的已办'].filter(item => item !== '我的已办')" :key="index">{{ item }}</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">我的已办</h1>
          <div class="page-desc">展示您已处理过的审批任务，含审批结果与审批意见。</div>
        </div>
        <div class="page-actions"><el-button plain @click="exportDone"><Icon name="download" :size="16" />导出</el-button></div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="panel-head">
        <div style="width: 320px"><el-input v-model="kw" clearable placeholder="搜索流程 / 发起人 / 单据" @keyup.enter="loadDone(1)" /></div>
        <el-button plain @click="loadDone(1)"><Icon name="search" :size="16" />查询</el-button>
        <el-button plain @click="resetSearch"><Icon name="refresh" :size="16" />重置</el-button>
        <span class="cell-muted" style="font-size: 13px">共 {{ total }} 条已办</span>
      </div>
      <el-table
        v-loading="loading"
        :data="list"
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
      <template v-if="col.key === 'bizTitle'">
        <div>
            <div class="cell-strong">{{ row.bizTitle }}</div>
            <div class="cell-muted" style="font-size: 12.5px; margin-top: 2px; display: flex; align-items: center; gap: 6px">
              <Icon name="flow" :size="13" />{{ row.procName }} · {{ row.taskName }}
            </div>
          </div>
      </template>
<template v-else-if="col.key === 'starter'">
        <span style="display: flex; align-items: center; gap: 8px"><el-avatar :size="28" :style="{ background: avatarColor(row.starter), fontSize: Number(28) * 0.4 + 'px' }">{{ avatarText(row.starter) }}</el-avatar>{{ row.starter }}</span>
      </template>
<template v-else-if="col.key === 'result'">
        <el-tag :type="row.tone === 'ok' ? 'success' : row.tone === 'warn' ? 'warning' : row.tone === 'danger' ? 'danger' : row.tone === 'info' || row.tone === 'purple' ? 'primary' : 'info'" effect="light" round><span class="el-tag-dot-inline" />{{ row.result }}</el-tag>
      </template>
<template v-else-if="col.key === 'comment'">
        <span class="cell-muted" style="font-size: 13px">{{ row.comment }}</span>
      </template>
<template v-else-if="col.key === 'approveTime'">
        <span class="tnum cell-muted" style="font-size: 12.5px">{{ row.approveTime }}</span>
      </template>
      <template v-else>{{ row[col.key] }}</template>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @update:current-page="loadDone"
          @update:page-size="loadDone(1)"
        />
      </div>
    </el-card>
  </div>
</template>
