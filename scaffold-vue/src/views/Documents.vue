<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { collaborationApi, type UserDocument, ApiError } from '@/api'
import { currentUser } from '@/composables/auth'
import { pushToast } from '@/composables/toast'
import { downloadBlob } from '@/utils/download'

const rows = ref<UserDocument[]>([]), loading = ref(false), reviewing = ref(false), review = ref<UserDocument | null>(null), history = ref<UserDocument | null>(null)
const resubmitInput = ref<HTMLInputElement>(), resubmitRow = ref<UserDocument | null>(null), resubmitting = ref(false)
const filters = reactive({ keyword: '', type: '', status: '' })
const feedback = reactive({ status: 'APPROVED' as 'APPROVED' | 'REVISION_REQUIRED', feedback: '' })
const admin = computed(() => currentUser.value?.roles?.some(role => ['super_admin', 'biz_admin'].includes(role)) ?? false)
const statusLabels: Record<string, string> = { SUBMITTED: '待审核', APPROVED: '已通过', REVISION_REQUIRED: '退回修改' }
const typeLabels: Record<string, string> = { SELECTION: '甄选结果', INITIATION: '项目立项', OTHER: '其他PPT' }
const pendingCount = computed(() => rows.value.filter(row => row.status === 'SUBMITTED').length)
const approvedCount = computed(() => rows.value.filter(row => row.status === 'APPROVED').length)
const returnedCount = computed(() => rows.value.filter(row => row.status === 'REVISION_REQUIRED').length)
const visibleRows = computed(() => rows.value.filter((row) => {
  const keyword = filters.keyword.trim().toLowerCase()
  const haystack = [row.projectName, row.title, row.originalName, row.submitterName, row.deptName].filter(Boolean).join(' ').toLowerCase()
  return (!keyword || haystack.includes(keyword)) && (!filters.type || row.businessType === filters.type) && (!filters.status || row.status === filters.status)
}))
const historyRows = computed(() => history.value ? rows.value
  .filter(row => row.businessType === history.value!.businessType && row.projectId === history.value!.projectId && row.submitterId === history.value!.submitterId)
  .sort((a, b) => (a.revisionNo || 1) - (b.revisionNo || 1)) : [])

async function load() { loading.value = true; try { rows.value = await collaborationApi.documents() } catch (error) { pushToast(error instanceof ApiError ? error.message : 'PPT记录加载失败', 'danger') } finally { loading.value = false } }
async function download(row: UserDocument) { downloadBlob(await collaborationApi.downloadDocument(row.id), row.originalName) }
function openReview(row: UserDocument) { if (row.status !== 'SUBMITTED') return; review.value = row; feedback.status = 'APPROVED'; feedback.feedback = '' }
async function saveFeedback() {
  if (!review.value) return
  if (feedback.status === 'REVISION_REQUIRED' && !feedback.feedback.trim()) { pushToast('退回修改时必须填写原因', 'danger'); return }
  reviewing.value = true
  try { await collaborationApi.feedback(review.value.id, { status: feedback.status, feedback: feedback.feedback.trim() }); review.value = null; pushToast(feedback.status === 'APPROVED' ? 'PPT已审核通过' : 'PPT已退回修改', 'ok'); await load() }
  catch (error) { pushToast(error instanceof ApiError ? error.message : '审核提交失败', 'danger'); await load() }
  finally { reviewing.value = false }
}
function chooseResubmit(row: UserDocument) { resubmitRow.value = row; resubmitInput.value?.click() }
async function resubmit(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0], row = resubmitRow.value
  if (!file || !row?.projectId || !row.businessType || row.businessType === 'OTHER') { input.value = ''; return }
  let summary = ''
  try {
    const result = await ElMessageBox.prompt(row.feedback ? `上一轮审核意见：${row.feedback}\n\n请说明本轮如何修改。` : '请说明本轮修改内容。', '提交新一轮PPT', { inputPlaceholder: '填写本轮修改说明', inputValidator: value => !!value?.trim() || '请填写本轮修改说明', confirmButtonText: '提交审核' })
    summary = result.value.trim()
  } catch { input.value = ''; return }
  resubmitting.value = true
  try {
    await collaborationApi.uploadFinishedPpt(row.businessType, row.projectId, row.projectName || row.title, file, summary)
    pushToast('新一轮PPT已提交，当前状态为待审核', 'ok')
    history.value = null
    await load()
  } catch (error) { pushToast(error instanceof ApiError ? error.message : '新一轮提交失败', 'danger') }
  finally { resubmitting.value = false; input.value = ''; resubmitRow.value = null }
}
onMounted(load)
</script>

