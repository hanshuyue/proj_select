<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import Icon from '@/components/Icon.vue'
import type { Column } from '@/types/table'
import FlowSteps from '@/components/flow/FlowSteps.vue'
import type { WfDef } from '@/data'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'
import { portalApi } from '@/api'
import { downloadCsv } from '@/utils/csv'

const list = ref<WfDef[]>([])
const kw = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
const drawer = ref<WfDef | null>(null)
const startDef = ref<WfDef | null>(null)
const startForm = reactive({ businessKey: '', businessType: 'purchase', businessTitle: '' })
const startErr = reactive<Record<string, string>>({})

onMounted(loadDefs)

async function loadDefs(targetPage = page.value) {
  page.value = targetPage
  loading.value = true
  try {
    const data = await portalApi.workflow.definitions.list({ pageNum: page.value, pageSize: pageSize.value, keyword: kw.value.trim() || undefined })
    list.value = data.rows.map(row => ({ ...row, nodes: Array.isArray(row.nodes) ? row.nodes : [] })) as unknown as WfDef[]
    total.value = data.total
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '流程定义加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

async function toggleSuspend(d: WfDef) {
  if (submitting.value) return
  submitting.value = true
  try {
    const suspended = !d.suspended
    await portalApi.workflow.definitions.suspend(d.id, suspended)
    list.value = list.value.map(x => x.id === d.id ? { ...x, suspended } : x)
    pushToast(suspended ? '流程已挂起' : '流程已激活', 'ok')
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '操作失败', 'danger')
  } finally {
    submitting.value = false
  }
}

function openStart(row: WfDef) {
  startDef.value = row
  Object.assign(startForm, {
    businessKey: `WF-${Date.now()}`,
    businessType: 'purchase',
    businessTitle: `${row.processName}-${new Date().toLocaleString('zh-CN', { hour12: false })}`,
  })
  Object.keys(startErr).forEach(key => delete startErr[key])
}

async function startInstance() {
  if (!startDef.value || submitting.value) return
  Object.keys(startErr).forEach(key => delete startErr[key])
  if (!startForm.businessType.trim()) startErr.businessType = '请输入业务类型'
  if (!startDef.value.processDefinitionId) startErr.processDefinitionId = '流程定义ID缺失'
  if (Object.keys(startErr).length) return
  submitting.value = true
  try {
    await portalApi.workflow.instances.start({
      processDefinitionId: startDef.value.processDefinitionId,
      businessKey: startForm.businessKey.trim() || undefined,
      businessType: startForm.businessType.trim(),
      businessTitle: startForm.businessTitle.trim() || undefined,
    })
    pushToast('流程已发起', 'ok')
    startDef.value = null
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '流程发起失败', 'danger')
  } finally {
    submitting.value = false
  }
}

function exportDefinitions() {
  downloadCsv('workflow-definitions.csv', [
    { title: '流程名称', key: 'processName' },
    { title: '流程标识', key: 'processKey' },
    { title: '分类', key: 'category' },
    { title: '版本', key: 'version' },
    { title: '节点数', value: row => Array.isArray(row.nodes) ? row.nodes.length : row.nodes },
    { title: '状态', value: row => row.suspended ? '已挂起' : '激活' },
    { title: '部署时间', key: 'deployTime' },
  ], list.value)
  pushToast(`已导出 ${list.value.length} 条流程定义`, 'ok')
}

function resetSearch() {
  kw.value = ''
  loadDefs(1)
}

const sort = ref<{ key: string; dir: 'asc' | 'desc' } | null>(null)
function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

