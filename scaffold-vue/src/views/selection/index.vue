<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElLoading, ElMessageBox } from 'element-plus'
import { selectionApi, collaborationApi, type SelectionProject } from '@/api'
import { pushToast } from '@/composables/toast'
import { downloadBlob } from '@/utils/download'

const router = useRouter()
const rows = ref<SelectionProject[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)
const exportingId = ref<number | null>(null)
const uploadingId = ref<number | null>(null)
const uploadInput = ref<HTMLInputElement | null>(null)
const uploadProject = ref<SelectionProject | null>(null)

async function load() {
  loading.value = true
  try {
    const data = await selectionApi.projects.list({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
    rows.value = data.rows
    total.value = data.total
  } finally { loading.value = false }
}
async function remove(row: SelectionProject) {
  await ElMessageBox.confirm(`确认删除“${row.projectName}”吗？`, '删除确认', { type: 'warning' })
  await selectionApi.projects.remove(row.id!)
  pushToast('甄选结果已删除', 'ok')
  await load()
}
async function exportPpt(row: SelectionProject) {
  if (!row.id || exportingId.value !== null) return
  exportingId.value = row.id
  const exporting = ElLoading.service({
    lock: true,
    text: '正在生成 PPT，请稍候…',
    background: 'rgba(255, 255, 255, 0.72)',
  })
  try {
    const blob = await selectionApi.projects.exportPpt(row.id)
    downloadBlob(blob, `甄选结果_${row.projectName}.pptx`)
    pushToast('PPT 已生成并开始下载', 'ok')
  } catch (error) {
    pushToast(error instanceof Error ? error.message : 'PPT 导出失败，请稍后重试', 'danger')
  } finally {
    exporting.close()
    exportingId.value = null
  }
}
async function downloadReport(row: SelectionProject) {
  const blob = await selectionApi.projects.downloadReviewReport(row.id!)
  downloadBlob(blob, row.reviewReportName || '评审报告')
  pushToast('评审报告已开始下载', 'ok')
}
function chooseFinishedPpt(row: SelectionProject) { uploadProject.value = row; uploadInput.value?.click() }
async function uploadFinishedPpt(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0], row = uploadProject.value
  if (!file || !row?.id) return
  let changeSummary = ''
  try { changeSummary = (await ElMessageBox.prompt('请说明本轮提交的修改内容；首次提交可填写“首次提交”。', '本轮修改说明', { inputValidator: value => !!value?.trim() || '请填写本轮修改说明' })).value.trim() }
  catch { (event.target as HTMLInputElement).value = ''; return }
  uploadingId.value = row.id
  try { await collaborationApi.uploadFinishedPpt('SELECTION', row.id, row.projectName, file, changeSummary); pushToast('PPT已提交审核', 'ok'); await router.push('/documents') }
  catch (error) { pushToast(error instanceof Error ? error.message : '上传失败', 'danger') }
  finally { uploadingId.value = null; (event.target as HTMLInputElement).value = '' }
}
onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <el-breadcrumb separator="/" class="page-crumb"><el-breadcrumb-item>甄选结果</el-breadcrumb-item></el-breadcrumb>
      <div class="page-main">
        <div><h1 class="page-title">甄选结果管理</h1><div class="page-desc">维护甄选汇报数据，并基于默认模板导出 PowerPoint。</div></div>
        <div><el-button @click="router.push('/documents')">查看上传记录</el-button><el-button type="primary" @click="router.push('/selection/new')">新建甄选结果</el-button></div>
      </div>
    </div>
    <el-card class="filter-card" shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="关键词"><el-input v-model="keyword" clearable placeholder="项目名称 / 商机编号 / 中选单位" style="width:280px" @keyup.enter="load" /></el-form-item>
        <el-form-item><el-button type="primary" @click="pageNum=1;load()">查询</el-button><el-button @click="keyword='';pageNum=1;load()">重置</el-button></el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="projectName" label="项目名称" min-width="190" />
        <el-table-column prop="opportunityNo" label="商机编号" min-width="140" />
        <el-table-column prop="reportYear" label="汇报年月" width="110">
          <template #default="{row}">{{ row.reportYear }}-{{ String(row.reportMonth).padStart(2,'0') }}</template>
        </el-table-column>
        <el-table-column prop="selectedCompany" label="中选单位" min-width="170" />
        <el-table-column label="评审报告" min-width="170">
          <template #default="{row}">
            <el-button link type="primary" :disabled="!row.reviewReportName" @click="downloadReport(row)">
              查看/下载评审报告
            </el-button>
            <span v-if="!row.reviewReportName" style="color:var(--el-text-color-placeholder);margin-left:6px">未上传</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90"><template #default="{row}"><el-tag>{{ row.status === 'DRAFT' ? '草稿' : '已完成' }}</el-tag></template></el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="160" />
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{row}">
            <el-button link type="primary" @click="router.push(`/selection/${row.id}/edit`)">编辑</el-button>
            <el-button link type="success" :loading="exportingId === row.id" :disabled="exportingId !== null" @click="exportPpt(row)">
              {{ exportingId === row.id ? '正在导出…' : '导出生成 PPT' }}
            </el-button>
            <el-button link type="primary" :loading="uploadingId === row.id" @click="chooseFinishedPpt(row)">提交PPT审核</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager"><el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :total="total" layout="total, sizes, prev, pager, next" @change="load" /></div>
    </el-card>
    <input ref="uploadInput" type="file" accept=".pptx,application/vnd.openxmlformats-officedocument.presentationml.presentation" hidden @change="uploadFinishedPpt" />
  </div>
</template>
