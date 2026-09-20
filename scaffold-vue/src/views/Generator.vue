<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import Icon from '@/components/Icon.vue'
import type { Column } from '@/types/table'
import type { GenTable, GenField } from '@/data'
import { appearance } from '@/composables/appearance'
import { pushToast } from '@/composables/toast'
import { portalApi } from '@/api'
import { downloadBlob } from '@/utils/download'
import { downloadCsv } from '@/utils/csv'
import { copyText } from '@/utils/clipboard'

const FORM_TYPES = ['文本框', '文本域', '下拉框', '数字框', '日期框', '单选框', '隐藏']

const list = ref<GenTable[]>([])
const kw = ref('')
const appliedKw = ref('')
const importOpen = ref(false)
const importKw = ref('')
const importLoading = ref(false)
const importCandidates = ref<Array<{ tableName: string; tableComment: string; imported: boolean }>>([])
const importSel = ref<string[]>([])
const config = ref<GenTable | null>(null)
const preview = ref<GenTable | null>(null)
const loading = ref(false)
const previewFiles = ref<Array<{ name: string; label?: string; type?: string }>>([])
const previewCode = ref('')
const previewActive = ref('')
const downloading = ref(false)

const filtered = computed(() => list.value.filter(t => !appliedKw.value || t.tableName.includes(appliedKw.value) || t.tableComment.includes(appliedKw.value)))

onMounted(loadTables)

async function loadTables() {
  loading.value = true
  try {
    const data = await portalApi.generator.tables.list({ pageNum: 1, pageSize: 200 })
    list.value = data.rows as unknown as GenTable[]
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '代码生成表加载失败', 'danger')
  } finally {
    loading.value = false
  }
}

async function openImport() {
  importOpen.value = true
  importSel.value = []
  await loadImportableTables()
}

async function loadImportableTables() {
  importLoading.value = true
  try {
    const data = await portalApi.generator.tables.importable({ keyword: importKw.value })
    importCandidates.value = data as unknown as Array<{ tableName: string; tableComment: string; imported: boolean }>
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '数据库表加载失败', 'danger')
  } finally {
    importLoading.value = false
  }
}

function toggleImport(tableName: string) {
  importSel.value = importSel.value.includes(tableName)
    ? importSel.value.filter(name => name !== tableName)
    : [...importSel.value, tableName]
}

async function doImport() {
  if (!importSel.value.length) return
  try {
    await portalApi.generator.tables.import({ tableNames: importSel.value })
    pushToast(`已导入 ${importSel.value.length} 张表`, 'ok')
    importOpen.value = false
    importSel.value = []
    await loadTables()
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '数据库表导入失败', 'danger')
  }
}

// —— 字段配置 ——
const fields = reactive<GenField[]>([])
async function openConfig(t: GenTable) {
  try {
    const data = await portalApi.generator.tables.columns(t.id)
    fields.splice(0, fields.length, ...(data as unknown as GenField[]).map(f => ({ ...f })))
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '字段配置加载失败', 'danger')
  }
  config.value = t
}

async function openPreview(t: GenTable) {
  try {
    const data = await portalApi.generator.tables.preview(t.id)
    previewFiles.value = (data.files as Array<{ name: string; type?: string }>) ?? []
    previewActive.value = String(data.activeFile ?? previewFiles.value[0]?.name ?? '')
    previewCode.value = String(data.code ?? '')
    preview.value = t
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '代码预览加载失败', 'danger')
  }
}

async function saveConfig() {
  if (!config.value) return
  try {
    await portalApi.generator.tables.update(config.value.id, {
      tableComment: config.value.tableComment,
      synced: config.value.synced,
    })
    pushToast('字段配置已保存', 'ok')
    config.value = null
    await loadTables()
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '配置保存失败', 'danger')
  }
}

async function generateCode(row: GenTable) {
  downloading.value = true
  try {
    await portalApi.generator.tables.generate(row.id)
    const blob = await portalApi.generator.tables.download(row.id)
    downloadBlob(blob, `${row.className}.zip`)
    pushToast(`已下载「${row.tableComment}」代码包`, 'ok')
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '代码包下载失败', 'danger')
  } finally {
    downloading.value = false
  }
}

function exportTables() {
  downloadCsv('generator-tables.csv', [
    { title: '表名称', key: 'tableName' },
    { title: '表说明', key: 'tableComment' },
    { title: '实体类', key: 'className' },
    { title: '模块', key: 'module' },
    { title: '同步状态', value: row => row.synced ? '已同步' : '待同步' },
    { title: '导入时间', key: 'createTime' },
  ], filtered.value)
  pushToast(`已导出 ${filtered.value.length} 条生成配置`, 'ok')
}

function applyFilters() {
  appliedKw.value = kw.value.trim()
}

function resetFilters() {
  kw.value = ''
  appliedKw.value = ''
  loadTables()
}