<template>
  <div class="page">
    <input ref="resubmitInput" type="file" accept=".pptx,application/vnd.openxmlformats-officedocument.presentationml.presentation" hidden @change="resubmit" />
    <div class="page-head"><div><h1>{{ admin ? 'PPT审核中心' : '我的PPT提交' }}</h1><p>{{ admin ? '查看全平台员工提交的成品PPT，下载核验后完成审批。' : '在甄选结果或项目立项中制作并上传PPT，在这里查看审核进度和意见。' }}</p></div><el-button plain @click="load">刷新</el-button></div>
    <div class="stat-grid" style="grid-template-columns:repeat(3,minmax(0,1fr));margin-bottom:16px"><el-card shadow="never"><div class="stat-label">待审核</div><div class="stat-value">{{ pendingCount }}</div></el-card><el-card shadow="never"><div class="stat-label">已通过</div><div class="stat-value">{{ approvedCount }}</div></el-card><el-card shadow="never"><div class="stat-label">退回修改</div><div class="stat-value">{{ returnedCount }}</div></el-card></div>
    <el-card shadow="never">
      <div class="panel-head" style="gap:12px;flex-wrap:wrap"><el-input v-model="filters.keyword" clearable placeholder="搜索项目、文件、上传人或地区" style="width:280px"/><el-select v-model="filters.type" style="width:140px"><el-option label="全部类型" value=""/><el-option label="甄选结果" value="SELECTION"/><el-option label="项目立项" value="INITIATION"/></el-select><el-select v-model="filters.status" style="width:140px"><el-option label="全部状态" value=""/><el-option label="待审核" value="SUBMITTED"/><el-option label="已通过" value="APPROVED"/><el-option label="退回修改" value="REVISION_REQUIRED"/></el-select><span style="margin-left:auto;color:var(--ink-muted-48);font-size:13px">共 {{ visibleRows.length }} 条</span></div>
      <el-table v-loading="loading" :data="visibleRows" border>
        <el-table-column label="业务类型" width="110"><template #default="s"><el-tag>{{ typeLabels[s.row.businessType || 'OTHER'] }}</el-tag></template></el-table-column>
        <el-table-column label="轮次" width="80"><template #default="s"><el-tag type="info">第 {{ s.row.revisionNo || 1 }} 轮</el-tag></template></el-table-column>
        <el-table-column prop="projectName" label="项目" min-width="180"><template #default="s">{{ s.row.projectName || s.row.title }}</template></el-table-column>
        <el-table-column prop="originalName" label="PPT文件" min-width="210"/>
        <el-table-column prop="description" label="本轮修改说明" min-width="220"><template #default="s">{{ s.row.description || (s.row.revisionNo > 1 ? '未填写' : '首次提交') }}</template></el-table-column>
        <el-table-column v-if="admin" prop="submitterName" label="上传人" width="110"/><el-table-column v-if="admin" prop="deptName" label="所属地区" width="130"/>
        <el-table-column label="状态" width="100"><template #default="s"><el-tag :type="s.row.status === 'APPROVED' ? 'success' : s.row.status === 'REVISION_REQUIRED' ? 'danger' : 'warning'">{{ statusLabels[s.row.status] || s.row.status }}</el-tag></template></el-table-column>
        <el-table-column prop="feedback" label="审核意见" min-width="170"><template #default="s">{{ s.row.feedback || '—' }}</template></el-table-column><el-table-column prop="reviewerName" label="审批人" width="110"><template #default="s">{{ s.row.reviewerName || '—' }}</template></el-table-column><el-table-column prop="submitTime" label="提交时间" width="170"/>
        <el-table-column label="操作" width="260" fixed="right"><template #default="s"><el-button link type="primary" @click="history = s.row">历次记录</el-button><el-button link type="primary" @click="download(s.row)">下载</el-button><el-button v-if="admin && s.row.status === 'SUBMITTED'" link type="primary" @click="openReview(s.row)">审核</el-button><span v-else-if="admin" style="color:var(--ink-muted-48);font-size:12px">已处理</span><el-button v-if="!admin && s.row.status !== 'SUBMITTED' && s.row.businessType !== 'OTHER'" link type="warning" :loading="resubmitting && resubmitRow?.id === s.row.id" @click="chooseResubmit(s.row)">提交新一轮</el-button></template></el-table-column>
      </el-table>
      <el-empty v-if="!loading && visibleRows.length === 0" :description="admin && filters.status === 'SUBMITTED' ? '当前没有待审核PPT' : '暂无PPT提交记录'"/>
    </el-card>
    <el-dialog :model-value="!!review" title="审核成品PPT" width="560px" @close="review = null"><el-alert v-if="review" :title="`${review.submitterName} · ${review.projectName || review.title} · ${review.originalName}`" type="info" :closable="false" style="margin-bottom:16px"/><el-form label-position="top"><el-form-item label="审核结果"><el-radio-group v-model="feedback.status"><el-radio value="APPROVED">通过</el-radio><el-radio value="REVISION_REQUIRED">退回修改</el-radio></el-radio-group></el-form-item><el-form-item :label="feedback.status === 'REVISION_REQUIRED' ? '退回原因（必填）' : '审核意见（选填）'"><el-input v-model="feedback.feedback" type="textarea" :rows="6" maxlength="2000" show-word-limit/></el-form-item></el-form><template #footer><el-button @click="review = null">取消</el-button><el-button type="primary" :loading="reviewing" @click="saveFeedback">确认提交审核</el-button></template></el-dialog>
    <el-dialog :model-value="!!history" :title="`${history?.projectName || history?.title || ''} · 提交与审核记录`" width="720px" @close="history = null">
      <el-timeline>
        <el-timeline-item v-for="item in historyRows" :key="item.id" :timestamp="item.submitTime" placement="top" :type="item.status === 'APPROVED' ? 'success' : item.status === 'REVISION_REQUIRED' ? 'danger' : 'warning'">
          <el-card shadow="never"><div style="display:flex;justify-content:space-between;gap:12px"><b>第 {{ item.revisionNo || 1 }} 轮</b><el-tag :type="item.status === 'APPROVED' ? 'success' : item.status === 'REVISION_REQUIRED' ? 'danger' : 'warning'">{{ statusLabels[item.status] || item.status }}</el-tag></div><p><b>提交文件：</b>{{ item.originalName }}</p><p><b>本轮变化：</b>{{ item.description || (item.revisionNo > 1 ? '未填写' : '首次提交') }}</p><p v-if="item.feedback"><b>审核意见：</b>{{ item.feedback }}</p><p v-if="item.reviewerName" style="color:var(--ink-muted-48)">审核员：{{ item.reviewerName }}<span v-if="item.reviewTime"> · {{ item.reviewTime }}</span></p><el-button link type="primary" @click="download(item)">下载本轮PPT</el-button><el-button v-if="!admin && item.status !== 'SUBMITTED' && item.businessType !== 'OTHER'" link type="warning" @click="chooseResubmit(item)">按此意见提交新一轮</el-button></el-card>
        </el-timeline-item>
      </el-timeline>
    </el-dialog>
  </div>
</template>
