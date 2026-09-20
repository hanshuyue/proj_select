<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import Icon from '@/components/Icon.vue'

import FlowSteps from '@/components/flow/FlowSteps.vue'
import ApproveDrawer from '@/components/flow/ApproveDrawer.vue'
import type { WfTodo } from '@/data'
import { pushToast } from '@/composables/toast'
import { portalApi } from '@/api'
import { downloadCsv } from '@/utils/csv'
import { avatarText, avatarColor } from '@/utils/avatar'

const PRIORITY: Record<string, { label: string; tone: string }> = {
  high: { label: '紧急', tone: 'danger' }, mid: { label: '普通', tone: 'info' }, low: { label: '较低', tone: 'neutral' },
}

const list = ref<WfTodo[]>([])
const kw = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const approving = ref(false)
const drawer = ref<WfTodo | null>(null)

const totalText = computed(() => loading.value ? '加载中' : `${total.value} 项待处理`)

onMounted(loadTodos)

async function loadTodos(targetPage = page.value) {
  page.value = targetPage
  loading.value = true
  try {
    const data = await portalApi.workflow.tasks.todo({ pageNum: page.value, pageSize: pageSize.value, keyword: kw.value.trim() || undefined })
    list.value = data.rows as unknown as WfTodo[]
    total.value = data.total
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '待办任务加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

async function submit(task: WfTodo, resultLabel: string, result: string, comment: string) {
  if (approving.value) return
  approving.value = true
  try {
    await portalApi.workflow.tasks.approve(task.id, { result, comment })
    await loadTodos()
    drawer.value = null
    pushToast(`「${task.procName}」${resultLabel}`, 'ok')
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '审批失败', 'danger')
  } finally {
    approving.value = false
  }
}

function resetSearch() {
  kw.value = ''
  loadTodos(1)
}

function exportTodos() {
  downloadCsv('workflow-todo.csv', [
    { title: '业务单据', key: 'bizTitle' },
    { title: '业务编号', key: 'businessKey' },
    { title: '流程名称', key: 'procName' },
    { title: '任务节点', key: 'taskName' },
    { title: '业务类型', key: 'bizType' },
    { title: '发起人', key: 'starter' },
    { title: '创建时间', key: 'createTime' },
    { title: '截止时间', key: 'due' },
  ], list.value)
  pushToast(`已导出 ${list.value.length} 条待办`, 'ok')
}
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb v-if="['工作流程', '我的待办'].filter(item => item !== '我的待办').length" separator="/" class="page-crumb">
        <el-breadcrumb-item v-for="(item, index) in ['工作流程', '我的待办'].filter(item => item !== '我的待办')" :key="index">{{ item }}</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">我的待办</h1>
          <div class="page-desc">展示需要您处理的审批任务，点击任务进入审批详情，可通过、驳回或退回；发起人可撤回。</div>
        </div>
        <div class="page-actions"><el-button plain @click="exportTodos"><Icon name="download" :size="14" />导出</el-button>
        <span class="tag tag-warn" style="height: 28px; font-size: 13px">{{ totalText }}</span></div>
      </div>
    </div>

    <el-card  class="filter-card" shadow="never">
      <div class="todo-filter-row">
        <div style="width: 320px"><el-input v-model="kw" clearable placeholder="搜索流程 / 发起人 / 单据" @keyup.enter="loadTodos(1)" /></div>
        <el-button plain @click="loadTodos(1)"><Icon name="search" :size="14" />查询</el-button>
        <el-button plain @click="resetSearch"><Icon name="refresh" :size="14" />重置</el-button>
      </div>
    </el-card>

    <el-card v-if="loading" shadow="never">
      <div class="tbl-empty"><div class="em-ico"><Icon name="search" :size="24" /></div>待办任务加载中</div>
    </el-card>
    <el-card v-else-if="list.length === 0" shadow="never">
      <div class="tbl-empty"><div class="em-ico"><Icon name="check" :size="24" /></div>太棒了，没有待处理的任务</div>
    </el-card>
    <div v-else class="todo-grid">
      <div v-for="t in list" :key="t.id" class="todo-card" @click="drawer = t">
        <div class="todo-card-top">
          <el-tag type="info" effect="light" round>{{ t.bizType }}</el-tag>
          <el-tag :type="(PRIORITY[t.priority]?.tone as any) === 'ok' ? 'success' : (PRIORITY[t.priority]?.tone as any) === 'warn' ? 'warning' : (PRIORITY[t.priority]?.tone as any) === 'danger' ? 'danger' : (PRIORITY[t.priority]?.tone as any) === 'info' || (PRIORITY[t.priority]?.tone as any) === 'purple' ? 'primary' : 'info'" effect="light" round>{{ PRIORITY[t.priority]?.label ?? '普通' }}</el-tag>
        </div>
        <div class="todo-card-title">{{ t.bizTitle }}</div>
        <div class="todo-card-task"><Icon name="flow" :size="14" />{{ t.procName }} · {{ t.taskName }}</div>
        <div class="todo-progress"><FlowSteps :current="t.node" :total="t.total" :nodes="t.nodes" /></div>
        <div class="todo-card-foot">
          <span class="todo-starter"><el-avatar :size="24" :style="{ background: avatarColor(t.starter), fontSize: Number(24) * 0.4 + 'px' }">{{ avatarText(t.starter) }}</el-avatar>{{ t.starter }}</span>
          <span class="todo-due"><Icon name="clock" :size="13" />截止 {{ (t.due || '未设置').slice(5) || '未设置' }}</span>
        </div>
        <div class="todo-card-actions">
          <el-button type="primary" size="small" @click.stop="drawer = t"><Icon name="check" :size="14" />审批处理</el-button>
        </div>
      </div>
    </div>
    <div class="pager">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @update:current-page="loadTodos"
        @update:page-size="loadTodos(1)"
      />
    </div>

    <ApproveDrawer v-if="drawer" :task="drawer" :submitting="approving" @close="drawer = null" @submit="submit" />
  </div>
</template>

<style scoped>
.filter-card { margin-bottom: 18px; padding: 16px 22px; }
.todo-filter-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
</style>