async function copyPreviewCode() {
  const copied = await copyText(previewCode.value)
  pushToast(copied ? '当前代码已复制' : '当前浏览器不支持剪贴板写入', copied ? 'ok' : 'danger')
}

const sort = ref<{ key: string; dir: 'asc' | 'desc' } | null>(null)
function onSortChange({ prop, order }: { prop: string; order: 'ascending' | 'descending' | null }) {
  sort.value = prop && order
    ? { key: prop, dir: order === 'ascending' ? 'asc' : 'desc' }
    : null
}

const columns: Column[] = [
  { key: 'tableName', title: '表名称' },
  { key: 'className', title: '实体类' },
  { key: 'module', title: '模块', width: 100 },
  { key: 'synced', title: '同步状态', width: 110 },
  { key: 'createTime', title: '导入时间', width: 160 },
  { key: 'actions', title: '操作', width: 230, className: 'col-actions' },
]
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb v-if="['系统工具', '代码生成'].filter(item => item !== '代码生成').length" separator="/" class="page-crumb">
        <el-breadcrumb-item v-for="(item, index) in ['系统工具', '代码生成'].filter(item => item !== '代码生成')" :key="index">{{ item }}</el-breadcrumb-item>
      </el-breadcrumb>
      <div class="page-main">
        <div>
          <h1 class="page-title">代码生成</h1>
          <div class="page-desc">导入数据库表，配置字段查询与表单规则，一键生成前后端 CRUD 代码与菜单权限。</div>
        </div>
        <div class="page-actions"><el-button plain @click="loadTables"><Icon name="refresh" :size="16" />同步</el-button>
        <el-button type="primary" @click="openImport"><Icon name="plus" :size="16" />导入表</el-button></div>
      </div>
    </div>

    <el-card shadow="never">
      <div class="panel-head">
        <div style="display: flex; align-items: center; gap: 12px">
          <div style="width: 280px"><el-input v-model="kw" clearable placeholder="搜索表名 / 注释" @keyup.enter="applyFilters" /></div>
          <el-button type="primary" @click="applyFilters"><Icon name="search" :size="16" />查询</el-button>
          <el-button plain @click="resetFilters"><Icon name="refresh" :size="16" />重置</el-button>
          <span class="cell-muted" style="font-size: 13px">共 {{ filtered.length }} 张表</span>
        </div>
        <el-tooltip content="导出列表" placement="top"><el-button aria-label="导出列表" title="导出列表" plain circle @click="exportTables"><Icon name="download" :size="17" /></el-button></el-tooltip>
      </div>
      <el-table
        v-loading="loading"
        :data="filtered"
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
      <template v-if="col.key === 'tableName'">
        <div style="display: flex; align-items: center; gap: 11px">
            <span class="menu-ico-box" style="width: 34px; height: 34px; background: var(--primary-soft); color: var(--primary)"><Icon name="database" :size="17" /></span>
            <div>
              <div style="margin-bottom: 2px"><span class="code-chip">{{ row.tableName }}</span></div>
              <div class="cell-muted" style="font-size: 12.5px">{{ row.tableComment }}</div>
            </div>
          </div>
      </template>
<template v-else-if="col.key === 'className'">
        <span class="tnum" style="font-size: 13px">{{ row.className }}</span>
      </template>
<template v-else-if="col.key === 'module'">
        <el-tag type="info" effect="light" round>{{ row.module }}</el-tag>
      </template>
<template v-else-if="col.key === 'synced'">
        <el-tag v-if="row.synced" type="success" effect="light" round><span class="el-tag-dot-inline" />已同步</el-tag>
          <el-tag v-else type="warning" effect="light" round><span class="el-tag-dot-inline" />待同步</el-tag>
      </template>
<template v-else-if="col.key === 'createTime'">
        <span class="tnum cell-muted" style="font-size: 12.5px">{{ row.createTime }}</span>
      </template>
