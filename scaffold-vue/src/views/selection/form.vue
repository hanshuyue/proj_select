<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { selectionApi, collaborationApi, type SelectionBidder, type SelectionProject } from '@/api'
import { downloadBlob } from '@/utils/download'
import { pushToast } from '@/composables/toast'

const route = useRoute()
const router = useRouter()
const saving = ref(false)
const reportUploading = ref(false)
const reportFile = ref<File>()
const reportInput = ref<HTMLInputElement>()
const finishedPptInput = ref<HTMLInputElement>()
const uploadingFinishedPpt = ref(false)
const generating = ref(false)
const id = computed(() => route.params.id ? Number(route.params.id) : undefined)
const now = new Date()
const form = reactive<SelectionProject>({
  projectName:'', opportunityNo:'', departmentName:'政企客户部',
  reportYear:now.getFullYear(), reportMonth:now.getMonth()+1,
  serviceTaxRate:6, resaleTaxRate:6, feeTaxRate:6,
  publicityEnabled:true, publicityMethod:'网站', publicityWebsite:'https://xe.sd.chinamobile.com/pms-portal-react/#/console4',
  executionMethod:'与中选合作伙伴签署承诺书',
  status:'DRAFT', bidders:[],
})
const reportPeriod = computed({
  get: () => `${form.reportYear}-${String(form.reportMonth).padStart(2, '0')}`,
  set: (value: string) => {
    if (!value) return
    const [year, month] = value.split('-').map(Number)
    form.reportYear = year
    form.reportMonth = month
  },
})
function addBidder() { form.bidders.push({ bidderName:'', serviceTaxRate:6, resaleTaxRate:6, priceScore:0, businessScore:0 }) }
function removeBidder(index:number) { form.bidders.splice(index,1); recalculate() }
function amount(ex?:number, rate?:number) { return ex == null ? undefined : Number((ex*(1+(rate||0)/100)).toFixed(2)) }
function recalculate() {
  form.bidders.forEach(b => {
    b.serviceIncTax=amount(b.serviceExTax,b.serviceTaxRate)
    b.resaleIncTax=amount(b.resaleExTax,b.resaleTaxRate)
    b.totalScore=Number(((b.priceScore||0)+(b.businessScore||0)).toFixed(2))
  })
  ;[...form.bidders].sort((a,b)=>(b.totalScore||0)-(a.totalScore||0)).forEach((b,i)=>b.ranking=i+1)
}
async function load() {
  if(id.value) {
    const data = await selectionApi.projects.detail(id.value)
    Object.assign(form, data, { publicityEnabled: Boolean(data.publicityEnabled) })
  } else {
    form.publicityEnabled = true
    addBidder()
  }
}
async function save(status='DRAFT', silent=false): Promise<number | undefined> {
  if(!form.projectName.trim() || !form.opportunityNo.trim()) {
    pushToast('请填写项目名称和商机编号','danger')
    return undefined
  }
  recalculate(); form.status=status; saving.value=true
  try {
    let projectId = id.value
    if(projectId) await selectionApi.projects.update(projectId, form)
    else projectId = (await selectionApi.projects.create(form)).id
    if(reportFile.value) {
      const uploaded = await selectionApi.projects.uploadReviewReport(projectId, reportFile.value)
      form.reviewReportName = uploaded.filename
      reportFile.value = undefined
    }
    if (!silent) pushToast('甄选结果已保存','ok')
    if (status === 'COMPLETED') router.push('/selection')
    else if (!id.value && projectId) await router.replace(`/selection/${projectId}/edit`)
    return projectId
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '保存或上传评审报告失败','danger')
  } finally { saving.value=false }
}
async function saveAndExport() {
  const projectId = await save('DRAFT', true)
  if (!projectId) return
  generating.value = true
  try {
    downloadBlob(await selectionApi.projects.exportPpt(projectId), `甄选结果-${form.projectName}.pptx`)
    pushToast('PPT已生成并下载；修改完成后可在本页提交审核', 'ok')
  } finally { generating.value = false }
}
function chooseFinishedPpt() { finishedPptInput.value?.click() }
async function uploadFinishedPpt(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  let changeSummary = ''
  try {
    const result = await ElMessageBox.prompt('请说明本轮提交相较上一轮做了哪些修改；首次提交可填写“首次提交”。', '本轮修改说明', { inputPlaceholder: '例如：已按审核意见补充预算依据并调整第6页', inputValidator: value => !!value?.trim() || '请填写本轮修改说明', confirmButtonText: '继续提交' })
    changeSummary = result.value.trim()
  } catch { input.value = ''; return }
  const projectId = await save('DRAFT', true)
  if (!projectId) { input.value = ''; return }
  uploadingFinishedPpt.value = true
  try {
    await collaborationApi.uploadFinishedPpt('SELECTION', projectId, form.projectName, file, changeSummary)
    pushToast('PPT已提交审核，可到“PPT提交与审核”查看进度', 'ok')
    await router.push('/documents')
  } catch (error) {
    pushToast(error instanceof Error ? error.message : 'PPT提交失败', 'danger')
  } finally { uploadingFinishedPpt.value = false; input.value = '' }
}
async function selectReport(event: Event) {
  const files = (event.target as HTMLInputElement).files
  reportFile.value = files?.[0]
  if(!id.value || !reportFile.value) return
  reportUploading.value = true
  try {
    const uploaded = await selectionApi.projects.uploadReviewReport(id.value, reportFile.value)
    form.reviewReportName = uploaded.filename
    reportFile.value = undefined
    if(reportInput.value) reportInput.value.value = ''
    pushToast('评审报告已上传并保存','ok')
  } catch (error) {
    pushToast(error instanceof Error ? error.message : '评审报告上传失败','danger')
  } finally {
    reportUploading.value = false
  }
}
async function downloadReport() {
  if(!id.value) return
  const blob = await selectionApi.projects.downloadReviewReport(id.value)
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = form.reviewReportName || '评审报告'
  a.click()
  URL.revokeObjectURL(url)
}
onMounted(load)
</script>
<template>
  <div>
    <div class="page-head"><div class="page-main"><div><h1 class="page-title">{{ id ? '编辑' : '新建' }}甄选结果</h1><div class="page-desc">先保存数据并生成 PPT，修改确认后在这里上传成品并提交管理员审核。</div></div><div style="display:flex;gap:8px;flex-wrap:wrap"><el-button @click="router.push('/selection')">返回</el-button><el-button :loading="saving" @click="save('DRAFT')">暂存</el-button><el-button type="success" :loading="generating" @click="saveAndExport">保存并生成PPT</el-button><el-button type="warning" :loading="uploadingFinishedPpt" @click="chooseFinishedPpt">提交PPT审核</el-button><el-button @click="router.push('/documents')">查看审核进度</el-button><el-button type="primary" :loading="saving" @click="save('COMPLETED')">完成填报</el-button></div></div></div>
    <input ref="finishedPptInput" type="file" accept=".pptx,application/vnd.openxmlformats-officedocument.presentationml.presentation" hidden @change="uploadFinishedPpt" />
    <el-form label-position="top">
      <el-card shadow="never" style="margin-bottom:16px"><template #header><b>基础信息</b></template>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :lg="7"><el-form-item label="项目名称" required><el-input v-model="form.projectName" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="6"><el-form-item label="商机编号" required><el-input v-model="form.opportunityNo" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="6"><el-form-item label="汇报部门"><el-input v-model="form.departmentName" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="5"><el-form-item label="汇报年月"><el-date-picker v-model="reportPeriod" type="month" value-format="YYYY-MM" format="YYYY年MM月" placeholder="选择年月" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </el-card>
      <el-card shadow="never" style="margin-bottom:16px"><template #header><b>预算信息</b></template>
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :lg="6"><el-form-item label="服务上限（不含税）"><el-input-number v-model="form.serviceLimitExTax" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="6"><el-form-item label="服务上限（含税）"><el-input-number v-model="form.serviceLimitIncTax" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="4"><el-form-item label="服务增值税率 %"><el-select v-model="form.serviceTaxRate" style="width:100%"><el-option :value="6" label="6%" /><el-option :value="9" label="9%" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="4"><el-form-item label="代销预算（不含税）"><el-input-number v-model="form.resaleBudgetExTax" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="4"><el-form-item label="代销预算（含税）"><el-input-number v-model="form.resaleBudgetIncTax" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="4"><el-form-item label="代销增值税率 %"><el-select v-model="form.resaleTaxRate" style="width:100%"><el-option :value="6" label="6%" /><el-option :value="9" label="9%" /></el-select></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="5"><el-form-item label="手续费最低金额（不含税）"><el-input-number v-model="form.feeMinExTax" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="5"><el-form-item label="手续费最低金额（含税）"><el-input-number v-model="form.feeMinIncTax" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :xs="24" :sm="12" :lg="4"><el-form-item label="手续费增值税率 %"><el-select v-model="form.feeTaxRate" style="width:100%"><el-option :value="6" label="6%" /><el-option :value="9" label="9%" /></el-select></el-form-item></el-col>
        </el-row>
      </el-card>
      <el-card shadow="never" style="margin-bottom:16px"><template #header><div style="display:flex;justify-content:space-between"><b>投标人及评分</b><el-button type="primary" plain @click="addBidder">新增投标人</el-button></div></template>
        <el-table :data="form.bidders" border>
          <el-table-column label="投标人" min-width="150"><template #default="{row}"><el-input v-model="row.bidderName" /></template></el-table-column>
          <el-table-column label="服务不含税" width="130"><template #default="{row}"><el-input-number v-model="row.serviceExTax" :controls="false" :precision="2" @change="recalculate" style="width:100%" /></template></el-table-column>
          <el-table-column label="增值税率%" width="110"><template #default="{row}"><el-select v-model="row.serviceTaxRate" @change="recalculate"><el-option :value="6" label="6%" /><el-option :value="9" label="9%" /></el-select></template></el-table-column>
          <el-table-column label="服务含税" width="120"><template #default="{row}">{{ row.serviceIncTax?.toFixed(2) }}</template></el-table-column>
          <el-table-column label="代销不含税" width="130"><template #default="{row}"><el-input-number v-model="row.resaleExTax" :controls="false" :precision="2" @change="recalculate" style="width:100%" /></template></el-table-column>
          <el-table-column label="代销增值税率%" width="125"><template #default="{row}"><el-select v-model="row.resaleTaxRate" @change="recalculate"><el-option :value="6" label="6%" /><el-option :value="9" label="9%" /></el-select></template></el-table-column>
          <el-table-column label="价格得分" width="110"><template #default="{row}"><el-input-number v-model="row.priceScore" :controls="false" @change="recalculate" style="width:100%" /></template></el-table-column>
          <el-table-column label="商务技术得分" width="130"><template #default="{row}"><el-input-number v-model="row.businessScore" :controls="false" @change="recalculate" style="width:100%" /></template></el-table-column>
          <el-table-column label="综合得分" width="90"><template #default="{row}">{{ row.totalScore }}</template></el-table-column>
          <el-table-column label="排名" width="70"><template #default="{row}">{{ row.ranking }}</template></el-table-column>
          <el-table-column label="操作" width="70"><template #default="{$index}"><el-button link type="danger" @click="removeBidder($index)">删除</el-button></template></el-table-column>
        </el-table>
      </el-card>
      <el-card shadow="never"><template #header><b>公示与决策事项</b></template>
        <el-row :gutter="16">
          <el-col :span="6"><el-form-item label="中选单位"><el-input v-model="form.selectedCompany" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="候选单位"><el-input v-model="form.candidateCompany" /></el-form-item></el-col>
          <el-col :span="4"><el-form-item label="是否公示"><el-switch v-model="form.publicityEnabled" active-text="公示" inactive-text="不公示" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="公示方式"><el-input v-model="form.publicityMethod" :disabled="!form.publicityEnabled" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="公示网站"><el-input v-model="form.publicityWebsite" :disabled="!form.publicityEnabled" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="建议执行方式"><el-select v-model="form.executionMethod" style="width:100%">
            <el-option label="与中选合作伙伴签署承诺书" value="与中选合作伙伴签署承诺书" />
            <el-option label="与中选合作伙伴签署常规合同（其他成本类项目）" value="与中选合作伙伴签署常规合同（其他成本类项目）" />
            <el-option label="投资类项目由供管部门向合作伙伴进行相关采购（投资类项目）" value="投资类项目由供管部门向合作伙伴进行相关采购（投资类项目）" />
          </el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="拟合作公司"><el-input v-model="form.cooperationCompany" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="拓展项目"><el-input v-model="form.expansionProject" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="附件：评审报告">
            <input ref="reportInput" type="file" accept=".doc,.docx,.pdf,.xls,.xlsx,.ppt,.pptx,.txt,.rtf,.odt,.ods,.odp,.csv,.wps,.et,.dps" style="display:none" @change="selectReport" />
            <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
              <el-button :loading="reportUploading" @click="reportInput?.click()">{{ form.reviewReportName ? '重新上传' : '选择文档' }}</el-button>
              <span v-if="reportFile">待上传：{{ reportFile.name }}</span>
              <template v-else-if="form.reviewReportName">
                <span>已上传：{{ form.reviewReportName }}</span>
                <el-button link type="primary" @click="downloadReport">查看/下载</el-button>
              </template>
              <span v-else>尚未上传评审报告</span>
            </div>
            <div v-if="form.reviewReportName" style="width:100%;color:var(--el-text-color-secondary);font-size:12px;margin-top:6px">
              不选择新文件直接保存时，将继续保留当前评审报告。
            </div>
          </el-form-item></el-col>
        </el-row>
      </el-card>
    </el-form>
  </div>
</template>
