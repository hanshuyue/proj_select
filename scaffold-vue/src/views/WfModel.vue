<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import Icon from '@/components/Icon.vue'
import type { Column } from '@/types/table'
import type { WfModel } from '@/data'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'
import { portalApi } from '@/api'
import { downloadCsv } from '@/utils/csv'
import { createDefaultBpmnXml } from '@/utils/bpmn'

const list = ref<WfModel[]>([])
const kw = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const submitting = ref(false)
type ModalState = { type: 'add' } | { type: 'edit'; row: WfModel } | { type: 'deploy'; row: WfModel } | { type: 'delete'; row: WfModel } | null
const modal = ref<ModalState>(null)
const form = reactive<Partial<WfModel>>({})
const err = reactive<Record<string, string>>({})

onMounted(loadModels)

function openAdd() {
  Object.assign(form, { modelKey: '', modelName: '', category: '', desc: '', bpmnXml: '' })
  clearErr()
  modal.value = { type: 'add' }
}

function openEdit(row: WfModel) {
  Object.assign(form, { ...row })
  clearErr()
  modal.value = { type: 'edit', row }
}

function clearErr() {
  Object.keys(err).forEach(key => delete err[key])
}

async function loadModels(targetPage = page.value) {
  page.value = targetPage
  loading.value = true
  try {
    const data = await portalApi.workflow.models.list({ pageNum: page.value, pageSize: pageSize.value, keyword: kw.value.trim() || undefined })
    list.value = data.rows as unknown as WfModel[]
    total.value = data.total
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '流程模型加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

async function deploy() {
  if (modal.value?.type !== 'deploy' || submitting.value) return
  submitting.value = true
  try {
    const id = modal.value.row.id
    await portalApi.workflow.models.deploy(id)
    pushToast('流程已部署', 'ok'); modal.value = null
    await loadModels()
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '部署失败', 'danger')
  } finally {
    submitting.value = false
  }
}

async function saveModel() {
  if (submitting.value) return
  clearErr()
  if (!form.modelName?.trim()) err.modelName = '请输入流程名称'
  if (modal.value?.type === 'add' && !form.modelKey?.trim()) err.modelKey = '请输入流程标识'
  if (Object.keys(err).length) return
  submitting.value = true
  try {
    const mode = modal.value?.type
    const payload = {
      modelKey: form.modelKey,
      modelName: form.modelName,
      category: form.category,
      desc: form.desc,
      bpmnXml: form.bpmnXml,
    }
    if (modal.value?.type === 'edit') {
      await portalApi.workflow.models.update(modal.value.row.id, payload)
      pushToast('流程模型已保存', 'ok')
    } else {
      await portalApi.workflow.models.create(payload)
      pushToast('流程模型已创建', 'ok')
    }
    modal.value = null
    await loadModels(mode === 'edit' ? page.value : 1)
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '流程模型保存失败', 'danger')
  } finally {
    submitting.value = false
  }
}

function fillBpmnTemplate() {
  const key = form.modelKey?.trim() || 'purchase'
  const name = form.modelName?.trim() || '审批流程'
  form.bpmnXml = createDefaultBpmnXml(key, name)
}

async function deleteModel() {
  if (modal.value?.type !== 'delete' || submitting.value) return
  submitting.value = true
  try {
    await portalApi.workflow.models.remove(modal.value.row.id)
    modal.value = null
    await loadModels()
    pushToast('流程模型已删除', 'ok')
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '删除失败', 'danger')
  } finally {
    submitting.value = false
  }
}

function exportModels() {
  downloadCsv('workflow-models.csv', [
    { title: '流程名称', key: 'modelName' },
    { title: '流程标识', key: 'modelKey' },
    { title: '分类', key: 'category' },
    { title: '版本', key: 'version' },
    { title: '状态', value: row => row.deployed ? '已部署' : '草稿' },
    { title: '创建时间', key: 'createTime' },
    { title: '最后更新', key: 'updateTime' },
    { title: '说明', key: 'desc' },
  ], list.value)
  pushToast(`已导出 ${list.value.length} 条流程模型`, 'ok')
}

function resetSearch() {
  kw.value = ''
  loadModels(1)
}

const sort = ref<{ key: string; dir: 'asc' | 'desc' } | null>(null)
function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