<template v-else-if="col.key === 'actions'">
        <div class="row-actions">
            <el-button text size="small" @click="openPreview(row)"><Icon name="eye" :size="15" />预览</el-button>
            <el-button text size="small" @click="openConfig(row)"><Icon name="gear" :size="15" />配置</el-button>
            <el-button text size="small" :disabled="downloading" @click="generateCode(row)"><Icon name="download" :size="15" />生成</el-button>
          </div>
      </template>
      <template v-else>{{ row[col.key] }}</template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 导入表弹窗 -->
    <el-dialog :model-value="true" v-if="importOpen" title="导入数据库表" width="620px" destroy-on-close append-to-body @close="importOpen = false">
      <div style="margin-bottom: 12px; display: flex; gap: 10px">
        <el-input v-model="importKw" clearable placeholder="搜索数据库表名 / 注释" />
        <el-button plain @click="loadImportableTables"><Icon name="search" :size="16" />查询</el-button>
      </div>
      <div class="import-list">
        <div
          v-for="c in importCandidates" :key="c.tableName"
          :class="['import-row', importSel.includes(c.tableName) ? 'on' : '', c.imported ? 'disabled' : '']"
          @click="!c.imported && toggleImport(c.tableName)"
        >
          <el-checkbox v-if="!c.imported" :model-value="importSel.includes(c.tableName)" @click.stop @update:model-value="toggleImport(c.tableName)" />
          <span v-else class="checkbox" />
          <Icon name="database" :size="16" style="color: var(--ink-muted-48)" />
          <span class="code-chip">{{ c.tableName }}</span>
          <span style="flex: 1; font-size: 13px; color: var(--ink-72)">{{ c.tableComment || '未填写表说明' }}</span>
          <el-tag v-if="c.imported" type="info" effect="light" round>已导入</el-tag>
        </div>
        <div v-if="!importLoading && importCandidates.length === 0" class="tbl-empty">暂无可导入表</div>
        <div v-if="importLoading" class="tbl-empty">正在加载数据库表...</div>
      </div>
      <template #footer>
        <el-button plain @click="importOpen = false">取消</el-button>
        <el-button type="primary" :disabled="!importSel.length" @click="doImport">导入 {{ importSel.length ? `(${importSel.length})` : '' }}</el-button>
      </template>
    </el-dialog>

    <!-- 字段配置抽屉 -->
    <el-drawer :model-value="true" v-if="config" title="`字段配置 · ${config.tableComment}`" size="860px" destroy-on-close append-to-body @close="config = null">
      <p class="panel-sub">{{ config.tableName }}</p>
      <div class="form-grid" style="margin-bottom: 20px">
        <el-form-item label="实体类名称"><input class="input" :value="config.className" /></el-form-item>
        <el-form-item label="生成模块名"><input class="input" :value="config.module" /></el-form-item>
        <el-form-item label="业务名称"><input class="input" :value="config.tableComment.replace('表', '')" /></el-form-item>
        <el-form-item label="生成模板">
          <el-select model-value="crud" filterable @update:model-value="() => {}">
            <el-option label="单表 CRUD" value="crud" />
            <el-option label="树表" value="tree" />
            <el-option label="主子表" value="sub" />
          </el-select>
        </el-form-item>
      </div>
      <div class="approve-sec-label">字段配置</div>
      <div class="gen-field-table">
        <table class="tbl density-compact">
          <thead>
            <tr>
              <th>字段列名</th>
              <th>字段描述</th>
              <th style="width: 110px">Java 类型</th>
              <th style="width: 56px; text-align: center">插入</th>
              <th style="width: 56px; text-align: center">编辑</th>
              <th style="width: 56px; text-align: center">列表</th>
              <th style="width: 56px; text-align: center">查询</th>
              <th style="width: 120px">表单类型</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(f, i) in fields" :key="f.col">
              <td><span class="code-chip">{{ f.col }}</span></td>
              <td><input class="input" style="height: 30px; font-size: 12.5px" v-model="fields[i].comment" /></td>
              <td><span class="cell-muted" style="font-size: 12.5px">{{ f.javaType }}</span></td>
              <td style="text-align: center"><el-checkbox v-model="fields[i].insert" @click.stop /></td>
              <td style="text-align: center"><el-checkbox v-model="fields[i].edit" @click.stop /></td>
              <td style="text-align: center"><el-checkbox v-model="fields[i].list" @click.stop /></td>
              <td style="text-align: center"><el-checkbox v-model="fields[i].query" @click.stop /></td>
              <td>
                <el-select :model-value="f.formType" filterable style="width: 108px" @update:model-value="fields[i].formType = String($event)">
                  <el-option v-for="option in FORM_TYPES" :key="option" :label="option" :value="option" />
                </el-select>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <template #footer>
        <el-button plain @click="config = null">取消</el-button>
        <el-button type="primary" @click="saveConfig"><Icon name="check" :size="16" />保存配置</el-button>
      </template>
    </el-drawer>

    <!-- 代码预览弹窗 -->
    <el-dialog :model-value="true" v-if="preview" title="`代码预览 · ${preview.tableComment}`" width="880px" destroy-on-close append-to-body @close="preview = null">
      <div class="preview-wrap">
        <div class="preview-files">
          <div
            v-for="f in previewFiles" :key="f.name"
            :class="['preview-file', previewActive === f.name ? 'on' : '']"
            @click="previewActive = f.name"
          >
            <Icon name="file" :size="14" />
            <div style="flex: 1; min-width: 0">
              <div class="preview-file-name">{{ f.name.split('/').pop() }}</div>
              <div class="preview-file-label">{{ f.label ?? f.type }}</div>
            </div>
          </div>
        </div>
        <pre class="preview-code">{{ previewCode }}</pre>
      </div>
      <template #footer>
        <el-button plain @click="copyPreviewCode"><Icon name="copy" :size="16" />复制当前</el-button>
        <el-button type="primary" :disabled="downloading" @click="preview && generateCode(preview)"><Icon name="download" :size="16" />下载全部</el-button>
      </template>
    </el-dialog>

  </div>
</template>