const columns: Column[] = [
  { key: 'processName', title: '流程定义' },
  { key: 'category', title: '分类' },
  { key: 'version', title: '版本', width: 80 },
  { key: 'nodes', title: '节点数', width: 80 },
  { key: 'suspended', title: '状态', width: 110 },
  { key: 'deployTime', title: '部署时间', width: 160 },
  { key: 'actions', title: '操作', width: 260, className: 'col-actions' },
]
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb v-if="['工作流程', '流程定义'].filter(item => item !== '流程定义').length" separator="/" class="page-crumb">
        <el-breadcrumb-item v-for="(item, index) in ['工作流程', '流程定义'].filter(item => item !== '流程定义')" :key="index">{{ item }}</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">流程定义</h1>
          <div class="page-desc">管理已部署的流程定义，支持挂起 / 激活与流程图查看。挂起后不能发起新流程。</div>
        </div>
        <div class="page-actions"><el-button plain @click="exportDefinitions"><Icon name="download" :size="16" />导出</el-button></div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="panel-head">
        <div style="width: 280px"><el-input v-model="kw" clearable placeholder="搜索流程名称 / 标识" @keyup.enter="loadDefs(1)" /></div>
        <el-button plain @click="loadDefs(1)"><Icon name="search" :size="16" />查询</el-button>
        <el-button plain @click="resetSearch"><Icon name="refresh" :size="16" />重置</el-button>
        <span class="cell-muted" style="font-size: 13px">共 {{ total }} 条定义</span>
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
      <template v-if="col.key === 'processName'">
        <div style="display: flex; align-items: center; gap: 11px">
            <span class="menu-ico-box" style="width: 34px; height: 34px" :style="{ background: row.suspended ? 'var(--parchment)' : 'var(--primary-soft)', color: row.suspended ? 'var(--ink-muted-48)' : 'var(--primary)' }"><Icon name="layers" :size="17" /></span>
            <div>
              <div class="cell-strong">{{ row.processName }}</div>
              <div style="margin-top: 2px"><span class="code-chip">{{ row.processKey }}</span></div>
            </div>
          </div>
      </template>
<template v-else-if="col.key === 'category'">
        <el-tag type="info" effect="light" round>{{ row.category }}</el-tag>
      </template>
<template v-else-if="col.key === 'version'">
        <span class="code-chip">v{{ row.version }}</span>
      </template>
<template v-else-if="col.key === 'nodes'">
        <span class="tnum cell-muted">{{ row.nodes.length }}</span>
      </template>
<template v-else-if="col.key === 'suspended'">
        <el-tag v-if="row.suspended" type="info" effect="light" round><span class="el-tag-dot-inline" />已挂起</el-tag>
          <el-tag v-else type="success" effect="light" round><span class="el-tag-dot-inline" />激活</el-tag>
      </template>
<template v-else-if="col.key === 'deployTime'">
        <span class="tnum cell-muted" style="font-size: 12.5px">{{ row.deployTime }}</span>
      </template>
<template v-else-if="col.key === 'actions'">
        <div class="row-actions">
            <el-button text size="small" :disabled="row.suspended || submitting" @click="openStart(row)"><Icon name="play" :size="15" />发起</el-button>
            <el-button text size="small" @click="drawer = row"><Icon name="eye" :size="15" />流程图</el-button>
            <el-button v-if="row.suspended" text size="small" @click="toggleSuspend(row)"><Icon name="play" :size="15" />激活</el-button>
            <el-button v-else text size="small" @click="toggleSuspend(row)"><Icon name="pause" :size="15" />挂起</el-button>
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
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @update:current-page="loadDefs"
          @update:page-size="loadDefs(1)"
        />
      </div>
    </el-card>

    <el-drawer :model-value="true" v-if="drawer" title="`流程图 · ${drawer.processName}`" size="560px" destroy-on-close append-to-body @close="drawer = null">
      <p class="panel-sub">{{ drawer.processKey }} · v{{ drawer.version }}</p>
      <div class="flow-diagram">
        <FlowSteps :current="drawer.nodes.length - 1" :total="drawer.nodes.length - 1" :nodes="drawer.nodes" />
      </div>
      <div style="margin-top: 20px; font-size: 13px; color: var(--ink-muted-48); line-height: 1.6">
        该流程共 {{ drawer.nodes.length }} 个节点，部署于 {{ drawer.deployTime }}。此处按 BPMN 节点名称展示流转示意。
      </div>
      <template #footer>
        <el-button plain @click="drawer = null">关闭</el-button>
      </template>
    </el-drawer>

    <el-dialog :model-value="true" v-if="startDef" title="`发起流程 · ${startDef.processName}`" width="520px" destroy-on-close append-to-body @close="startDef = null">
      <div class="form-grid one">
        <el-form-item label="业务编号">
          <input class="input" v-model="startForm.businessKey" />
        </el-form-item>
        <el-form-item label="业务类型" required :error="startErr.businessType">
          <input :class="['input', startErr.businessType ? 'invalid' : '']" v-model="startForm.businessType" />
        </el-form-item>
        <el-form-item label="业务标题">
          <input class="input" v-model="startForm.businessTitle" />
        </el-form-item>
      </div>
      <template #footer>
        <el-button plain @click="startDef = null">取消</el-button>
        <el-button type="primary" :disabled="submitting" @click="startInstance"><Icon name="send" :size="16" />发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>