const columns: Column[] = [
  { key: 'modelName', title: '流程模型' },
  { key: 'category', title: '分类' },
  { key: 'version', title: '版本', width: 80 },
  { key: 'deployed', title: '状态', width: 110 },
  { key: 'updateTime', title: '最后更新', width: 160 },
  { key: 'actions', title: '操作', width: 200, className: 'col-actions' },
]
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb v-if="['工作流程', '流程模型'].filter(item => item !== '流程模型').length" separator="/" class="page-crumb">
        <el-breadcrumb-item v-for="(item, index) in ['工作流程', '流程模型'].filter(item => item !== '流程模型')" :key="index">{{ item }}</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">流程模型</h1>
          <div class="page-desc">维护 BPMN XML 流程模型，保存后可部署为可发起的 Flowable 流程定义。</div>
        </div>
        <div class="page-actions"><el-button type="primary" @click="openAdd"><Icon name="plus" :size="16" />新建模型</el-button></div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="panel-head">
        <div style="width: 280px"><el-input v-model="kw" clearable placeholder="搜索流程名称 / 标识" @keyup.enter="loadModels(1)" /></div>
        <el-button plain @click="loadModels(1)"><Icon name="search" :size="16" />查询</el-button>
        <el-button plain @click="resetSearch"><Icon name="refresh" :size="16" />重置</el-button>
        <el-tooltip content="导出" placement="top"><el-button aria-label="导出" title="导出" plain circle @click="exportModels"><Icon name="download" :size="17" /></el-button></el-tooltip>
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
      <template v-if="col.key === 'modelName'">
        <div style="display: flex; align-items: center; gap: 11px">
            <span class="menu-ico-box" style="width: 34px; height: 34px; background: var(--primary-soft); color: var(--primary)"><Icon name="flow" :size="17" /></span>
            <div>
              <div class="cell-strong">{{ row.modelName }}</div>
              <div style="margin-top: 2px"><span class="code-chip">{{ row.modelKey }}</span></div>
            </div>
          </div>
      </template>
<template v-else-if="col.key === 'category'">
        <el-tag type="info" effect="light" round>{{ row.category }}</el-tag>
      </template>
<template v-else-if="col.key === 'version'">
        <span class="code-chip">v{{ row.version }}</span>
      </template>
<template v-else-if="col.key === 'deployed'">
        <el-tag v-if="row.deployed" type="success" effect="light" round><span class="el-tag-dot-inline" />已部署</el-tag>
          <el-tag v-else type="warning" effect="light" round><span class="el-tag-dot-inline" />草稿</el-tag>
      </template>
<template v-else-if="col.key === 'updateTime'">
        <span class="tnum cell-muted" style="font-size: 12.5px">{{ row.updateTime }}</span>
      </template>
<template v-else-if="col.key === 'actions'">
        <div class="row-actions">
            <el-button text size="small" @click="openEdit(row)"><Icon name="edit" :size="15" />配置</el-button>
            <el-button text size="small" @click="modal = { type: 'deploy', row }"><Icon name="play" :size="15" />部署</el-button>
            <el-tooltip content="删除" placement="top"><el-button aria-label="删除" title="删除" text circle @click="modal = { type: 'delete', row }"><Icon name="trash" :size="16" /></el-button></el-tooltip>
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
          @update:current-page="loadModels"
          @update:page-size="loadModels(1)"
        />
      </div>
    </el-card>

    <el-dialog :model-value="true" v-if="modal && (modal.type === 'add' || modal.type === 'edit')"
      :title="modal.type === 'edit' ? '配置流程模型' : '新建流程模型'" width="720px" destroy-on-close append-to-body @close="modal = null">
      <div class="form-grid one">
        <el-form-item label="流程名称" required :error="err.modelName">
          <input :class="['input', err.modelName ? 'invalid' : '']" v-model="form.modelName" placeholder="如：采购申请流程" />
        </el-form-item>
        <el-form-item label="流程标识" required :error="err.modelKey">
          <input :class="['input', err.modelKey ? 'invalid' : '']" v-model="form.modelKey" :disabled="modal.type === 'edit'" placeholder="如：purchase" />
        </el-form-item>
        <el-form-item label="流程分类">
          <input class="input" v-model="form.category" placeholder="如：财务审批" />
        </el-form-item>
        <el-form-item label="模型说明">
          <textarea class="input" style="min-height: 88px" v-model="form.desc" placeholder="填写流程适用场景和审批规则" />
        </el-form-item>
        <el-form-item label="BPMN XML">
          <div class="bpmn-toolbar">
            <el-button plain size="small" @click="fillBpmnTemplate"><Icon name="file" :size="15" />生成模板</el-button>
          </div>
          <textarea class="input bpmn-editor" v-model="form.bpmnXml" spellcheck="false" placeholder="保存为空时后端将使用默认审批模板" />
        </el-form-item>
      </div>
      <template #footer>
        <el-button plain @click="modal = null">取消</el-button>
        <el-button type="primary" :disabled="submitting" @click="saveModel"><Icon name="check" :size="16" />保存</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="true" v-if="modal && modal.type === 'deploy'"
      title="部署流程模型" width="420px" destroy-on-close append-to-body @close="modal = null">
      <div class="confirm-body">
        <div>确认将模型 <b>{{ modal.row.modelName }}</b> 部署为流程定义 v{{ modal.row.version + (modal.row.deployed ? 1 : 0) }} 吗？</div>
      </div>
      <template #footer>
        <el-button @click="modal = null">取消</el-button>
        <el-button type="primary" @click="deploy">确认部署</el-button>
      </template>
    </el-dialog>
    <el-dialog :model-value="true" v-if="modal && modal.type === 'delete'"
      title="删除流程模型" width="420px" destroy-on-close append-to-body @close="modal = null">
      <div class="confirm-body">
        <div>确认删除流程模型 <b>{{ modal.row.modelName }}</b> 吗？</div>
      </div>
      <template #footer>
        <el-button @click="modal = null">取消</el-button>
        <el-button type="danger" @click="deleteModel">删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.bpmn-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.bpmn-editor {
  min-height: 260px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre;
}
</style>
