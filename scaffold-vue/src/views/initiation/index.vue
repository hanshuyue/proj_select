<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { initiationApi, collaborationApi, type InitiationProject } from '@/api'
import { downloadBlob } from '@/utils/download'
import { pushToast } from '@/composables/toast'

const router = useRouter()
const rows = ref<InitiationProject[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const loading = ref(false)
const exportingId = ref<number | null>(null)
const uploadingId = ref<number | null>(null)
const uploadInput = ref<HTMLInputElement | null>(null)
const uploadProject = ref<InitiationProject | null>(null)

async function load() {
  loading.value = true
  try {
    const d = await initiationApi.projects.list({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value })
    rows.value = d.rows
    total.value = d.total
  } finally {
    loading.value = false
  }
}

async function remove(row: InitiationProject) {
  await ElMessageBox.confirm(`确认删除“${row.projectName}”吗？`, '删除确认', { type: 'warning' })
  await initiationApi.projects.remove(row.id!)
  await load()
}

async function exportPpt(row: InitiationProject) {
  if (exportingId.value) return
  exportingId.value = row.id!
  try {
    const issues = await initiationApi.projects.validate(row.id!)
    if (issues.length) {
      await ElMessageBox.confirm(`以下内容尚未完善：${issues.join('、')}。仍要导出吗？`, '完整性提示', { type: 'warning' })
    }
    downloadBlob(await initiationApi.projects.exportPpt(row.id!), `项目立项-${row.projectName}.pptx`)
    pushToast('PPT已生成', 'ok')
  } finally {
    exportingId.value = null
  }
}
function chooseFinishedPpt(row: InitiationProject) { uploadProject.value = row; uploadInput.value?.click() }
async function uploadFinishedPpt(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0], row = uploadProject.value
  if (!file || !row?.id) return
  let changeSummary = ''
  try { changeSummary = (await ElMessageBox.prompt('请说明本轮提交的修改内容；首次提交可填写“首次提交”。', '本轮修改说明', { inputValidator: value => !!value?.trim() || '请填写本轮修改说明' })).value.trim() }
  catch { (event.target as HTMLInputElement).value = ''; return }
  uploadingId.value = row.id
  try { await collaborationApi.uploadFinishedPpt('INITIATION', row.id, row.projectName, file, changeSummary); pushToast('PPT已提交审核', 'ok'); await router.push('/documents') }
  catch (error) { pushToast(error instanceof Error ? error.message : '上传失败', 'danger') }
  finally { uploadingId.value = null; (event.target as HTMLInputElement).value = '' }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-head">
      <div class="page-main">
        <div>
          <h1 class="page-title">项目立项管理</h1>
          <div class="page-desc">维护项目立项数据，并按默认模板生成上会PPT。</div>
        </div>
        <div><el-button @click="router.push('/documents')">查看上传记录</el-button><el-button type="primary" @click="router.push('/initiation/new')">新建立项材料</el-button></div>
      </div>
    </div>
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="keyword" clearable placeholder="项目 / 商机 / 客户" @keyup.enter="load" />
        </el-form-item>
        <el-button type="primary" @click="pageNum = 1; load()">查询</el-button>
      </el-form>
    </el-card>
    <el-card shadow="never">
      <el-table v-loading="loading" :data="rows" border>
        <el-table-column prop="projectName" label="项目名称" min-width="180" />
        <el-table-column prop="opportunityNo" label="商机编号" min-width="130" />
        <el-table-column prop="customerName" label="客户" min-width="160" />
        <el-table-column label="签约金额（含税）" width="150">
          <template #default="{ row }">{{ row.contractAmountIncTax ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="净利润率" width="110">
          <template #default="{ row }">{{ row.overallProfitRate == null ? '-' : `${row.overallProfitRate}%` }}</template>
        </el-table-column>
        <el-table-column prop="threeLineLevel" label="三线判定" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag>{{ row.status === 'DRAFT' ? '草稿' : '已完成' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="160" />
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push(`/initiation/${row.id}/edit`)">编辑</el-button>
            <el-button link type="success" :loading="exportingId === row.id" @click="exportPpt(row)">生成PPT</el-button>
            <el-button link type="primary" :loading="uploadingId === row.id" @click="chooseFinishedPpt(row)">提交PPT审核</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, sizes, prev, pager, next"
          @change="load"
        />
      </div>
    </el-card>
    <input ref="uploadInput" type="file" accept=".pptx,application/vnd.openxmlformats-officedocument.presentationml.presentation" hidden @change="uploadFinishedPpt" />
  </div>
</template>
